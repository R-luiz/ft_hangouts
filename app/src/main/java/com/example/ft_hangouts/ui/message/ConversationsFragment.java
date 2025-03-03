package com.example.ft_hangouts.ui.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Conversation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ConversationsFragment extends Fragment implements ConversationsAdapter.OnConversationClickListener {

    private ConversationsViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView textNoConversations;
    private ConversationsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ConversationsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_conversations);
        textNoConversations = view.findViewById(R.id.text_no_conversations);
        FloatingActionButton fabNewMessage = view.findViewById(R.id.fab_new_message);

        adapter = new ConversationsAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Observe the conversations LiveData
        viewModel.getConversations().observe(getViewLifecycleOwner(), conversations -> {
            adapter.updateConversations(conversations);
            
            // Show/hide empty state
            if (conversations.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                textNoConversations.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                textNoConversations.setVisibility(View.GONE);
            }
        });

        // FAB click to create new message
        fabNewMessage.setOnClickListener(v -> {
            try {
                // Navigate to contact selector
                if (getActivity() != null) {
                    Fragment fragment = new ContactSelectorFragment();
                    
                    FragmentTransaction transaction = getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, fragment)
                        .addToBackStack(null);
                        
                    transaction.commit();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Could not open contact selector", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadConversations();
    }

    @Override
    public void onConversationClick(Conversation conversation, int position) {
        try {
            // Navigate to message fragment
            Fragment fragment = MessageFragment.newInstance(conversation.getContact());
            
            FragmentTransaction transaction = getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null);
                
            transaction.commit();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error opening conversation", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}