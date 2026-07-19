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

import com.example.finalyearprojectnew.models.FriendItem;
import com.google.android.material.button.MaterialButton;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    public interface OnFriendActionListener {
        void onAddFriend(FriendItem item);
        void onAcceptRequest(FriendItem item);
        void onRejectRequest(FriendItem item);
        void onOpenChat(FriendItem item);
    }

    private final List<FriendItem> friendList;
    private final OnFriendActionListener listener;

    public FriendAdapter(List<FriendItem> friendList, OnFriendActionListener listener) {
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendItem item = friendList.get(position);

        holder.tvStudentName.setText(item.getFullName() != null ? item.getFullName() : item.getStudentId());
        
        String details = item.getStudentId();
        if (item.getBatchId() != null && !item.getBatchId().isEmpty()) {
            details += " • " + item.getBatchId();
        }
        holder.tvStudentDetails.setText(details);

        if (item.getProfileImageBase64() != null && !item.getProfileImageBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(item.getProfileImageBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivFriendAvatar.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.ivFriendAvatar.setImageResource(R.drawable.ic_profile_filled);
            }
        } else {
            holder.ivFriendAvatar.setImageResource(R.drawable.ic_profile_filled);
        }

        String status = item.getStatus() != null ? item.getStatus() : "none";
        holder.btnRejectAction.setVisibility(View.GONE);

        switch (status) {
            case "friend":
            case "accepted":
                holder.btnFriendAction.setText("Message");
                holder.btnFriendAction.setEnabled(true);
                holder.btnFriendAction.setOnClickListener(v -> {
                    if (listener != null) listener.onOpenChat(item);
                });
                break;
            case "pending_sent":
                holder.btnFriendAction.setText("Pending");
                holder.btnFriendAction.setEnabled(false);
                break;
            case "pending_received":
                holder.btnFriendAction.setText("Accept");
                holder.btnFriendAction.setEnabled(true);
                holder.btnRejectAction.setVisibility(View.VISIBLE);
                holder.btnFriendAction.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptRequest(item);
                });
                holder.btnRejectAction.setOnClickListener(v -> {
                    if (listener != null) listener.onRejectRequest(item);
                });
                break;
            default: // "none"
                holder.btnFriendAction.setText("Add Friend");
                holder.btnFriendAction.setEnabled(true);
                holder.btnFriendAction.setOnClickListener(v -> {
                    if (listener != null) listener.onAddFriend(item);
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFriendAvatar;
        TextView tvStudentName, tvStudentDetails;
        MaterialButton btnFriendAction, btnRejectAction;

        ViewHolder(View itemView) {
            super(itemView);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentDetails = itemView.findViewById(R.id.tvStudentDetails);
            btnFriendAction = itemView.findViewById(R.id.btnFriendAction);
            btnRejectAction = itemView.findViewById(R.id.btnRejectAction);
        }
    }
}
