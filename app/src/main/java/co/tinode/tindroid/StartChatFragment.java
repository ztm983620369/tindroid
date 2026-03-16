package co.tinode.tindroid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class StartChatFragment extends Fragment {
    private static final String STATE_ACTIVE_TAB = "activeTab";
    private static final int COUNT_OF_TABS = 3;
    private static final int TAB_SEARCH = 0;
    private static final int TAB_NEW_GROUP = 1;
    private static final int TAB_BY_ID = 2;

    private static final int[] TAB_NAMES = new int[] {R.string.find, R.string.group, R.string.by_id};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {
        final FragmentActivity activity = requireActivity();

        int initialTab = resolveInitialTab(activity, savedInstance);
        boolean startScan = activity.getIntent().getBooleanExtra(StartChatActivity.EXTRA_BY_ID_START_SCAN, false);

        final TabLayout tabLayout = view.findViewById(R.id.tabsCreationOptions);
        final ViewPager2 viewPager = view.findViewById(R.id.tabPager);
        viewPager.setAdapter(new PagerAdapter(activity, startScan));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(TAB_NAMES[position])).attach();
        int safeInitialTab = initialTab;
        viewPager.post(() -> viewPager.setCurrentItem(safeInitialTab, false));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        final View view = getView();
        if (view == null) {
            return;
        }
        final ViewPager2 viewPager = view.findViewById(R.id.tabPager);
        if (viewPager != null) {
            outState.putInt(STATE_ACTIVE_TAB, viewPager.getCurrentItem());
        }
    }

    private int resolveInitialTab(@NonNull FragmentActivity activity, Bundle savedInstance) {
        int initialTab;
        if (savedInstance != null) {
            initialTab = savedInstance.getInt(STATE_ACTIVE_TAB, 0);
        } else {
            initialTab = activity.getIntent().getIntExtra(StartChatActivity.EXTRA_INITIAL_TAB, 0);
        }
        return (initialTab >= 0 && initialTab < COUNT_OF_TABS) ? initialTab : 0;
    }

    private static class PagerAdapter extends FragmentStateAdapter {
        private final boolean mStartByIdScan;

        PagerAdapter(FragmentActivity fa, boolean startByIdScan) {
            super(fa);
            mStartByIdScan = startByIdScan;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case TAB_SEARCH -> new FindFragment();
                case TAB_NEW_GROUP -> new CreateGroupFragment();
                case TAB_BY_ID -> {
                    AddByIDFragment fragment = new AddByIDFragment();
                    if (mStartByIdScan) {
                        Bundle args = new Bundle();
                        args.putBoolean(AddByIDFragment.ARG_START_IN_SCAN_MODE, true);
                        fragment.setArguments(args);
                    }
                    yield fragment;
                }
                default -> throw new IllegalArgumentException("Invalid TAB position " + position);
            };
        }

        @Override
        public int getItemCount() {
            return COUNT_OF_TABS;
        }
    }
}
