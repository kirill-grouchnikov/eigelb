# Session Prompts - Sparky Compose Multiplatform Port
## Date: 2026-11-08

---

1. Make a new "sparky" package under org.pushingpixels.eigelb in sharedUI/commonMain

2. The local ~/Downloads/PaulAppletFiles/064_Sparky/Sparky.java is a Java applet that uses an offscreen image sized to the viewport size to create an animated graphic effect that is then drawn on the screen for every frame. Port this to Compose Multiplatform as a @Composable function. If possible, use the same approach with an offscreen BitmapImage, and a GraphicsCanvas + CanvasDrawScope wrapping it as Grappa does for drawing pixels onto an image, and then using Canvas.drawImage to draw that image in the Canvas child of the converted Composable.

3. For the star pattern, take Star.gif from the same folder where Sparky.java is, and use it in this port. For the base brightness map, use the Image.jpeg from that same folder.

4. Add Compose multiplatform resources support to sharedUI module. After that, add Image,jpg and Star.gif there

5. In desktopApp module, add a new demo to show the Sparky composable in action, driven by these two image files

6. The frames are precalculated at 600x450 size in LaunchedEffect. Instead, they should be calculated based on the actual size of the canvas after that size is known.

7. Move the call to precalculateFrames to happen after the size is known, instead of in the LaunchEffect that is never actually triggered there

8. The main demo frame is never shown now. Is there a deadlock somewhere?

9. Refactor the code so that the call to precalculateFrame happens in a separate worker unit (coroutine?). Canvas still tracks its size so that precalculateFrame work is kicked off. When the frames are done calculating, the Canvas can draw them based on the value of currentFrame.

10. Update the session prompt history MD file with the recent prompts

11. Rename all German variables in Sparky.kt

12. Add another Sparky demo, call it SparkyPS. The starBitmap image in its settings is the same, but the baseImageBitmap should be 064_Sparky/fuerHomepage/Image_PS.jpg from the original source folder. Place this image as a Compose multiplatform resource in sharedUI next to Image.jpg

