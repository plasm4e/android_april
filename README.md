# android_april: a privacy-first Android ROM

A LineageOS-based ROM, plus a set of bundled apps, that hide what is on the screen from
people around you: anonymous notifications, hidden caller IDs, no previews in recent apps,
hidden typing, and search that does not react to every letter.

See [docs/ROADMAP.md](docs/ROADMAP.md) for the feature list and how each one is built.

## Layout

| Path | What |
|---|---|
| `apps/` | Gradle project with the bundled apps (one module per app) |
| `apps/keyboard/` | **Privacy Keyboard**: whole-word mode + hidden typing |
| `scripts/emu.sh` | Sets up and drives the Android 15 dev emulator (the base of LineageOS 22) |
| `docs/` | Feature specs and roadmap |

## Dev setup (GitHub Codespace, 8 GB)

```bash
scripts/emu.sh setup                     # once per Codespace: SDK, Android 15 image, AVD
cd apps && ./gradlew :keyboard:assembleDebug   # build (JAVA_HOME = JDK 21)
cd .. && scripts/emu.sh start            # boots headless in tmux (~90 s)
scripts/emu.sh install                   # installs + activates the keyboard
scripts/emu.sh shot /tmp/screen.png      # screenshot
scripts/emu.sh stop
```

On an 8 GB Codespace, **do not build while the emulator is running**: the two together run
out of memory and the emulator gets shut down. Order: stop, build, start.

The keyboard sets `FLAG_SECURE`, so screenshots of it come out black. For development
screenshots build with `./gradlew -PdevScreenshots=true :keyboard:assembleDebug`.
