package co.tinode.tindroid.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

import co.tinode.tindroid.R;
import co.tinode.tindroid.UtilsString;
import co.tinode.tinodesdk.model.MsgOneReaction;

/**
 * A popup emoji picker for message reactions, styled like Telegram/WhatsApp.
 * Shows a grid of available reactions that users can select.
 */
public class ReactionPickerDialog {

    // Number of emojis to show in collapsed state
    private static final int REACTIONS_COLLAPSED_COUNT = 6;
    // Maximum number of emojis to show when expanded
    private static final int MAX_EMOJIS = 40;
    // Number of columns in the grid
    private static final int GRID_COLUMNS = 6;

    private final PopupWindow mPopupWindow;
    private final View mContentView;
    private final RecyclerView mRecyclerView;
    private final LinearLayout mExpandContainer;
    private final ImageView mExpandButton;
    private final ReactionAdapter mAdapter;

    private boolean mExpanded = false;
    private OnReactionSelectedListener mListener;

    public interface OnReactionSelectedListener {
        void onReactionSelected(String reaction);
    }

    public ReactionPickerDialog(@NonNull Context context, @NonNull String[] reactionList,
                                 @Nullable MsgOneReaction[] currentReactions, @Nullable String myUserId) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mContentView = inflater.inflate(R.layout.reaction_picker_dialog, null);

        mRecyclerView = mContentView.findViewById(R.id.reactionGrid);
        mExpandContainer = mContentView.findViewById(R.id.expandContainer);
        mExpandButton = mContentView.findViewById(R.id.expandButton);

        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(context, GRID_COLUMNS);
        mRecyclerView.setLayoutManager(layoutManager);

        // Create adapter
        mAdapter = new ReactionAdapter(reactionList, currentReactions, myUserId);
        mRecyclerView.setAdapter(mAdapter);

        // Show expand button if there are more reactions than collapsed count
        if (reactionList.length > REACTIONS_COLLAPSED_COUNT) {
            mExpandContainer.setVisibility(View.VISIBLE);
            mExpandButton.setOnClickListener(v -> toggleExpanded());
        }

        // Create popup window
        mPopupWindow = new PopupWindow(mContentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setElevation(16f);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);

        // Enable smooth transitions
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Update displayed count
        updateDisplayedReactions();
    }

    /**
     * Set the listener for reaction selection events.
     */
    public void setOnReactionSelectedListener(OnReactionSelectedListener listener) {
        mListener = listener;
    }

    /**
     * Show the picker anchored to a view.
     *
     * @param anchor the view to anchor the popup to
     */
    public void show(View anchor) {
        // Measure content to determine size
        mContentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int contentWidth = mContentView.getMeasuredWidth();
        int contentHeight = mContentView.getMeasuredHeight();

        // Get anchor location on screen
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // Get screen dimensions
        int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = anchor.getResources().getDisplayMetrics().heightPixels;

        // Calculate position - prefer showing above the anchor
        int x, y;

        // Horizontal positioning: center on anchor, but keep within screen bounds
        x = anchorX + (anchorWidth / 2) - (contentWidth / 2);
        x = Math.max(16, Math.min(x, screenWidth - contentWidth - 16));

        // Vertical positioning: prefer above the anchor
        int spaceAbove = anchorY;
        int spaceBelow = screenHeight - anchorY - anchorHeight;

        if (spaceAbove >= contentHeight + 16 || spaceAbove > spaceBelow) {
            // Show above
            y = anchorY - contentHeight - 8;
        } else {
            // Show below
            y = anchorY + anchorHeight + 8;
        }

        // Keep within screen bounds
        y = Math.max(16, Math.min(y, screenHeight - contentHeight - 16));

        mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    /**
     * Show the picker at a specific location relative to a parent view.
     *
     * @param parent the parent view
     * @param x x coordinate
     * @param y y coordinate
     */
    public void showAtLocation(View parent, int x, int y) {
        mContentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int contentWidth = mContentView.getMeasuredWidth();
        int contentHeight = mContentView.getMeasuredHeight();

        int screenWidth = parent.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = parent.getResources().getDisplayMetrics().heightPixels;

        // Adjust position to keep within screen bounds
        int adjustedX = Math.max(16, Math.min(x - contentWidth / 2, screenWidth - contentWidth - 16));
        int adjustedY = Math.max(16, Math.min(y - contentHeight - 8, screenHeight - contentHeight - 16));

        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, adjustedX, adjustedY);
    }

    /**
     * Dismiss the picker.
     */
    public void dismiss() {
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * Check if the picker is currently showing.
     */
    public boolean isShowing() {
        return mPopupWindow.isShowing();
    }

    private void toggleExpanded() {
        mExpanded = !mExpanded;
        mExpandButton.setRotation(mExpanded ? 180f : 0f);
        updateDisplayedReactions();
    }

    private void updateDisplayedReactions() {
        mAdapter.setDisplayCount(mExpanded ? MAX_EMOJIS : REACTIONS_COLLAPSED_COUNT);
        if (mExpanded) {
            mExpandContainer.setVisibility(View.GONE);
        }
    }

    private void onReactionClicked(String reaction) {
        dismiss();
        if (mListener != null) {
            mListener.onReactionSelected(reaction);
        }
    }

    /**
     * Adapter for the reaction grid.
     */
    private class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ViewHolder> {

        private final String[] mReactionList;
        private final MsgOneReaction[] mCurrentReactions;
        private final String mMyUserId;
        private int mDisplayCount;

        ReactionAdapter(String[] reactionList, MsgOneReaction[] currentReactions, String myUserId) {
            mReactionList = reactionList;
            mCurrentReactions = currentReactions;
            mMyUserId = myUserId;
            mDisplayCount = Math.min(REACTIONS_COLLAPSED_COUNT, reactionList.length);
        }

        void setDisplayCount(int count) {
            int oldCount = mDisplayCount;
            mDisplayCount = Math.min(count, mReactionList.length);
            if (mDisplayCount > oldCount) {
                notifyItemRangeInserted(oldCount, mDisplayCount - oldCount);
            } else if (mDisplayCount < oldCount) {
                notifyItemRangeRemoved(mDisplayCount, oldCount - mDisplayCount);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.reaction_picker_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String emoji = mReactionList[position];
            holder.mEmoji.setText(emoji);

            // Find if this reaction is already applied
            MsgOneReaction applied = findReaction(emoji);
            boolean isMine = isMyReaction(applied);

            // Show count if more than 1
            if (applied != null && applied.count != null && applied.count > 1) {
                holder.mCount.setVisibility(View.VISIBLE);
                holder.mCount.setText(UtilsString.shortenCount(applied.count));
            } else {
                holder.mCount.setVisibility(View.GONE);
            }

            // Set selected state for user's own reaction
            holder.itemView.setSelected(isMine);
            // Set activated state for reactions that have been applied by others
            holder.itemView.setActivated(applied != null && !isMine);

            holder.itemView.setOnClickListener(v -> onReactionClicked(emoji));
        }

        @Override
        public int getItemCount() {
            return mDisplayCount;
        }

        @Nullable
        private MsgOneReaction findReaction(String emoji) {
            if (mCurrentReactions == null) {
                return null;
            }
            for (MsgOneReaction r : mCurrentReactions) {
                if (emoji.equals(r.val)) {
                    return r;
                }
            }
            return null;
        }

        private boolean isMyReaction(@Nullable MsgOneReaction reaction) {
            if (reaction == null || reaction.users == null || mMyUserId == null) {
                return false;
            }
            return Arrays.asList(reaction.users).contains(mMyUserId);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mEmoji;
            final TextView mCount;

            ViewHolder(View itemView) {
                super(itemView);
                mEmoji = itemView.findViewById(R.id.emoji);
                mCount = itemView.findViewById(R.id.count);
            }
        }
    }
}
