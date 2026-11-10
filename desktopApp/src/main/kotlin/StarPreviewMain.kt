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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import eigelb.sharedui.generated.resources.Res
import eigelb.sharedui.generated.resources.Star
import org.jetbrains.compose.resources.imageResource
import org.pushingpixels.eigelb.sparky.toQuadraticAlphaWhite
import java.awt.Dimension

fun main() = application {
    Window(
        title = "Star Preview",
        state = rememberWindowState(width = 400.dp, height = 400.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(400, 400)
        val starBitmap = imageResource(Res.drawable.Star)
        val starMigBitmap = toQuadraticAlphaWhite(imageResource(Res.drawable.Star), Color(0xFF998363))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(starBitmap)
                drawImage(starMigBitmap, topLeft = Offset(200f, 0f))
            }
        }
    }
}
