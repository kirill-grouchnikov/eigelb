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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import kotlin.math.pow

/**
 * Scales down an [ImageBitmap] by the given [scaleFactor] using nearest-neighbor sampling.
 *
 * The output dimensions are computed as `source.width / scaleFactor` and `source.height / scaleFactor`,
 * rounded down to the nearest integer. Each output pixel is sampled from the source at the
 * corresponding scaled-up coordinate.
 *
 * @param source The input image bitmap
 * @param scaleFactor The scale factor (must be > 1 to scale down; e.g., 2.0 halves the dimensions)
 * @return A new bitmap scaled down by the given factor
 */
public fun scaleDown(source: ImageBitmap, scaleFactor: Double): ImageBitmap {
    val srcWidth = source.width
    val srcHeight = source.height
    val dstWidth = (srcWidth * scaleFactor).toInt().coerceAtLeast(1)
    val dstHeight = (srcHeight * scaleFactor).toInt().coerceAtLeast(1)
    val result = ImageBitmap(dstWidth, dstHeight)

    // Apply a scale transformation on the output canvas and draw the original bitmap directly
    val canvas = GraphicsCanvas(result)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = androidx.compose.ui.unit.Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(dstWidth.toFloat(), dstHeight.toFloat())
    ) {
        val scaleX = dstWidth.toFloat() / srcWidth.toFloat()
        val scaleY = dstHeight.toFloat() / srcHeight.toFloat()
        canvas.scale(scaleX, scaleY)
        drawImage(source)
    }

    return result
}

/**
 * Converts an [ImageBitmap] to a new bitmap of the same size where each pixel is white
 * with an alpha channel that has a quadratic relationship to the original pixel's brightness.
 *
 * - A pixel with zero brightness gets zero alpha (fully transparent).
 * - A pixel with half brightness (128) gets ~25% alpha.
 * - A pixel with full brightness (255) gets 100% alpha (fully opaque white).
 *
 * The brightness of each input pixel is computed as the average of its RGB channels.
 * The alpha is calculated as: `alpha = (brightness / 255)^2 * 255`.
 *
 * @param source The input image bitmap
 * @return A new bitmap of the same size with white pixels and quadratic alpha values
 */
public fun toQuadraticAlphaWhite(source: ImageBitmap, target: Color): ImageBitmap {
    val width = source.width
    val height = source.height
    val result = ImageBitmap(width, height)

    // Read all pixels from the source bitmap
    val sourcePixels = IntArray(width * height)
    source.readPixels(sourcePixels)

    // Pre-compute alpha values for each pixel
    val alphas = FloatArray(width * height)
    for (i in 0 until width * height) {
        val pixel = sourcePixels[i]
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        val alpha = (pixel shr 24) and 0xFF

        // Compute brightness as average of RGB channels, weighted by source alpha
        val brightness = ((red + green + blue) / 3.0 * alpha / 255.0).toInt()

        // Quadratic mapping: alpha = (brightness / 255)^2 * 255
        val normalized = brightness / 255.0f
        alphas[i] = normalized.pow(0.7f).coerceIn(0f, 1f)
    }

    // Write pixels to the result bitmap via GraphicsCanvas + CanvasDrawScope
    val canvas = GraphicsCanvas(result)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = androidx.compose.ui.unit.Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = alphas[y * width + x]
                if (alpha > 0f) {
                    drawCircle(
                        color = Color(red = target.red, green = target.green, blue = target.blue, alpha = alpha),
                        radius = 0.5f,
                        center = Offset(x + 0.5f, y + 0.5f)
                    )
                }
            }
        }
    }

    return result
}
