package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Interactive circular profile picture adjuster supporting drag-to-pan,
 * pinch-to-zoom, zoom slider, and 90-degree rotation before saving.
 */
@Composable
fun AvatarAdjustDialog(
    sourceUri: Uri? = null,
    existingFilePath: String? = null,
    onSaveBitmap: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val loadedBitmap by produceState<Bitmap?>(initialValue = null, sourceUri, existingFilePath) {
        value = withContext(Dispatchers.IO) {
            when {
                sourceUri != null -> decodeOrientedBitmapFromUri(context, sourceUri, maxDim = 1200)
                !existingFilePath.isNullOrBlank() -> decodeOrientedBitmapFromFile(existingFilePath, maxDim = 1200)
                else -> null
            }
        }
    }

    var zoom by remember(sourceUri, existingFilePath) { mutableFloatStateOf(1f) }
    var rotationDeg by remember(sourceUri, existingFilePath) { mutableFloatStateOf(0f) }
    var panOffset by remember(sourceUri, existingFilePath) { mutableStateOf(Offset.Zero) }
    var stageSizePx by remember { mutableFloatStateOf(600f) }

    val drawMatrix = remember { Matrix() }
    val drawPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Profile Photo") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Drag to reposition, pinch or slide to zoom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmoledBlack)
                        .onSizeChanged { stageSizePx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(loadedBitmap) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val nextZoom = (zoom * gestureZoom).coerceIn(0.8f, 4.0f)
                                val limit = stageSizePx * 0.75f * nextZoom
                                zoom = nextZoom
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-limit, limit),
                                    y = (panOffset.y + pan.y).coerceIn(-limit, limit)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = loadedBitmap
                    if (bmp == null) {
                        CircularProgressIndicator(color = ElectricVolt)
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bw = bmp.width.toFloat().coerceAtLeast(1f)
                            val bh = bmp.height.toFloat().coerceAtLeast(1f)
                            val s = size.minDimension
                            val baseScale = max(s / bw, s / bh)
                            val totalScale = baseScale * zoom

                            drawMatrix.reset()
                            drawMatrix.postTranslate(-bw / 2f, -bh / 2f)
                            drawMatrix.postScale(totalScale, totalScale)
                            drawMatrix.postRotate(rotationDeg)
                            drawMatrix.postTranslate(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

                            drawContext.canvas.nativeCanvas.drawBitmap(bmp, drawMatrix, drawPaint)

                            // Circular crop overlay mask
                            val radius = s * 0.44f
                            val circlePath = Path().apply {
                                addOval(
                                    androidx.compose.ui.geometry.Rect(
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = radius
                                    )
                                )
                            }
                            clipPath(circlePath, clipOp = ClipOp.Difference) {
                                drawRect(Color.Black.copy(alpha = 0.62f))
                            }
                            drawCircle(
                                color = ElectricVolt,
                                radius = radius,
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zoom ${"%.1f".format(zoom)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { rotationDeg = (rotationDeg + 90f) % 360f },
                                modifier = Modifier.testTag("avatar_rotate_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 degrees", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rotate")
                            }
                            OutlinedButton(
                                onClick = {
                                    zoom = 1f
                                    rotationDeg = 0f
                                    panOffset = Offset.Zero
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 0.8f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = ElectricVolt, activeTrackColor = ElectricVolt)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = loadedBitmap != null,
                onClick = {
                    val src = loadedBitmap ?: return@Button
                    val cropped = renderCroppedSquareBitmap(
                        source = src,
                        stageSizePx = stageSizePx,
                        zoom = zoom,
                        rotationDeg = rotationDeg,
                        panOffset = panOffset,
                        outputPx = 512
                    )
                    onSaveBitmap(cropped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt),
                modifier = Modifier.testTag("avatar_save_crop_btn")
            ) {
                Text("Save Photo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun renderCroppedSquareBitmap(
    source: Bitmap,
    stageSizePx: Float,
    zoom: Float,
    rotationDeg: Float,
    panOffset: Offset,
    outputPx: Int
): Bitmap {
    val out = Bitmap.createBitmap(outputPx, outputPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawColor(android.graphics.Color.BLACK)

    val bw = source.width.toFloat().coerceAtLeast(1f)
    val bh = source.height.toFloat().coerceAtLeast(1f)
    // Circle aperture has radius = 0.44 * stageSize (diameter = 0.88 * stageSize)
    val apertureRatio = 0.88f
    val outRatio = outputPx.toFloat() / (stageSizePx * apertureRatio)
    val baseStageScale = max(stageSizePx / bw, stageSizePx / bh)
    val finalScale = baseStageScale * zoom * outRatio

    val matrix = Matrix().apply {
        postTranslate(-bw / 2f, -bh / 2f)
        postScale(finalScale, finalScale)
        postRotate(rotationDeg)
        postTranslate(
            (outputPx / 2f) + (panOffset.x * outRatio),
            (outputPx / 2f) + (panOffset.y * outRatio)
        )
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return out
}

private fun decodeOrientedBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val sample = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        applyExifOrientation(raw, orientation)
    }.getOrNull()
}

private fun decodeOrientedBitmapFromFile(path: String, maxDim: Int): Bitmap? {
    return runCatching {
        val file = File(path)
        if (!file.exists()) return null
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
        val sample = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        applyExifOrientation(raw, orientation)
    }.getOrNull()
}

private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w > maxDim * 2 || h > maxDim * 2) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val m = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}
