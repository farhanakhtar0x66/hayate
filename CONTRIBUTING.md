# Contributing to Hayate

Thanks for helping make a small keyboard feel better.

## Report a bug

Open an issue with your Android version, device model, Hayate version, host app, and steps to reproduce. Describe what you expected and what happened. Please use sample text in screenshots and logs; omit personal messages, passwords and clipboard contents.

For feature ideas, explain the typing problem and how you would use the feature. Keep Hayate’s focus in mind: a minimal, offline directional flick keyboard.

## Make a change

1. Fork the repository and create a branch for one focused change.
2. Use JDK 21, Android SDK 36 and build-tools 36.0.0. See the README for setup.
3. Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`.
4. For input or gesture changes, run `./gradlew connectedDebugAndroidTest` on an emulator and try the keyboard in a real editor. Test portrait and landscape, light and dark themes, and cancellation.
5. Open a pull request describing the user-visible change and what you verified. Include screenshots for visual changes.

Preserve the existing letter flick map, password-field protections and offline operation. Keep slow dictionary work off the UI thread. Add a focused regression test for behavioral fixes. Include license and provenance notices for any new dependency or dictionary.

Never commit signing keys, passwords, local SDK paths or personal text. Release signing is maintained separately; contributors can use debug builds.

Contributions to Hayate’s original code are provided under its MIT License. Third-party material retains its own license.
