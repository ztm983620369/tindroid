package co.tinode.tindroid.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import co.tinode.tindroid.R;
import co.tinode.tindroid.UtilsString;
import co.tinode.tinodesdk.model.MsgOneReaction;

/**
 * A view that displays message reactions as a horizontal strip of emoji badges.
 * Each reaction shows the emoji and optionally a count if more than one user reacted.
 * Active reactions (from the current user) are highlighted.
 */
public class ReactionStripView extends LinearLayout {

    public interface OnReactionClickListener {
        /**
         * Called when a reaction is clicked.
         * @param reaction the reaction value (emoji)
         */
        void onReactionClicked(String reaction);
    }

    private static final int DEFAULT_MAX_REACTIONS = 5;

    private int mMaxReactions = DEFAULT_MAX_REACTIONS;
    private String mMyUserId;
    private OnReactionClickListener mListener;

    public ReactionStripView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ReactionStripView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReactionStripView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    /**
     * Set the maximum number of reactions to display before showing "more".
     */
    public void setMaxReactions(int max) {
        mMaxReactions = max;
    }

    /**
     * Set the current user's ID to determine which reactions are "active".
     */
    public void setMyUserId(String userId) {
        mMyUserId = userId;
    }

    /**
     * Set the listener for reaction click events.
     */
    public void setOnReactionClickListener(OnReactionClickListener listener) {
        mListener = listener;
    }


    /**
     * Bind reactions to this view.
     *
     * @param reactions array of reactions to display, may be null
     */
    public void setReactions(@Nullable MsgOneReaction[] reactions) {
        removeAllViews();

        if (reactions == null || reactions.length == 0) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int displayCount = Math.min(reactions.length, mMaxReactions);

        // Add reaction badges
        for (int i = 0; i < displayCount; i++) {
            MsgOneReaction reaction = reactions[i];
            View badge = inflater.inflate(R.layout.reaction_badge, this, false);

            TextView emoji = badge.findViewById(R.id.reactionEmoji);
            TextView count = badge.findViewById(R.id.reactionCount);

            emoji.setText(reaction.val);

            // Show count if more than 1
            if (reaction.count != null && reaction.count > 1) {
                count.setVisibility(VISIBLE);
                count.setText(UtilsString.shortenCount(reaction.count));
            } else {
                count.setVisibility(GONE);
            }

            // Check if current user has this reaction
            boolean isActive = isUserReaction(reaction);
            badge.setSelected(isActive);
            if (isActive) {
                badge.setBackgroundResource(R.drawable.reaction_badge_active);
            } else {
                badge.setBackgroundResource(R.drawable.reaction_badge);
            }

            // Set click listener
            final String reactionVal = reaction.val;
            badge.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onReactionClicked(reactionVal);
                }
            });

            addView(badge);
        }
    }

    /**
     * Check if the current user has this reaction.
     */
    private boolean isUserReaction(MsgOneReaction reaction) {
        if (mMyUserId == null || reaction.users == null) {
            return false;
        }
        for (String user : reaction.users) {
            if (mMyUserId.equals(user)) {
                return true;
            }
        }
        return false;
    }
}
