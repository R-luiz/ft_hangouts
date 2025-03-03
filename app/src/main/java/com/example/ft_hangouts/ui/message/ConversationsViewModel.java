package com.example.ft_hangouts.ui.message;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ft_hangouts.db.ContactDbHelper;
import com.example.ft_hangouts.db.MessageDbHelper;
import com.example.ft_hangouts.model.Contact;
import com.example.ft_hangouts.model.Conversation;
import com.example.ft_hangouts.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Conversation>> conversations;

    public ConversationsViewModel(@NonNull Application application) {
        super(application);
        conversations = new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void loadConversations() {
        try {
            ContactDbHelper contactDbHelper = ContactDbHelper.getInstance(getApplication());
            MessageDbHelper messageDbHelper = MessageDbHelper.getInstance(getApplication());
            
            // Get the last message for each contact
            List<Message> lastMessages = messageDbHelper.getLastMessagesForAllContacts();
            List<Conversation> conversationList = new ArrayList<>();
            
            for (Message message : lastMessages) {
                String phoneNumber = message.getPhoneNumber();
                
                // Find the contact associated with this phone number
                Contact contact = null;
                try {
                    contact = contactDbHelper.getContactByPhoneNumber(phoneNumber);
                } catch (Exception e) {
                    Log.e("ConversationsViewModel", "Error getting contact by phone number", e);
                }
                
                // If no contact exists, create a basic one with just the phone number
                if (contact == null) {
                    contact = new Contact();
                    contact.setPhoneNumber(phoneNumber);
                    contact.setName(phoneNumber); // Use phone number as name if contact doesn't exist
                }
                
                // Create conversation object
                Conversation conversation = new Conversation(
                        contact,
                        message.getContent(),
                        message.getTimestamp(),
                        message.isSent()
                );
                
                conversationList.add(conversation);
            }
            
            if (conversationList.isEmpty()) {
                // If there are no messages yet, create default conversations
                createDefaultConversations(contactDbHelper);
            } else {
                conversations.postValue(conversationList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            conversations.postValue(new ArrayList<>());
        }
    }
    
    private void createDefaultConversations(ContactDbHelper dbHelper) {
        try {
            List<Contact> contacts = dbHelper.getAllContacts();
            List<Conversation> conversationList = new ArrayList<>();
            long now = System.currentTimeMillis();
            
            // Create default conversations based on contacts
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                
                // Only include contacts with phone numbers
                if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
                    String lastMessage = "Start a conversation with " + contact.getName();
                    boolean isOutgoing = true;
                    long timestamp = now - (i * 60000); // 1 minute intervals
                    
                    Conversation conversation = new Conversation(contact, lastMessage, timestamp, isOutgoing);
                    conversationList.add(conversation);
                }
            }
            
            conversations.postValue(conversationList);
        } catch (Exception e) {
            e.printStackTrace();
            conversations.postValue(new ArrayList<>());
        }
    }
}