package de.westnordost.streetcomplete.quests.building_levels

import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.quests.TestMapDataWithGeometry
import de.westnordost.streetcomplete.quests.answerApplied
import de.westnordost.streetcomplete.testutils.way
import kotlin.test.Test
import kotlin.test.assertEquals

class AddBuildingLevelsTest {

    private val questType = AddBuildingLevels()

    @Test fun `apply building levels answer`() {
        assertEquals(
            setOf(StringMapEntryAdd("building:levels", "5")),
            questType.answerApplied(BuildingLevels(5, null))
        )
    }

    @Test fun `apply building levels and roof levels answer`() {
        assertEquals(
            setOf(
                StringMapEntryAdd("building:levels", "5"),
                StringMapEntryAdd("roof:levels", "0")
            ),
            questType.answerApplied(BuildingLevels(5, 0))
        )
    }

    @Test fun `not applicable to industrial buildings`() {
        // see for example
        // https://commons.wikimedia.org/wiki/File:Linden_power_plant_Elisenstrasse_Ihme_river_Linden-Nord_Hannover_Germany_02.jpg
        // https://en.wikipedia.org/wiki/File:Martinlaakson_voimalaitos.JPG
        // https://commons.wikimedia.org/wiki/File:Walters_Ranch_Hop_Kiln_in_2019,_4×5.jpg
        // https://en.wikipedia.org/wiki/File:Koebergnps.jpg
        // https://commons.wikimedia.org/wiki/File:Kernkraftwerk_Stendal_Panorama_2012.jpg
        val mapData = TestMapDataWithGeometry(listOf(
            way(1L, listOf(1, 2, 3, 4), mapOf(
                "building" to "industrial"
            ))
        ))
        assertEquals(0, questType.getApplicableElements(mapData).toList().size)
    }

    @Test fun `applicable to residential buildings`() {
        val mapData = TestMapDataWithGeometry(listOf(
            way(1L, listOf(1, 2, 3, 4), mapOf(
                "building" to "residential"
            ))
        ))
        assertEquals(1, questType.getApplicableElements(mapData).toList().size)
    }
}
