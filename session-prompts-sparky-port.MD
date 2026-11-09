# Session Prompts - Sparky Compose Multiplatform Port
## Date: 2026-11-08

---

1. Make a new "sparky" package under org.pushingpixels.eigelb in sharedUI/commonMain

2. The local ~/Downloads/PaulAppletFiles/064_Sparky/Sparky.java is a Java applet that uses an offscreen image sized to the viewport size to create an animated graphic effect that is then drawn on the screen for every frame. Port this to Compose Multiplatform as a @Composable function. If possible, use the same approach with an offscreen BitmapImage, and a GraphicsCanvas + CanvasDrawScope wrapping it as Grappa does for drawing pixels onto an image, and then using Canvas.drawImage to draw that image in the Canvas child of the converted Composable.

3. For the star pattern, take Star.gif from the same folder where Sparky.java is, and use it in this port. For the base brightness map, use the Image.jpeg from that same folder.

4. Add Compose multiplatform resources support to sharedUI module. After that, add Image,jpg and Star.gif there

5. In desktopApp module, add a new demo to show the Sparky composable in action, driven by these two image files

