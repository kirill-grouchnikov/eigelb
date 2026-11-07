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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.random.Random
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

/**
 * Particle with position, velocity, friction, and attraction properties.
 * Port of the original [Partikel_c] Java class.
 */
class Particle {
    var x: Double = 0.0
    var y: Double = 0.0
    var vx: Double = 0.0
    var vy: Double = 0.0
    var friction: Double = 0.0
    var attraction: Double = 0.0
    var color: Color = Color(0xFFFF320A)
}

/**
 * Settings for particle system randomization.
 * Port of the original [Settings_c] Java class.
 */
public data class Settings(
    var activeParticleCount: Int = 50,
    val maxParticleCount: Int = 150,
    private var frictionMin: Double = 0.0,
    private var frictionMax: Double = 0.0,
    private var frictionLinearity: Double = 0.0,
    private var attractionMin: Double = 0.0,
    private var attractionMax: Double = 0.0,
    private var attractionLinearity: Double = 0.0,
    var particleColor: Color = Color(0xFFFF320A),
    var particleAlphaMultiplier: Double = 0.15,
    var canvasColor: Color = Color.Black,
    var buttonColor: Color = Color(0xFF80E0FF),
    var blendMode: BlendMode = BlendMode.Plus
) {

    fun randomize() {
        activeParticleCount = 1 + Random.nextInt(maxParticleCount)
        frictionMin = 1.0 - 0.1 * Random.nextDouble() * Random.nextDouble()
        frictionMax = frictionMin * (1.0 - 0.1 * Random.nextDouble() * Random.nextDouble())
        frictionLinearity = Random.nextDouble()
        attractionMin = 60.0 * Random.nextDouble() * Random.nextDouble()
        attractionMax = attractionMin * (1.0 + 2.0 * Random.nextDouble() * Random.nextDouble())
        attractionLinearity = Random.nextDouble()
        // Randomize color channels, ensuring at least one is 255
        val channel = Random.nextInt(3)
        val colorRed = if (channel == 0) 255 else Random.nextInt(256)
        val colorGreen = if (channel == 1) 255 else Random.nextInt(256)
        val colorBlue = if (channel == 2) 255 else Random.nextInt(256)
        particleColor = Color(red = colorRed, green = colorGreen, blue = colorBlue, alpha = 255)
        particleAlphaMultiplier = 0.15
        canvasColor = Color.Black
        buttonColor = Color(0xFF80E0FF)
        blendMode = BlendMode.Plus
    }

    fun updateParticles(particles: Array<Particle>, density: Float, target: Offset = Offset.Zero) {
        for (j in particles.indices) {
            if (j >= activeParticleCount) break
            particles[j].x = target.x.toDouble()
            particles[j].y = target.y.toDouble()
            particles[j].vx = 0.0
            particles[j].vy = 0.0
            val t = if (activeParticleCount > 1) j / (activeParticleCount - 1).toDouble() else 0.0
            particles[j].friction =
                frictionMin + frictionLinearity * t * (frictionMax - frictionMin) +
                        (1.0 - frictionLinearity) * Random.nextDouble() * (frictionMax - frictionMin)
            particles[j].attraction =
                attractionMin + attractionLinearity * t * (attractionMax - attractionMin) +
                        (1.0 - attractionLinearity) * Random.nextDouble() * (attractionMax - attractionMin)
            particles[j].attraction *= density
            particles[j].color = particleColor
        }
    }

    companion object {
        fun preset(): Settings {
            return Settings(
                activeParticleCount = 101,
                frictionMin = 0.995,
                frictionMax = 0.977,
                frictionLinearity = 0.954,
                attractionMin = 2.74,
                attractionMax = 3.07,
                attractionLinearity = 0.93,
                particleColor = Color(0xFFFF320A),
                particleAlphaMultiplier = 0.15,
                canvasColor = Color.Black,
                buttonColor = Color(0xFF80E0FF),
                blendMode = BlendMode.Plus,
            )
        }

        fun blue(): Settings {
            val colorRed = 40 + Random.nextInt(40)
            val colorGreen = 100 + Random.nextInt(155)
            val colorBlue = 100 + Random.nextInt(155)

            return Settings(
                activeParticleCount = 101,
                frictionMin = 0.995,
                frictionMax = 0.977,
                frictionLinearity = 0.954,
                attractionMin = 2.74,
                attractionMax = 3.07,
                attractionLinearity = 0.93,
                particleColor = Color(red = colorRed, green = colorGreen, blue = colorBlue, alpha = 255),
                particleAlphaMultiplier = 0.15,
                canvasColor = Color.Black,
                buttonColor = Color(0xFF80E0FF),
                blendMode = BlendMode.Plus,
            )
        }

        fun white(): Settings {
            return Settings(
                activeParticleCount = 101,
                frictionMin = 0.995,
                frictionMax = 0.977,
                frictionLinearity = 0.954,
                attractionMin = 2.74,
                attractionMax = 3.07,
                attractionLinearity = 0.93,
                particleColor = Color(0x60FF4008),
                particleAlphaMultiplier = 0.1,
                canvasColor = Color.White,
                buttonColor = Color(0xFF1050C0),
                blendMode = BlendMode.Difference,
            )
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
    private val canvasColor: Color,
    private val density: Density
) {
    val bitmap = ImageBitmap(width, height)
    val canvas = GraphicsCanvas(bitmap)
    val drawScope = CanvasDrawScope()

    init {
        // Initialize the canvas background to the initial color
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = canvasColor)
        }
    }

    /**
     * Clears the buffer to the specified color.
     */
    fun clear(canvasColor: Color) {
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = canvasColor)
        }
    }

    /**
     * Draws a small circle at the given position with the specified RGB color and opacity.
     * Uses [BlendMode.Plus] to accumulate colors additively, matching the original Java behavior
     * where overlapping particles brighten toward white.
     */
    fun drawPixel(x: Double, y: Double, r: Float, g: Float, b: Float, opacity: Double, blendMode: BlendMode) {
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawCircle(
                color = Color(
                    red = r,
                    green = g,
                    blue = b,
                    alpha = opacity.toFloat()
                ),
                radius = 1f,
                center = Offset(x.toFloat(), y.toFloat()),
                blendMode = blendMode
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
@Composable
fun Grappa(
    modifier: Modifier = Modifier,
    startingSettings: Settings
) {
    var settings by remember { mutableStateOf(startingSettings) }
    val particles = remember { Array(settings.maxParticleCount) { Particle() } }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isPointerPressed by remember { mutableStateOf(false) }
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

    // Button positions (cached and recalculated only when canvas size changes)
    var clearButtonRect by remember { mutableStateOf(Rect.Zero) }
    var randomizeButtonRect by remember { mutableStateOf(Rect.Zero) }
    var presetButtonRect by remember { mutableStateOf(Rect.Zero) }
    var lastCanvasSize by remember { mutableStateOf(Size.Zero) }

    // Cache text measurements for buttons (measured once, not on every recomposition)
    val buttonStyle = TextStyle(color = settings.buttonColor.copy(alpha = 0.7f), fontSize = 14.sp)
    val buttonLayouts = remember {
        Triple(
            textMeasurer.measure(text = "Clear", style = buttonStyle),
            textMeasurer.measure(text = "Randomize", style = buttonStyle),
            textMeasurer.measure(text = "Preset", style = buttonStyle)
        )
    }
    val cachedClearLayout = buttonLayouts.first
    val cachedRandomizeLayout = buttonLayouts.second
    val cachedPresetLayout = buttonLayouts.third

    // Size all buttons to the widest label
    val buttonWidth = max(
        max(cachedClearLayout.size.width, cachedRandomizeLayout.size.width),
        cachedPresetLayout.size.width
    ) + buttonTextPadding

    // Animation loop — initializes settings and runs the animation
    LaunchedEffect(Unit) {
        settings.updateParticles(particles, density, pointerPosition)
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
                        for (j in 0 until settings.activeParticleCount) {
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
                                    p.color.red, p.color.green, p.color.blue,
                                    p.color.alpha * settings.particleAlphaMultiplier,
                                    settings.blendMode
                                )
                            }
                        }
                    }
                    // Fire the lean draw invalidation signal to force Compose to re-read the draw scope block
                    drawInvalidationToken++
                }
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
                                            settings = Settings.preset()
                                            settings.updateParticles(particles, density, pointerPosition)
                                            trailBuffer?.clear(settings.canvasColor)
                                            drawInvalidationToken++
                                        } else if (randomizeButtonRect.contains(change.position)) {
                                            settings.randomize()
                                            settings.updateParticles(particles, density, pointerPosition)
                                        } else if (clearButtonRect.contains(change.position)) {
                                            trailBuffer?.clear(settings.canvasColor)
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
                    trailBuffer = TrailBuffer(bitmapWidth, bitmapHeight, settings.canvasColor, densityObj)
                }
            }

            // Calculate button positions only when canvas size changes
            if (size != lastCanvasSize) {
                lastCanvasSize = size
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
            }

            // Draw-phase isolation: reference the token to register a dependency strictly bound to the Draw phase
            val readTokenForInvalidation = drawInvalidationToken

            // Blit the offscreen trail bitmap onto the screen
            trailBuffer?.bitmap?.let { drawImage(it) }

            // Draw action buttons
            val buttonColor = settings.buttonColor.copy(alpha = 0.7f)
            drawButton(clearButtonRect, cachedClearLayout, buttonWidth, buttonHeight, buttonColor)
            drawButton(randomizeButtonRect, cachedRandomizeLayout, buttonWidth, buttonHeight, buttonColor)
            drawButton(presetButtonRect, cachedPresetLayout, buttonWidth, buttonHeight, buttonColor)

            // Draw FPS counter at bottom-left
            cachedFpsLayout?.let { fpsLayout ->
                drawText(
                    textLayoutResult = fpsLayout,
                    topLeft = Offset(12.dp.toPx(), size.height - fpsLayout.size.height - 12.dp.toPx())
                )
            }
        }
    }
}

/**
 * Draws a single action button outline and centered text label.
 */
private fun DrawScope.drawButton(
    rect: Rect,
    layout: TextLayoutResult,
    buttonWidth: Float,
    buttonHeight: Float,
    buttonColor: Color
) {
    drawRect(
        color = buttonColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.right - rect.left, rect.bottom - rect.top),
        style = Stroke()
    )
    val textX = rect.left + (buttonWidth - layout.size.width) / 2f
    val textY = rect.top + (buttonHeight - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(textX, textY))
}
