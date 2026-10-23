# Hayate 0.1.1 validation

Verified on 24 October 2026 (Asia/Kolkata).

| Check | Result |
| --- | --- |
| JVM unit tests | 17 passed |
| Android instrumented tests | 12 passed, 0 skipped |
| Android lint | 0 errors; 14 warnings |
| Debug and optimized release builds | Passed |
| APK signing | v2 and v3 signature verification passed |
| APK size | 33,523 bytes |
| Runtime dependencies | None |
| Requested app permissions | None (confirmed from the packaged manifest) |
| System keyboard | Signed release enabled and selected in Android Settings |
| Real touch input | Typed hello into Android Settings using Hayate’s flick keys |
| Visual review | Light/dark portrait and dark landscape screenshots reviewed |

Final verification command:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:connectedDebugAndroidTest
```

The unit tests cover all 26 letters, the supplied example flicks, direction thresholds, layer escape/delete controls, all digits, shift/caps behavior, and common Unicode deletion cases. Device tests exercise real key Views and EditText InputConnections: hello, shift/caps, numbers, selected/emoji deletion, cancellation, multiple pointers, repeat delete, space dragging, multiline return, Search, grapheme-safe cursor movement followed by deletion, and long-press character choices.

Both review regressions were confirmed with a mutation run: reverting grapheme movement and disabling physical long press produced exactly two failures out of twelve. Restoring the fixes produced twelve passes. The popup was also checked with real pointer input and used to commit x.

The signed release was checked outside the practice area. System navigation controls have separate space below the bottom key row, including after theme and orientation changes. The setup status refreshes when the keyboard picker closes. Patch 0.1.1 makes the practice window independent of Android’s IME, so returning from another app cannot put the system keyboard over its local keyboard.

Environment: macOS arm64; JDK 21; Gradle 9.5.0; AGP 9.2.1; compile/target API 36. Android emulator: API 37 / Android 17, arm64 16 KB-page Google Play image, 1080 × 2400 portrait / 2400 × 1080 landscape at 420 dpi. Transient ADB transport and signed/debug installation conflicts were resolved before the final passing run.

Lint warnings are limited to pinned tool/SDK version suggestions, the deliberately programmatic custom View constructor, and English-only UI strings. The built-in Kotlin opt-out emits a build-tool deprecation warning; the pinned Android plugin supports it.

Physical devices, Android 8–16 hardware, TalkBack end-to-end navigation, battery usage and typing latency have not been benchmarked or fully exercised. Minimum API 26 is declared and platform API usage is checked by lint. The accessibility long-click path and tappable popup are implemented; the device test exercises popup selection via Android’s accessibility API. This is an early usable release, not a comparative performance result.

SHA-256 of the shipped APK:

```text
941cf46e4c00fec99ee2e2d7b63dd00c63050708fc458d2d84800f3da87a4830
```

Screenshots: [setup](screenshots/setup.png), [light keyboard](screenshots/keyboard-light.png), [dark keyboard](screenshots/keyboard-dark.png), [landscape](screenshots/keyboard-landscape.png).

Published to the private [GitHub repository](https://github.com/farhanakhtar0x66/hayate). [Release v0.1.1](https://github.com/farhanakhtar0x66/hayate/releases/tag/v0.1.1) contains the signed APK and checksum; the uploaded asset digest matches the local SHA-256 above.
