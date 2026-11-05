# Session Prompts - Grappa Compose Multiplatform Port
## Date: 2026-11-05

---

1. Make a new "grappa" package under org.pushingpixels.eigelb in sharedUI/commonMain

2. The local ~/Downloads/PaulAppletFiles/090_Grappa2/Grappa.java is a Java applet that uses an offscreen image sized to the viewport size to create an animated graphic effect that is then drawn on the screen for every frame. Port this to Compose Multiplatform as a @Composable function. Consider using PixelMapper for migrating the original SmiSchirm_c, with Skia's Bitmap allocPixels and installPixels to get them to a bitmap that is then rendered on a Canvas in the composable function.

3. Rename Partikel to Particle.

4. Suggest a better name for SmiSchirm

5. rename SmiSchirm to PixelBuffer

6. Make PixelBuffer private as it's only used inside Grappa.kt

7. Rename rot, gruen and blau to red, green and blue

8. In the original implementation, when the mouse moves, the particle trails get wider depending on the velocity of the mouse movement. In this implementation, that should scale with the screen density obtained from LocalDensity.current.

9. Revert this last change

10. When particle friction is updated in updateParticles, it should be multiplied by the current screen density obtained from LocalDensity.current - maybe passed as a parameter to updateParticles.

11. In the original implementation, when the mouse is moved, the particles are spread apart more. Fix this.

12. Settings.n and Settings.nMax are based on the total number of particles initialized in Grappa function. Update these two fields to stay in sync with the total number of particles instead of being hardcoded

13. Revert the last change

14. In the original implementation, the particles move towards the current mouse position slower. In this one, it looks like it's immediate. Fix this by accounting for the passage of time in withFrameNanos instead of a hardcoded dt value

15. Update the scaling factor so that the particles move slower towards the last mouse event location

16. Add a double click pointer event to clear the offscreen image

17. Revert this last change

18. Add a key event handler to clear the canvas on either C or R key tap

19. Not seeing anything cleared when I press C

20. Look at Glitzer.kt and how it tracks the framerate. Add the same tracking in Grappa.

21. Remove the keyboard tracker logic

22. Revert the framerate tracking

23. Update the scaling factor so that the particles move slower towards the last mouse event location

24. Increase the number of particles from 100 to 150

25. Increase them to 200

26. Decrease them back to 150

27. In randomize, print values of all the parameters

28. Add key presses for C to clear the canvas and for R to reset the parameters with randomize

29. The key events are not getting to the onKeyEvent

30. Still not getting any key events

31. Still not getting any key events - move the handling to the window in the demo, and add Grappa APIs to handle the logic

32. Update the handling of key events in GrappaMain based on the example in https://kotlinlang.org/docs/multiplatform/compose-desktop-keyboard.html#events-in-a-window-scope, including the usage of singleWindowApplication

33. Revert the last change

34. Revert key handling

35. Add emulated action buttons in the bottom right corner of the canvas. Each action button is just a box that is handled in a custom way in the existing pointer input handler. The first button is clear - clearing the canvas. The second button is randomize - calling the randomize function. The buttons are stacked vertically in the bottom right corner, and have light blue text and light blue rectangular outline.

36. Add another emulated button called Preset. Don't wire its clicks to anything yet

37. Size all the buttons horizontally based on the widest one

38. Make the button text slightly bigger

39. Add the framerate tracking from Glitzer to Grappa

40. Add a bit more horizontal paddings to all the emulated buttons

41. The button paddings should be 12.dp, scaling with LocalDensity.current, instead of hardcoded pixel values

42. Take the randomize function, and create a preset function that takes the second block in randomize to set all the values

43. Wire the preset button to call this preset() function

44. Add a clear() function that encapsulates the logic of the clear button

45. Lines 116-118 set the initial color for particles. Extract this color values to a new field in Settings, and update the particles based on the r/g/b values of that field

46. Update the preset() function to set these three fields to this specific values (255, 50, 10)

47. Update the randomize implementation to give random values to these three fields. The logic should give at least one of them the value of 255

48. Add Grappa to the list of web demos alongside all the Glitzer ones in GlitzerMain.kt under webApp module

49. Rename that GlitzerMain.kt to WebDemoMain.kt

50. Make a full copy of Grappa.kt

51. Look at DynamicPixels.kt. It has an offscreen ImageBitmap, with a CanvasDrawScope that is created from it. That draw scope is used to draw circles based on mouse events, and then the bitmap is drawn to the onscreen canvas.

52. Take GrappaNew, and instead of using an offscreen Bitmap with ByteArray  that is updated on per-pixel level with subsequent Bitmap.installPixels call, apply the logic from DynamicPixels. The accumulatePixel logic that writes entries into the underlying pixelData byte array can be replaced by a call to Canvas.drawCircle to draw a single pixel with the relevant RGB color.

53. Replace Grappa with GrappaNew and remove GrappaNew and its desktop demo
