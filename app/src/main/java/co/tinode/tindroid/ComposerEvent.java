package co.tinode.tindroid;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import co.tinode.tinodesdk.model.Drafty;

final class ComposerEvent {
    enum Type {
        RESUME,
        MORE_CLICKED,
        EDITOR_TOUCH_DOWN,
        EDITOR_CLICKED,
        EDITOR_FOCUS_CHANGED,
        TEXT_CHANGED,
        HIDE_TRAY,
        AUDIO_RECORDING_STARTED,
        AUDIO_RECORDING_LOCKED,
        AUDIO_RECORDING_CANCELLED,
        AUDIO_RECORDING_READY_TO_SEND,
        TOPIC_STATE_CHANGED,
        CANCEL_PREVIEW,
        BEGIN_EDITING,
        BEGIN_REPLY,
        BEGIN_FORWARD
    }

    final Type type;
    @Nullable final View anchor;
    @Nullable final CharSequence text;
    @Nullable final String reason;
    @Nullable final String original;
    @Nullable final Drafty primaryDraft;
    @Nullable final Drafty secondaryDraft;
    final int intValue;
    final boolean boolValue;
    final boolean boolValue2;
    final boolean boolValue3;

    private ComposerEvent(@NonNull Type type,
                          @Nullable View anchor,
                          @Nullable CharSequence text,
                          @Nullable String reason,
                          @Nullable String original,
                          @Nullable Drafty primaryDraft,
                          @Nullable Drafty secondaryDraft,
                          int intValue,
                          boolean boolValue,
                          boolean boolValue2,
                          boolean boolValue3) {
        this.type = type;
        this.anchor = anchor;
        this.text = text;
        this.reason = reason;
        this.original = original;
        this.primaryDraft = primaryDraft;
        this.secondaryDraft = secondaryDraft;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.boolValue2 = boolValue2;
        this.boolValue3 = boolValue3;
    }

    static ComposerEvent onResume() {
        return new ComposerEvent(Type.RESUME, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent moreClicked(@Nullable View anchor) {
        return new ComposerEvent(Type.MORE_CLICKED, anchor, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent editorTouchDown(@Nullable View anchor) {
        return new ComposerEvent(Type.EDITOR_TOUCH_DOWN, anchor, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent editorClicked(@Nullable View anchor) {
        return new ComposerEvent(Type.EDITOR_CLICKED, anchor, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent editorFocusChanged(boolean hasFocus, @Nullable View anchor) {
        return new ComposerEvent(Type.EDITOR_FOCUS_CHANGED, anchor, null, null, null, null, null,
                0, hasFocus, false, false);
    }

    static ComposerEvent textChanged(@NonNull CharSequence text) {
        return new ComposerEvent(Type.TEXT_CHANGED, null, text, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent hideTray(@NonNull String reason, @Nullable View anchor) {
        return new ComposerEvent(Type.HIDE_TRAY, anchor, null, reason, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent audioRecordingStarted() {
        return new ComposerEvent(Type.AUDIO_RECORDING_STARTED, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent audioRecordingLocked() {
        return new ComposerEvent(Type.AUDIO_RECORDING_LOCKED, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent audioRecordingCancelled() {
        return new ComposerEvent(Type.AUDIO_RECORDING_CANCELLED, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent audioRecordingReadyToSend() {
        return new ComposerEvent(Type.AUDIO_RECORDING_READY_TO_SEND, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent topicStateChanged(boolean canWrite, boolean hasForwardContent, boolean peerDisabled) {
        return new ComposerEvent(Type.TOPIC_STATE_CHANGED, null, null, null, null, null, null,
                0, canWrite, hasForwardContent, peerDisabled);
    }

    static ComposerEvent cancelPreview() {
        return new ComposerEvent(Type.CANCEL_PREVIEW, null, null, null, null, null, null,
                0, false, false, false);
    }

    static ComposerEvent beginEditing(@Nullable String original, @NonNull Drafty quote, int seqId) {
        return new ComposerEvent(Type.BEGIN_EDITING, null, null, null, original, quote, null,
                seqId, false, false, false);
    }

    static ComposerEvent beginReply(@NonNull Drafty quote, int seqId) {
        return new ComposerEvent(Type.BEGIN_REPLY, null, null, null, null, quote, null,
                seqId, false, false, false);
    }

    static ComposerEvent beginForward(@NonNull Drafty sender, @NonNull Drafty content) {
        return new ComposerEvent(Type.BEGIN_FORWARD, null, null, null, null, sender, content,
                0, false, false, false);
    }
}
