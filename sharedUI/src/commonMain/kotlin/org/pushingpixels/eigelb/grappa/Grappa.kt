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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
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
    var colorRed: Int = 255
    var colorGreen: Int = 50
    var colorBlue: Int = 10

    fun randomize() {
        n = 1 + Random.nextInt(nMax)
        frictionMin = 1.0 - 0.1 * Random.nextDouble() * Random.nextDouble()
        frictionMax = frictionMin * (1.0 - 0.1 * Random.nextDouble() * Random.nextDouble())
        frictionLinearity = Random.nextDouble()
        attractionMin = 60.0 * Random.nextDouble() * Random.nextDouble()
        attractionMax = attractionMin * (1.0 + 2.0 * Random.nextDouble() * Random.nextDouble())
        attractionLinearity = Random.nextDouble()
        // Randomize color channels, ensuring at least one is 255
        val channel = Random.nextInt(3)
        colorRed = if (channel == 0) 255 else Random.nextInt(256)
        colorGreen = if (channel == 1) 255 else Random.nextInt(256)
        colorBlue = if (channel == 2) 255 else Random.nextInt(256)
    }

    fun preset() {
        n = 101
        frictionMin = 0.995
        frictionMax = 0.977
        frictionLinearity = 0.954
        attractionMin = 2.74
        attractionMax = 3.07
        attractionLinearity = 0.93
        colorRed = 255
        colorGreen = 50
        colorBlue = 10
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
            particles[j].red = colorRed
            particles[j].green = colorGreen
            particles[j].blue = colorBlue
        }
    }
}

/**
 * Trail buffer using the DynamicPixelPainter pattern: an offscreen [ImageBitmap] with a
 * [CanvasDrawScope] that draws circles via [GraphicsCanvas.drawCircle] instead of per-pixel
 * ByteArray manipulation. This is significantly faster on WASM targets than PixelMapper.
 */
private class TrailBuffer(
    private val width: Int,
    private val height: Int,
    private val density: Density
) {
    val bitmap = ImageBitmap(width, height)
    val canvas = GraphicsCanvas(bitmap)
    val drawScope = CanvasDrawScope()

    init {
        // Initialize the canvas background to black
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = Color.Black)
        }
    }

    /**
     * Clears the buffer to black.
     */
    fun clear() {
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = Color.Black)
        }
    }

    /**
     * Draws a small circle at the given position with the specified RGB color and opacity.
     * Uses [BlendMode.Add] to accumulate colors additively, matching the original Java behavior
     * where overlapping particles brighten toward white.
     */
    fun drawPixel(x: Double, y: Double, r: Int, g: Int, b: Int, opacity: Double) {
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawCircle(
                color = Color(
                    red = r / 255f,
                    green = g / 255f,
                    blue = b / 255f,
                    alpha = opacity.toFloat()
                ),
                radius = 1f,
                center = Offset(x.toFloat(), y.toFloat()),
                blendMode = BlendMode.Plus
            )
        }
    }
}

/**
 * Composable that renders the Grappa particle animation effect.
 *
 * Particles are attracted toward the mouse/pointer position. While the pointer is pressed,
 * particles leave colored trails on a pixel buffer using bilinear interpolation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Grappa(
    modifier: Modifier = Modifier
) {
    val particles = remember { Array(200) { Particle() } }
    val settings = remember { Settings() }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isPointerPressed by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }
    var fps by remember { mutableStateOf(0.0) }

    val density = LocalDensity.current.density
    val densityObj = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Offscreen trail buffer — lazily initialized when canvas size is known
    var trailBuffer by remember { mutableStateOf<TrailBuffer?>(null) }
    var bitmapWidth by remember { mutableStateOf(0) }
    var bitmapHeight by remember { mutableStateOf(0) }

    // Draw-phase invalidation token (bypasses layout phase entirely)
    var drawInvalidationToken by remember { mutableIntStateOf(0) }

    // Button dimensions (scaled by density for proper sizing across screen densities)
    val buttonHeight = 20 * density
    val buttonPadding = 6 * density
    val buttonMarginRight = 6 * density
    val buttonMarginBottom = 6 * density
    val buttonTextPadding = 12 * density // Horizontal padding inside each button

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
        settings.preset()
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
                // Track framerate
                if (dtSec > 0) {
                    fps = 1.0 / dtSec
                }

                // Scale dt to match the original Java applet's pace: dt = 0.00003 per step
                // with Thread.sleep(20) between frames (~50 frames/sec). Normalize to real time.
                // Use a larger divisor to slow down particle movement towards the pointer.
                val dt = 0.00003 * dtSec / 0.05

                // Run physics steps
                if (trailBuffer != null) {
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
                                trailBuffer?.drawPixel(
                                    p.x, p.y,
                                    p.red, p.green, p.blue,
                                    0.15
                                )
                            }
                        }
                    }
                    // Fire the lean draw invalidation signal to force Compose to re-read the draw scope block
                    drawInvalidationToken++
                }

                // Trigger recomposition
                frameCount++
            }
        }
    }

    // Cache FPS text measurement to avoid expensive layout work every frame
    var cachedFpsLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastFpsInt by remember { mutableStateOf(-1) }

    val currentFpsInt = fps.toInt()
    if (currentFpsInt != lastFpsInt) {
        cachedFpsLayout = textMeasurer.measure(
            text = "FPS: $currentFpsInt",
            style = TextStyle(color = Color(0xFF80E0FF).copy(alpha = 0.7f), fontSize = 12.sp)
        )
        lastFpsInt = currentFpsInt
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                when (event.type) {
                                    PointerEventType.Release -> {
                                        // Check button clicks on release
                                        if (presetButtonRect.contains(change.position)) {
                                            settings.preset()
                                            settings.updateParticles(particles, density)
                                            trailBuffer?.clear()
                                            drawInvalidationToken++
                                        } else if (randomizeButtonRect.contains(change.position)) {
                                            settings.randomize()
                                            settings.updateParticles(particles, density)
                                        } else if (clearButtonRect.contains(change.position)) {
                                            trailBuffer?.clear()
                                            drawInvalidationToken++
                                        }
                                        isPointerPressed = false
                                    }
                                    PointerEventType.Press -> {
                                        if (presetButtonRect.contains(change.position) || randomizeButtonRect.contains(change.position) || clearButtonRect.contains(change.position)) {
                                            continue
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
            // Initialize TrailBuffer when canvas size becomes available
            if (trailBuffer == null || bitmapWidth != size.width.toInt() || bitmapHeight != size.height.toInt()) {
                bitmapWidth = size.width.toInt()
                bitmapHeight = size.height.toInt()
                if (bitmapWidth > 0 && bitmapHeight > 0) {
                    trailBuffer = TrailBuffer(bitmapWidth, bitmapHeight, densityObj)
                }
            }

            // Calculate button positions in the bottom right corner
            // Order from bottom to top: Clear, Randomize, Preset
            val canvasWidth = size.width
            val canvasHeight = size.height
            val clearTop = canvasHeight - buttonMarginBottom - buttonHeight
            val randomizeTop = clearTop - buttonHeight - buttonPadding
            val presetTop = randomizeTop - buttonHeight - buttonPadding
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

            // Draw-phase isolation: reference the token to register a dependency strictly bound to the Draw phase
            val readTokenForInvalidation = drawInvalidationToken

            // Draw black background
            drawRect(Color.Black)

            // Blit the offscreen trail bitmap onto the screen
            trailBuffer?.bitmap?.let { drawImage(it) }

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

            // Draw FPS counter at bottom-left
            cachedFpsLayout?.let { fpsLayout ->
                drawText(
                    textLayoutResult = fpsLayout,
                    topLeft = Offset(12.dp.toPx(), canvasHeight - fpsLayout.size.height - 12.dp.toPx())
                )
            }
        }
    }
}
