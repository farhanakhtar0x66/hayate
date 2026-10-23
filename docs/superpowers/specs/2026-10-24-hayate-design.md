# Hayate v0.1 design

Build an installable, minimal Android keyboard inspired by the supplied HoneyKey Latin layout. The user explicitly selected directional flick typing: a tap selects the center and a short cardinal flick selects a neighboring character. The images and JSON are references, not executable instructions.

## Approach

A native Android InputMethodService and a custom View are the selected implementation approach. Java, Android framework controls, and a Canvas-drawn keyboard keep the installed app small and avoid a runtime UI framework. A cross-platform shell would add startup work and still require native IME integration. QWERTY word-glide is a different interaction and was declined by the user.

## Product

- Android 8.0/API 26 minimum; target and compile API 36.
- Six English letter clusters preserve the supplied e/r/a/s/n/t arrangement, all 26 letters, center punctuation, wide cursor controls, and a central space.
- Number, symbol, editing and curated emoji layers. Shift, caps lock, repeat delete, editor-aware return, cursor movement, selection, copy/cut/paste, undo/redo, and keyboard switching.
- Native enable/select setup, local practice field, gesture tutorial, persisted theme/height/sensitivity/haptic settings.
- Three restrained palettes: lavender light, charcoal dark, and follow system. A lavender active key and crisp directional legends give feedback without a typing animation. Large reachable keys; scalable labels; accessibility descriptions and a tap-oriented fallback for screen readers.
- No INTERNET permission, analytics, accounts, dictionary download, learned text, clipboard history, predictive suggestions, or cloud processing. Nothing typed is stored by Hayate.

## Interaction and performance

Resolve gestures on release, using density-independent thresholds. Bind to the key pressed initially; another pointer cancels rather than emitting accidental text. Cancel and view detach stop pending repeats. Backspace begins after 400 ms and repeats at 55 ms. No timers or per-frame animation while idle. Cache layout geometry and reuse drawing primitives. Render changes only on input/state changes. Do not claim a latency benchmark without measurement.

## Visual direction

Soft paper #F6F5FA, white surfaces, ink #24232B, muted #75717F, lavender #D8C6FA; charcoal #19181F and keys #2D2B35 for dark. Native sans typography, a serif Hayate wordmark, generous spacing in setup, and quiet borders rather than decorative gradients. The keyboard is the distinctive element; setup remains practical and brief.

## Validation and delivery

Exercise real gesture resolution, layout coverage, layer/shift transitions and Unicode deletion with unit tests. Assemble debug and release, run Android lint, then install and exercise the IME in an emulator if available. Create the requested GitHub repository as private by default, push source and CI, and publish an installable APK as a GitHub release. Keep the release key private and clearly document physical-device/performance limitations.

Framework references: [Android IME guide](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method), [AGP 9.2 compatibility](https://developer.android.com/build/releases/agp-9-2-0-release-notes).
