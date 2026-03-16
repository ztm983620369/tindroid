package co.tinode.tindroid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class MiniProgramsPanelController {
    enum Id {
        SCAN,
        PAY,
        FAVORITES,
        NEARBY,
        MOMENTS,
        GAMES,
        SEARCH,
        MORE
    }

    interface Listener {
        void onMiniProgramSelected(@NonNull Id id, @StringRes int labelResId);
    }

    private static final int SPAN_COUNT = 4;

    @Nullable
    private final View mPanelRoot;
    @Nullable
    private final Runnable mClosePanelAction;
    @Nullable
    private final Listener mListener;

    MiniProgramsPanelController(@Nullable View panelRoot,
                               @Nullable Runnable closePanelAction,
                               @Nullable Listener listener) {
        mPanelRoot = panelRoot;
        mClosePanelAction = closePanelAction;
        mListener = listener;

        if (mPanelRoot == null) {
            return;
        }

        RecyclerView grid = mPanelRoot.findViewById(R.id.miniProgramGrid);
        if (grid == null) {
            return;
        }

        grid.setLayoutManager(new GridLayoutManager(mPanelRoot.getContext(), SPAN_COUNT));
        grid.setHasFixedSize(true);
        grid.setNestedScrollingEnabled(false);
        grid.setAdapter(new Adapter(buildDefaultItems()));
    }

    void setEnabled(boolean enabled) {
        if (mPanelRoot == null) {
            return;
        }
        mPanelRoot.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private static List<Item> buildDefaultItems() {
        List<Item> items = new ArrayList<>(8);
        items.add(new Item(Id.SCAN, R.drawable.ic_qr_code, R.string.mini_program_scan));
        items.add(new Item(Id.PAY, R.drawable.ic_person_circle, R.string.mini_program_pay));
        items.add(new Item(Id.FAVORITES, R.drawable.ic_bookmark_ol, R.string.mini_program_favorites));
        items.add(new Item(Id.NEARBY, R.drawable.ic_at, R.string.mini_program_nearby));
        items.add(new Item(Id.MOMENTS, R.drawable.ic_image_ol, R.string.mini_program_moments));
        items.add(new Item(Id.GAMES, R.drawable.ic_play_circle, R.string.mini_program_games));
        items.add(new Item(Id.SEARCH, R.drawable.ic_search, R.string.mini_program_search));
        items.add(new Item(Id.MORE, R.drawable.ic_more_vert, R.string.mini_program_more));
        return items;
    }

    private static final class Item {
        @NonNull
        final Id id;
        @DrawableRes
        final int iconResId;
        @StringRes
        final int labelResId;

        Item(@NonNull Id id, @DrawableRes int iconResId, @StringRes int labelResId) {
            this.id = id;
            this.iconResId = iconResId;
            this.labelResId = labelResId;
        }
    }

    private final class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        private final List<Item> mItems;

        Adapter(@NonNull List<Item> items) {
            mItems = items;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).id.ordinal();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mini_program, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = mItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private final class ViewHolder extends RecyclerView.ViewHolder {
        @Nullable
        private final ImageView mIcon;
        @Nullable
        private final TextView mLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.miniProgramIcon);
            mLabel = itemView.findViewById(R.id.miniProgramLabel);
        }

        void bind(@NonNull Item item) {
            if (mIcon != null) {
                mIcon.setImageResource(item.iconResId);
            }
            if (mLabel != null) {
                mLabel.setText(item.labelResId);
            }
            itemView.setOnClickListener(v -> {
                if (mClosePanelAction != null) {
                    mClosePanelAction.run();
                }
                if (mListener != null) {
                    mListener.onMiniProgramSelected(item.id, item.labelResId);
                }
            });
            if (mLabel != null) {
                itemView.setContentDescription(mLabel.getText());
            }
        }
    }
}
