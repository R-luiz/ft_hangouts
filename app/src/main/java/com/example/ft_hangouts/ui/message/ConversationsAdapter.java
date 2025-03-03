package com.example.ft_hangouts.ui.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private List<Conversation> conversations;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation, int position);
    }

    public ConversationsAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations != null ? conversations : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        
        // Set contact name
        holder.contactName.setText(conversation.getContact().getName());
        
        // Set last message
        String lastMsg = conversation.getLastMessage();
        if (conversation.isOutgoing()) {
            lastMsg = "You: " + lastMsg;
        }
        holder.lastMessage.setText(lastMsg);
        
        // Set time
        holder.messageTime.setText(formatTime(conversation.getTimestamp()));
        
        // Set contact photo
        if (conversation.getContact().getPhoto() != null && conversation.getContact().getPhoto().length > 0) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        conversation.getContact().getPhoto(), 0, conversation.getContact().getPhoto().length);
                if (bitmap != null) {
                    holder.contactPhoto.setImageBitmap(bitmap);
                } else {
                    holder.contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
                }
            } catch (Exception e) {
                holder.contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
                e.printStackTrace();
            }
        } else {
            holder.contactPhoto.setImageResource(R.mipmap.ic_launcher_round);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.conversations = newConversations;
        notifyDataSetChanged();
    }

    public void addConversation(Conversation conversation) {
        // Check if conversation with this contact already exists
        boolean exists = false;
        int index = -1;
        
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getContact().getId() == conversation.getContact().getId()) {
                exists = true;
                index = i;
                break;
            }
        }
        
        if (exists) {
            // Update existing conversation
            conversations.get(index).setLastMessage(conversation.getLastMessage());
            conversations.get(index).setTimestamp(conversation.getTimestamp());
            conversations.get(index).setOutgoing(conversation.isOutgoing());
            notifyItemChanged(index);
        } else {
            // Add new conversation
            conversations.add(0, conversation);
            notifyItemInserted(0);
        }
    }

    private String formatTime(long timestamp) {
        if (DateUtils.isToday(timestamp)) {
            return android.text.format.DateFormat.format("hh:mm a", timestamp).toString();
        } else if (DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)) {
            return "Yesterday";
        } else {
            return android.text.format.DateFormat.format("MM/dd/yyyy", timestamp).toString();
        }
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView contactPhoto;
        TextView contactName;
        TextView lastMessage;
        TextView messageTime;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            contactName = itemView.findViewById(R.id.contact_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}