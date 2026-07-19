package com.example.finalyearprojectnew;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.ChatMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ImageView btnBack;
    private CircleImageView ivFriendAvatar;
    private TextView tvFriendName, tvFriendStatus;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private FloatingActionButton btnSend;

    private FirebaseFirestore db;
    private String currentStudentId;
    private String currentStudentName;
    private String friendId;
    private String friendName;
    private String friendImageBase64;
    private String chatId;

    private List<ChatMessage> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("student_id", "STU-0000");
        currentStudentName = prefs.getString("student_name", "Student");

        friendId = getIntent().getStringExtra("friend_id");
        friendName = getIntent().getStringExtra("friend_name");
        friendImageBase64 = getIntent().getStringExtra("friend_image");

        if (friendId == null) {
            Toast.makeText(this, "Friend not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate deterministic chatId
        chatId = getChatId(currentStudentId, friendId);

        initViews();
        setupRecyclerView();
        listenForMessages();
        resetUnreadCount();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivFriendAvatar = findViewById(R.id.ivFriendAvatar);
        tvFriendName = findViewById(R.id.tvFriendName);
        tvFriendStatus = findViewById(R.id.tvFriendStatus);
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);

        btnBack.setOnClickListener(v -> finish());
        tvFriendName.setText(friendName != null ? friendName : friendId);
        tvFriendStatus.setText(friendId);

        if (friendImageBase64 != null && !friendImageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(friendImageBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivFriendAvatar.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivFriendAvatar.setImageResource(R.drawable.ic_profile_filled);
            }
        }

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(currentStudentId, messageList);
        rvMessages.setAdapter(messageAdapter);
    }

    private void listenForMessages() {
        messageListener = db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (snapshots != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                message.setMessageId(doc.getId());
                                messageList.add(message);
                            }
                        }
                        messageAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                        resetUnreadCount();
                    }
                });
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessageInput.setText("");

        String messageId = db.collection("Chats").document(chatId).collection("Messages").document().getId();
        Timestamp now = Timestamp.now();

        ChatMessage message = new ChatMessage(messageId, currentStudentId, friendId, text, now, false);

        // Save message document
        db.collection("Chats").document(chatId)
                .collection("Messages").document(messageId)
                .set(message);

        // Update conversation summary document
        Map<String, Object> conversationUpdate = new HashMap<>();
        conversationUpdate.put("chatId", chatId);
        conversationUpdate.put("participants", Arrays.asList(currentStudentId, friendId));
        conversationUpdate.put("lastMessage", text);
        conversationUpdate.put("lastSenderId", currentStudentId);
        conversationUpdate.put("lastUpdated", now);

        // Increment unread count for recipient
        conversationUpdate.put("unreadCounts." + friendId, FieldValue.increment(1));
        conversationUpdate.put("unreadCounts." + currentStudentId, 0);

        db.collection("Chats").document(chatId)
                .set(conversationUpdate, com.google.firebase.firestore.SetOptions.merge());
    }

    private void resetUnreadCount() {
        db.collection("Chats").document(chatId)
                .update("unreadCounts." + currentStudentId, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    private String getChatId(String id1, String id2) {
        if (id1.compareTo(id2) < 0) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }
}
