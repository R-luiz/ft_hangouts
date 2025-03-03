package com.example.ft_hangouts.ui.contacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ContactsFragment extends Fragment implements ContactsAdapter.OnContactClickListener {

    private ContactsViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView textNoContacts;
    private ContactsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_contacts);
        textNoContacts = view.findViewById(R.id.text_no_contacts);
        FloatingActionButton fabAddContact = view.findViewById(R.id.fab_add_contact);

        adapter = new ContactsAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Observe the contacts LiveData
        viewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            adapter.updateContacts(contacts);
            
            // Show/hide empty state
            if (contacts.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                textNoContacts.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                textNoContacts.setVisibility(View.GONE);
            }
        });

        // FAB click to add new contact
        fabAddContact.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_nav_contacts_to_contactFormFragment);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadContacts();
    }

    @Override
    public void onContactClick(Contact contact, int position) {
        // Navigate to contact details/edit screen
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putSerializable("contact", contact);
        navController.navigate(R.id.action_nav_contacts_to_contactFormFragment, args);
    }
    
    @Override
    public void onMessageClick(Contact contact, int position) {
        // Check if contact has a phone number
        if (contact.getPhoneNumber() == null || contact.getPhoneNumber().isEmpty()) {
            Toast.makeText(requireContext(), "Contact has no phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Navigate to message screen
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putSerializable("contact", contact);
        navController.navigate(R.id.action_nav_contacts_to_messageFragment, args);
    }
}