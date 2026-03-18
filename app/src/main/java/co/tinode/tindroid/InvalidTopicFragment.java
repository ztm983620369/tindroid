package co.tinode.tindroid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

public class InvalidTopicFragment extends Fragment implements MenuProvider {
    public InvalidTopicFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invalid_topic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        ((TextView) view.findViewById(R.id.stateTitle)).setText(
                args.getString("state_title", getString(R.string.topic_not_found_or_invalid)));
        ((TextView) view.findViewById(R.id.stateMessage)).setText(
                args.getString("state_message", getString(R.string.topic_not_found_or_invalid)));

        bindButton(view.findViewById(R.id.statePrimaryButton),
                args.getString("state_primary_label", null),
                args.getString("state_primary_action", null));
        bindButton(view.findViewById(R.id.stateSecondaryButton),
                args.getString("state_secondary_label", null),
                args.getString("state_secondary_action", null));
    }

    private void bindButton(@NonNull Button button, String label, String action) {
        if (label == null || action == null) {
            button.setVisibility(View.GONE);
            return;
        }
        button.setText(label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> {
            if (requireActivity() instanceof MessageActivity) {
                ((MessageActivity) requireActivity()).handleRuntimeAction(action);
            }
        });
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }
}
