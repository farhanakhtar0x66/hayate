package dev.hayate.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

final class Palette {
    final boolean dark;
    final int background, surface, control, ink, muted, accent, accentInk, line;
    Palette(Context context, Preferences preferences) {
        dark = preferences.theme().equals("dark") || (preferences.theme().equals("system") &&
            (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        background = color(dark ? "#19181F" : "#F6F5FA");
        surface = color(dark ? "#2D2B35" : "#FFFFFF");
        control = color(dark ? "#24222B" : "#EAE7F0");
        ink = color(dark ? "#F5F1FB" : "#24232B");
        muted = color(dark ? "#B4ADBF" : "#696273");
        accent = color("#D8C6FA");
        accentInk = color("#3E2C58");
        line = color(dark ? "#3A3644" : "#DED9E7");
    }
    static int color(String hex) { return Color.parseColor(hex); }
}
