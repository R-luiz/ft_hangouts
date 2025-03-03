package com.example.ft_hangouts.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.ft_hangouts.db.MessageDbHelper;
import com.example.ft_hangouts.model.Message;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    public static final String SMS_RECEIVED_ACTION = "com.example.ft_hangouts.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received");
        
        if (intent.getAction() != null && 
                intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Extract messages
                SmsMessage[] messages;
                String format = bundle.getString("format");
                
                // Use appropriate API based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                } else {
                    // For older Android versions
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus == null) {
                        Log.e(TAG, "No PDUs in intent");
                        return;
                    }
                    
                    messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        } else {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                    }
                }
                
                if (messages != null && messages.length > 0) {
                    processReceivedMessages(context, messages);
                }
            }
        }
    }
    
    private void processReceivedMessages(Context context, SmsMessage[] messages) {
        StringBuilder fullMessage = new StringBuilder();
        String sender = null;
        
        // Combine the message parts
        for (SmsMessage smsMessage : messages) {
            if (sender == null) {
                sender = smsMessage.getOriginatingAddress();
            }
            fullMessage.append(smsMessage.getMessageBody());
        }
        
        if (sender != null) {
            // Format phone number if needed
            if (sender.startsWith("+")) {
                // Keep international format
            } else if (sender.length() <= 10) {
                // For US numbers, could add country code
                // sender = "+1" + sender;
            }
            
            // Create and save message
            Message message = new Message(
                    fullMessage.toString(),
                    sender,
                    System.currentTimeMillis(),
                    false // Received message
            );
            
            try {
                // Store in database
                MessageDbHelper dbHelper = MessageDbHelper.getInstance(context);
                if (dbHelper != null) {
                    long id = dbHelper.insertMessage(message);
                    
                    if (id > 0) {
                        Log.d(TAG, "Message saved to database with ID: " + id);
                        
                        // Broadcast to update UI if app is open
                        Intent messageIntent = new Intent(SMS_RECEIVED_ACTION);
                        messageIntent.putExtra("phone_number", sender);
                        context.sendBroadcast(messageIntent);
                    } else {
                        Log.e(TAG, "Failed to save message to database");
                    }
                } else {
                    Log.e(TAG, "Database helper is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving message to database", e);
            }
        }
    }
}