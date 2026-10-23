package dev.hayate.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

final class Preferences {
    private final SharedPreferences values;
    Preferences(Context context) { values = context.getSharedPreferences("appearance", Context.MODE_PRIVATE); }
    String theme() { return values.getString("theme", "system"); }
    void theme(String theme) { values.edit().putString("theme", theme).apply(); }
    boolean haptics() { return values.getBoolean("haptics", true); }
    void haptics(boolean enabled) { values.edit().putBoolean("haptics", enabled).apply(); }
    int threshold() { return values.getInt("threshold", 18); }
    void threshold(int dp) { values.edit().putInt("threshold", dp).apply(); }
    int height() { return values.getInt("height", 78); }
    void height(int dp) { values.edit().putInt("height", dp).apply(); }
}
