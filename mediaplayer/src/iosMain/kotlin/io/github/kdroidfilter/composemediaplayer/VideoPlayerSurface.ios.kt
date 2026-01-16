@file:OptIn(ExperimentalForeignApi::class)

package io.github.kdroidfilter.composemediaplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.composemediaplayer.subtitle.ComposeSubtitleLayer
import io.github.kdroidfilter.composemediaplayer.util.toCanvasModifier
import io.github.kdroidfilter.composemediaplayer.util.toTimeMs
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.AVFoundation.AVLayerVideoGravityResize
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayerLayer
import platform.AVKit.AVPictureInPictureController
import platform.CoreGraphics.CGRect
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayerSurface(
    playerState: VideoPlayerState,
    modifier: Modifier,
    contentScale: ContentScale,
    overlay: @Composable () -> Unit
) {
    // Set pauseOnDispose to false to prevent pausing during screen rotation
    VideoPlayerSurfaceImpl(
        playerState,
        modifier,
        contentScale,
        overlay,
        isInFullscreenView = false,
        pauseOnDispose = false
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun VideoPlayerSurfaceImpl(
    playerState: VideoPlayerState,
    modifier: Modifier,
    contentScale: ContentScale,
    overlay: @Composable () -> Unit,
    isInFullscreenView: Boolean = false,
    pauseOnDispose: Boolean = true
) {
    // Cleanup when deleting the view
    DisposableEffect(Unit) {
        onDispose {
            Logger.d { "[VideoPlayerSurface] Disposing" }
            // Only pause if pauseOnDispose is true (prevents pausing during rotation or fullscreen transitions)
            if (pauseOnDispose) {
                Logger.d { "[VideoPlayerSurface] Pausing on dispose" }
                playerState.pause()
            } else {
                Logger.d { "[VideoPlayerSurface] Not pausing on dispose (rotation or fullscreen transition)" }
            }
        }
    }

    val currentPlayer = (playerState as? DefaultVideoPlayerState)?.player

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (playerState.hasMedia) {
            UIKitView(
                modifier = contentScale.toCanvasModifier(
                    aspectRatio = playerState.aspectRatio,
                    width = playerState.metadata.width,
                    height = playerState.metadata.height
                ),
                factory = {
                    val playerLayer = AVPlayerLayer()
                    playerLayer.player = currentPlayer

                    (playerState as? DefaultVideoPlayerState)?.let { state ->
                        state.playerLayer = playerLayer
                        state.pipController?.pipController = AVPictureInPictureController(playerLayer = playerLayer)
                    }

                    PlayerContainerView(playerLayer).apply {
                        backgroundColor = UIColor.blackColor
                    }
                },
                update = { view ->

                    view.playerLayer.player = currentPlayer
                    view.hidden = !playerState.hasMedia

                    val videoGravity = when (contentScale) {
                        ContentScale.Crop,
                        ContentScale.FillHeight -> AVLayerVideoGravityResizeAspectFill

                        ContentScale.FillWidth -> AVLayerVideoGravityResizeAspectFill
                        ContentScale.FillBounds -> AVLayerVideoGravityResize
                        ContentScale.Fit,
                        ContentScale.Inside -> AVLayerVideoGravityResizeAspect

                        else -> AVLayerVideoGravityResizeAspect
                    }
                    view.playerLayer.videoGravity = videoGravity

                    view.updateLayerFrame()

                    Logger.d { "View configured with contentScale: $contentScale, videoGravity: $videoGravity" }

                },
                onRelease = { view ->
                    if (view is PlayerContainerView) {
                        view.playerLayer.player = null
                    }
                }
            )

            // Add Compose-based subtitle layer
            if (playerState.subtitlesEnabled && playerState.currentSubtitleTrack != null) {
                // Calculate current time in milliseconds
                val currentTimeMs = (playerState.sliderPos / 1000f *
                        playerState.durationText.toTimeMs()).toLong()

                // Calculate duration in milliseconds
                val durationMs = playerState.durationText.toTimeMs()

                ComposeSubtitleLayer(
                    currentTimeMs = currentTimeMs,
                    durationMs = durationMs,
                    isPlaying = playerState.isPlaying,
                    subtitleTrack = playerState.currentSubtitleTrack,
                    subtitlesEnabled = playerState.subtitlesEnabled,
                    textStyle = playerState.subtitleTextStyle,
                    backgroundColor = playerState.subtitleBackgroundColor
                )
            }
        }

        // Render the overlay content on top of the video with fillMaxSize modifier
        // to ensure it takes the full height of the parent Box
        Box(modifier = Modifier.fillMaxSize()) {
            overlay()
        }
    }

    // Handle fullscreen mode
    if (playerState.isFullscreen && !isInFullscreenView) {
        openFullscreenView(playerState) { state, mod, inFullscreen ->
            // Set pauseOnDispose to false to prevent pausing during fullscreen transitions
            VideoPlayerSurfaceImpl(state, mod, contentScale, overlay, inFullscreen, pauseOnDispose = false)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PlayerContainerView(val playerLayer: AVPlayerLayer) : UIView(frame = cValue<CGRect>()) {
    init {
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        updateLayerFrame()
    }

    fun updateLayerFrame() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}
