package com.contactsnap.app.ui.screens

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

/**
 * Full-screen custom camera with a card-framing guide and a tactile shutter.
 * Captures to the app cache and hands back the file Uri.
 */
@Composable
fun CameraScreen(
    onCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Framing guide
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .aspectRatio(1.6f)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        )

        Text(
            text = "Align the card within the frame",
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
        }

        // Shutter
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShutterButton {
                val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    output,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                            ContextCompat.getMainExecutor(context).execute {
                                onCaptured(Uri.fromFile(photoFile))
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Swallow; the user can simply tap again.
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(Color.White, RoundedCornerShape(50))
        ) {
            Spacer(Modifier.size(0.dp))
        }
    }
}
