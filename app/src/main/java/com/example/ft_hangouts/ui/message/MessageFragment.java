package com.example.ft_hangouts.ui.message;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.example.ft_hangouts.model.Contact;
import com.example.ft_hangouts.model.Message;

import java.util.ArrayList;

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
            
            // Add fake messages for demo (remove in production)
            addDemoMessages();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error initializing message screen", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Register SMS broadcast receivers
        requireActivity().registerReceiver(smsSentReceiver, new IntentFilter(SMS_SENT));
        requireActivity().registerReceiver(smsDeliveredReceiver, new IntentFilter(SMS_DELIVERED));
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Unregister SMS broadcast receivers
        try {
            requireActivity().unregisterReceiver(smsSentReceiver);
            requireActivity().unregisterReceiver(smsDeliveredReceiver);
        } catch (IllegalArgumentException e) {
            // Receivers not registered
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
                        contactPhoto.setImageBitmap(BitmapFactory.decodeByteArray(
                                contact.getPhoto(), 0, contact.getPhoto().length));
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
                Navigation.findNavController(requireView()).navigateUp();
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
            
            // Add the message to the UI
            Message sentMessage = new Message(message, phoneNumber, true);
            messageAdapter.addMessage(sentMessage);
            
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
    
    // Add some demo messages for UI testing (remove in production)
    private void addDemoMessages() {
        if (contact != null && messageAdapter != null && messagesRecyclerView != null) {
            String phone = contact.getPhoneNumber();
            if (phone == null || phone.isEmpty()) {
                phone = "Unknown";
            }
            
            // Simulate a conversation
            long now = System.currentTimeMillis();
            
            try {
                messageAdapter.addMessage(new Message("Hello! How are you?", phone, now - 3600000, false));
                messageAdapter.addMessage(new Message("I'm doing well, thanks for asking!", phone, now - 3500000, true));
                messageAdapter.addMessage(new Message("Do you want to meet up later?", phone, now - 3400000, false));
                messageAdapter.addMessage(new Message("Sure, what time works for you?", phone, now - 3300000, true));
                messageAdapter.addMessage(new Message("How about 6pm at the usual place?", phone, now - 3200000, false));
                
                // Scroll to the bottom
                messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}