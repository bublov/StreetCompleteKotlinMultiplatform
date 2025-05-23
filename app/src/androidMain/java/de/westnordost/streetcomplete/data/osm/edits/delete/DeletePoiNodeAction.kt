package de.westnordost.streetcomplete.data.osm.edits.delete

import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.edits.ElementEditAction
import de.westnordost.streetcomplete.data.osm.edits.ElementIdProvider
import de.westnordost.streetcomplete.data.osm.edits.IsActionRevertable
import de.westnordost.streetcomplete.data.osm.edits.update_tags.isGeometrySubstantiallyDifferent
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataChanges
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataRepository
import de.westnordost.streetcomplete.data.osm.mapdata.Node
import de.westnordost.streetcomplete.data.osm.mapdata.key
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import kotlinx.serialization.Serializable

/** Action that deletes a POI node.
 *
 *  This is different from a generic element deletion seen in other editors in as ...
 *
 *  1. it only works on nodes. This is mainly to reduce complexity, because when deleting ways, it
 *     is expected to implicitly also delete all nodes of that way that are not part of any other
 *     way (or relation).
 *
 *  2. if that node is a vertex in a way or has a role in a relation, the node is not deleted but
 *     just "degraded" to be a vertex, i.e. the tags are cleared.
 */
@Serializable
data class DeletePoiNodeAction(
    val originalNode: Node
) : ElementEditAction, IsActionRevertable {

    override val elementKeys get() = listOf(originalNode.key)

    override fun idsUpdatesApplied(updatedIds: Map<ElementKey, Long>) = copy(
        originalNode = originalNode.copy(id = updatedIds[originalNode.key] ?: originalNode.id)
    )

    override fun createUpdates(
        mapDataRepository: MapDataRepository,
        idProvider: ElementIdProvider
    ): MapDataChanges {
        val currentNode = mapDataRepository.getNode(originalNode.id)
            ?: throw ConflictException("Element deleted")

        if (isGeometrySubstantiallyDifferent(originalNode, currentNode)) {
            throw ConflictException("Element geometry changed substantially")
        }

        val isInWay = mapDataRepository.getWaysForNode(currentNode.id).isNotEmpty()
        val isInRelation = mapDataRepository.getRelationsForNode(currentNode.id).isNotEmpty()

        // if it is a vertex in a way or has a role in a relation: clear the tags
        return if (isInWay || isInRelation) {
            // for relations specifically, we want to add a marker, because maybe it should be
            // deleted fully or at least deleted from the relation if it doesn't fulfill a role
            val emptyNode = currentNode.copy(
                tags =
                    if (!isInRelation) emptyMap()
                    else mapOf("fixme" to FIXME_DELETED_NODE_IN_RELATION_TEXT),
                timestampEdited = nowAsEpochMilliseconds()
            )
            MapDataChanges(modifications = listOf(emptyNode))
        }
        // otherwise, can safely delete the free-floating node
        else {
            MapDataChanges(deletions = listOf(currentNode))
        }
    }

    override fun createReverted(idProvider: ElementIdProvider) =
        RevertDeletePoiNodeAction(originalNode)
}

private const val FIXME_DELETED_NODE_IN_RELATION_TEXT =
    "object not found on a survey, check if it should be deleted or deleted from the relation"
