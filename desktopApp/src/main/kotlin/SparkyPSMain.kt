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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import eigelb.sharedui.generated.resources.Image
import eigelb.sharedui.generated.resources.Res
import eigelb.sharedui.generated.resources.Image_PS
import eigelb.sharedui.generated.resources.Star
import eigelb.sharedui.generated.resources.Star_Alpha
import org.jetbrains.compose.resources.imageResource
import org.pushingpixels.eigelb.sparky.Sparky
import org.pushingpixels.eigelb.sparky.SparkyDirect
import org.pushingpixels.eigelb.sparky.SparkySettings
import org.pushingpixels.eigelb.sparky.scaleDown
import org.pushingpixels.eigelb.sparky.toQuadraticAlphaWhite
import java.awt.Dimension

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

fun main() = application {
    Window(
        title = "SparkyPS",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(800, 600)
        val baseImage = imageResource(Res.drawable.Image_PS)

        val starImage = imageResource(Res.drawable.Star)
        //val scaledDownStarImage = scaleDown(starImage, 0.5)
        val remappedStarImage = toQuadraticAlphaWhite(starImage, Color(0xFF998363))

        SparkyDirect(
            modifier = Modifier.fillMaxSize(),
            settings = SparkySettings(
                frameCount = 25,
                scale0Multiplier = 100,
                thresholdMultiplier = 60,
                starBitmap = remappedStarImage,
                baseImageBitmap = baseImage)
        )
    }
}
