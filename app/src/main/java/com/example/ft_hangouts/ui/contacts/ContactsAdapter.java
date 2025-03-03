package com.example.ft_hangouts.ui.contacts;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Contact;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<Contact> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact, int position);
        void onMessageClick(Contact contact, int position);
    }

    public ContactsAdapter(List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.bind(contact, position);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateContacts(List<Contact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private ImageView contactImage;
        private TextView contactName;
        private TextView contactPhone;
        private ImageButton messageButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactImage = itemView.findViewById(R.id.contact_image);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            messageButton = itemView.findViewById(R.id.btn_message);
        }

        public void bind(final Contact contact, final int position) {
            contactName.setText(contact.getName());
            contactPhone.setText(contact.getPhoneNumber());

            // Set contact image if available
            if (contact.getPhoto() != null && contact.getPhoto().length > 0) {
                contactImage.setImageBitmap(BitmapFactory.decodeByteArray(
                        contact.getPhoto(), 0, contact.getPhoto().length));
            } else {
                contactImage.setImageResource(R.mipmap.ic_launcher_round);
            }

            // Set click listener for the whole item (edit contact)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(contact, position);
                }
            });
            
            // Set click listener for the message button
            messageButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(contact, position);
                }
            });
            
            // Only enable message button if a phone number exists
            boolean hasPhoneNumber = contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty();
            messageButton.setEnabled(hasPhoneNumber);
            messageButton.setAlpha(hasPhoneNumber ? 1.0f : 0.3f);
        }
    }
}