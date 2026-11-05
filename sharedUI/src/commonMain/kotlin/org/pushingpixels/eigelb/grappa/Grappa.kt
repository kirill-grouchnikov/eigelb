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
package org.pushingpixels.eigelb.grappa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

/**
 * Particle with position, velocity, friction, and attraction properties.
 * Port of the original [Partikel_c] Java class.
 */
internal class Particle {
    var x: Double = 0.0
    var y: Double = 0.0
    var vx: Double = 0.0
    var vy: Double = 0.0
    var friction: Double = 0.0
    var attraction: Double = 0.0
    var red: Int = 255
    var green: Int = 50
    var blue: Int = 10
}

/**
 * Settings for particle system randomization.
 * Port of the original [Settings_c] Java class.
 */
internal class Settings {
    var n: Int = 50
    private val nMax = 150
    private var frictionMin: Double = 0.0
    private var frictionMax: Double = 0.0
    private var frictionLinearity: Double = 0.0
    private var attractionMin: Double = 0.0
    private var attractionMax: Double = 0.0
    private var attractionLinearity: Double = 0.0

    fun randomize() {
        n = 1 + Random.nextInt(nMax)
        frictionMin = 1.0 - 0.1 * Random.nextDouble() * Random.nextDouble()
        frictionMax = frictionMin * (1.0 - 0.1 * Random.nextDouble() * Random.nextDouble())
        frictionLinearity = Random.nextDouble()
        attractionMin = 60.0 * Random.nextDouble() * Random.nextDouble()
        attractionMax = attractionMin * (1.0 + 2.0 * Random.nextDouble() * Random.nextDouble())
        attractionLinearity = Random.nextDouble()

        n = 101
        frictionMin = 0.995
        frictionMax = 0.977
        frictionLinearity = 0.954
        attractionMin = 2.74
        attractionMax = 3.07
        attractionLinearity = 0.93
    }

    fun updateParticles(particles: Array<Particle>, density: Float) {
        for (j in particles.indices) {
            if (j >= n) break
            particles[j].x = 0.0
            particles[j].y = 0.0
            particles[j].vx = 0.0
            particles[j].vy = 0.0
            val t = if (n > 1) j / (n - 1).toDouble() else 0.0
            particles[j].friction =
                frictionMin + frictionLinearity * t * (frictionMax - frictionMin) +
                        (1.0 - frictionLinearity) * Random.nextDouble() * (frictionMax - frictionMin)
            particles[j].attraction =
                attractionMin + attractionLinearity * t * (attractionMax - attractionMin) +
                        (1.0 - attractionLinearity) * Random.nextDouble() * (attractionMax - attractionMin)
            particles[j].attraction *= density
            particles[j].red = 255
            particles[j].green = 50
            particles[j].blue = 10
        }
    }
}

/**
 * Pixel buffer that wraps a Skia Bitmap for direct pixel manipulation.
 * Port of the original [SmiSchirm_c] Java class, which used 2D int arrays for R/G/B channels
 * scaled by 256. This implementation uses a Skia Bitmap with BGRA_8888 pixel format and
 * [Bitmap.allocPixels]/[Bitmap.installPixels] to get direct access to the pixel buffer.
 */
private class PixelBuffer(private val width: Int, private val height: Int) {
    private val pixelData = ByteArray(width * height * 4)
    private val skiaBitmap = Bitmap()

    init {
        val imageInfo = ImageInfo(
            colorInfo = ColorInfo(
                colorType = ColorType.BGRA_8888,
                alphaType = ColorAlphaType.OPAQUE,
                colorSpace = null
            ),
            width = width,
            height = height
        )
        skiaBitmap.allocPixels(imageInfo)
        skiaBitmap.installPixels(pixelData)
    }

    /**
     * Fills the entire buffer with a solid color.
     */
    fun fill(r: Int, g: Int, b: Int) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                setPixelRaw(x, y, r, g, b)
            }
        }
    }

    /**
     * Sets a pixel directly in the buffer without clamping.
     * The original Java code stored RGB values scaled by 256 in separate arrays.
     * Here we accumulate directly in the BGRA byte buffer.
     */
    private fun setPixelRaw(x: Int, y: Int, r: Int, g: Int, b: Int) {
        val idx = (y * width + x) * 4
        pixelData[idx] = b.toByte()          // B
        pixelData[idx + 1] = g.toByte()      // G
        pixelData[idx + 2] = r.toByte()      // R
        pixelData[idx + 3] = 255.toByte()    // A
    }

    /**
     * Draws a pixel with bilinear interpolation across the 4 neighboring pixels,
     * accumulating color with the given opacity.
     * Port of the original [SmiSchirm_c.drawPixel] method.
     */
    fun drawPixel(x: Double, y: Double, r: Int, g: Int, b: Int, opacity: Double) {
        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
            val x1 = x.toInt()
            val x2 = x1 + 1
            val y1 = y.toInt()
            val y2 = y1 + 1
            val x1f = x2 - x
            val x2f = 1.0 - x1f
            val y1f = y2 - y
            val y2f = 1.0 - y1f

            // Accumulate color at each of the 4 neighboring pixels
            accumulatePixel(x1, y1, r, g, b, x1f * y1f * opacity)
            accumulatePixel(x2, y1, r, g, b, x2f * y1f * opacity)
            accumulatePixel(x1, y2, r, g, b, x1f * y2f * opacity)
            accumulatePixel(x2, y2, r, g, b, x2f * y2f * opacity)
        }
    }

    private fun accumulatePixel(px: Int, py: Int, r: Int, g: Int, b: Int, weight: Double) {
        val idx = (py * width + px) * 4
        // Read current values
        val curB = pixelData[idx].toInt() and 0xFF
        val curG = pixelData[idx + 1].toInt() and 0xFF
        val curR = pixelData[idx + 2].toInt() and 0xFF

        // Accumulate: original Java code used 256*scale factor; here we use weight directly
        val newR = min((curR + weight * r).toInt(), 255)
        val newG = min((curG + weight * g).toInt(), 255)
        val newB = min((curB + weight * b).toInt(), 255)

        pixelData[idx] = newB.toByte()
        pixelData[idx + 1] = newG.toByte()
        pixelData[idx + 2] = newR.toByte()
    }

    /**
     * Returns the current pixel buffer as a Compose [ImageBitmap].
     */
    fun toImageBitmap(): ImageBitmap {
        return skiaBitmap.asComposeImageBitmap()
    }

    /**
     * Reinstalls the pixel data to ensure the Skia bitmap reflects current buffer state.
     */
    fun updatePixels() {
        val imageInfo = ImageInfo(
            colorInfo = ColorInfo(
                colorType = ColorType.BGRA_8888,
                alphaType = ColorAlphaType.OPAQUE,
                colorSpace = null
            ),
            width = width,
            height = height
        )
        skiaBitmap.installPixels(imageInfo, pixelData, width * 4)
    }
}

/**
 * Composable that renders the Grappa particle animation effect.
 *
 * Particles are attracted toward the mouse/pointer position. While the pointer is pressed,
 * particles leave colored trails on a pixel buffer using bilinear interpolation.
 */
@Composable
fun Grappa(
    modifier: Modifier = Modifier
) {
    val particles = remember { Array(200) { Particle() } }
    val settings = remember { Settings() }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isPointerPressed by remember { mutableStateOf(false) }
    var bitmapWidth by remember { mutableStateOf(0) }
    var bitmapHeight by remember { mutableStateOf(0) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Lazy initialization of PixelBuffer when size is known
    var pixelBuffer by remember { mutableStateOf<PixelBuffer?>(null) }

    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    // Button dimensions
    val buttonHeight = 30
    val buttonPadding = 12
    val buttonMarginRight = 12
    val buttonMarginBottom = 12
    val buttonTextPadding = 16 // Horizontal padding inside each button

    // Button positions (calculated when canvas size is known)
    var clearButtonRect by remember { mutableStateOf(Rect.Zero) }
    var randomizeButtonRect by remember { mutableStateOf(Rect.Zero) }
    var presetButtonRect by remember { mutableStateOf(Rect.Zero) }

    // Cache text measurements for buttons
    var cachedClearLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var cachedRandomizeLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var cachedPresetLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val buttonStyle = TextStyle(color = Color(0xFF80E0FF).copy(alpha = 0.7f), fontSize = 14.sp)
    cachedClearLayout = textMeasurer.measure(text = "Clear", style = buttonStyle)
    cachedRandomizeLayout = textMeasurer.measure(text = "Randomize", style = buttonStyle)
    cachedPresetLayout = textMeasurer.measure(text = "Preset", style = buttonStyle)

    // Size all buttons to the widest label
    val buttonWidth = max(
        max(cachedClearLayout?.size?.width ?: 0, cachedRandomizeLayout?.size?.width ?: 0),
        cachedPresetLayout?.size?.width ?: 0
    ) + buttonTextPadding

    // Initialize settings on first composition
    LaunchedEffect(Unit) {
        settings.randomize()
        settings.updateParticles(particles, density)
    }

    // Animation loop
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                // Calculate time delta from actual frame time passage
                val dtSec = if (lastFrameNanos > 0) {
                    (frameTimeNanos - lastFrameNanos) / 1_000_000_000.0
                } else 0.016
                lastFrameNanos = frameTimeNanos
                // Scale dt to match the original Java applet's pace: dt = 0.00003 per step
                // with Thread.sleep(20) between frames (~50 frames/sec). Normalize to real time.
                // Use a larger divisor to slow down particle movement towards the pointer.
                val dt = 0.00003 * dtSec / 0.05

                // Run physics steps
                if (bitmapWidth > 0 && bitmapHeight > 0) {
                    repeat(10) {
                        for (j in 0 until settings.n) {
                            val p = particles[j]
                            p.vx += (pointerPosition.x - p.x) * p.attraction
                            p.vy += (pointerPosition.y - p.y) * p.attraction
                            p.vx *= p.friction
                            p.vy *= p.friction
                            p.x += dt * p.vx
                            p.y += dt * p.vy
                            if (isPointerPressed) {
                                pixelBuffer?.drawPixel(
                                    p.x, p.y,
                                    p.red, p.green, p.blue,
                                    0.15
                                )
                            }
                        }
                    }
                    pixelBuffer?.updatePixels()
                    imageBitmap = pixelBuffer?.toImageBitmap()
                }
            }
        }
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        when (event.type) {
                            PointerEventType.Release -> {
                                // Check button clicks on release
                                if (randomizeButtonRect.contains(change.position)) {
                                    settings.randomize()
                                    settings.updateParticles(particles, density)
                                } else if (clearButtonRect.contains(change.position)) {
                                    pixelBuffer?.fill(0, 0, 0)
                                    pixelBuffer?.updatePixels()
                                    imageBitmap = pixelBuffer?.toImageBitmap()
                                }
                                isPointerPressed = false
                            }
                            PointerEventType.Press -> {
                                if (randomizeButtonRect.contains(change.position) || clearButtonRect.contains(change.position)) {
                                    continue;
                                }
                                pointerPosition = change.position
                                isPointerPressed = true
                            }
                            PointerEventType.Move -> {
                                pointerPosition = change.position
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    ) {
        // Initialize PixelBuffer when canvas size becomes available
        if (pixelBuffer == null || bitmapWidth != size.width.toInt() || bitmapHeight != size.height.toInt()) {
            bitmapWidth = size.width.toInt()
            bitmapHeight = size.height.toInt()
            if (bitmapWidth > 0 && bitmapHeight > 0) {
                pixelBuffer = PixelBuffer(bitmapWidth, bitmapHeight).also {
                    it.fill(0, 0, 0)
                    it.updatePixels()
                    imageBitmap = it.toImageBitmap()
                }
            }
        }

        // Calculate button positions in the bottom right corner
        val canvasWidth = size.width
        val canvasHeight = size.height
        val presetTop = canvasHeight - buttonMarginBottom - buttonHeight
        val randomizeTop = presetTop - buttonHeight - buttonPadding
        val clearTop = randomizeTop - buttonHeight - buttonPadding
        val buttonLeft = canvasWidth - buttonMarginRight - buttonWidth

        presetButtonRect = Rect(
            left = buttonLeft,
            top = presetTop,
            right = buttonLeft + buttonWidth,
            bottom = presetTop + buttonHeight
        )
        randomizeButtonRect = Rect(
            left = buttonLeft,
            top = randomizeTop,
            right = buttonLeft + buttonWidth,
            bottom = randomizeTop + buttonHeight
        )
        clearButtonRect = Rect(
            left = buttonLeft,
            top = clearTop,
            right = buttonLeft + buttonWidth,
            bottom = clearTop + buttonHeight
        )

        // Draw black background
        drawRect(Color.Black)

        // Draw the pixel buffer
        imageBitmap?.let { drawImage(it) }

        // Draw button outlines
        val buttonColor = Color(0xFF80E0FF).copy(alpha = 0.7f)
        drawRect(
            color = buttonColor,
            topLeft = Offset(clearButtonRect.left, clearButtonRect.top),
            size = androidx.compose.ui.geometry.Size(
                clearButtonRect.right - clearButtonRect.left,
                clearButtonRect.bottom - clearButtonRect.top
            ),
            style = Stroke()
        )
        drawRect(
            color = buttonColor,
            topLeft = Offset(randomizeButtonRect.left, randomizeButtonRect.top),
            size = androidx.compose.ui.geometry.Size(
                randomizeButtonRect.right - randomizeButtonRect.left,
                randomizeButtonRect.bottom - randomizeButtonRect.top
            ),
            style = Stroke()
        )
        drawRect(
            color = buttonColor,
            topLeft = Offset(presetButtonRect.left, presetButtonRect.top),
            size = androidx.compose.ui.geometry.Size(
                presetButtonRect.right - presetButtonRect.left,
                presetButtonRect.bottom - presetButtonRect.top
            ),
            style = Stroke()
        )

        // Draw button text, centered in each button
        cachedClearLayout?.let { layout ->
            val textX = buttonLeft + (buttonWidth - layout.size.width) / 2f
            val textY = clearTop + (buttonHeight - layout.size.height) / 2f
            drawText(layout, topLeft = Offset(textX, textY))
        }
        cachedRandomizeLayout?.let { layout ->
            val textX = buttonLeft + (buttonWidth - layout.size.width) / 2f
            val textY = randomizeTop + (buttonHeight - layout.size.height) / 2f
            drawText(layout, topLeft = Offset(textX, textY))
        }
        cachedPresetLayout?.let { layout ->
            val textX = buttonLeft + (buttonWidth - layout.size.width) / 2f
            val textY = presetTop + (buttonHeight - layout.size.height) / 2f
            drawText(layout, topLeft = Offset(textX, textY))
        }
    }
}
