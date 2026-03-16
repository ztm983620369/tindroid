package co.tinode.tindroid;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import co.tinode.tinodesdk.model.Drafty;

final class ComposerFacade {
    static final class SendSpec {
        final Drafty content;
        final int seqId;
        final boolean replacement;
        final boolean fromForward;
        final boolean fromQuote;

        SendSpec(@NonNull Drafty content, int seqId, boolean replacement,
                 boolean fromForward, boolean fromQuote) {
            this.content = content;
            this.seqId = seqId;
            this.replacement = replacement;
            this.fromForward = fromForward;
            this.fromQuote = fromQuote;
        }
    }

    private final ComposerController mComposerController;
    private final ComposerPanelController mPanelController;
    private final ComposerDraftController mDraftController;
    private final ComposerStateStore mStateStore;

    ComposerFacade(@NonNull MessageActivity activity,
                   @NonNull ViewGroup contentContainer,
                   @NonNull View trayHost,
                   @NonNull EditText editor,
                   @NonNull AppCompatImageButton audioButton,
                   @NonNull AppCompatImageButton sendButton,
                   @NonNull AppCompatImageButton editDoneButton) {
        mPanelController = new ComposerPanelController(activity, audioButton, sendButton, editDoneButton);
        mDraftController = new ComposerDraftController(activity, editor, mPanelController);
        mComposerController = new ComposerController(activity, contentContainer, trayHost, editor);
        mStateStore = new ComposerStateStore();
    }

    void dispatch(@NonNull ComposerEvent event) {
        switch (event.type) {
            case RESUME -> onResume();
            case MORE_CLICKED -> onMoreClick(event.anchor);
            case EDITOR_TOUCH_DOWN -> onEditorTouchDown(event.anchor);
            case EDITOR_CLICKED -> onEditorClick(event.anchor);
            case EDITOR_FOCUS_CHANGED -> onEditorFocusChanged(event.boolValue, event.anchor);
            case TEXT_CHANGED -> updateInputModeForText(event.text != null ? event.text : "");
            case HIDE_TRAY -> hideTray(event.reason != null ? event.reason : "", event.anchor);
            case AUDIO_RECORDING_STARTED -> onAudioRecordingStarted();
            case AUDIO_RECORDING_LOCKED -> onAudioRecordingLocked();
            case AUDIO_RECORDING_CANCELLED -> onAudioRecordingCancelled();
            case AUDIO_RECORDING_READY_TO_SEND -> onAudioRecordingReadyToSend();
            case TOPIC_STATE_CHANGED -> applyTopicState(event.boolValue, event.boolValue2, event.boolValue3);
            case CANCEL_PREVIEW -> cancelPreview();
            case BEGIN_EDITING -> beginEditing(event.original, event.primaryDraft, event.intValue);
            case BEGIN_REPLY -> beginReply(event.primaryDraft, event.intValue);
            case BEGIN_FORWARD -> beginForwardContent(event.primaryDraft, event.secondaryDraft);
        }
    }

    void onResume() {
        mComposerController.onResume();
    }

    void onMoreClick(@Nullable View anchor) {
        mComposerController.onMoreClick(anchor);
    }

    void onEditorTouchDown(@Nullable View anchor) {
        mComposerController.onEditorTouchDown(anchor);
    }

    void onEditorClick(@Nullable View anchor) {
        mComposerController.onEditorClick(anchor);
    }

    void onEditorFocusChanged(boolean hasFocus, @Nullable View anchor) {
        mComposerController.onEditorFocusChanged(hasFocus, anchor);
    }

    void hideTray(@NonNull String reason, @Nullable View anchor) {
        mComposerController.hideTray(reason, anchor);
    }

    boolean handleBackPressed() {
        return mComposerController.handleBackPressed();
    }

    void logExternalState(@NonNull String event, @Nullable View anchor) {
        mComposerController.logExternalState(event, anchor);
    }

    void showPanel(@NonNull ComposerPanelController.Panel panel) {
        mPanelController.showPanel(panel);
    }

    boolean applyTopicPresentation(boolean canWrite,
                                   boolean peerDisabled,
                                   @Nullable String pendingDraft,
                                   @NonNull EditText editor) {
        if (!canWrite) {
            showDisabledPanel();
            return false;
        }

        if (peerDisabled) {
            showPeerDisabledPanel();
            return false;
        }

        boolean consumedPendingDraft = pendingDraft == null;
        if (pendingDraft != null && editor.getText().length() == 0) {
            editor.append(pendingDraft);
        }
        if (pendingDraft != null && editor.getText().length() > 0) {
            consumedPendingDraft = true;
        }

        if (hasForwardContent()) {
            beginForwardContent(getForwardSender(), getForwardContent());
            return consumedPendingDraft;
        }

        if (hasQuotedContent()) {
            String original = isEditing() ? editor.getText().toString() : null;
            showQuotedText(mStateStore.getTextAction(), original, getQuote(), false);
            updateInputModeForText(editor.getText());
            return consumedPendingDraft;
        }

        showSendPanel();
        updateInputModeForText(editor.getText());
        return consumedPendingDraft;
    }

    void showSendPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.SEND);
    }

    void showDisabledPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.DISABLED);
    }

    void showPeerDisabledPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.PEER_DISABLED);
    }

    void showRecordPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.RECORD);
    }

    void showRecordShortPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.RECORD_SHORT);
    }

    void showForwardPanel() {
        mPanelController.showPanel(ComposerPanelController.Panel.FORWARD);
    }

    void onAudioRecordingStarted() {
        showRecordShortPanel();
    }

    void onAudioRecordingLocked() {
        showRecordPanel();
    }

    void onAudioRecordingCancelled() {
        showSendPanel();
        showInputMode(ComposerPanelController.InputMode.AUDIO);
    }

    void onAudioRecordingReadyToSend() {
        showSendPanel();
        showInputMode(ComposerPanelController.InputMode.AUDIO);
    }

    @NonNull
    ComposerPanelController.Panel getVisiblePanel() {
        return mPanelController.getVisiblePanel();
    }

    boolean isRecordPanelVisible() {
        return mPanelController.getVisiblePanel() == ComposerPanelController.Panel.RECORD;
    }

    boolean isRecordShortPanelVisible() {
        return mPanelController.getVisiblePanel() == ComposerPanelController.Panel.RECORD_SHORT;
    }

    void showInputMode(@NonNull ComposerPanelController.InputMode mode) {
        mPanelController.showInputMode(mode);
    }

    void updateInputModeForText(@NonNull CharSequence text) {
        if (isEditing()) {
            mPanelController.showInputMode(ComposerPanelController.InputMode.EDIT);
        } else if (text.length() > 0) {
            mPanelController.showInputMode(ComposerPanelController.InputMode.SEND);
        } else {
            mPanelController.showInputMode(ComposerPanelController.InputMode.AUDIO);
        }
    }

    boolean isEditing() {
        return mStateStore.isEditing();
    }

    boolean hasQuotedContent() {
        return mStateStore.hasQuotedContent();
    }

    int getQuotedSeqId() {
        return mStateStore.getQuotedSeqId();
    }

    @Nullable
    Drafty getQuote() {
        return mStateStore.getQuote();
    }

    boolean hasForwardContent() {
        return mStateStore.hasForwardContent();
    }

    @Nullable
    Drafty getForwardSender() {
        return mStateStore.getForwardSender();
    }

    @Nullable
    Drafty getForwardContent() {
        return mStateStore.getContentToForward();
    }

    void clearForwardState() {
        mStateStore.clearForwardState();
    }

    void clearQuotedState() {
        mStateStore.clearQuotedState();
    }

    void clearAllState() {
        mStateStore.clearAll();
    }

    void setQuotedState(@NonNull UiUtils.MsgAction action, @Nullable Drafty quote, int seqId) {
        mStateStore.setQuotedState(action, quote, seqId);
    }

    void setForwardState(@Nullable Drafty sender, @Nullable Drafty content) {
        mStateStore.setForwardState(sender, content);
    }

    void saveStateToArgs(@NonNull Bundle args,
                         @NonNull String textActionKey,
                         @NonNull String quotedSeqKey,
                         @NonNull String quoteKey,
                         @NonNull String forwardContentKey,
                         @NonNull String forwardSenderKey) {
        mStateStore.saveToArgs(args, textActionKey, quotedSeqKey, quoteKey, forwardContentKey, forwardSenderKey);
    }

    void restoreStateFromArgs(@NonNull Bundle args,
                              @NonNull String textActionKey,
                              @NonNull String quotedSeqKey,
                              @NonNull String quoteKey,
                              @NonNull String forwardContentKey,
                              @NonNull String forwardSenderKey) {
        mStateStore.restoreFromArgs(args, textActionKey, quotedSeqKey, quoteKey,
                forwardContentKey, forwardSenderKey);
    }

    void hideReplyPreview() {
        mDraftController.hideReplyPreview();
    }

    void clearPreviewUi(boolean clearEditor, boolean editMode) {
        mDraftController.clearPreviewUi(clearEditor, editMode);
    }

    void clearPreviewState(boolean editMode) {
        mDraftController.clearPreviewUi(editMode, editMode);
        mStateStore.clearAll();
    }

    void showQuotedText(@NonNull UiUtils.MsgAction action,
                        @Nullable String original,
                        @NonNull Drafty quote,
                        boolean wasEditMode) {
        mDraftController.showQuotedText(action, original, quote, wasEditMode);
    }

    void beginQuotedContent(@NonNull UiUtils.MsgAction action,
                            @Nullable String original,
                            @NonNull Drafty quote,
                            int seqId) {
        boolean wasEditMode = isEditing();
        mStateStore.setQuotedState(action, quote, seqId);
        mDraftController.showQuotedText(action, original, quote, wasEditMode);
    }

    void showForwardedContent(@NonNull Drafty sender, @NonNull Drafty content) {
        mDraftController.showForwardedContent(sender, content);
    }

    void beginForwardContent(@NonNull Drafty sender, @NonNull Drafty content) {
        mStateStore.setForwardState(sender, content);
        mDraftController.showForwardedContent(sender, content);
    }

    void applyTopicState(boolean canWrite, boolean hasForwardContent, boolean peerDisabled) {
        if (!canWrite) {
            showDisabledPanel();
        } else if (hasForwardContent) {
            showForwardPanel();
        } else if (peerDisabled) {
            showPeerDisabledPanel();
        } else {
            showSendPanel();
        }
    }

    void cancelPreview() {
        clearPreviewState(isEditing());
    }

    void beginEditing(@Nullable String original, @NonNull Drafty quote, int seqId) {
        beginQuotedContent(UiUtils.MsgAction.EDIT, original, quote, seqId);
    }

    void beginReply(@NonNull Drafty quote, int seqId) {
        beginQuotedContent(UiUtils.MsgAction.REPLY, null, quote, seqId);
    }

    @Nullable
    SendSpec createSendSpec(@NonNull String message) {
        if (hasForwardContent()) {
            return new SendSpec(
                    getForwardSender().appendLineBreak().append(getForwardContent()),
                    -1,
                    false,
                    true,
                    false
            );
        }

        if (message.isEmpty()) {
            return null;
        }

        Drafty msg = Drafty.parse(message);
        boolean replacement = isEditing();
        boolean fromQuote = hasQuotedContent() && !replacement;
        if (fromQuote) {
            msg = getQuote().append(msg);
        }
        return new SendSpec(msg, getQuotedSeqId(), replacement, false, fromQuote);
    }

    void onSendCommitted(@NonNull SendSpec spec) {
        if (spec.fromForward) {
            clearForwardState();
            showSendPanel();
            return;
        }
        if (hasQuotedContent()) {
            clearQuotedState();
            hideReplyPreview();
        }
    }
}
