/*
 * Copyright 2020-2026 EigelB, Kirill Grouchnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pushingpixels.eigelb.sparky

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eigelb.sharedui.generated.resources.Image
import eigelb.sharedui.generated.resources.Res
import eigelb.sharedui.generated.resources.Star
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.imageResource
import org.pushingpixels.eigelb.sparky.SparkyState.Animating
import org.pushingpixels.eigelb.sparky.SparkyState.Precalculating
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

/**
 * Settings for the Sparky star animation effect.
 */
public data class SparkySettings(
    val frameCount: Int = 15,
    val starSize: Int = 100,
    val scaleMultiplier: Int = 70,
    val thresholdMultiplier: Int = 100,
    val frameDelayMs: Long = 30,
    val animateBrightness: Boolean = true,
    val starBitmap: ImageBitmap,
    val baseImageBitmap: ImageBitmap,
)

/**
 * Lifecycle state of the Sparky composable.
 */
public enum class SparkyState {
    Precalculating,
    Animating
}

/**
 * Composable that renders the Sparky star animation effect.
 *
 * The algorithm generates a base brightness pattern and star template, then precalculates
 * animation frames by stamping rotated star patterns onto offscreen [ImageBitmap]s using
 * [GraphicsCanvas] + [CanvasDrawScope] (same pattern as Grappa). The animation loop cycles
 * through precalculated frames at configurable intervals.
 *
 * @param modifier Layout modifier
 * @param settings Configuration for the animation
 */
@Composable
fun Sparky(
    modifier: Modifier = Modifier,
    settings: SparkySettings
) {
    val density = LocalDensity.current

    // Load default images from Compose resources if not provided in settings
    val defaultStarBitmap = imageResource(Res.drawable.Star)
    val defaultBaseImageBitmap = imageResource(Res.drawable.Image)

    // Canvas size tracking — updated in the draw scope when size becomes available
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    // Generated images and precalculated frames
    var frameBitmaps by remember { mutableStateOf<List<ImageBitmap>?>(null) }

    // Animation state
    var currentFrame by remember { mutableIntStateOf(0) }

    // Lifecycle state: starts Precalculating, moves to Animating when frames are ready
    var sparkyState by remember { mutableStateOf(Precalculating) }

    // Draw-phase invalidation token (bypasses layout phase entirely, matching Grappa pattern)
    var drawInvalidationToken by remember { mutableIntStateOf(0) }

    // Progress tracking: number of frames completed out of total
    var precalcProgress by remember { mutableStateOf(0) }

    // Worker coroutine: precalculate frames on the main thread, one frame at a time.
    // On Web, Dispatchers.Default is single-threaded and would block the UI, so we run on the
    // main thread and yield after each frame to keep the progress indicator updating.
    // The frame loop is directly in LaunchedEffect (not in a suspend function) so that the
    // drawInvalidationToken increment can be processed by Compose between frames.
    // The LaunchedEffect keys include bitmap dimensions so it waits for images to be decoded
    // on Web where imageResource() may return an undecoded bitmap initially.
    LaunchedEffect(canvasWidth, canvasHeight, settings.starBitmap.width, settings.starBitmap.height, settings.baseImageBitmap.width, settings.baseImageBitmap.height) {
        if (canvasWidth > 0 && canvasHeight > 0) {
            precalcProgress = 0

            // Setup: read base brightness data and determine star active positions
            val animationFramesCount = settings.frameCount
            val scale0 = 0.007 * settings.scaleMultiplier
            val threshold = 0.0003 * settings.thresholdMultiplier
            val baseBrightnessData = readGreenChannelScaled(settings.baseImageBitmap!!, canvasWidth, canvasHeight)
            val actualStarW = settings.starBitmap!!.width
            val starActivePositions = Array(canvasWidth) { BooleanArray(canvasHeight) }
            for (y in 0 until canvasHeight) {
                for (x in 0 until canvasWidth) {
                    if (Random.nextDouble() < baseBrightnessData[x][y] / 255.0 * threshold) {
                        starActivePositions[x][y] = true
                        baseBrightnessData[x][y] = (35.0 + 220.0 * Random.nextDouble()).toInt()
                    } else {
                        starActivePositions[x][y] = false
                    }
                }
            }

            // Compute frames one at a time, yielding between each so the UI can update
            val frames = mutableListOf<ImageBitmap>()
            val bitmap = settings.starBitmap
            for (i in 0 until animationFramesCount) {
                val frameBitmap = ImageBitmap(canvasWidth, canvasHeight)
                val frameCanvas = GraphicsCanvas(frameBitmap)
                val frameDrawScope = CanvasDrawScope()

                frameDrawScope.draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = frameCanvas,
                    size = Size(canvasWidth.toFloat(), canvasHeight.toFloat())
                ) {
                    drawRect(color = Color.Black)
                    for (y in 0 until canvasHeight) {
                        for (x in 0 until canvasWidth) {
                            if (starActivePositions[x][y]) {
                                val baseBrightness = baseBrightnessData[x][y]
                                val scale = scale0 * baseBrightness / 255.0
                                val starRadius = density.density * actualStarW * scale * 1.414 / 2.0
                                val rotationAngle = x + y + (((x + y) % 2) * 2 - 1) * i * PI / (2.0 * animationFramesCount)
                                val brightness = if (settings.animateBrightness) {
                                    (sin(x + y + 2.0 * PI * i / animationFramesCount) * 0.5 + 0.5) * 0.7 + 0.3
                                } else {
                                    1.0
                                }
                                withTransform({
                                    translate(x.toFloat(), y.toFloat())
                                    rotate(degrees = rotationAngle.toFloat() * 180.0f / PI.toFloat(), pivot = Offset.Zero)
                                    scale(scale.toFloat(), scale.toFloat(), pivot = Offset.Zero)
                                }) {
                                    drawImage(
                                        image = bitmap,
                                        topLeft = Offset(-bitmap.width / 2.0f, -bitmap.height / 2.0f),
                                        alpha = brightness.toFloat(),
                                        blendMode = BlendMode.Plus,
                                    )
                                }
                            }
                        }
                    }
                }
                frames.add(frameBitmap)

                // Update progress and fire draw invalidation BEFORE yielding
                precalcProgress = i + 1
                drawInvalidationToken++
                // Yield control back to the event loop so Compose can process the state changes
                // and re-render the progress indicator. delay(1) ensures the next event loop tick
                // processes the drawInvalidationToken increment on Web's single-threaded runtime.
                delay(1)
            }

            frameBitmaps = frames
            sparkyState = Animating
            drawInvalidationToken++
        }
    }

    // Animation loop - driven by display frame timing, matching Grappa pattern
    // Uses LaunchedEffect(Unit) like Grappa to avoid Web target issues with nullable collection keys
    LaunchedEffect(Unit) {
        var startFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                // Wait until frames are precalculated
                val frames = frameBitmaps ?: return@withFrameNanos
                // Track elapsed time since the very beginning of the animation
                if (startFrameNanos == 0L) {
                    startFrameNanos = frameTimeNanos
                } else {
                    val elapsedMs = (frameTimeNanos - startFrameNanos) / 1_000_000
                    val msWithinFrame = (elapsedMs % 1000).toInt()
                    val nominalFrameInterval = 1000 / frames.size
                    val frameIndex = (msWithinFrame / nominalFrameInterval).coerceIn(0, frames.size - 1)
                    currentFrame = frameIndex
                }
                // Fire draw invalidation so the Canvas draw scope re-reads currentFrame on Web
                drawInvalidationToken++
            }
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw-phase isolation: reference the token to register a dependency strictly bound to the Draw phase
            val readTokenForInvalidation = drawInvalidationToken

            // Track canvas size when it becomes available
            if (canvasWidth != size.width.toInt() || canvasHeight != size.height.toInt()) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()
                currentFrame = 0
                sparkyState = Precalculating
            }

            // Show black background during precalculation
            drawRect(color = Color.Black)

            when (sparkyState) {
                Precalculating -> {
                    // Draw progress indicator during precalculation
                    val totalFrames = settings.frameCount
                    val progress = precalcProgress.coerceIn(0, totalFrames)
                    val fillFraction = if (totalFrames > 0) progress / totalFrames.toFloat() else 0f

                    // Progress bar: 16dp tall, 80% of canvas width, vertically centered
                    val barHeightPx = with(density) { 10.dp.toPx() }
                    val barWidth = (size.width * 0.8f)
                    val barX = (size.width - barWidth) / 2f
                    val barY = (size.height - barHeightPx) / 2f

                    // Outer white rectangular outline
                    drawRect(
                        color = Color.White,
                        size = Size(barWidth, barHeightPx),
                        topLeft = Offset(barX, barY),
                        style = Stroke(width = 2f)
                    )
                    // Inner white fill based on progress percentage
                    if (fillFraction > 0f) {
                        drawRect(
                            color = Color.White,
                            size = Size(barWidth * fillFraction, barHeightPx),
                            topLeft = Offset(barX, barY)
                        )
                    }
                }

                Animating -> {
                    // Draw the current frame scaled to fit the canvas
                    val frame = frameBitmaps!![currentFrame]
                    val scale = minOf(size.width / frame.width, size.height / frame.height)
                    val drawW = frame.width * scale
                    val drawH = frame.height * scale
                    val x = (size.width - drawW) / 2f
                    val y = (size.height - drawH) / 2f
                    drawImage(frame, topLeft = Offset(x, y))
                }
            }
//            // Display current state in the top left corner
//            val stateLayout: TextLayoutResult = textMeasurer.measure(
//                text = sparkyState.name + ":" + canvasWidth + "x" + canvasHeight + ":" + currentFrame +
//                        ":" + frameBitmaps?.size + " - " + frameBitmaps?.get(currentFrame)?.width +
//                        "x" + frameBitmaps?.get(currentFrame)?.height +
//                        " " + settings.starBitmap.width + "x" + settings.starBitmap.height +
//                        " " + settings.baseImageBitmap.width + "x" + settings.baseImageBitmap.height
//                ,
//                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
//            )
//            drawText(textLayoutResult = stateLayout, topLeft = Offset(8f, 8f))
        }
   }
}

/**
 * Scales the green channel data from a bitmap to the target dimensions using nearest-neighbor sampling.
 */
private fun readGreenChannelScaled(bitmap: ImageBitmap, targetW: Int, targetH: Int): Array<IntArray> {
    val srcW = bitmap.width
    val srcH = bitmap.height
    val pixels = IntArray(srcW * srcH)
    bitmap.readPixels(pixels)
    val data = Array(targetW) { IntArray(targetH) }
    for (y in 0 until targetH) {
        for (x in 0 until targetW) {
            // Map target coordinates back to source coordinates
            val srcX = (x * srcW / targetW).coerceIn(0, srcW - 1)
            val srcY = (y * srcH / targetH).coerceIn(0, srcH - 1)
            val pixel = pixels[srcY * srcW + srcX]
            val green = (pixel shr 8) and 0xFF
            data[x][y] = green
        }
    }
    return data
}

