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
package org.pushingpixels.eigelb

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.pushingpixels.eigelb.glitzer.Glitzer
import org.pushingpixels.eigelb.glitzer.Lissajous
import org.pushingpixels.eigelb.glitzer.Maus
import org.pushingpixels.eigelb.glitzer.Paint
import org.pushingpixels.eigelb.grappa.Grappa
import org.pushingpixels.eigelb.grappa.Settings
import eigelb.sharedui.generated.resources.Image_PS
import eigelb.sharedui.generated.resources.Res
import org.jetbrains.compose.resources.imageResource
import org.pushingpixels.eigelb.sparky.Sparky
import org.pushingpixels.eigelb.sparky.SparkySettings

enum class Applet(val displayName: String) {
    GlitzerDefault("Glitzer Default"),
    GlitzerMaus("Glitzer Maus"),
    GlitzerPaint("Glitzer Paint"),
    GlitzerLissajous("Glitzer Lissajous"),
    GrappaDefault("Grappa Default"),
    GrappaBlue("Grappa Blue"),
    GrappaWhite("Grappa White"),
    GrappaRainbow("Grappa Rainbow"),
    Sparky("Sparky"),
    SparkyPS("Sparky PS");

    @Composable
    fun render(modifier: Modifier) {
        when (this) {
            GlitzerDefault -> Glitzer(modifier = modifier)
            GlitzerMaus -> Maus(modifier = modifier)
            GlitzerPaint -> Paint(modifier = modifier)
            GlitzerLissajous -> Lissajous(modifier = modifier)
            GrappaDefault -> Grappa(modifier = modifier, startingSettings = Settings.preset())
            GrappaBlue -> Grappa(modifier = modifier, startingSettings = Settings.blue())
            GrappaWhite -> Grappa(modifier = modifier, startingSettings = Settings.white())
            GrappaRainbow -> Grappa(modifier = modifier, startingSettings = Settings.rainbow())
            Sparky -> Sparky(modifier = modifier, settings = SparkySettings())
            SparkyPS -> {
                val psImage = imageResource(Res.drawable.Image_PS)
                Sparky(modifier = modifier, settings = SparkySettings(baseImageBitmap = psImage))
            }
        }
    }
}
