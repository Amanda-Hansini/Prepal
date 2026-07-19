package com.example.finalyearprojectnew;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.ChatMessage;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final String currentUserId;
    private final List<ChatMessage> messageList;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public MessageAdapter(String currentUserId, List<ChatMessage> messageList) {
        this.currentUserId = currentUserId;
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String formattedTime = "";
        if (message.getTimestamp() != null) {
            formattedTime = timeFormat.format(message.getTimestamp().toDate());
        }

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).tvText.setText(message.getText());
            ((SentMessageViewHolder) holder).tvTime.setText(formattedTime);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).tvText.setText(message.getText());
            ((ReceivedMessageViewHolder) holder).tvTime.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        SentMessageViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
