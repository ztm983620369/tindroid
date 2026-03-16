package co.tinode.tindroid;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import co.tinode.tinodesdk.model.Drafty;

final class ComposerStateStore {
    private UiUtils.MsgAction mTextAction = UiUtils.MsgAction.NONE;
    private int mQuotedSeqId = -1;
    private Drafty mQuote;
    private Drafty mContentToForward;
    private Drafty mForwardSender;

    UiUtils.MsgAction getTextAction() {
        return mTextAction;
    }

    boolean isEditing() {
        return mTextAction == UiUtils.MsgAction.EDIT;
    }

    boolean hasQuotedContent() {
        return mQuote != null && mQuotedSeqId > 0;
    }

    int getQuotedSeqId() {
        return mQuotedSeqId;
    }

    @Nullable
    Drafty getQuote() {
        return mQuote;
    }

    boolean hasForwardContent() {
        return mContentToForward != null && mForwardSender != null;
    }

    @Nullable
    Drafty getContentToForward() {
        return mContentToForward;
    }

    @Nullable
    Drafty getForwardSender() {
        return mForwardSender;
    }

    void setQuotedState(@NonNull UiUtils.MsgAction action, @Nullable Drafty quote, int seqId) {
        mTextAction = action;
        mQuotedSeqId = seqId;
        mQuote = quote;
        mContentToForward = null;
        mForwardSender = null;
    }

    void setForwardState(@Nullable Drafty sender, @Nullable Drafty content) {
        mTextAction = UiUtils.MsgAction.FORWARD;
        mQuotedSeqId = -1;
        mQuote = null;
        mForwardSender = sender;
        mContentToForward = content;
    }

    void clearQuotedState() {
        if (mTextAction != UiUtils.MsgAction.FORWARD) {
            mTextAction = UiUtils.MsgAction.NONE;
        }
        mQuotedSeqId = -1;
        mQuote = null;
    }

    void clearForwardState() {
        if (mTextAction == UiUtils.MsgAction.FORWARD) {
            mTextAction = UiUtils.MsgAction.NONE;
        }
        mContentToForward = null;
        mForwardSender = null;
    }

    void clearAll() {
        mTextAction = UiUtils.MsgAction.NONE;
        mQuotedSeqId = -1;
        mQuote = null;
        mContentToForward = null;
        mForwardSender = null;
    }

    void saveToArgs(@NonNull Bundle args,
                    @NonNull String textActionKey,
                    @NonNull String quotedSeqKey,
                    @NonNull String quoteKey,
                    @NonNull String forwardContentKey,
                    @NonNull String forwardSenderKey) {
        args.putString(textActionKey, mTextAction.name());
        args.putInt(quotedSeqKey, mQuotedSeqId);
        args.putSerializable(quoteKey, mQuote);
        args.putSerializable(forwardContentKey, mContentToForward);
        args.putSerializable(forwardSenderKey, mForwardSender);
    }

    void restoreFromArgs(@NonNull Bundle args,
                         @NonNull String textActionKey,
                         @NonNull String quotedSeqKey,
                         @NonNull String quoteKey,
                         @NonNull String forwardContentKey,
                         @NonNull String forwardSenderKey) {
        String textAction = args.getString(textActionKey);
        mTextAction = TextUtils.isEmpty(textAction) ? UiUtils.MsgAction.NONE :
                UiUtils.MsgAction.valueOf(textAction);
        mQuotedSeqId = args.getInt(quotedSeqKey);
        mQuote = (Drafty) args.getSerializable(quoteKey);
        mContentToForward = (Drafty) args.getSerializable(forwardContentKey);
        mForwardSender = (Drafty) args.getSerializable(forwardSenderKey);
    }
}
