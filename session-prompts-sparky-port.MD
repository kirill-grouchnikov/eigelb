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

13. Add a state enum to Sparky. The state has two values - Precalculating and Animating. Sparky starts in Precalculating state, and moves to Animating when precalculareFrames returns

14. Draw a progress indicator to the main Sparky composable when it is in the Precalculating phase. The indicator tracks how many of the frameBitmaps have been calculated, and updates the visual display of the indicator. The visuals of the indicator are a 16-dp tall white rectangular outline, with an inner white fill that matches the percentage of the calculated bitmaps.

15. In this new implementation with the progress indicator, its visuals should span the full width of the canvas, with 40dp side paddings. Its height should be 16dp, and centered vertically within the canvas.

16. Update the progress indicator width to be 80% of the canvas width

17. In the web version of Compose Multiplatform, calling precalculateFrames in withContext(Dispatchers.Default) doesn't work since the web target is a single threaded environment. How can the current precalculation be implemented (maybe on the main thread) in a way that still shows the preloading progress?

18. The first time the web demo for Sparky loads, the precalculated frames are all black. They are the right size, but they don't have the right pixels.
