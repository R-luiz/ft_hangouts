package com.example.ft_hangouts.ui.message;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ft_hangouts.db.ContactDbHelper;
import com.example.ft_hangouts.model.Contact;
import com.example.ft_hangouts.model.Conversation;

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
        // In a real app, this would query a database for actual conversations
        // For demonstration purposes, we'll create mock conversations based on contacts
        
        try {
            ContactDbHelper dbHelper = new ContactDbHelper(getApplication());
            List<Contact> contacts = dbHelper.getAllContacts();
            
            List<Conversation> conversationList = new ArrayList<>();
            long now = System.currentTimeMillis();
            
            // Create mock conversations
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                
                // Only include contacts with phone numbers
                if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
                    String lastMessage;
                    boolean isOutgoing;
                    long timestamp = now - (i * 1800000); // 30 minute intervals
                    
                    // Alternate between incoming and outgoing messages
                    if (i % 2 == 0) {
                        lastMessage = "Hey, how are you doing?";
                        isOutgoing = false;
                    } else {
                        lastMessage = "Let's meet tomorrow for coffee!";
                        isOutgoing = true;
                    }
                    
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