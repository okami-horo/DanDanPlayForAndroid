package com.xyoye.local_component.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeUtilsTest {

    @Test
    fun nullTypeReturnsFallback() {
        assertEquals("其 它", getAnimeType(null))
    }

    @Test
    fun knownTypeTvSeries() {
        assertEquals("TV动画", getAnimeType("tvseries"))
    }

    @Test
    fun knownTypeMovie() {
        assertEquals("剧场版", getAnimeType("movie"))
    }

    @Test
    fun knownTypeOva() {
        assertEquals("OVA", getAnimeType("ova"))
    }

    @Test
    fun knownTypeJpDrama() {
        assertEquals("日 剧", getAnimeType("jpdrama"))
    }

    @Test
    fun knownTypeJpMovie() {
        assertEquals("日本电影", getAnimeType("jpmovie"))
    }

    @Test
    fun knownTypeWeb() {
        assertEquals("网络放送", getAnimeType("web"))
    }

    @Test
    fun knownTypeUnknown() {
        assertEquals("未知分类", getAnimeType("unknown"))
    }

    @Test
    fun knownTypeMusicVideo() {
        assertEquals("MV", getAnimeType("musicvideo"))
    }

    @Test
    fun knownTypeOther() {
        assertEquals("其 它", getAnimeType("other"))
    }

    @Test
    fun unknownKeyReturnsFallback() {
        assertEquals("其 它", getAnimeType("nonexistent_type"))
    }

    @Test
    fun emptyStringReturnsFallback() {
        assertEquals("其 它", getAnimeType(""))
    }

    @Test
    fun caseSensitiveKeyDoesNotMatch() {
        // The map keys are lowercase; uppercase keys should fall through to fallback.
        assertEquals("其 它", getAnimeType("TVSERIES"))
        assertEquals("其 它", getAnimeType("Movie"))
    }

    @Test
    fun knownTypeTvSpecial() {
        assertEquals("TV特送", getAnimeType("tvspecial"))
    }
}
