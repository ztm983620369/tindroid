package co.tinode.tindroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

final class ComposerController {
    private static final String TRACE_TAG = "ImeTrace";
    private static final long TRAY_TOGGLE_POLL_DELAY_MS = 32L;
    private static final int TRAY_TOGGLE_POLL_MAX = 12;
    private static final String PREF_LAST_IME_HEIGHT = "last_ime_height";

    enum Mode {
        COLLAPSED,
        KEYBOARD,
        TRAY
    }

    private final MessageActivity mActivity;
    private final ViewGroup mContentContainer;
    private final View mTrayHost;
    private final EditText mEditor;
    private final SharedPreferences mPreferences;

    private Mode mMode = Mode.COLLAPSED;
    private int mLastImeHeight = 0;

    ComposerController(@NonNull MessageActivity activity,
                       @NonNull ViewGroup contentContainer,
                       @NonNull View trayHost,
                       @NonNull EditText editor) {
        mActivity = activity;
        mContentContainer = contentContainer;
        mTrayHost = trayHost;
        mEditor = editor;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        mLastImeHeight = mPreferences.getInt(PREF_LAST_IME_HEIGHT, 0);

        if (BuildConfig.DEBUG) {
            mTrayHost.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom) ->
                    logState("trayHost layout oldH=" + (oldBottom - oldTop) + " newH=" + (bottom - top), v));
            mEditor.addOnLayoutChangeListener((v, left, top, right, bottom,
                                              oldLeft, oldTop, oldRight, oldBottom) ->
                    logState("editor layout oldH=" + (oldBottom - oldTop) + " newH=" + (bottom - top), v));
        }
    }

    boolean isTrayVisible() {
        return mMode == Mode.TRAY;
    }

    void onResume() {
        if (isTrayVisible() && isImeVisible(mEditor)) {
            setTrayVisible(false, "resume/imeVisible", mEditor);
            setMode(Mode.KEYBOARD, "resume/imeVisible", mEditor);
        }
        logState("onResume", mEditor);
    }

    void onEditorTouchDown(@Nullable View anchor) {
        logState("editor touchDown", anchor);
    }

    void onEditorClick(@Nullable View anchor) {
        logState("editor click", anchor);
        requestKeyboardMode("editorClick", anchor);
    }

    void onEditorFocusChanged(boolean hasFocus, @Nullable View anchor) {
        logState("editor focusChanged hasFocus=" + hasFocus, anchor);
    }

    void onMoreClick(@Nullable View anchor) {
        logState("more click", anchor);
        if (isTrayVisible()) {
            requestKeyboardMode("toggleFromTray", anchor);
        } else {
            requestTrayMode(anchor);
        }
    }

    void hideTray(@NonNull String reason, @Nullable View anchor) {
        if (isTrayVisible()) {
            setTrayVisible(false, reason, anchor);
        }
    }

    boolean handleBackPressed() {
        if (isTrayVisible()) {
            setEditorSoftInputEnabled(true, mEditor);
            setTrayVisible(false, "backPressed/hideTray", mEditor);
            return true;
        }
        if (isImeVisible(mEditor)) {
            setEditorSoftInputEnabled(true, mEditor);
            hideSoftKeyboard();
            setMode(Mode.COLLAPSED, "backPressed/hideIme", mEditor);
            return true;
        }
        return false;
    }

    void logExternalState(@NonNull String event, @Nullable View anchor) {
        logState(event, anchor);
    }

    private void requestTrayMode(@Nullable View anchor) {
        if (isImeVisible(mEditor)) {
            captureImeHeight();
            suppressLayout(true, "keyboard->tray start");
            prepareEditorForTray(true, anchor);
            waitForImeToHideThenShowTray(TRAY_TOGGLE_POLL_MAX);
        } else {
            prepareEditorForTray(false, anchor);
            setTrayVisible(true, "requestTrayMode/direct", anchor);
        }
    }

    private void requestKeyboardMode(@NonNull String reason, @Nullable View anchor) {
        if (isTrayVisible()) {
            suppressLayout(true, "tray->keyboard start");
            setTrayVisible(false, "requestKeyboardMode/hideTray", anchor);
        }

        if (isImeVisible(mEditor)) {
            setEditorSoftInputEnabled(true, anchor);
            mEditor.requestFocus();
            setMode(Mode.KEYBOARD, "imeAlreadyVisible/" + reason, anchor);
            suppressLayout(false, "keyboard already visible");
            return;
        }

        showSoftKeyboard(anchor);
        waitForImeToShowThenReleaseLayout(TRAY_TOGGLE_POLL_MAX);
    }

    private void setTrayVisible(boolean visible, @NonNull String reason, @Nullable View anchor) {
        if (isTrayVisible() == visible) {
            logState("setTrayVisible noop visible=" + visible + " reason=" + reason, anchor);
            return;
        }
        ViewGroup.LayoutParams params = mTrayHost.getLayoutParams();
        params.height = visible ? resolveTrayHeight() : 0;
        mTrayHost.setLayoutParams(params);
        setMode(visible ? Mode.TRAY : Mode.COLLAPSED, reason, anchor);
        logState("setTrayVisible visible=" + visible + " reason=" + reason, anchor);
    }

    private void showSoftKeyboard(@Nullable View anchor) {
        logState("showSoftKeyboard request", anchor != null ? anchor : mEditor);
        setEditorSoftInputEnabled(true, anchor);
        mEditor.requestFocus();
        mEditor.post(() -> {
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(mActivity.getWindow(), mEditor);
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.ime());
            }
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mEditor, InputMethodManager.SHOW_IMPLICIT);
            }
            logState("showSoftKeyboard posted", mEditor);
        });
    }

    private void hideSoftKeyboard() {
        View focus = mActivity.getCurrentFocus();
        if (focus == null) {
            focus = mEditor;
        }
        logState("hideSoftKeyboard request", focus);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(mActivity.getWindow(), focus);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void prepareEditorForTray(boolean hideIme, @Nullable View anchor) {
        setEditorSoftInputEnabled(false, anchor);
        setMode(Mode.COLLAPSED, "prepareEditorForTray", anchor);
        if (hideIme) {
            hideSoftKeyboard();
        }
        logState("prepareEditorForTray hideIme=" + hideIme, anchor != null ? anchor : mEditor);
    }

    private void waitForImeToHideThenShowTray(int attemptsLeft) {
        if (getImeInsetBottom() == 0 || attemptsLeft <= 0) {
            setTrayVisible(true, "waitForImeToHideThenShowTray", mEditor);
            suppressLayout(false, "keyboard->tray done");
            return;
        }
        logState("waitForImeToHideThenShowTray poll attemptsLeft=" + attemptsLeft, mEditor);
        mTrayHost.postDelayed(() -> waitForImeToHideThenShowTray(attemptsLeft - 1), TRAY_TOGGLE_POLL_DELAY_MS);
    }

    private void waitForImeToShowThenReleaseLayout(int attemptsLeft) {
        if (getImeInsetBottom() > 0 || attemptsLeft <= 0) {
            setMode(Mode.KEYBOARD, "waitForImeToShowThenReleaseLayout", mEditor);
            suppressLayout(false, "tray->keyboard done");
            return;
        }
        logState("waitForImeToShowThenReleaseLayout poll attemptsLeft=" + attemptsLeft, mEditor);
        mEditor.postDelayed(() -> waitForImeToShowThenReleaseLayout(attemptsLeft - 1), TRAY_TOGGLE_POLL_DELAY_MS);
    }

    private void suppressLayout(boolean suppress, @NonNull String reason) {
        mContentContainer.suppressLayout(suppress);
        logState("suppressLayout=" + suppress + " reason=" + reason, mContentContainer);
    }

    private void setEditorSoftInputEnabled(boolean enabled, @Nullable View anchor) {
        mEditor.setShowSoftInputOnFocus(enabled);
        logState("setEditorSoftInputEnabled=" + enabled, anchor != null ? anchor : mEditor);
    }

    private boolean isImeVisible(@Nullable View anchor) {
        return getImeInsetBottom(anchor) > 0;
    }

    private int getImeInsetBottom() {
        return getImeInsetBottom(mEditor);
    }

    private int getImeInsetBottom(@Nullable View anchor) {
        if (anchor == null) {
            return 0;
        }
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(anchor);
        return insets != null ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
    }

    private void captureImeHeight() {
        int imeHeight = getImeInsetBottom();
        if (imeHeight > 0) {
            mLastImeHeight = imeHeight;
            mPreferences.edit().putInt(PREF_LAST_IME_HEIGHT, imeHeight).apply();
            logState("captureImeHeight=" + imeHeight, mEditor);
        }
    }

    private int resolveTrayHeight() {
        if (mLastImeHeight > 0) {
            return mLastImeHeight;
        }

        float density = mActivity.getResources().getDisplayMetrics().density;
        int stored = mPreferences.getInt(PREF_LAST_IME_HEIGHT, 0);
        if (stored > 0) {
            mLastImeHeight = stored;
            return stored;
        }

        int minHeight = (int) (280 * density);
        int maxHeight = (int) (380 * density);
        View root = mActivity.findViewById(android.R.id.content);
        int rootHeight = root != null ? root.getHeight() : 0;
        if (rootHeight > 0) {
            int estimated = Math.round(rootHeight * 0.36f);
            return Math.max(minHeight, Math.min(maxHeight, estimated));
        }
        return (int) (320 * density);
    }

    private void setMode(@NonNull Mode mode, @NonNull String reason, @Nullable View anchor) {
        if (mMode == mode) {
            return;
        }
        mMode = mode;
        logState("setMode=" + mode + " reason=" + reason, anchor);
    }

    private void logState(@NonNull String event, @Nullable View anchor) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(anchor != null ? anchor : mEditor);
        int imeBottom = insets != null ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : -1;
        boolean imeVisible = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        ViewGroup.LayoutParams lp = mTrayHost.getLayoutParams();
        View rootContent = mActivity.findViewById(android.R.id.content);
        Log.d(TRACE_TAG, "ComposerController " + event
                + " mode=" + mMode
                + " trayLpH=" + (lp != null ? lp.height : -1)
                + " trayH=" + mTrayHost.getHeight()
                + " imeBottom=" + imeBottom
                + " imeVisible=" + imeVisible
                + " lastImeHeight=" + mLastImeHeight
                + " focus=" + describeView(mActivity.getCurrentFocus())
                + " anchor=" + describeView(anchor)
                + " editorFocus=" + mEditor.hasFocus()
                + " editorShowImeOnFocus=" + mEditor.getShowSoftInputOnFocus()
                + " rootPadBottom=" + (rootContent != null ? rootContent.getPaddingBottom() : -1));
    }

    private String describeView(@Nullable View view) {
        if (view == null) {
            return "null";
        }
        return viewName(view) + "/" + view.getClass().getSimpleName()
                + " vis=" + visibilityName(view.getVisibility())
                + " focused=" + view.hasFocus()
                + " h=" + view.getHeight()
                + " w=" + view.getWidth();
    }

    private String viewName(@NonNull View view) {
        if (view.getId() == View.NO_ID) {
            return "no-id";
        }
        try {
            return mActivity.getResources().getResourceEntryName(view.getId());
        } catch (Exception ignored) {
            return String.valueOf(view.getId());
        }
    }

    private String visibilityName(int visibility) {
        return switch (visibility) {
            case View.VISIBLE -> "VISIBLE";
            case View.INVISIBLE -> "INVISIBLE";
            case View.GONE -> "GONE";
            default -> String.valueOf(visibility);
        };
    }
}
