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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import eigelb.sharedui.generated.resources.Res
import eigelb.sharedui.generated.resources.Image
import eigelb.sharedui.generated.resources.Star
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
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
    val scale0Multiplier: Int = 70,
    val thresholdMultiplier: Int = 200,
    val frameDelayMs: Long = 30,
    val animateBrightness: Boolean = true,
    val starBitmap: ImageBitmap? = null,
    val baseImageBitmap: ImageBitmap? = null,
)

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
fun Sparky(
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

    // Animation loop - cycle through precalculated frames
    LaunchedEffect(frameBitmaps) {
        if (frameBitmaps == null) return@LaunchedEffect
        while (isActive) {
            kotlinx.coroutines.delay(settings.frameDelayMs)
            currentFrame = (currentFrame + 1) % frameBitmaps!!.size
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
            val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
            val maxDist = center * 0.8
            if (dist < maxDist) {
                // Star shape with points
                val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                val points = 5
                val starFactor = 0.5 + 0.5 * kotlin.math.cos(points * angle)
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
    density: androidx.compose.ui.unit.Density,
    settings: SparkySettings
): List<ImageBitmap> {
    val nBilder = settings.frameCount
    val starW = settings.starSize
    val scale0 = 0.007 * settings.scale0Multiplier
    val threshold = 0.0003 * settings.thresholdMultiplier

    // Read or generate the base brightness data
    val bildDaten = if (settings.baseImageBitmap != null) {
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

    // Read or generate the star template data
    val sternDaten = if (settings.starBitmap != null) {
        readGreenChannel(settings.starBitmap)
    } else {
        generateStarData(starW)
    }

    // Determine star active positions
    val sternAktiv = Array(width) { BooleanArray(height) }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (Random.nextDouble() < bildDaten[x][y] / 255.0 * threshold) {
                sternAktiv[x][y] = true
                bildDaten[x][y] = (35.0 + 220.0 * Random.nextDouble()).toInt()
            } else {
                sternAktiv[x][y] = false
            }
        }
    }

    val c1 = actualStarW / 2

    // Precalculate animation frames
    val frames = mutableListOf<ImageBitmap>()

    for (i in 0 until nBilder) {
        // Create offscreen bitmap for this frame
        val frameBitmap = ImageBitmap(width, height)
        val frameCanvas = GraphicsCanvas(frameBitmap)
        val frameDrawScope = CanvasDrawScope()

        // Pixel buffer for accumulation - matches Java's int[][] Buffer
        val buffer = Array(width) { IntArray(height) }

        // Star brightness accumulation in int array
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (sternAktiv[x][y]) {
                    val bildBrightness = bildDaten[x][y]
                    val scale = scale0 * bildBrightness / 255.0
                    val scaleInv = 1.0 / scale
                    val c2 = actualStarW * scale * 1.414 / 2.0

                    val w = x + y + (((x + y) % 2) * 2 - 1) * i * PI / (2.0 * nBilder)
                    val brightness = if (settings.animateBrightness) {
                        (sin(x + y + 2.0 * PI * i / nBilder) * 0.5 + 0.5) * 0.7 + 0.3
                    } else {
                        1.0
                    }
                    val cosi = cos(w)
                    val sinu = sin(w)

                    val xMin = (x - c2).toInt()
                    val xMax = (x + c2).toInt()
                    val yMin = (y - c2).toInt()
                    val yMax = (y + c2).toInt()

                    var ys2 = 0
                    for (y2 in yMin until yMax) {
                        var xs2 = 0
                        for (x2 in xMin until xMax) {
                            if (x2 >= 0 && y2 >= 0 && x2 < width && y2 < height) {
                                val xsh = xs2 - c2
                                val ysh = ys2 - c2
                                val xs = ((xsh * cosi - ysh * sinu) * scaleInv + c1).toInt()
                                val ys = ((ysh * cosi + xsh * sinu) * scaleInv + c1).toInt()
                                if (xs >= 0 && ys >= 0 && xs < actualStarW && ys < actualStarW) {
                                    val sternVal = sternDaten[xs][ys]
                                    if (sternVal > 5) {
                                        val idx = y2 * width + x2
                                        val ro = (buffer[x2][y2] and 0xFF0000) ushr 16
                                        val gr = (buffer[x2][y2] and 0x00FF00) ushr 8
                                        val bl = buffer[x2][y2] and 0x0000FF
                                        val help = (brightness * sternVal).toInt()
                                        val roNew = ro + (help shl 1) / 2
                                        val grNew = gr + (help shl 1) / 3
                                        val blNew = bl + (help ushr 1)

                                        // Nested clamping - only check green/blue if red is already clamped
                                        val roClamped = if (roNew > 255) 255 else roNew
                                        val grClamped = if (roNew > 255 && grNew > 255) 255 else grNew.coerceAtMost(255)
                                        val blClamped = if (roNew > 255 && grNew > 255 && blNew > 255) 255 else blNew.coerceAtMost(255)

                                        buffer[x2][y2] = (roClamped shl 16) or (grClamped shl 8) or blClamped
                                    }
                                }
                                xs2++
                            }
                        }
                        ys2++
                    }
                }
            }
        }

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
                    val colorInt = buffer[x][y]
                    if (colorInt > 0) {
                        val r = ((colorInt shr 16) and 0xFF) / 255f
                        val g = ((colorInt shr 8) and 0xFF) / 255f
                        val b = (colorInt and 0xFF) / 255f
                        drawCircle(
                            color = Color(red = r, green = g, blue = b, alpha = 1f),
                            radius = 0.5f,
                            center = Offset(x + 0.5f, y + 0.5f)
                        )
                    }
                }
            }
        }

        frames.add(frameBitmap)
    }

    return frames
}
