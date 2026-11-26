# Kodanda Bow (NeoForge 1.21.1)

Divine golden bow of Shree Ram.

- Get it: Creative > Combat tab, `/give @s kodanda:kodanda_bow`, or craft it
  (gold block / nether star / blaze rod around a bow).
- Hold right-click to charge, release to fire. Infinite arrows.
- Press **R** (rebindable: Controls > Kodanda Bow) to switch astra.
- Flying OP: just hold right-click - you fly and 10 golden arrows/tick (10,000 total) stream out
  and pass through mobs harmlessly.
- Big explosions don't break terrain by default: set `DESTROY_BLOCKS = true` in `Astras.java` to change that.

Build: push to GitHub (workflow in `.github/workflows/build.yml` builds the jar),
or locally with Gradle 8.10+ and Java 21: `gradle build` -> `build/libs/kodanda-bow-1.0.0.jar`.
