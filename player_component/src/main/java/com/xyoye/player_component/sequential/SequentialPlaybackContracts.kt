package com.xyoye.player_component.sequential

import com.xyoye.common_component.source.base.BaseVideoSource
import com.xyoye.data_component.enums.PlayerType

data class SequentialPlaybackSessionToken(
    val generation: Long,
    val sourceUniqueKey: String,
    val playerType: PlayerType,
)

@JvmInline
value class SequentialPlaybackQueueItemId(
    val value: Long,
)

data class SequentialPlaybackTransition(
    val previousSource: BaseVideoSource,
    val nextSource: BaseVideoSource,
    val sessionToken: SequentialPlaybackSessionToken,
    val queueItemId: SequentialPlaybackQueueItemId,
)

fun interface SequentialPlaybackStateApplier {
    fun apply(transition: SequentialPlaybackTransition)
}

fun interface SequentialPlaybackFallbackSwitcher {
    fun switchTo(index: Int)
}

interface SequentialPlaybackBackendAdapter {
    val supportsSeamlessPlayback: Boolean

    fun setEventListener(listener: EventListener?)

    fun beginSession(
        sessionToken: SequentialPlaybackSessionToken,
        currentSource: BaseVideoSource,
    )

    fun queueNextSource(
        sessionToken: SequentialPlaybackSessionToken,
        queueItemId: SequentialPlaybackQueueItemId,
        nextSource: BaseVideoSource,
    ): Result<Unit>

    fun invalidateSession(sessionToken: SequentialPlaybackSessionToken)

    interface EventListener {
        fun onTransitionToQueuedItem(
            sessionToken: SequentialPlaybackSessionToken,
            queueItemId: SequentialPlaybackQueueItemId,
        )
    }
}

class FallbackSequentialPlaybackAdapter : SequentialPlaybackBackendAdapter {
    override val supportsSeamlessPlayback: Boolean = false

    override fun setEventListener(listener: SequentialPlaybackBackendAdapter.EventListener?) = Unit

    override fun beginSession(
        sessionToken: SequentialPlaybackSessionToken,
        currentSource: BaseVideoSource,
    ) = Unit

    override fun queueNextSource(
        sessionToken: SequentialPlaybackSessionToken,
        queueItemId: SequentialPlaybackQueueItemId,
        nextSource: BaseVideoSource,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Fallback adapter does not support queueing"))

    override fun invalidateSession(sessionToken: SequentialPlaybackSessionToken) = Unit
}
