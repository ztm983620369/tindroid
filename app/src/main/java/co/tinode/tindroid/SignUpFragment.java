package co.tinode.tindroid;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import co.tinode.tindroid.account.Utils;
import co.tinode.tindroid.widgets.AttachmentPickerDialog;
import co.tinode.tinodesdk.PocketBaseAuth;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.ServerMessage;

/**
 * Fragment for managing registration of a new account.
 */
public class SignUpFragment extends Fragment
        implements View.OnClickListener, UtilsMedia.MediaPreviewer, MenuProvider {

    private static final String TAG ="SignUpFragment";

    private final ActivityResultLauncher<PickVisualMediaRequest> mRequestAvatarLauncher =
            UtilsMedia.pickMediaLauncher(this, this);

    private final ActivityResultLauncher<Void> mThumbTakePhotoLauncher =
            UtilsMedia.takePreviewPhotoLauncher(this, this);

    private final ActivityResultLauncher<String> mRequestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    mThumbTakePhotoLauncher.launch(null);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();

        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.sign_up);
        }

        View fragment = inflater.inflate(R.layout.fragment_signup, container, false);

        // Get avatar from the gallery or photo camera.
        fragment.findViewById(R.id.uploadAvatar).setOnClickListener(v ->
                new AttachmentPickerDialog.Builder().
                    setGalleryLauncher(mRequestAvatarLauncher).
                    setCameraPreviewLauncher(mThumbTakePhotoLauncher, mRequestCameraPermissionLauncher).
                    build().
                    show(getChildFragmentManager()));
        // Handle click on the sign up button.
        fragment.findViewById(R.id.signUp).setOnClickListener(this);

        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final LoginActivity parent = (LoginActivity) requireActivity();
        if (parent.isFinishing() || parent.isDestroyed()) {
            return;
        }

        parent.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        AvatarViewModel avatarVM = new ViewModelProvider(parent).get(AvatarViewModel.class);
        avatarVM.getAvatar().observe(getViewLifecycleOwner(), bmp ->
            UiUtils.acceptAvatar(parent, parent.findViewById(R.id.imageAvatar), bmp)
        );
    }

    /**
     * Create new account.
     */
    @Override
    public void onClick(View v) {
        final LoginActivity parent = (LoginActivity) requireActivity();
        if (parent.isFinishing() || parent.isDestroyed()) {
            return;
        }

        final String email = ((EditText) parent.findViewById(R.id.newLogin)).getText().toString().trim();
        if (email.isEmpty()) {
            ((EditText) parent.findViewById(R.id.newLogin)).setError(getText(R.string.email_required));
            return;
        }

        final String password = ((EditText) parent.findViewById(R.id.newPassword)).getText().toString().trim();
        if (password.isEmpty()) {
            ((EditText) parent.findViewById(R.id.newPassword)).setError(getText(R.string.password_required));
            return;
        }

        final String passwordConfirm = ((EditText) parent.findViewById(R.id.newPasswordConfirm)).getText().toString().trim();
        if (passwordConfirm.isEmpty()) {
            ((EditText) parent.findViewById(R.id.newPasswordConfirm)).setError(getText(R.string.password_required));
            return;
        }
        if (!password.equals(passwordConfirm)) {
            ((EditText) parent.findViewById(R.id.newPasswordConfirm)).setError(getText(R.string.password_mismatch));
            return;
        }

        String fn = ((EditText) parent.findViewById(R.id.fullName)).getText().toString().trim();
        if (fn.isEmpty()) {
            ((EditText) parent.findViewById(R.id.fullName)).setError(getText(R.string.full_name_required));
            return;
        }
        final String fullName = fn.length() > Const.MAX_TITLE_LENGTH ?
                fn.substring(0, Const.MAX_TITLE_LENGTH) : fn;

        final Button signUp = parent.findViewById(R.id.signUp);
        signUp.setEnabled(false);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(parent);
        @SuppressLint("UnsafeOptInUsageError")
        String hostName = sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName());
        @SuppressLint("UnsafeOptInUsageError")
        boolean tls = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS());

        final Tinode tinode = Cache.getTinode();
        PocketBaseAuth.SignUpRequest request = new PocketBaseAuth.SignUpRequest(
                email, password, passwordConfirm, fullName, false);

        // This is called on the websocket thread.
        tinode.signUpAndLoginPocketBase(hostName, tls, false, request)
                .thenApply(new PromisedReply.SuccessListener<>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(final ServerMessage msg) {
                                sharedPref.edit().putString(LoginActivity.PREFS_LAST_LOGIN, email).apply();
                                UiUtils.updateAndroidAccount(parent, tinode.getMyId(),
                                        PocketBaseAuth.encodeAccountSecret(email, password),
                                        tinode.getAuthToken(), tinode.getAuthTokenExpiration());

                                // Remove used avatar from the view model.
                                new ViewModelProvider(parent).get(AvatarViewModel.class).clear();

                                // Flip back to login screen on success;
                                parent.runOnUiThread(() -> {
                                    if (msg.ctrl.code >= 300 && msg.ctrl.text.contains("validate credentials")) {
                                        signUp.setEnabled(true);
                                        parent.showFragment(LoginActivity.FRAGMENT_CREDENTIALS, null);
                                    } else {
                                        // We are requesting immediate login with the new account.
                                        // If the action succeeded, assume we have logged in.
                                        tinode.setAutoLoginToken(tinode.getAuthToken());
                                        UiUtils.onLoginSuccess(parent, signUp, tinode.getMyId());
                                    }
                                });
                                return null;
                            }
                        })
                .thenCatch(new PromisedReply.FailureListener<>() {
                            @Override
                            public PromisedReply<ServerMessage> onFailure(Exception err) {
                                if (!SignUpFragment.this.isVisible() || parent.isFinishing() || parent.isDestroyed()) {
                                    return null;
                                }
                                parent.runOnUiThread(() -> {
                                    signUp.setEnabled(true);
                                    Log.w(TAG, "Failed create account", err);
                                    Toast.makeText(parent, parent.getString(R.string.action_failed),
                                            Toast.LENGTH_SHORT).show();
                                });
                                parent.reportError(err, signUp, 0, R.string.error_new_account_failed);
                                return null;
                            }
                        });
    }

    @Override
    public void handleMedia(Bundle args) {
        final FragmentActivity activity = requireActivity();
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        ((LoginActivity) activity).showFragment(LoginActivity.FRAGMENT_AVATAR_PREVIEW, args);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }
}
