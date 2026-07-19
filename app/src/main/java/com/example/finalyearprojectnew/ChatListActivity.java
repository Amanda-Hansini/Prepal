package com.example.finalyearprojectnew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.ChatConversation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnConversationClickListener {

    private RecyclerView rvConversations;
    private LinearLayout llEmptyState;
    private MaterialButton btnFindFriends;
    private BottomNavigationView bottomNavigation;

    private FirebaseFirestore db;
    private String currentStudentId;

    private List<ChatConversation> conversationList = new ArrayList<>();
    private ChatListAdapter adapter;
    private ListenerRegistration conversationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("student_id", "STU-0000");

        initViews();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForConversations();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (conversationListener != null) {
            conversationListener.remove();
        }
    }

    private void initViews() {
        rvConversations = findViewById(R.id.rvConversations);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnFindFriends = findViewById(R.id.btnFindFriends);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        btnFindFriends.setOnClickListener(v -> {
            startActivity(new Intent(this, FriendsActivity.class));
        });

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(conversationList, this);
        rvConversations.setAdapter(adapter);

        bottomNavigation.setSelectedItemId(R.id.nav_chats);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chats) return true;
            else if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, StudentHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, StudentCalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, StudentProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void listenForConversations() {
        conversationListener = db.collection("Chats")
                .whereArrayContains("participants", currentStudentId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshots != null) {
                        conversationList.clear();
                        if (snapshots.isEmpty()) {
                            rvConversations.setVisibility(View.GONE);
                            llEmptyState.setVisibility(View.VISIBLE);
                            return;
                        }

                        llEmptyState.setVisibility(View.GONE);
                        rvConversations.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String chatId = doc.getId();
                            List<String> participants = (List<String>) doc.get("participants");
                            String lastMessage = doc.getString("lastMessage");
                            String lastSenderId = doc.getString("lastSenderId");
                            Timestamp lastUpdated = doc.getTimestamp("lastUpdated");

                            long unreadCount = 0;
                            Map<String, Object> unreadMap = (Map<String, Object>) doc.get("unreadCounts");
                            if (unreadMap != null && unreadMap.containsKey(currentStudentId)) {
                                Object countObj = unreadMap.get(currentStudentId);
                                if (countObj instanceof Long) {
                                    unreadCount = (Long) countObj;
                                }
                            }

                            String friendId = "";
                            if (participants != null) {
                                for (String p : participants) {
                                    if (!p.equals(currentStudentId)) {
                                        friendId = p;
                                        break;
                                    }
                                }
                            }

                            if (!friendId.isEmpty()) {
                                final String targetFriendId = friendId;
                                final long finalUnreadCount = unreadCount;

                                db.collection("AllStudents").document(targetFriendId).get()
                                        .addOnSuccessListener(friendDoc -> {
                                            String name = friendDoc.getString("fullName");
                                            String img = friendDoc.getString("profile_image_base64");

                                            ChatConversation conversation = new ChatConversation(
                                                    chatId,
                                                    targetFriendId,
                                                    name != null ? name : targetFriendId,
                                                    img,
                                                    lastMessage,
                                                    lastSenderId,
                                                    lastUpdated,
                                                    finalUnreadCount
                                            );

                                            // Avoid duplicates in list during async callback
                                            boolean exists = false;
                                            for (int i = 0; i < conversationList.size(); i++) {
                                                if (conversationList.get(i).getChatId().equals(chatId)) {
                                                    conversationList.set(i, conversation);
                                                    exists = true;
                                                    break;
                                                }
                                            }
                                            if (!exists) {
                                                conversationList.add(conversation);
                                            }
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    }
                });
    }

    @Override
    public void onConversationClick(ChatConversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("friend_id", conversation.getFriendId());
        intent.putExtra("friend_name", conversation.getFriendName());
        intent.putExtra("friend_image", conversation.getFriendProfileImageBase64());
        startActivity(intent);
    }
}
