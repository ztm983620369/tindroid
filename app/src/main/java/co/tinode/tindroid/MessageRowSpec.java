package co.tinode.tindroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

/**
 * Pure presentation rules for a single message row.
 */
final class MessageRowSpec {
    enum Side {
        LEFT,
        RIGHT
    }

    private final Side mSide;
    private final boolean mShowDateDivider;
    private final boolean mShowSenderName;

    private MessageRowSpec(@NonNull Side side, boolean showDateDivider, boolean showSenderName) {
        mSide = side;
        mShowDateDivider = showDateDivider;
        mShowSenderName = showSenderName;
    }

    @NonNull
    Side getSide() {
        return mSide;
    }

    boolean showDateDivider() {
        return mShowDateDivider;
    }

    boolean showSenderName() {
        return mShowSenderName;
    }

    static MessageRowSpec resolve(boolean isMine, boolean isGroup, boolean isChannel,
                                  @Nullable Date currentMessageDate, @Nullable Date olderMessageDate) {
        return new MessageRowSpec(
                isMine ? Side.RIGHT : Side.LEFT,
                !isSameDate(olderMessageDate, currentMessageDate),
                isGroup && !isChannel && !isMine
        );
    }

    private static boolean isSameDate(@Nullable Date one, @Nullable Date two) {
        if (one == null || two == null) {
            return false;
        }
        final long oneDay = 24L * 60L * 60L * 1000L;
        return one.getTime() / oneDay == two.getTime() / oneDay;
    }
}
