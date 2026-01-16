package io.github.kdroidfilter.composemediaplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVKit.AVPictureInPictureController

actual class PipController {
    private val _isInPipMode = MutableStateFlow(false)
    actual val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()
    
    internal var pipController: AVPictureInPictureController? = null

    actual fun enterPip() {
        if (pipController?.isPictureInPicturePossible() == true) {
            pipController?.startPictureInPicture()
        }
    }

    actual fun isPipSupported(): Boolean {
        return AVPictureInPictureController.isPictureInPictureSupported()
    }
    
    internal fun setInPipMode(value: Boolean) {
        _isInPipMode.value = value
    }
}