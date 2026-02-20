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
    private final LinearLayout mExpandContainer;
    private final ImageView mExpandButton;
    private final ReactionAdapter mAdapter;

    private boolean mExpanded = false;
    private View mAnchor;
    private OnReactionSelectedListener mListener;

    public interface OnReactionSelectedListener {
        void onReactionSelected(String reaction);
    }

    public ReactionPickerDialog(@NonNull Context context, @NonNull String[] reactionList,
                                 @Nullable MsgOneReaction[] currentReactions, @Nullable String myUserId) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mContentView = inflater.inflate(R.layout.reaction_picker_dialog, null);

        RecyclerView recyclerView = mContentView.findViewById(R.id.reactionGrid);
        mExpandContainer = mContentView.findViewById(R.id.expandContainer);
        mExpandButton = mContentView.findViewById(R.id.expandButton);

        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(context, GRID_COLUMNS);
        recyclerView.setLayoutManager(layoutManager);
        // Disable item animator: DefaultItemAnimator fades items in from alpha 0, which makes
        // emoji appear at partial opacity when the popup first appears.
        recyclerView.setItemAnimator(null);

        // Create adapter
        mAdapter = new ReactionAdapter(reactionList, currentReactions, myUserId);
        recyclerView.setAdapter(mAdapter);

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
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setClippingEnabled(false);


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

        // Get the visible display area (excludes status bar, navigation bar, toolbar, etc.)
        android.graphics.Rect visibleFrame = new android.graphics.Rect();
        anchor.getWindowVisibleDisplayFrame(visibleFrame);

        // Get anchor location on screen
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        int margin = 16;

        // Calculate position - prefer showing above the anchor
        int x, y;

        // Horizontal positioning: center on anchor, but keep within visible bounds
        x = anchorX + (anchorWidth / 2) - (contentWidth / 2);
        x = Math.max(visibleFrame.left + margin,
                Math.min(x, visibleFrame.right - contentWidth - margin));

        // Vertical positioning: prefer above the anchor, but within the visible area
        int spaceAbove = anchorY - visibleFrame.top;
        int spaceBelow = visibleFrame.bottom - anchorY - anchorHeight;

        if (spaceAbove >= contentHeight + margin) {
            // Enough room above: show above the anchor
            y = anchorY - contentHeight;
        } else if (spaceBelow >= contentHeight + margin) {
            // Enough room below: show below the anchor
            y = anchorY + anchorHeight;
        } else {
            // Not enough room either way: show wherever there's more space,
            // clamped to the visible frame.
            if (spaceAbove > spaceBelow) {
                y = anchorY - contentHeight;
            } else {
                y = anchorY + anchorHeight;
            }
        }

        // Clamp to visible frame
        y = Math.max(visibleFrame.top + margin,
                Math.min(y, visibleFrame.bottom - contentHeight - margin));

        mAnchor = anchor;
        mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    /**
     * Dismiss the picker.
     */
    public void dismiss() {
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private void toggleExpanded() {
        mExpanded = !mExpanded;
        mExpandButton.setRotation(mExpanded ? 180f : 0f);
        updateDisplayedReactions();

        // After expanding, the popup is taller. Reposition it so it stays fully visible.
        if (mExpanded && mAnchor != null) {
            mContentView.post(this::repositionPopup);
        }
    }

    /**
     * Reposition the popup window if the expanded content would be clipped by the visible frame.
     */
    private void repositionPopup() {
        if (mAnchor == null || !mPopupWindow.isShowing()) {
            return;
        }

        mContentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int contentWidth = mContentView.getMeasuredWidth();
        int contentHeight = mContentView.getMeasuredHeight();

        android.graphics.Rect visibleFrame = new android.graphics.Rect();
        mAnchor.getWindowVisibleDisplayFrame(visibleFrame);

        // Get the popup's current location on screen.
        int[] popupLocation = new int[2];
        mContentView.getLocationOnScreen(popupLocation);
        int currentX = popupLocation[0];
        int currentY = popupLocation[1];

        // Check if the popup fits at its current position.
        if (currentY >= visibleFrame.top && currentY + contentHeight <= visibleFrame.bottom &&
                currentX >= visibleFrame.left && currentX + contentWidth <= visibleFrame.right) {
            // No clipping — nothing to do.
            return;
        }

        // Recalculate position.
        int[] anchorLocation = new int[2];
        mAnchor.getLocationOnScreen(anchorLocation);
        int anchorX = anchorLocation[0];
        int anchorY = anchorLocation[1];
        int anchorWidth = mAnchor.getWidth();
        int anchorHeight = mAnchor.getHeight();

        int margin = 16;

        int x = anchorX + (anchorWidth / 2) - (contentWidth / 2);
        x = Math.max(visibleFrame.left + margin,
                Math.min(x, visibleFrame.right - contentWidth - margin));

        int spaceAbove = anchorY - visibleFrame.top;
        int spaceBelow = visibleFrame.bottom - anchorY - anchorHeight;
        int y;
        if (spaceAbove >= contentHeight + margin) {
            y = anchorY - contentHeight;
        } else if (spaceBelow >= contentHeight + margin) {
            y = anchorY + anchorHeight;
        } else if (spaceAbove > spaceBelow) {
            y = anchorY - contentHeight;
        } else {
            y = anchorY + anchorHeight;
        }
        y = Math.max(visibleFrame.top + margin,
                Math.min(y, visibleFrame.bottom - contentHeight - margin));

        mPopupWindow.update(x, y, contentWidth, contentHeight);
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
            // Ensure full opacity regardless of RecyclerView item animator or state changes.
            holder.itemView.setAlpha(1f);

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
