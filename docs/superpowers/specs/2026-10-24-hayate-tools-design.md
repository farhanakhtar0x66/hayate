# Hayate 0.2: suggestions, clipboard, code

The user requested a suggestion row, clipboard, programmer mode, reuse of an existing engine, and credit to HoneyKey's designer. Keep the existing native Android app, gesture map, palette, minimum Android 8, and offline operation.

Use the MIT-licensed JSymSpell 1.0 Java library for spelling candidates, with a small prefix-completion adapter and a bundled 30,000-word subset of SymSpell's English frequency dictionary. Load and query off the UI thread, discard stale requests, preserve case, and apply candidates only when editor context still matches. Suggestions are explicit choices; never silently change typing. Passwords, no-suggestions fields, numeric fields and code mode suppress suggestions. This is English completion/correction, not next-word prediction.

Use Android ClipboardManager for a text clipboard panel. Show the current clip on demand. Optional recent clips are off by default: at most ten, memory only, expire after one hour, capture only while the keyboard is visible, exclude sensitive clips, and provide clear/delete controls. Password fields cannot open the panel. No background clipboard monitor or disk persistence.

Programmer mode is a session toggle, retains the familiar letter flicks, uses straight punctuation, adds an operator/bracket layer and a shortcut row with four-space indentation, literal tab, paired delimiters, and cursor controls. Suppress prose suggestions and editor-driven capitalization while enabled. Pair insertion wraps selection and puts the cursor inside when editor selection is available. No language-specific code completion or IDE parser.

Credit u/thefizzynator prominently in app Credits and README with the canonical original Reddit post. Include engine and dictionary provenance/licenses in the APK and repository.

## Approaches

Builder's choice: JSymSpell gives an embeddable, zero-dependency Java correction engine and works offline across devices. Android's installed spell-checker service depends on device/provider configuration and offers corrections rather than a reliable built-in completion engine. AOSP LatinIME would require a much larger native dictionary integration; Gboard's proprietary engine has no supported embedding interface. Clipboard uses the Android platform API rather than another app's private history.

## Validation

Exercise case/typo/completion behavior, context-safe replacement, clipboard expiry/deduplication/sensitive clips, pairing/indentation in real EditText connections, and all existing gestures. Run unit tests, lint, optimized APK build, emulator tests and visual checks. Keep signing continuity and publish an installable 0.2.0 update to the existing GitHub repository.
