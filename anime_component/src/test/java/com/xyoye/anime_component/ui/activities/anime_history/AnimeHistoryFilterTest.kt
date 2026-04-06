package com.xyoye.anime_component.ui.activities.anime_history

import com.xyoye.data_component.data.AnimeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeHistoryFilterTest {

    private fun anime(title: String?) = AnimeData(animeId = 0, animeTitle = title)

    @Test
    fun emptySearchWordReturnsAllHistories() {
        val histories = listOf(anime("Attack on Titan"), anime("Naruto"), anime(null))
        val result = filterAnimeHistory(histories, "")
        assertEquals(histories, result)
    }

    @Test
    fun noMatchingTitleReturnsEmptyList() {
        val histories = listOf(anime("Attack on Titan"), anime("Naruto"))
        val result = filterAnimeHistory(histories, "One Piece")
        assertTrue(result.isEmpty())
    }

    @Test
    fun exactTitleMatchReturnsItem() {
        val target = anime("Death Note")
        val histories = listOf(anime("Naruto"), target, anime("Bleach"))
        val result = filterAnimeHistory(histories, "Death Note")
        assertEquals(listOf(target), result)
    }

    @Test
    fun partialTitleMatchReturnsItem() {
        val match = anime("Fullmetal Alchemist: Brotherhood")
        val histories = listOf(anime("Naruto"), match)
        val result = filterAnimeHistory(histories, "Fullmetal")
        assertEquals(listOf(match), result)
    }

    @Test
    fun searchIsCaseInsensitive() {
        val match = anime("Sword Art Online")
        val histories = listOf(match, anime("Naruto"))
        assertEquals(listOf(match), filterAnimeHistory(histories, "sword art online"))
        assertEquals(listOf(match), filterAnimeHistory(histories, "SWORD ART ONLINE"))
        assertEquals(listOf(match), filterAnimeHistory(histories, "Sword Art Online"))
    }

    @Test
    fun itemWithNullTitleIsExcluded() {
        val histories = listOf(anime("One Punch Man"), anime(null))
        val result = filterAnimeHistory(histories, "One")
        assertEquals(1, result.size)
        assertEquals("One Punch Man", result[0].animeTitle)
    }

    @Test
    fun emptyHistoriesListReturnsEmpty() {
        val result = filterAnimeHistory(emptyList(), "Naruto")
        assertTrue(result.isEmpty())
    }

    @Test
    fun multipleMatchesReturnedInOrder() {
        val a = anime("Dragon Ball Z")
        val b = anime("Dragon Ball Super")
        val c = anime("Naruto")
        val histories = listOf(a, c, b)
        val result = filterAnimeHistory(histories, "Dragon Ball")
        assertEquals(listOf(a, b), result)
    }
}
