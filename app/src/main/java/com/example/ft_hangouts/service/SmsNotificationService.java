package com.example.ft_hangouts.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.ft_hangouts.MainActivity;
import com.example.ft_hangouts.R;
import com.example.ft_hangouts.db.ContactDbHelper;
import com.example.ft_hangouts.model.Contact;
import com.example.ft_hangouts.receivers.SmsReceiver;

public class SmsNotificationService extends Service {
    private static final String TAG = "SmsNotificationSvc";
    private static final String CHANNEL_ID = "sms_notifications";
    private static final int NOTIFICATION_ID = 1001;
    
    private SmsReceiver smsReceiver;
    private ContactDbHelper contactDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SMS notification service created");
        
        // Initialize the contact database helper
        contactDbHelper = ContactDbHelper.getInstance(getApplicationContext());
        
        // Create the notification channel for Android O and above
        createNotificationChannel();
        
        // Register a foreground service notification
        startForeground(NOTIFICATION_ID, createServiceNotification());
        
        // Register the SMS receiver
        registerSmsReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SMS notification service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SMS notification service destroyed");
        
        // Unregister the receiver
        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering SMS receiver", e);
            }
        }
    }
    
    private void registerSmsReceiver() {
        // Create SMS receiver
        smsReceiver = new SmsNotificationReceiver();
        
        // Register it with high priority
        IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        filter.setPriority(999); // High priority to get it before others
        registerReceiver(smsReceiver, filter);
    }
    
    private void createNotificationChannel() {
        // Create the notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for incoming SMS messages");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createServiceNotification() {
        // Intent to open the app when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Create the notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FT Hangouts")
                .setContentText("Running in background to monitor SMS")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    // Show a notification for an incoming SMS
    public void showSmsNotification(String senderPhone, String messageContent) {
        // Look up contact info if available
        String sender = senderPhone;
        try {
            Contact contact = contactDbHelper.getContactByPhoneNumber(senderPhone);
            if (contact != null && contact.getName() != null && !contact.getName().isEmpty()) {
                sender = contact.getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding contact for phone: " + senderPhone, e);
            // Continue with just the phone number if contact lookup fails
        }
        
        // Truncate message if too long
        String message = messageContent;
        if (message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }
        
        // Create intent for opening the message
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("phone_number", senderPhone);
        intent.putExtra("action", "view_message");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New message from " + sender)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Get notification manager
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            // Use phone number hash as ID to ensure different conversations get different notifications
            int notificationId = Math.abs(senderPhone.hashCode());
            notificationManager.notify(notificationId, builder.build());
        }
    }
    
    /**
     * Custom SMS receiver that shows notifications
     */
    private class SmsNotificationReceiver extends SmsReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Process the SMS message (parent class method)
            super.onReceive(context, intent);
            
            // Extract the message and phone number (available in the intent extras)
            if (intent.getAction() != null && 
                    intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                
                android.telephony.SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (messages != null && messages.length > 0) {
                    StringBuilder messageBody = new StringBuilder();
                    String sender = messages[0].getOriginatingAddress();
                    
                    // Combine message parts
                    for (android.telephony.SmsMessage message : messages) {
                        messageBody.append(message.getMessageBody());
                    }
                    
                    // Show notification
                    showSmsNotification(sender, messageBody.toString());
                }
            }
        }
    }
}