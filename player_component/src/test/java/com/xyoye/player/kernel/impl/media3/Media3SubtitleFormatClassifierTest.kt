package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.Format
import com.xyoye.common_component.media3.testing.Media3Dependent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Media3Dependent("Ensure Media3 subtitle format classification stays stable for libass bypass")
class Media3SubtitleFormatClassifierTest {
    @Test
    fun isSsaMime_matchesKnownSsaAliases() {
        assertTrue(Media3SubtitleFormatClassifier.isSsaMime("text/x-ssa"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaMime("TEXT/X-SSA"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaMime("application/x-ass"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaMime("TEXT/SSA"))
        assertFalse(Media3SubtitleFormatClassifier.isSsaMime("text/vtt"))
    }

    @Test
    fun isSsaCodecs_handlesCommaSeparatedCodecs() {
        assertTrue(Media3SubtitleFormatClassifier.isSsaCodecs("text/x-ssa"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaCodecs("TEXT/X-SSA"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaCodecs("mp4a.40.2, text/x-ssa"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaCodecs("mp4a.40.2, TEXT/X-SSA"))
        assertTrue(Media3SubtitleFormatClassifier.isSsaCodecs("ASS"))
        assertFalse(Media3SubtitleFormatClassifier.isSsaCodecs("wvtt"))
    }

    @Test
    fun isSsaFamilyTrack_detectsMedia3CueWrappedSsaCodecs() {
        val media3CueSsaFormat =
            Format
                .Builder()
                .setSampleMimeType("application/x-media3-cues")
                .setCodecs("text/x-ssa")
                .build()
        val media3CueVttFormat =
            Format
                .Builder()
                .setSampleMimeType("application/x-media3-cues")
                .setCodecs("text/vtt")
                .build()

        assertTrue(Media3SubtitleFormatClassifier.isSsaFamilyTrack(media3CueSsaFormat))
        assertFalse(Media3SubtitleFormatClassifier.isSsaFamilyTrack(media3CueVttFormat))
    }
}
