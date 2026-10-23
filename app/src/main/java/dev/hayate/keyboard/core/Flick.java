package dev.hayate.keyboard.core;

public enum Flick {
    CENTER, LEFT, UP, RIGHT, DOWN;
    public static Flick resolve(float dx, float dy, float threshold) {
        if (Math.max(Math.abs(dx), Math.abs(dy)) < threshold) return CENTER;
        if (Math.abs(dx) >= Math.abs(dy)) return dx < 0 ? LEFT : RIGHT;
        return dy < 0 ? UP : DOWN;
    }
}
