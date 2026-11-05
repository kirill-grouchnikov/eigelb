# Session Prompts - Glitzer Compose Multiplatform Port
## Date: 2026-11-04

---

1. Look at ~/Downloads/PaulAppletFiles/016_Glitzer/Glitzer.java file. It is a Java applet with graphics effects. Port this code and, if necessary, other Java files in that folder to Compose multiplatform and place the converted code in sharedUI/src/commonMain module under the new glitzer sub-package.

2. add Lissajous applet

3. add Maus and Paint applets

4. Modules desktopApp and webApp have main.kt files that reference the existing App() composable. Add entry points in both modules for all four of the recently converted applets

5. The original Glitzer.java tracks the mouse location to dynamically update the canvas content. Is this missing in the ported code?

6. Still not seeing any visuals in the Glitzer main demo

7. It looks like the particles list in Glitzer function isn't properly tracked as a Compose data source.

8. The current logic in Maus.kt uses hardcoded values for emulating the mouse position in lines 36-37. Use the onSizeChanged modifier to keep track of this composable, and use that dynamic size to compute the emulated mouse position.

9. Revert this last change in Maus, and then apply those changes in Lissajous instead

10. Is an infinite while loop in LaunchedEffect with kotlinx.coroutines.delay(20) the best practice in running an event loop in Compose? Is there a better or more performant alternative?

11. apply withFrameNanos refactor

12. Restore the usage of mutableStateOf for every field of the Particle class

13. Particle.p tracks the "lifespan" of each particle, and it decays by 3% in every frame. On higher performing devices with higher frame rate, that means particles decay too quickly. Update this logic to be based on the elapsed time, keeping the original logic of decaying by 3% about every 20 milliseconds

14. Particles decay too quickly. Make the decay twice as slow.

15. Right now particles are tracked in a list with fixed number of entries. When a particle decays enough, it is marked as not active, and is skipped for drawing. Instead, have the list keep have only active particles. New particles are added as needed while the mouse is pressed. Decayed particle marked as inactive are removed from the list.

16. clean up particleCount parameter

17. Should particles be tracked by mutable state list?

18. Replace mutableListOf with mutableStateListOf for all particles tracking in all the composables

19. When a particle is spawned, its location is given random offsets around the mouse position, like in lines 64-65 of Glitzer.kt. This does not account for the screen density that comes from LocalDensity.current. Make the change to have the offsets be density-aware.

20. When the mouse moves quickly across the screen, it feels like the onDrag is called too infrequently. Is detectDragGestures the best Compose multiplatform way to track mouse movement?

21. replace detectDragGestures with awaitPointerEventScope

22. Add an overlay in the bottom right corner. It should display two things - current framerate (driven by the "dt" value in the withFrameWithNanos loop) and current number of displayed particles.

23. Looks like this Text() composable in the corner is preventing the pointerInput modifier on Canvas from tracking actual mouse events

24. Still not seeing any particles being generated on mouse events. When I comment out that Text() box, particles are generated.

25. Mouse events are still not delivered properly, and no particles are generated.

26. This is still not working. Can the structure be tweaked so that the text overlay is in a column with the canvas? The canvas will track mouse events, and will span as much height as it can in the column.

27. Revert all the changes that were done for the text overlays

28. Add tracking for the current framerate (driven by the "dt" value in the withFrameWithNanos loop) in all applets. Don't display it anywhere

29. Look at how Glitzer.kt draws text overlays in the bottom left corner that track FPS and particle count, and do the same change in the other three applets

30. Framerate drops significantly when there are about 3000 particles displayed. Is there a way to improve performance for tracking and displaying particles?

31. Update texts shown on the canvas in all applets to be the same color 0xFF80E0FF as in Glitzer

32. In all applets, vs and vy for particles is the velocity that is used to change the particle location as it decays. These two should also be scaled during the particle creation by the density.

33. Rename fields in Particle to reflect their usage to have more readable code

34. Change Particle.shapeType to an enum with two values - Plus and Cross.

35. Is the Particle.active still being used anywhere?

36. Remove the active field

37. brightnessMin and brightnessMax are used for two separate things - the particle brightness and the particle size. Introduce two new fields - lengthMin and lengthMax, both Dp units based - to be used for the size of the particle when it's rendered. And the brightness fields should be double instead of DP. Both field pairs should be in 0.2-1.0 range

38. Rename posX and posY to positionX and positionY. Then velX and velY to velocityX and velocityY

39. Rename ro, gr and bl variables in particle drawing with red, green and blue

40. Replace "if (particle.shapeType == ParticleShape.Plus)" and the else block with an exhaustive "when" on the underlying enum

41. Is there a case where isMousePressed has a different value from isPressed?

42. Remove the local isPressed variable and rename isMousePressed to isPointerPressed

43. Rename mousePos to pointerPosition

44. Remove canvasSize tracking and height checks for removing particles 

45. In all applets, there's "drawLine(color, Offset(x.toFloat(), y.toFloat()), Offset(x.toFloat(), y.toFloat()), strokeWidth = 1f)" which is a no-op in Compose canvas. What was the original code in Java doing, and what is the Compose's equivalent? 

46. Fix this across all 4 files
