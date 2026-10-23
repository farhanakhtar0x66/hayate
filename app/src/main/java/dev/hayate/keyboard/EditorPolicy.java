package dev.hayate.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

final class EditorPolicy {
    static boolean privateField(EditorInfo info) {
        if (info == null) return true;
        int type = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        return type == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
            || type == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }
    static boolean suggestions(EditorInfo info) {
        if (privateField(info) || (info.inputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        return (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0 &&
            (info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == 0 &&
            variation != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS && variation != InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS &&
            variation != InputType.TYPE_TEXT_VARIATION_URI;
    }
}
