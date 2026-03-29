package com.xyoye.common_component.log.tcp

data class TcpLogServerState(
    val enabled: Boolean,
    val running: Boolean,
    val requestedPort: Int,
    val boundPort: Int,
    val clientCount: Int,
    val lastError: String? = null
)
