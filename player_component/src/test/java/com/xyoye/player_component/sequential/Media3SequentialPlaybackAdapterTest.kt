package com.xyoye.player_component.sequential

import com.xyoye.common_component.source.base.BaseVideoSource
import com.xyoye.data_component.enums.MediaType
import com.xyoye.data_component.enums.PlayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3SequentialPlaybackAdapterTest {
    @Test
    fun queueNextSourceTrimsQueueAndEnqueuesTaggedNextItem() {
        val queuePlayer = FakeQueuePlayer(currentIndex = 1, itemCount = 3)
        val adapter = Media3SequentialPlaybackAdapter.createForTests { queuePlayer }
        val token = SequentialPlaybackSessionToken(1, "episode-1", PlayerType.TYPE_EXO_PLAYER)
        val nextSource = FakeVideoSource("episode-2")

        adapter.setEventListener(RecordingEventListener())
        adapter.beginSession(token, FakeVideoSource("episode-1"))
        val result =
            adapter.queueNextSource(
                sessionToken = token,
                queueItemId = SequentialPlaybackQueueItemId(7),
                nextSource = nextSource,
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(IntRange(0, 0), IntRange(1, 1)), queuePlayer.removedRanges)
        assertEquals(listOf("sequential-1-7"), queuePlayer.enqueuedMediaIds)
    }

    @Test
    fun mediaItemTransitionPropagatesSessionTokenAndQueueItemId() {
        val queuePlayer = FakeQueuePlayer(currentIndex = 0, itemCount = 1)
        val listener = RecordingEventListener()
        val adapter = Media3SequentialPlaybackAdapter.createForTests { queuePlayer }
        val token = SequentialPlaybackSessionToken(4, "episode-4", PlayerType.TYPE_EXO_PLAYER)

        adapter.setEventListener(listener)
        adapter.beginSession(token, FakeVideoSource("episode-4"))
        adapter.queueNextSource(
            sessionToken = token,
            queueItemId = SequentialPlaybackQueueItemId(9),
            nextSource = FakeVideoSource("episode-5"),
        )

        queuePlayer.emitTransition("sequential-4-9")

        assertEquals(
            listOf(token to SequentialPlaybackQueueItemId(9)),
            listener.transitions,
        )
    }

    @Test
    fun invalidateSessionPreventsLateTransitionsFromLeakingState() {
        val queuePlayer = FakeQueuePlayer(currentIndex = 0, itemCount = 1)
        val listener = RecordingEventListener()
        val adapter = Media3SequentialPlaybackAdapter.createForTests { queuePlayer }
        val token = SequentialPlaybackSessionToken(8, "episode-8", PlayerType.TYPE_EXO_PLAYER)

        adapter.setEventListener(listener)
        adapter.beginSession(token, FakeVideoSource("episode-8"))
        adapter.queueNextSource(
            sessionToken = token,
            queueItemId = SequentialPlaybackQueueItemId(3),
            nextSource = FakeVideoSource("episode-9"),
        )
        adapter.invalidateSession(token)

        queuePlayer.emitTransition("sequential-8-3")

        assertTrue(listener.transitions.isEmpty())
    }

    private class RecordingEventListener : SequentialPlaybackBackendAdapter.EventListener {
        val transitions = mutableListOf<Pair<SequentialPlaybackSessionToken, SequentialPlaybackQueueItemId>>()

        override fun onTransitionToQueuedItem(
            sessionToken: SequentialPlaybackSessionToken,
            queueItemId: SequentialPlaybackQueueItemId,
        ) {
            transitions += sessionToken to queueItemId
        }
    }

    private class FakeQueuePlayer(
        private var currentIndex: Int,
        private var itemCount: Int,
    ) : Media3SequentialPlaybackAdapter.Media3QueuePlayer {
        override val identity: Any = Any()

        private var transitionListener: ((String?) -> Unit)? = null

        val removedRanges = mutableListOf<IntRange>()
        val enqueuedMediaIds = mutableListOf<String>()

        override fun setTransitionListener(listener: ((String?) -> Unit)?) {
            transitionListener = listener
        }

        override fun currentMediaItemIndex(): Int = currentIndex

        override fun mediaItemCount(): Int = itemCount

        override fun removeMediaItems(
            fromIndex: Int,
            toIndex: Int,
        ) {
            removedRanges += IntRange(fromIndex, toIndex - 1)
            itemCount -= (toIndex - fromIndex)
            if (fromIndex == 0) {
                currentIndex = (currentIndex - (toIndex - fromIndex)).coerceAtLeast(0)
            }
        }

        override fun enqueueNextSource(
            nextSource: BaseVideoSource,
            mediaId: String,
        ) {
            enqueuedMediaIds += mediaId
            itemCount += 1
        }

        fun emitTransition(mediaId: String) {
            transitionListener?.invoke(mediaId)
        }
    }

    private class FakeVideoSource(
        private val key: String,
    ) : BaseVideoSource(0, listOf(key)) {
        override fun indexTitle(index: Int): String = key

        override suspend fun indexSource(index: Int): BaseVideoSource? = null

        override fun getVideoUrl(): String = "file:///$key.mp4"

        override fun getVideoTitle(): String = key

        override fun getCurrentPosition(): Long = 0L

        override fun getMediaType(): MediaType = MediaType.LOCAL_STORAGE

        override fun getUniqueKey(): String = key

        override fun getStorageId(): Int = 1
    }
}
