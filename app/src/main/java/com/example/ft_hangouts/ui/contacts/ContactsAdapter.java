package com.example.ft_hangouts.ui.contacts;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Contact;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<Contact> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact, int position);
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
        private CircleImageView contactImage;
        private TextView contactName;
        private TextView contactPhone;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactImage = itemView.findViewById(R.id.contact_image);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
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

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(contact, position);
                }
            });
        }
    }
}