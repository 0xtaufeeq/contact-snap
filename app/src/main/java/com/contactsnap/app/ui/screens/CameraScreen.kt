package com.contactsnap.app.ui.screens

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Full-screen camera. Capture one or more shots of the same card (e.g. front +
 * back), then tap Done. Optional auto-capture fires when the scene holds steady.
 */
@Composable
fun CameraScreen(
    onCaptured: (List<Uri>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var shots by remember { mutableStateOf(listOf<Uri>()) }
    var autoOn by remember { mutableStateOf(false) }
    val capturing = remember { AtomicBoolean(false) }

    fun capture() {
        if (!capturing.compareAndSet(false, true)) return
        val photoFile = File(context.cacheDir, "scan_${shots.size}_${context.hashCode()}_${System.nanoTime()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            output, executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    ContextCompat.getMainExecutor(context).execute {
                        shots = shots + Uri.fromFile(photoFile)
                        capturing.set(false)
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    capturing.set(false)
                }
            }
        )
    }

    val analyzer = remember {
        StabilityAnalyzer {
            ContextCompat.getMainExecutor(context).execute {
                if (autoOn && !capturing.get()) capture()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis
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
            text = if (shots.isEmpty()) "Align the card within the frame"
            else "Got ${shots.size}. Capture another side, or tap Done.",
            color = Color.White.copy(alpha = 0.9f),
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

        // Auto-capture toggle
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(if (autoOn) Color(0xFFB6552F) else Color.Black.copy(alpha = 0.35f))
                .clickable { autoOn = !autoOn }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (autoOn) "Auto on" else "Auto", color = Color.White, fontWeight = FontWeight.Medium)
        }

        // Bottom controls
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 40.dp)) {
            if (shots.isNotEmpty()) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shots.takeLast(3).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.DarkGray)
                        )
                    }
                }
            }

            Box(Modifier.align(Alignment.Center)) { ShutterButton { capture() } }

            if (shots.isNotEmpty()) {
                Button(
                    onClick = { onCaptured(shots) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done (${shots.size})", fontWeight = FontWeight.SemiBold)
                }
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
            modifier = Modifier.size(60.dp).background(Color.White, RoundedCornerShape(50))
        ) {}
    }
}

/**
 * Fires [onStable] when consecutive frames have nearly-identical average
 * luminance (the camera is being held steady on a subject). Re-arms only after
 * motion is detected, so it captures once per "hold".
 */
private class StabilityAnalyzer(private val onStable: () -> Unit) : ImageAnalysis.Analyzer {
    private var prev: Double? = null
    private var stableFrames = 0
    private var armed = true

    override fun analyze(image: ImageProxy) {
        val luma = averageLuminance(image)
        val p = prev
        if (p != null) {
            val delta = abs(luma - p)
            if (delta > 6.0) armed = true
            if (delta < 1.5) stableFrames++ else stableFrames = 0
        }
        prev = luma
        if (armed && stableFrames >= 12) {
            armed = false
            stableFrames = 0
            onStable()
        }
        image.close()
    }

    private fun averageLuminance(image: ImageProxy): Double {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        // Sample every 16th byte for speed.
        var sum = 0L
        var count = 0
        var i = 0
        while (i < data.size) {
            sum += (data[i].toInt() and 0xFF)
            count++
            i += 16
        }
        return if (count == 0) 0.0 else sum.toDouble() / count
    }
}
