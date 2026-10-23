# Hayate privacy

Hayate 0.1.1 runs locally. Its manifest does not request the INTERNET permission.

Hayate does not collect, save, transmit, learn from, or log text you type, including passwords. The practice field is kept only in the current activity’s memory and is not saved to disk. No analytics, advertising, account, telemetry, or crash-reporting SDK is included.

Appearance, key height, flick distance and haptic preferences are stored in Android SharedPreferences. App backup and device transfer of those preferences are disabled.

The keyboard temporarily reads nearby editor text to delete Unicode characters, move the cursor, and apply the editor’s capitalization settings. It does not retain that text. Cut, copy and paste use Android’s clipboard only when you request them. Hayate does not maintain a clipboard history. Text committed to another app is governed by that app’s storage and privacy behavior.

The keyboard picker and enable flow are Android system interfaces. Android’s usual third-party-keyboard warning applies because any enabled keyboard can process text entered through it.
