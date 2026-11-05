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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import org.pushingpixels.eigelb.glitzer.*
import org.pushingpixels.eigelb.grappa.Grappa

// This is a modified version of the original source code by Paul Schmidinger
// from the Java applet effect demos at https://eigelb.at/

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    var selectedApplet by remember { mutableStateOf("Glitzer") }
    val applets = listOf("Glitzer", "Grappa", "Lissajous", "Maus", "Paint")

    Column(modifier = Modifier.fillMaxSize()) {
        // Applet selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (applet in applets) {
                val isSelected = applet == selectedApplet
                OutlinedButton(
                    onClick = { selectedApplet = applet },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(applet)
                }
            }
        }

        // Selected applet canvas
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedApplet) {
                "Glitzer" -> Glitzer(modifier = Modifier.fillMaxSize())
                "Lissajous" -> Lissajous(modifier = Modifier.fillMaxSize())
                "Maus" -> Maus(modifier = Modifier.fillMaxSize())
                "Paint" -> Paint(modifier = Modifier.fillMaxSize())
                "Grappa" -> Grappa(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
