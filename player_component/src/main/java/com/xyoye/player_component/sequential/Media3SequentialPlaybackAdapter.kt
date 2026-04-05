package com.xyoye.player_component.sequential

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.xyoye.common_component.enums.SubtitleRendererBackend
import com.xyoye.common_component.source.base.BaseVideoSource
import com.xyoye.player.info.PlayerInitializer
import com.xyoye.player.kernel.impl.media3.Media3MediaSourceHelper

@UnstableApi
class Media3SequentialPlaybackAdapter private constructor(
    private val queuePlayerProvider: () -> Media3QueuePlayer?,
) : SequentialPlaybackBackendAdapter {
    override val supportsSeamlessPlayback: Boolean = true

    private var eventListener: SequentialPlaybackBackendAdapter.EventListener? = null
    private var activeSessionToken: SequentialPlaybackSessionToken? = null
    private var attachedQueuePlayer: Media3QueuePlayer? = null
    private var queuedItem: QueuedMediaItem? = null

    override fun setEventListener(listener: SequentialPlaybackBackendAdapter.EventListener?) {
        eventListener = listener
        if (listener == null) {
            detachPlayerListener()
        }
    }

    override fun beginSession(
        sessionToken: SequentialPlaybackSessionToken,
        currentSource: BaseVideoSource,
    ) {
        activeSessionToken = sessionToken
        val player = attachPlayerListener() ?: return
        trimQueueToCurrent(player)
        queuedItem = null
    }

    override fun queueNextSource(
        sessionToken: SequentialPlaybackSessionToken,
        queueItemId: SequentialPlaybackQueueItemId,
        nextSource: BaseVideoSource,
    ): Result<Unit> =
        runCatching {
            val player = attachPlayerListener() ?: error("Media3 player is unavailable")
            val mediaId = buildMediaId(sessionToken, queueItemId)
            trimQueueToCurrent(player)
            player.enqueueNextSource(nextSource, mediaId)
            queuedItem =
                QueuedMediaItem(
                    sessionToken = sessionToken,
                    queueItemId = queueItemId,
                    mediaId = mediaId,
                )
        }

    override fun invalidateSession(sessionToken: SequentialPlaybackSessionToken) {
        if (activeSessionToken != sessionToken) {
            return
        }
        attachedQueuePlayer?.let { trimQueueToCurrent(it) }
        queuedItem = null
        activeSessionToken = null
    }

    private fun attachPlayerListener(): Media3QueuePlayer? {
        val queuePlayer = queuePlayerProvider() ?: return null
        if (attachedQueuePlayer?.identity === queuePlayer.identity) {
            return attachedQueuePlayer
        }

        detachPlayerListener(clearSessionState = false)
        attachedQueuePlayer = queuePlayer
        queuePlayer.setTransitionListener { mediaId ->
            handleMediaItemTransition(mediaId)
        }
        return queuePlayer
    }

    private fun detachPlayerListener(clearSessionState: Boolean = true) {
        attachedQueuePlayer?.setTransitionListener(null)
        attachedQueuePlayer = null
        queuedItem = null
        if (clearSessionState) {
            activeSessionToken = null
        }
    }

    private fun handleMediaItemTransition(mediaId: String?) {
        val activeToken = activeSessionToken ?: return
        val queued = queuedItem

        if (queued != null && mediaId == queued.mediaId && activeToken == queued.sessionToken) {
            queuedItem = null
            eventListener?.onTransitionToQueuedItem(
                queued.sessionToken,
                queued.queueItemId,
            )
            return
        }

        val parsedQueueItemId = parseQueuedMediaId(mediaId, activeToken) ?: return
        queuedItem = null
        eventListener?.onTransitionToQueuedItem(
            activeToken,
            parsedQueueItemId,
        )
    }

    private fun trimQueueToCurrent(queuePlayer: Media3QueuePlayer) {
        if (queuePlayer.mediaItemCount() <= 0) {
            return
        }

        val currentIndex = queuePlayer.currentMediaItemIndex().coerceAtLeast(0)
        if (currentIndex > 0) {
            queuePlayer.removeMediaItems(0, currentIndex)
        }

        val mediaItemCount = queuePlayer.mediaItemCount()
        if (mediaItemCount > 1) {
            queuePlayer.removeMediaItems(1, mediaItemCount)
        }
    }

    private fun buildMediaId(
        sessionToken: SequentialPlaybackSessionToken,
        queueItemId: SequentialPlaybackQueueItemId,
    ): String = "sequential-${sessionToken.generation}-${queueItemId.value}"

    private fun parseQueuedMediaId(
        mediaId: String?,
        activeSessionToken: SequentialPlaybackSessionToken,
    ): SequentialPlaybackQueueItemId? {
        val parts = mediaId?.split("-") ?: return null
        if (parts.size != 3 || parts[0] != "sequential") {
            return null
        }

        val generation = parts[1].toLongOrNull() ?: return null
        if (generation != activeSessionToken.generation) {
            return null
        }

        val queueItemId = parts[2].toLongOrNull() ?: return null
        return SequentialPlaybackQueueItemId(queueItemId)
    }

    private data class QueuedMediaItem(
        val sessionToken: SequentialPlaybackSessionToken,
        val queueItemId: SequentialPlaybackQueueItemId,
        val mediaId: String,
    )

    internal companion object {
        fun create(playerProvider: () -> ExoPlayer?): Media3SequentialPlaybackAdapter =
            Media3SequentialPlaybackAdapter(
                queuePlayerProvider = {
                    playerProvider()?.let { exoPlayer ->
                        ExoPlayerQueuePlayer(exoPlayer)
                    }
                },
            )

        fun createForTests(queuePlayerProvider: () -> Media3QueuePlayer?): Media3SequentialPlaybackAdapter =
            Media3SequentialPlaybackAdapter(queuePlayerProvider)
    }

    internal interface Media3QueuePlayer {
        val identity: Any

        fun setTransitionListener(listener: ((String?) -> Unit)?)

        fun currentMediaItemIndex(): Int

        fun mediaItemCount(): Int

        fun removeMediaItems(
            fromIndex: Int,
            toIndex: Int,
        )

        fun enqueueNextSource(
            nextSource: BaseVideoSource,
            mediaId: String,
        )
    }

    private class ExoPlayerQueuePlayer(
        private val player: ExoPlayer,
    ) : Media3QueuePlayer {
        private var transitionListener: ((String?) -> Unit)? = null
        private val playerListener =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    transitionListener?.invoke(mediaItem?.mediaId)
                }
            }

        override val identity: Any
            get() = player

        override fun setTransitionListener(listener: ((String?) -> Unit)?) {
            transitionListener = listener
            player.removeListener(playerListener)
            if (listener != null) {
                player.addListener(playerListener)
            }
        }

        override fun currentMediaItemIndex(): Int = player.currentMediaItemIndex

        override fun mediaItemCount(): Int = player.mediaItemCount

        override fun removeMediaItems(
            fromIndex: Int,
            toIndex: Int,
        ) {
            player.removeMediaItems(fromIndex, toIndex)
        }

        override fun enqueueNextSource(
            nextSource: BaseVideoSource,
            mediaId: String,
        ) {
            val mediaSource =
                Media3MediaSourceHelper.getMediaSource(
                    uri = nextSource.getVideoUrl(),
                    headers = nextSource.getHttpHeader(),
                    parseSubtitlesDuringExtraction = PlayerInitializer.Subtitle.backend != SubtitleRendererBackend.LIBASS,
                    mediaId = mediaId,
                )
            player.addMediaSource(mediaSource)
        }
    }
}
