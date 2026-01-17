package io.github.kdroidfilter.composemediaplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class PipController actual constructor() {
    actual val isInPipMode: StateFlow<Boolean>
        get() = MutableStateFlow(false).asStateFlow()

    actual fun enterPip() {
    }

    actual fun isPipSupported(): Boolean = false

}
