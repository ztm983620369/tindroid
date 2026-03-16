package co.tinode.tindroid;

import android.app.Activity;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;

final class ComposerPanelController {
    enum Panel {
        SEND(R.id.sendMessagePanel),
        DISABLED(R.id.sendMessageDisabled),
        PEER_DISABLED(R.id.peersMessagingDisabled),
        RECORD(R.id.recordAudioPanel),
        RECORD_SHORT(R.id.recordAudioShortPanel),
        FORWARD(R.id.forwardMessagePanel);

        final int viewId;

        Panel(@IdRes int viewId) {
            this.viewId = viewId;
        }
    }

    enum InputMode {
        AUDIO,
        SEND,
        EDIT
    }

    private final Activity mActivity;
    private final AppCompatImageButton mAudioButton;
    private final AppCompatImageButton mSendButton;
    private final AppCompatImageButton mEditDoneButton;

    private Panel mVisiblePanel = Panel.SEND;

    ComposerPanelController(@NonNull Activity activity,
                            @NonNull AppCompatImageButton audioButton,
                            @NonNull AppCompatImageButton sendButton,
                            @NonNull AppCompatImageButton editDoneButton) {
        mActivity = activity;
        mAudioButton = audioButton;
        mSendButton = sendButton;
        mEditDoneButton = editDoneButton;
    }

    Panel getVisiblePanel() {
        return mVisiblePanel;
    }

    void showPanel(@NonNull Panel panel) {
        if (mVisiblePanel == panel) {
            return;
        }
        mActivity.findViewById(panel.viewId).setVisibility(View.VISIBLE);
        mActivity.findViewById(mVisiblePanel.viewId).setVisibility(View.GONE);
        mVisiblePanel = panel;
    }

    void showInputMode(@NonNull InputMode mode) {
        mAudioButton.setVisibility(mode == InputMode.AUDIO ? View.VISIBLE : View.INVISIBLE);
        mSendButton.setVisibility(mode == InputMode.SEND ? View.VISIBLE : View.INVISIBLE);
        mEditDoneButton.setVisibility(mode == InputMode.EDIT ? View.VISIBLE : View.INVISIBLE);
    }
}
