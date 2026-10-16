package com.coinden.app.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.camera_close
import coinden.composeapp.generated.resources.camera_gallery
import coinden.composeapp.generated.resources.camera_shutter
import com.coinden.app.perception.AppPerception
import com.coinden.app.perception.DownloadResult
import com.coinden.app.perception.ProcessingQueue
import com.coinden.app.perception.VoiceModel
import com.coinden.app.perception.VoiceModelManager
import com.coinden.app.theme.CoinDenIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Dictado local, permisos y cámara (CameraX) en Android. */
class AndroidCapturePlatform(
    context: Context,
    private val perception: AppPerception,
) : CapturePlatform {
    override val dictation: DictationPort = OnDeviceDictation(context.applicationContext)

    override val perceptionReaders get() = perception.readers

    override val processingQueue: ProcessingQueue get() = perception.queue

    override val voiceModels =
        object : VoiceModelManager {
            override fun deviceProfile() = perception.modelStore.deviceProfile()

            override fun installed() = perception.modelStore.installed()

            override suspend fun download(
                model: VoiceModel,
                onProgress: (Long, Long) -> Unit,
            ): Boolean {
                val ok = perception.modelStore.download(model, onProgress) is DownloadResult.Ready
                // Los audios que esperaban un modelo se transcriben solos.
                if (ok) perception.queue.kick()
                return ok
            }

            override fun delete(model: VoiceModel) = perception.forgetVoiceModel(model)
        }

    @Composable
    override fun rememberMicrophoneRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val callback by rememberUpdatedState(onResult)
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback(it) }
        return remember(launcher) {
            { if (context.has(Manifest.permission.RECORD_AUDIO)) callback(true) else launcher.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }

    @Composable
    override fun Camera(
        onPhoto: (bytes: ByteArray, mimeType: String) -> Unit,
        onDenied: () -> Unit,
        onClose: () -> Unit,
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val photo by rememberUpdatedState(onPhoto)
        val scope = rememberCoroutineScope()
        var granted by remember { mutableStateOf(context.has(Manifest.permission.CAMERA)) }
        val permission =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                granted = it
                if (!it) onDenied()
            }
        LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
        // La galería usa el selector de fotos del sistema: no pide permisos de almacenamiento.
        val gallery =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    scope.launch {
                        withContext(Dispatchers.IO) { Jpeg.fromUri(context, uri) }?.let { photo(it, Jpeg.MIME) }
                    }
                }
            }
        val controller =
            remember {
                LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
            }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (granted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also {
                            it.controller = controller
                            controller.bindToLifecycle(lifecycleOwner)
                        }
                    },
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp)) {
                Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.camera_close), tint = Color.White)
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(CoinDenIcon.IMAGE.vector, stringResource(Res.string.camera_gallery), tint = Color.White)
                }
                FilledIconButton(
                    onClick = {
                        controller.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) { image.use { Jpeg.fromImage(it) } }
                                        photo(bytes, Jpeg.MIME)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) = Unit
                            },
                        )
                    },
                    enabled = granted,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.size(76.dp),
                ) {
                    Icon(CoinDenIcon.CAMERA.vector, stringResource(Res.string.camera_shutter), modifier = Modifier.size(32.dp))
                }
                Box(Modifier.size(56.dp))
            }
        }
    }
}

private fun Context.has(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Fotos como JPEG reducido a [MAX_SIDE] px por lado y bien orientado. */
internal object Jpeg {
    const val MIME = "image/jpeg"
    private const val MAX_SIDE = 2000
    private const val QUALITY = 85

    fun fromImage(image: ImageProxy): ByteArray = encode(image.toBitmap(), image.imageInfo.rotationDegrees)

    fun fromUri(
        context: Context,
        uri: Uri,
    ): ByteArray? =
        runCatching {
            val bitmap =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // ImageDecoder aplica la orientación EXIF.
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                        val scale = scaleFor(info.size.width, info.size.height)
                        decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                } ?: return null
            encode(bitmap, rotation = 0)
        }.getOrNull()

    private fun scaleFor(
        width: Int,
        height: Int,
    ): Float = minOf(1f, MAX_SIDE.toFloat() / max(width, height))

    private fun encode(
        source: Bitmap,
        rotation: Int,
    ): ByteArray {
        val scale = scaleFor(source.width, source.height)
        val matrix = Matrix().apply { postScale(scale, scale) }
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val bitmap =
            if (scale < 1f ||
                rotation != 0
            ) {
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            } else {
                source
            }
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)
            it.toByteArray()
        }
    }
}
