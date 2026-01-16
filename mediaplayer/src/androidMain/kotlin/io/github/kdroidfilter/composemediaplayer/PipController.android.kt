package io.github.kdroidfilter.composemediaplayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import com.kdroid.androidcontextprovider.ContextProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

actual class PipController {
    actual val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()



    companion object {
        lateinit var activity: WeakReference<Activity>
        private val _isInPipMode = MutableStateFlow(false)
        
        fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
            _isInPipMode.value = isInPictureInPictureMode
        }
    }



    actual fun enterPip() {
        _isInPipMode.value = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentActivity = activity?.get()
            
            if (currentActivity != null && isPipSupported()) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                currentActivity.enterPictureInPictureMode(params)
            }
        }
    }

    actual fun isPipSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = activity?.get() ?: ContextProvider.getContext()
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }
        return false
    }
}