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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import eigelb.sharedui.generated.resources.Res
import eigelb.sharedui.generated.resources.Image
import eigelb.sharedui.generated.resources.Star
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Clock
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

/**
 * Composable that renders the Sparky star animation effect.
 *
 * The algorithm generates a base brightness pattern and star template, then precalculates
 * animation frames by stamping rotated star patterns onto offscreen [ImageBitmap]s using
 * [GraphicsCanvas] + [CanvasDrawScope] (same pattern as Grappa). The animation loop cycles
 * through precalculated frames at configurable intervals.
 *
 * By default, it loads the base brightness map from [Image] and the star pattern from [Star]
 * Compose resources. Pass custom bitmaps via [settings] to override.
 *
 * @param modifier Layout modifier
 * @param settings Configuration for the animation
 */
@Composable
fun SparkyDirect(
    modifier: Modifier = Modifier,
    settings: SparkySettings
) {
    val density = LocalDensity.current

    // Load default images from Compose resources if not provided in settings
    val defaultStarBitmap = imageResource(Res.drawable.Star)
    val defaultBaseImageBitmap = imageResource(Res.drawable.Image)

    // Resolve effective bitmaps: use settings if provided, otherwise fall back to resources
    val effectiveSettings = SparkySettings(
        frameCount = settings.frameCount,
        starSize = settings.starSize,
        scale0Multiplier = settings.scale0Multiplier,
        thresholdMultiplier = settings.thresholdMultiplier,
        frameDelayMs = settings.frameDelayMs,
        animateBrightness = settings.animateBrightness,
        starBitmap = settings.starBitmap ?: defaultStarBitmap,
        baseImageBitmap = settings.baseImageBitmap ?: defaultBaseImageBitmap
    )

    // Canvas size tracking — updated in the draw scope when size becomes available
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    // Generated images and precalculated frames
    var frameBitmaps by remember { mutableStateOf<List<ImageBitmap>?>(null) }

    // Animation state
    var currentFrame by remember { mutableIntStateOf(0) }

    // Worker coroutine: precalculate frames on a background dispatcher when canvas size is known
    LaunchedEffect(canvasWidth, canvasHeight) {
        if (canvasWidth > 0 && canvasHeight > 0) {
            frameBitmaps = withContext(Dispatchers.Default) {
                precalculateFrames(canvasWidth, canvasHeight, density, effectiveSettings)
            }
        }
    }

    // Animation loop - driven by display frame timing, matching Grappa pattern
    LaunchedEffect(frameBitmaps) {
        if (frameBitmaps == null) return@LaunchedEffect
        var startFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                // Track elapsed time since the very beginning of the animation
                if (startFrameNanos == 0L) {
                    startFrameNanos = frameTimeNanos
                } else {
                    val elapsedMs = (frameTimeNanos - startFrameNanos) / 1_000_000
                    val msWithinFrame = (elapsedMs % 1000).toInt()
                    val nominalFrameInterval = 1000 / frameBitmaps!!.size
                    val frameIndex = (msWithinFrame / nominalFrameInterval).coerceIn(0, frameBitmaps!!.size - 1)
                    currentFrame = frameIndex
                }
            }
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Track canvas size when it becomes available
            if (canvasWidth != size.width.toInt() || canvasHeight != size.height.toInt()) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()
                currentFrame = 0
            }

            // Show black background during precalculation
            if (frameBitmaps == null) {
                drawRect(color = Color.Black)
            } else {
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
    }
}

/**
 * Extracts the green channel from an [ImageBitmap] as a 2D int array.
 * Matches the Java applet's `ColorModel.getRGBdefault().getGreen(pixel)` behavior.
 */
private fun readGreenChannel(bitmap: ImageBitmap): Array<IntArray> {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.readPixels(pixels)
    val data = Array(w) { IntArray(h) }
    for (i in 0 until (w * h)) {
        val pixel = pixels[i]
        // Extract green channel from ARGB_8888: 0xAARRGGBB
        val green = (pixel shr 8) and 0xFF
        data[i % w][i / w] = green
    }
    return data
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

/**
 * Generates a procedural base image brightness pattern when no image is provided.
 */
private fun generateBaseImageData(width: Int, height: Int): Array<IntArray> {
    val data = Array(width) { IntArray(height) }
    for (y in 0 until height) {
        for (x in 0 until width) {
            data[x][y] = (35 + 220 * Random.nextDouble()).toInt()
        }
    }
    return data
}

/**
 * Generates a procedural star template when no star image is provided.
 * Creates a simple star-like pattern with a bright center fading to edges.
 */
private fun generateStarData(starW: Int): Array<IntArray> {
    val data = Array(starW) { IntArray(starW) }
    val center = starW / 2
    for (y in 0 until starW) {
        for (x in 0 until starW) {
            val dx = x - center
            val dy = y - center
            val dist = sqrt((dx * dx + dy * dy).toDouble())
            val maxDist = center * 0.8
            if (dist < maxDist) {
                // Star shape with points
                val angle = atan2(dy.toDouble(), dx.toDouble())
                val points = 5
                val starFactor = 0.5 + 0.5 * cos(points * angle)
                val brightness = ((1.0 - dist / maxDist) * starFactor * 255).toInt()
                data[x][y] = brightness.coerceIn(0, 255)
            } else {
                data[x][y] = 0
            }
        }
    }
    return data
}

/**
 * Precalculates all animation frames for the given canvas size.
 */
private fun precalculateFrames(
    width: Int,
    height: Int,
    density: Density,
    settings: SparkySettings
): List<ImageBitmap> {
    val animationFramesCount = settings.frameCount
    val starW = settings.starSize
    val scale0 = 0.007 * settings.scale0Multiplier
    val threshold = 0.0003 * settings.thresholdMultiplier

    // Read or generate the base brightness data
    val baseBrightnessData = if (settings.baseImageBitmap != null) {
        readGreenChannelScaled(settings.baseImageBitmap, width, height)
    } else {
        generateBaseImageData(width, height)
    }

    // Use the actual star bitmap size if provided, otherwise fall back to settings.
    val actualStarW = if (settings.starBitmap != null) {
        settings.starBitmap.width
    } else {
        starW
    }

    // Determine star active positions
    val nano1 = Clock.System.now()
    val starActivePositions = Array(width) { BooleanArray(height) }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (Random.nextDouble() < baseBrightnessData[x][y] / 255.0 * threshold) {
                starActivePositions[x][y] = true
                baseBrightnessData[x][y] = (35.0 + 220.0 * Random.nextDouble()).toInt()
            } else {
                starActivePositions[x][y] = false
            }
        }
    }
    val nano2 = Clock.System.now()
    println("Star positions ${nano2.minus(nano1).inWholeMilliseconds}")

    // Precalculate animation frames
    val frames = mutableListOf<ImageBitmap>()
    val reusableMatrix = Matrix()
    val bitmap = settings.starBitmap!!

    for (i in 0 until animationFramesCount) {
        // Create offscreen bitmap for this frame
        val nanoStart = Clock.System.now()
        val frameBitmap = ImageBitmap(width, height)
        val frameCanvas = GraphicsCanvas(frameBitmap)
        val frameDrawScope = CanvasDrawScope()
        var minScale = 10000000.0
        var maxScale = 0.0

        // Transfer buffer to bitmap via CanvasDrawScope
        frameDrawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = frameCanvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = Color.Black)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (starActivePositions[x][y]) {
                        val baseBrightness = baseBrightnessData[x][y]
                        val scale = scale0 * baseBrightness / 255.0
                        minScale = min(minScale, scale)
                        maxScale = max(maxScale, scale)
                        val scaleInv = 1.0 / scale
                        val starRadius = density.density * actualStarW * scale * 1.414 / 2.0

                        val rotationAngle = x + y + (((x + y) % 2) * 2 - 1) * i * PI / (2.0 * animationFramesCount)
                        val brightness = if (settings.animateBrightness) {
                            (sin(x + y + 2.0 * PI * i / animationFramesCount) * 0.5 + 0.5) * 0.7 + 0.3
                        } else {
                            1.0
                        }

                        // 3. Mutate the single matrix instead of allocating a new one
//                        reusableMatrix.reset() // Reset to Identity matrix
//
//                        // Transformations apply in reverse order (Translate -> Rotate -> Scale)
//                        reusableMatrix.translate(x.toFloat(), y.toFloat())
//                        reusableMatrix.rotateZ(rotationAngle.toFloat() * 180.0f / PI.toFloat())
//                        reusableMatrix.scale(scale.toFloat())
//                        reusableMatrix.translate(-bitmap.width / 2.0f, -bitmap.height /2.0f)

                        // Move pivot point to the center of the bitmap
//                        reusableMatrix.translate(-halfWidth, -halfHeight)

                        withTransform({
//                            transform(reusableMatrix)
                            translate(x.toFloat(), y.toFloat())
                            rotate(degrees = rotationAngle.toFloat() * 180.0f / PI.toFloat(),
                                pivot = Offset.Zero)
                            scale(scale.toFloat(), scale.toFloat(), pivot = Offset.Zero)
//                                //pivot = Offset(x.toFloat(), y.toFloat()))
                        }) {
//                            val rectSize = 40f// * scale.toFloat()
//                            drawRect(color = Color.Red.copy(alpha = 0.7f), topLeft = Offset(-rectSize/2f, -rectSize/2f), size = Size(rectSize, rectSize))
                            drawImage(
                                image = bitmap,
                                topLeft = Offset(-bitmap.width / 2.0f, -bitmap.height /2.0f),
                                alpha = brightness.toFloat(),
                                blendMode = BlendMode.Plus,
                            )
                        }
                    }
                }
            }
        }
        val nanoEnd = Clock.System.now()

        println("Frame $i, ${nanoEnd.minus(nanoStart).inWholeMilliseconds}")
        println("\tscale in [$minScale-$maxScale]")

        frames.add(frameBitmap)
    }

    return frames
}
