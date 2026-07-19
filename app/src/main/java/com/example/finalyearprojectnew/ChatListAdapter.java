package com.example.finalyearprojectnew;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.ChatConversation;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    private final List<ChatConversation> conversations;
    private final OnConversationClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatListAdapter(List<ChatConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatConversation conversation = conversations.get(position);

        holder.tvFriendName.setText(conversation.getFriendName() != null ? conversation.getFriendName() : conversation.getFriendId());
        holder.tvLastMessage.setText(conversation.getLastMessage() != null ? conversation.getLastMessage() : "No messages yet");

        if (conversation.getLastUpdated() != null) {
            holder.tvTime.setText(dateFormat.format(conversation.getLastUpdated().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        if (conversation.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        if (conversation.getFriendProfileImageBase64() != null && !conversation.getFriendProfileImageBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(conversation.getFriendProfileImageBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivFriendAvatar.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.ivFriendAvatar.setImageResource(R.drawable.ic_profile_filled);
            }
        } else {
            holder.ivFriendAvatar.setImageResource(R.drawable.ic_profile_filled);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFriendAvatar;
        TextView tvFriendName, tvLastMessage, tvTime, tvUnreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
