package co.tinode.tindroid;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import co.tinode.tindroid.format.SendForwardedFormatter;
import co.tinode.tindroid.format.SendReplyFormatter;
import co.tinode.tinodesdk.model.Drafty;

final class ComposerDraftController {
    private final Activity mActivity;
    private final EditText mEditor;
    private final View mReplyPreview;
    private final TextView mReplyPreviewText;
    private final TextView mForwardPreviewText;
    private final ComposerPanelController mPanelController;

    ComposerDraftController(@NonNull Activity activity,
                            @NonNull EditText editor,
                            @NonNull ComposerPanelController panelController) {
        mActivity = activity;
        mEditor = editor;
        mPanelController = panelController;
        mReplyPreview = activity.findViewById(R.id.replyPreviewWrapper);
        mReplyPreviewText = activity.findViewById(R.id.contentPreview);
        mForwardPreviewText = activity.findViewById(R.id.forwardedContentPreview);
    }

    void clearPreviewUi(boolean clearEditor, boolean editMode) {
        mReplyPreview.setVisibility(View.GONE);
        mPanelController.showPanel(ComposerPanelController.Panel.SEND);
        if (clearEditor) {
            mEditor.setText("");
        }
        if (editMode) {
            mPanelController.showInputMode(ComposerPanelController.InputMode.AUDIO);
        }
    }

    void showQuotedText(@NonNull UiUtils.MsgAction action,
                        @Nullable String original,
                        @NonNull Drafty quote,
                        boolean wasEditMode) {
        mPanelController.showPanel(ComposerPanelController.Panel.SEND);
        mReplyPreview.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(original)) {
            // Two steps: clear field, then append to move cursor to the end.
            mEditor.setText("");
            mEditor.append(original);
            mEditor.requestFocus();
            mPanelController.showInputMode(ComposerPanelController.InputMode.EDIT);
        } else {
            mPanelController.showInputMode(ComposerPanelController.InputMode.AUDIO);
            if (wasEditMode) {
                mEditor.setText("");
            }
        }

        mReplyPreviewText.setText(quote.format(new SendReplyFormatter(mReplyPreviewText)));
    }

    void showForwardedContent(@NonNull Drafty sender, @NonNull Drafty content) {
        mPanelController.showPanel(ComposerPanelController.Panel.FORWARD);
        Drafty preview = new Drafty()
                .append(sender)
                .appendLineBreak()
                .append(content.preview(Const.QUOTED_REPLY_LENGTH));
        mForwardPreviewText.setText(preview.format(new SendForwardedFormatter(mForwardPreviewText)));
    }

    void hideReplyPreview() {
        mReplyPreview.setVisibility(View.GONE);
    }
}
