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
package org.pushingpixels.eigelb.glitzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

@Composable
fun Paint(
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableListOf<Particle>() }
    var time by remember { mutableStateOf(0.0) }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isPointerPressed by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }
    var fps by remember { mutableStateOf(0.0) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val dt = if (lastFrameNanos > 0) {
                    (frameTimeNanos - lastFrameNanos) / 1_000_000_000.0
                } else 0.016
                lastFrameNanos = frameTimeNanos

                // Track framerate
                if (dt > 0) {
                    fps = 1.0 / dt
                }

                // Simulate physics
                time += dt

                // Remove decayed particles
                particles.removeAll { particle ->
                    particle.lifespan < 0.05
                }

                // Spawn new particles while mouse is pressed
                var spawned = 0
                while (spawned < 4 && isPointerPressed) {
                    val offset = 3.0 * density
                    val spread = 7.0 * density
                    val particle = Particle().apply {
                        positionX = pointerPosition.x - offset + (spread * Random.nextDouble())
                        positionY = pointerPosition.y - offset + (spread * Random.nextDouble())
                        velocityX = 5.0 * density * (Random.nextDouble() - 0.5)
                        velocityY = 5.0 * density * (Random.nextDouble() - 0.5)
                        brightnessMin = 0.2
                        brightnessMax = 1.0
                        lengthMin = 0.2.dp
                        lengthMax = 1.0.dp
                        lifespan = 1.0 + 6.0 * Random.nextDouble()
                        oscillationFreq = 15.0 * Random.nextDouble()
                        shapeType = if (Random.nextDouble() < 0.5) ParticleShape.Plus else ParticleShape.Cross
                    }
                    particles.add(particle)
                    spawned++
                }

                // Update existing particles
                for (particle in particles) {
                    particle.velocityX *= 0.97
                    particle.velocityY *= 0.97
                    particle.positionX += dt * particle.velocityX
                    particle.positionY += dt * particle.velocityY
                    particle.lifespan *= 0.99.pow(dt / 0.040)
                }

                // Trigger recomposition
                frameCount++
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()

    // Cache text measurements to avoid expensive layout work every frame
    var cachedFpsLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var cachedCountLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastFpsInt by remember { mutableStateOf(-1) }
    var lastCount by remember { mutableStateOf(-1) }

    val currentFpsInt = fps.toInt()
    val currentCount = particles.size
    if (currentFpsInt != lastFpsInt || currentCount != lastCount) {
        cachedFpsLayout = textMeasurer.measure(
            text = "FPS: $currentFpsInt",
            style = TextStyle(color = Color(0xFF80E0FF).copy(alpha = 0.7f), fontSize = 12.sp)
        )
        cachedCountLayout = textMeasurer.measure(
            text = "Particles: $currentCount",
            style = TextStyle(color = Color(0xFF80E0FF).copy(alpha = 0.7f), fontSize = 12.sp)
        )
        lastFpsInt = currentFpsInt
        lastCount = currentCount
    }

    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                for (change in event.changes) {
                    when (event.type) {
                        PointerEventType.Press -> {
                            isPointerPressed = true
                            pointerPosition = change.position
                        }
                        PointerEventType.Release -> {
                            isPointerPressed = false
                        }
                        PointerEventType.Move -> {
                            if (isPointerPressed) {
                                pointerPosition = change.position
                            }
                        }
                    }
                }
            }
        }
    }) {
        // Clear background
        drawRect(Color.Black)

        // Draw particles
        for (particle in particles) {

            val brightness = particle.lifespan * (particle.brightnessMin + (particle.brightnessMax - particle.brightnessMin) * (0.5 + 0.5 * sin(time * particle.oscillationFreq)))
            val x = particle.positionX.toInt()
            val y = particle.positionY.toInt()

            if (brightness > 1.5) {
                val halfLength = (particle.lengthMin.toPx() + (particle.lengthMax - particle.lengthMin).toPx() * brightness).toInt()
                val red = minOf((0.45 * 255.0 * brightness).toInt(), 255)
                val green = minOf((0.45 * 190.0 * brightness).toInt(), 255)
                val blue = minOf((0.45 * 70.0 * brightness).toInt(), 255)
                val color = Color(red, green, blue)

                when (particle.shapeType) {
                    ParticleShape.Plus -> {
                        // Plus sign
                        drawLine(color, Offset((x - halfLength).toFloat(), y.toFloat()), Offset((x + halfLength).toFloat(), y.toFloat()), strokeWidth = 1f)
                        drawLine(color, Offset(x.toFloat(), (y - halfLength).toFloat()), Offset(x.toFloat(), (y + halfLength).toFloat()), strokeWidth = 1f)
                    }
                    ParticleShape.Cross -> {
                        // Cross sign
                        drawLine(color, Offset((x - halfLength).toFloat(), (y - halfLength).toFloat()), Offset((x + halfLength).toFloat(), (y + halfLength).toFloat()), strokeWidth = 1f)
                        drawLine(color, Offset((x + halfLength).toFloat(), (y - halfLength).toFloat()), Offset((x - halfLength).toFloat(), (y + halfLength).toFloat()), strokeWidth = 1f)
                    }
                }
            }

            val red = minOf((255.0 * brightness).toInt(), 255)
            val green = minOf((190.0 * brightness).toInt(), 255)
            val blue = minOf((70.0 * brightness).toInt(), 255)
            val color = Color(red, green, blue)

            drawCircle(color, center = Offset(x.toFloat(), y.toFloat()), radius = 0.5f)
        }

        cachedFpsLayout?.let { fpsLayout ->
            cachedCountLayout?.let { countLayout ->
                drawText(
                    textLayoutResult = fpsLayout,
                    topLeft = Offset(12.dp.toPx(),
                        size.height - fpsLayout.size.height - countLayout.size.height - 12.dp.toPx())
                )
                drawText(
                    textLayoutResult = countLayout,
                    topLeft = Offset(12.dp.toPx(),
                        size.height - countLayout.size.height - 12.dp.toPx())
                )
            }
        }
    }
}
