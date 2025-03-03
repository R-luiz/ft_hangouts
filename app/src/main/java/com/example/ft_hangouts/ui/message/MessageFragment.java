package com.example.ft_hangouts.ui.message;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.db.ContactDbHelper;
import com.example.ft_hangouts.db.MessageDbHelper;
import com.example.ft_hangouts.model.Contact;
import com.example.ft_hangouts.model.Message;
import com.example.ft_hangouts.receivers.SmsReceiver;

import java.util.ArrayList;
import java.util.List;

public class MessageFragment extends Fragment {

    // Use the same key as in ContactsFragment
    private static final String ARG_CONTACT = "contact";
    private static final String SMS_SENT = "SMS_SENT";
    private static final String SMS_DELIVERED = "SMS_DELIVERED";

    private Contact contact;
    private ImageView contactPhoto;
    private TextView contactName;
    private TextView contactPhone;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView permissionMessage;
    private Button requestPermissionButton;
    private MessageAdapter messageAdapter;
    
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private BroadcastReceiver smsSentReceiver;
    private BroadcastReceiver smsDeliveredReceiver;
    private BroadcastReceiver smsReceivedReceiver;
    private MessageDbHelper messageDbHelper;

    // Static newInstance method, but we'll also handle direct instantiation
    public static MessageFragment newInstance(Contact contact) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT, contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Try both keys to be safe
            if (getArguments().containsKey(ARG_CONTACT)) {
                contact = (Contact) getArguments().getSerializable(ARG_CONTACT);
            } else if (getArguments().containsKey("contact")) {
                contact = (Contact) getArguments().getSerializable("contact");
            }
        }
        
        // Initialize database helper
        messageDbHelper = MessageDbHelper.getInstance(requireContext());
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted
                        setupMessagingUI();
                    } else {
                        // Permission denied
                        showPermissionRequiredMessage();
                    }
                });
        
        // Initialize SMS broadcast receivers
        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS sent successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        
        smsDeliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        
        // Initialize SMS received broadcast receiver
        smsReceivedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && 
                        intent.getAction().equals(SmsReceiver.SMS_RECEIVED_ACTION)) {
                    
                    String phoneNumber = intent.getStringExtra("phone_number");
                    
                    // If we're currently viewing this contact's messages, refresh the view
                    if (contact != null && phoneNumber != null && 
                            phoneNumber.equals(contact.getPhoneNumber())) {
                        loadMessagesFromDatabase();
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize views
            contactPhoto = view.findViewById(R.id.contact_photo);
            contactName = view.findViewById(R.id.contact_name);
            contactPhone = view.findViewById(R.id.contact_phone);
            messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
            messageInput = view.findViewById(R.id.message_input);
            sendButton = view.findViewById(R.id.send_button);
            permissionMessage = view.findViewById(R.id.permission_message);
            requestPermissionButton = view.findViewById(R.id.request_permission_button);
    
            // Check if we have a valid contact
            if (contact == null) {
                Toast.makeText(requireContext(), "Error: No contact information available", Toast.LENGTH_SHORT).show();
                if (getActivity() != null && getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            }
    
            // Setup contact info
            setupContactInfo();
    
            // Setup RecyclerView
            messageAdapter = new MessageAdapter();
            messagesRecyclerView.setAdapter(messageAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            layoutManager.setStackFromEnd(true); // Scroll to bottom for new messages
            messagesRecyclerView.setLayoutManager(layoutManager);
    
            // Check SMS permission and setup UI accordingly
            if (checkSmsPermission()) {
                setupMessagingUI();
            } else {
                showPermissionRequiredMessage();
            }
            
            // Load messages from database
            loadMessagesFromDatabase();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error initializing message screen", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Register SMS broadcast receivers - only if activity exists
        if (getActivity() != null) {
            try {
                getActivity().registerReceiver(smsSentReceiver, new IntentFilter(SMS_SENT));
                getActivity().registerReceiver(smsDeliveredReceiver, new IntentFilter(SMS_DELIVERED));
                getActivity().registerReceiver(smsReceivedReceiver, new IntentFilter(SmsReceiver.SMS_RECEIVED_ACTION));
                
                // Mark messages as read when viewing conversation
                if (contact != null) {
                    messageDbHelper.markMessagesAsRead(contact.getPhoneNumber());
                }
                
                // Load messages from database
                loadMessagesFromDatabase();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Unregister SMS broadcast receivers - only if activity exists
        if (getActivity() != null) {
            try {
                getActivity().unregisterReceiver(smsSentReceiver);
                getActivity().unregisterReceiver(smsDeliveredReceiver);
                getActivity().unregisterReceiver(smsReceivedReceiver);
            } catch (IllegalArgumentException e) {
                // Receivers not registered
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Ensure receivers are unregistered
        if (getActivity() != null) {
            try {
                getActivity().unregisterReceiver(smsSentReceiver);
                getActivity().unregisterReceiver(smsDeliveredReceiver);
                getActivity().unregisterReceiver(smsReceivedReceiver);
            } catch (Exception e) {
                // Ignore, just making sure we don't leak
            }
        }
    }
    
    /**
     * Load messages from database and update UI
     */
    private void loadMessagesFromDatabase() {
        if (contact != null && messageAdapter != null) {
            try {
                List<Message> messages = messageDbHelper.getMessagesForContact(contact.getPhoneNumber());
                messageAdapter.setMessages(messages);
                
                // Scroll to bottom if there are messages
                if (messages.size() > 0 && messagesRecyclerView != null) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error loading messages", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void setupContactInfo() {
        if (contact != null) {
            // Set contact name
            if (contactName != null) {
                contactName.setText(contact.getName());
            }
            
            // Set contact phone number
            if (contactPhone != null) {
                contactPhone.setText(contact.getPhoneNumber());
            }
            
            // Set contact photo
            if (contactPhoto != null) {
                if (contact.getPhoto() != null && contact.getPhoto().length > 0) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(
                                contact.getPhoto(), 0, contact.getPhoto().length);
                        if (bitmap != null) {
                            contactPhoto.setImageBitmap(bitmap);
                        } else {
                            contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
                        }
                    } catch (Exception e) {
                        contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
                        e.printStackTrace();
                    }
                } else {
                    contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
        } else {
            // Handle case where contact is null
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error: Contact not found", Toast.LENGTH_SHORT).show();
                
                // Only attempt navigation if fragment is attached to activity and view exists
                if (isAdded() && getView() != null) {
                    try {
                        Navigation.findNavController(getView()).navigateUp();
                    } catch (Exception e) {
                        // If navigation fails, try to pop back stack directly
                        if (getActivity() != null && getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        }
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupMessagingUI() {
        permissionMessage.setVisibility(View.GONE);
        requestPermissionButton.setVisibility(View.GONE);
        messageInput.setEnabled(true);
        sendButton.setEnabled(true);
        
        sendButton.setOnClickListener(v -> sendSmsMessage());
    }

    private void showPermissionRequiredMessage() {
        permissionMessage.setVisibility(View.VISIBLE);
        requestPermissionButton.setVisibility(View.VISIBLE);
        messageInput.setEnabled(false);
        sendButton.setEnabled(false);
        
        requestPermissionButton.setOnClickListener(v -> 
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS));
    }

    private void sendSmsMessage() {
        if (contact == null) {
            Toast.makeText(requireContext(), "Error: Contact not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (messageInput == null) {
            return;
        }
        
        String message = messageInput.getText().toString().trim();
        String phoneNumber = contact.getPhoneNumber();
        
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Create pending intents for SMS sent and delivered
            PendingIntent sentPI = PendingIntent.getBroadcast(requireContext(), 0,
                    new Intent(SMS_SENT), PendingIntent.FLAG_IMMUTABLE);
            
            PendingIntent deliveredPI = PendingIntent.getBroadcast(requireContext(), 0,
                    new Intent(SMS_DELIVERED), PendingIntent.FLAG_IMMUTABLE);
            
            // Check if the message is too long and needs to be divided
            if (message.length() > 160) {
                ArrayList<String> messageParts = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentPIs = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPIs = new ArrayList<>();
                
                for (int i = 0; i < messageParts.size(); i++) {
                    sentPIs.add(sentPI);
                    deliveredPIs.add(deliveredPI);
                }
                
                smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, sentPIs, deliveredPIs);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
            }
            
            // Create a Message object
            Message sentMessage = new Message(message, phoneNumber, true);
            
            // Save message to database
            long id = messageDbHelper.insertMessage(sentMessage);
            
            if (id > 0) {
                // Successfully saved, refresh messages
                loadMessagesFromDatabase();
            } else {
                // Database error, just add to adapter
                messageAdapter.addMessage(sentMessage);
            }
            
            // Clear the input field
            messageInput.setText("");
            
            // Scroll to the bottom
            messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to send SMS: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
}