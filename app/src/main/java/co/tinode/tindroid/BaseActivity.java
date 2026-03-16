package co.tinode.tindroid;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class BaseActivity extends AppCompatActivity {
    private static final String TRACE_TAG = "ImeTrace";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // On SDK 35+, edge-to-edge is enforced by default and EdgeToEdge.enable()
        // uses deprecated setStatusBarColor/setNavigationBarColor APIs.
        // Only call it on older versions for backward compatibility.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (BuildConfig.DEBUG) {
            Log.d(TRACE_TAG, getClass().getSimpleName() + " onCreate decorFits=false softInputMode="
                    + getWindow().getAttributes().softInputMode);
        }
    }

    protected void applyEdgeToEdgeInsets(View rootView) {
        applyEdgeToEdgeInsets(rootView, false, true);
    }

    protected void applyEdgeToEdgeInsets(View rootView, boolean includeIme, boolean consumeInsets) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        View topInsetTarget = null;
        if (toolbar != null) {
            View parent = (View) toolbar.getParent();
            topInsetTarget = parent != null ? parent : toolbar;
        }
        final View finalTopInsetTarget = topInsetTarget;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            int types = WindowInsetsCompat.Type.systemBars();
            if (includeIme) {
                types |= WindowInsetsCompat.Type.ime();
            }
            Insets insets = windowInsets.getInsets(types);
            if (BuildConfig.DEBUG) {
                Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                Insets sysInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                Log.d(TRACE_TAG, getClass().getSimpleName()
                        + " applyInsets includeIme=" + includeIme
                        + " consume=" + consumeInsets
                        + " root=" + viewName(rootView)
                        + " sys=[" + sysInsets.left + "," + sysInsets.top + "," + sysInsets.right + "," + sysInsets.bottom + "]"
                        + " ime=[" + imeInsets.left + "," + imeInsets.top + "," + imeInsets.right + "," + imeInsets.bottom + "]"
                        + " applied=[" + insets.left + "," + insets.top + "," + insets.right + "," + insets.bottom + "]"
                        + " imeVisible=" + windowInsets.isVisible(WindowInsetsCompat.Type.ime()));
            }
            v.setPadding(insets.left, finalTopInsetTarget != null ? 0 : insets.top, insets.right, insets.bottom);
            if (finalTopInsetTarget != null) {
                finalTopInsetTarget.setPadding(
                        finalTopInsetTarget.getPaddingLeft(),
                        insets.top,
                        finalTopInsetTarget.getPaddingRight(),
                        finalTopInsetTarget.getPaddingBottom()
                );
            }
            return consumeInsets ? WindowInsetsCompat.CONSUMED : windowInsets;
        });
    }

    private String viewName(View view) {
        if (view == null) {
            return "null";
        }
        if (view.getId() == View.NO_ID) {
            return view.getClass().getSimpleName() + "(no-id)";
        }
        try {
            return getResources().getResourceEntryName(view.getId());
        } catch (Exception ignored) {
            return view.getClass().getSimpleName() + "(" + view.getId() + ")";
        }
    }
}
