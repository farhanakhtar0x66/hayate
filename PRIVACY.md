# Hayate privacy

Hayate 0.2.0 runs locally. Its manifest does not request the INTERNET permission. No analytics, advertising, account, telemetry, or crash-reporting SDK is included.

Hayate does not save, transmit, log, or learn from the text you type. Nearby editor text is temporarily read to offer word suggestions, replace a chosen candidate, delete Unicode characters, move the cursor, carry code indentation and follow capitalization settings. Suggestion requests and snapshots are temporary memory only. Password fields suppress suggestions and the clipboard panel. Suggestions also respect no-suggestions/private-learning flags and are disabled in programmer mode. The bundled JSymSpell engine and dictionary work offline; there is no remote spell-checker provider.

The practice field is kept only in the current activity’s memory and is not saved to disk. Text committed to another app is governed by that app’s storage and privacy behavior.

The clipboard panel reads Android’s current text clip when opened. Clips marked sensitive are not previewed or retained in history; the current sensitive clip can be pasted explicitly through Android’s standard Paste action. Hayate does not preview image/file clips. Clear also clears Android’s current clip.

**Keep recent clips is optional and off by default.** If enabled, Hayate captures ordinary text clips only while its keyboard or practice area is visible, keeps at most ten clips of at most 10,000 characters each in process memory, and expires them after one hour. Turning it off clears the recent list. Nothing is written to disk or backed up. The list disappears when Android ends the app process. Clipboard listeners stop when the keyboard/activity is hidden. Android controls availability and retention of the current system clip separately. Hayate cannot read another keyboard’s private clipboard history.

Appearance, key height, flick distance, haptics, suggestion visibility and the recent-clip toggle are stored in Android SharedPreferences. App backup and device transfer are disabled.

The keyboard picker and enable flow are Android system interfaces. Android’s usual third-party-keyboard warning applies because any enabled keyboard can process text entered through it. Credits links open your browser only when tapped; the browser has its own network and privacy behavior.
