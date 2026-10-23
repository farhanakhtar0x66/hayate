# Hayate 0.2.0 validation

Verified on 24 October 2026 (Asia/Kolkata).

| Check | Result |
| --- | --- |
| JVM unit tests | 21 passed |
| Android instrumented tests | 21 passed, 0 skipped |
| Android lint | 0 errors; 15 warnings |
| Debug and optimized release builds | Passed |
| APK signing | v2 and v3 verification passed; same certificate as 0.1.1 |
| APK size | 291,711 bytes |
| Runtime dependencies | JSymSpell 1.0, MIT, no transitive runtime libraries |
| Requested app permissions | None, confirmed from packaged manifest |
| System keyboard | Signed release enabled and selected in Android Settings |
| Real input | Flick-typed `wurld`, tapped the `world` suggestion, verified `world ` in the host field |
| Clipboard | Copied through editing layer and opened the system clip in the new panel |
| Programmer mode | Wrapped selected text using the paired-delimiter shortcut; straight punctuation and code tools reviewed |
| Visual review | Light/dark portrait, landscape, clipboard, programmer and credits screens |

Final verification:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:connectedDebugAndroidTest
```

The original 17 unit and 12 device gesture/typing tests continue to pass. Added tests cover JSymSpell typo candidates, prefix completion and case; whole-word boundaries; clipboard deduplication, bounds and expiry; safe candidate insertion and stale context rejection; selected-text pairing with UTF-16 offsets and unsupported selected-text queries; password/no-suggestion policy; system clipboard/sensitive clip exclusion; real visible suggestions; straight code punctuation; indentation, pairing shortcuts and capitalization restoration on leaving code mode.

The selected-text-query regression was observed failing with `(name)` replaced by `()` before the fix. The code-mode capitalization regression was observed failing with lowercase `e` where `E` was requested; both pass after correction. A read-only review verified the selected-text and API 26 cleanup fixes. The clipboard expiration timer removes history even when panels are hidden and refreshes open shelves.

Manual lifecycle regression: show Hayate in Android Settings search, then return to MainActivity. An already visible IME could remain bound to Settings despite the practice window’s ALT_FOCUSABLE_IM flag. The live service now requests hide when practice resumes or gains focus. Verified `mInputShown=true` in Settings followed by `mInputShown=false` in Hayate, and reviewed the embedded keyboard without the system IME covering it. Return labels show a newline in programmer mode.

The APK bundles a 30,000-entry subset of the pinned upstream English dictionary. Its contents were compared against the recorded SymSpell revision. JSymSpell and dictionary notices are included in the app and repository. No personal dictionary or typing log is stored.

Environment: macOS arm64; JDK 21; Gradle 9.5.0; AGP 9.2.1; compile/target API 36. Emulator: API 37 / Android 17, arm64 16 KB-page Google Play image, 1080 × 2400 portrait / 2400 × 1080 landscape at 420 dpi. Minimum API 26 platform compatibility is checked by lint; device tests use the same clipboard-clearing fallback as production on API 26–27.

Lint warnings concern pinned tool/SDK versions, backup configuration metadata (backup is disabled), the programmatic custom View constructor, and English-only UI strings. The pinned plugin’s Kotlin opt-out emits a deprecation warning. Instrumentation reports that per-test logcat could not be collected; all device cases completed with zero failures.

Physical-device typing latency, battery use, TalkBack end-to-end navigation, and every Android 8–16 device/editor combination have not been measured or fully exercised. This is an early usable release, without comparative speed claims or next-word prediction.

SHA-256:

```text
00ec871b99312558658a8a089e7c6ba4c414725b5840ea53b896b2aa4c73bfe7
```

Screenshots: [practice](screenshots/setup.png), [suggestions](screenshots/keyboard-light.png), [dark](screenshots/keyboard-dark.png), [landscape](screenshots/keyboard-landscape.png), [clipboard](screenshots/clipboard.png), [programmer](screenshots/programmer.png), [credits](screenshots/credits.png).

Release target: the existing private [GitHub repository](https://github.com/farhanakhtar0x66/hayate), signed APK and checksum under v0.2.0.
