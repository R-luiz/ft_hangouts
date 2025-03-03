package com.example.ft_hangouts.ui.message;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;

    public MessageAdapter() {
        this.messages = new ArrayList<>();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView messageTime;
        private final LinearLayout messageContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            messageTime.setText(message.getFormattedTime());

            // Set the appropriate style based on whether the message was sent or received
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            
            if (message.isSent()) {
                // Message sent by the user
                params.gravity = Gravity.END;
                messageContainer.setBackground(itemView.getContext().getDrawable(R.drawable.message_sent_background));
                messageText.setTextColor(itemView.getContext().getColor(android.R.color.white));
                messageTime.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
            } else {
                // Message received from the contact
                params.gravity = Gravity.START;
                messageContainer.setBackground(itemView.getContext().getDrawable(R.drawable.message_received_background));
                messageText.setTextColor(itemView.getContext().getColor(android.R.color.black));
                messageTime.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
            }
            
            messageContainer.setLayoutParams(params);
        }
    }
}