package io.github.kdroidfilter.composemediaplayer

import kotlinx.coroutines.flow.StateFlow

expect class PipController() {
    val isInPipMode: StateFlow<Boolean>

    fun enterPip()
    fun isPipSupported(): Boolean
}