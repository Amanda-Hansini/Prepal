package com.example.finalyearprojectnew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.FriendItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity implements FriendAdapter.OnFriendActionListener {

    private ImageView btnBack;
    private EditText etSearchStudent;
    private MaterialButton tabMyFriends, tabRequests, tabFindNew;
    private RecyclerView rvFriends;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private FirebaseFirestore db;
    private String currentStudentId;
    private String currentStudentName;
    private String currentStudentBatchId;

    private String currentTab = "my_friends"; // "my_friends", "requests", "find"
    private List<FriendItem> friendList = new ArrayList<>();
    private FriendAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("student_id", "STU-0000");
        currentStudentName = prefs.getString("student_name", "Student");

        initViews();
        setupTabs();
        setupSearch();
        fetchCurrentStudentBatchAndLoad();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearchStudent = findViewById(R.id.etSearchStudent);
        tabMyFriends = findViewById(R.id.tabMyFriends);
        tabRequests = findViewById(R.id.tabRequests);
        tabFindNew = findViewById(R.id.tabFindNew);
        rvFriends = findViewById(R.id.rvFriends);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);

        btnBack.setOnClickListener(v -> finish());

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(friendList, this);
        rvFriends.setAdapter(adapter);
    }

    private void fetchCurrentStudentBatchAndLoad() {
        db.collection("AllStudents").document(currentStudentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentStudentBatchId = documentSnapshot.getString("batchId");
                        if (currentStudentBatchId == null || currentStudentBatchId.trim().isEmpty()) {
                            currentStudentBatchId = documentSnapshot.getString("batchName");
                        }
                    }
                    loadTabContent();
                })
                .addOnFailureListener(e -> loadTabContent());
    }

    private void setupTabs() {
        tabMyFriends.setOnClickListener(v -> {
            currentTab = "my_friends";
            updateTabStyles();
            loadTabContent();
        });

        tabRequests.setOnClickListener(v -> {
            currentTab = "requests";
            updateTabStyles();
            loadTabContent();
        });

        tabFindNew.setOnClickListener(v -> {
            currentTab = "find";
            updateTabStyles();
            loadTabContent();
        });
    }

    private void updateTabStyles() {
        styleTabButton(tabMyFriends, "my_friends".equals(currentTab));
        styleTabButton(tabRequests, "requests".equals(currentTab));
        styleTabButton(tabFindNew, "find".equals(currentTab));
    }

    private void styleTabButton(MaterialButton btn, boolean isActive) {
        int blueColor = getResources().getColor(R.color.colorAccent);
        int whiteColor = getResources().getColor(R.color.colorWhite);

        if (isActive) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blueColor));
            btn.setTextColor(whiteColor);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(blueColor));
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
            btn.setTextColor(blueColor);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(blueColor));
        }
    }

    private void setupSearch() {
        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCurrentList(s.toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTabContent() {
        friendList.clear();
        adapter.notifyDataSetChanged();
        llEmptyState.setVisibility(View.GONE);

        if ("my_friends".equals(currentTab)) {
            loadMyFriends();
        } else if ("requests".equals(currentTab)) {
            loadFriendRequests();
        } else if ("find".equals(currentTab)) {
            loadAllClassmates();
        }
    }

    private boolean isSameBatch(String b1, String b2) {
        if (b1 == null || b2 == null) return false;
        String clean1 = b1.trim().toLowerCase();
        String clean2 = b2.trim().toLowerCase();
        if (clean1.equals(clean2)) return true;
        return clean1.contains(clean2) || clean2.contains(clean1);
    }

    private void loadMyFriends() {
        db.collection("AllStudents").document(currentStudentId)
                .collection("Friends")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    friendList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState("No friends added yet", "Search for classmates in 'Find Classmates' tab.");
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String friendId = doc.getId();
                        db.collection("AllStudents").document(friendId).get()
                                .addOnSuccessListener(friendDoc -> {
                                    if (friendDoc.exists()) {
                                        String targetBatch = friendDoc.getString("batchId");
                                        if (targetBatch == null) targetBatch = friendDoc.getString("batchName");

                                        // Ensure friend belongs to the same batch
                                        if (currentStudentBatchId == null || isSameBatch(currentStudentBatchId, targetBatch)) {
                                            FriendItem item = new FriendItem(
                                                    friendDoc.getId(),
                                                    friendDoc.getString("fullName"),
                                                    friendDoc.getString("email"),
                                                    targetBatch,
                                                    friendDoc.getString("programId"),
                                                    friendDoc.getString("profile_image_base64"),
                                                    "friend"
                                            );
                                            friendList.add(item);
                                            adapter.notifyDataSetChanged();
                                            llEmptyState.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                });
    }

    private void loadFriendRequests() {
        db.collection("AllStudents").document(currentStudentId)
                .collection("Friends")
                .whereEqualTo("status", "pending_received")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    friendList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState("No pending requests", "You have no new friend requests.");
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String friendId = doc.getId();
                        db.collection("AllStudents").document(friendId).get()
                                .addOnSuccessListener(friendDoc -> {
                                    if (friendDoc.exists()) {
                                        String targetBatch = friendDoc.getString("batchId");
                                        if (targetBatch == null) targetBatch = friendDoc.getString("batchName");

                                        // Ensure requester belongs to the same batch
                                        if (currentStudentBatchId == null || isSameBatch(currentStudentBatchId, targetBatch)) {
                                            FriendItem item = new FriendItem(
                                                    friendDoc.getId(),
                                                    friendDoc.getString("fullName"),
                                                    friendDoc.getString("email"),
                                                    targetBatch,
                                                    friendDoc.getString("programId"),
                                                    friendDoc.getString("profile_image_base64"),
                                                    "pending_received"
                                            );
                                            friendList.add(item);
                                            adapter.notifyDataSetChanged();
                                            llEmptyState.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                });
    }

    private void loadAllClassmates() {
        db.collection("AllStudents").get().addOnSuccessListener(queryDocumentSnapshots -> {
            friendList.clear();
            List<DocumentSnapshot> allDocs = queryDocumentSnapshots.getDocuments();

            db.collection("AllStudents").document(currentStudentId)
                    .collection("Friends")
                    .get()
                    .addOnSuccessListener(friendsSnap -> {
                        Map<String, String> statusMap = new HashMap<>();
                        for (DocumentSnapshot fDoc : friendsSnap.getDocuments()) {
                            statusMap.put(fDoc.getId(), fDoc.getString("status"));
                        }

                        for (DocumentSnapshot doc : allDocs) {
                            String id = doc.getId();
                            if (id.equals(currentStudentId)) continue; // skip self

                            String targetBatch = doc.getString("batchId");
                            if (targetBatch == null) targetBatch = doc.getString("batchName");

                            // Filter strictly by the current student's batch
                            if (currentStudentBatchId != null && !currentStudentBatchId.isEmpty()) {
                                if (!isSameBatch(currentStudentBatchId, targetBatch)) {
                                    continue;
                                }
                            }

                            String status = statusMap.containsKey(id) ? statusMap.get(id) : "none";

                            // Skip students who are already accepted friends
                            if ("accepted".equalsIgnoreCase(status) || "friend".equalsIgnoreCase(status)) {
                                continue;
                            }

                            FriendItem item = new FriendItem(
                                    id,
                                    doc.getString("fullName"),
                                    doc.getString("email"),
                                    targetBatch,
                                    doc.getString("programId"),
                                    doc.getString("profile_image_base64"),
                                    status
                            );
                            friendList.add(item);
                        }

                        if (friendList.isEmpty()) {
                            String batchInfo = (currentStudentBatchId != null && !currentStudentBatchId.isEmpty()) ? "in " + currentStudentBatchId : "";
                            showEmptyState("No classmates found", "No other registered students found " + batchInfo + ".");
                        } else {
                            adapter.notifyDataSetChanged();
                            llEmptyState.setVisibility(View.GONE);
                        }
                    });
        });
    }

    private void filterCurrentList(String query) {
        if (query.isEmpty()) {
            adapter = new FriendAdapter(friendList, this);
            rvFriends.setAdapter(adapter);
            return;
        }

        List<FriendItem> filtered = new ArrayList<>();
        for (FriendItem item : friendList) {
            boolean nameMatch = item.getFullName() != null && item.getFullName().toLowerCase().contains(query);
            boolean idMatch = item.getStudentId() != null && item.getStudentId().toLowerCase().contains(query);
            if (nameMatch || idMatch) {
                filtered.add(item);
            }
        }
        rvFriends.setAdapter(new FriendAdapter(filtered, this));
    }

    private void showEmptyState(String title, String subtitle) {
        tvEmptyTitle.setText(title);
        tvEmptySubtitle.setText(subtitle);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAddFriend(FriendItem item) {
        Map<String, Object> sentData = new HashMap<>();
        sentData.put("friendId", item.getStudentId());
        sentData.put("friendName", item.getFullName());
        sentData.put("status", "pending_sent");
        sentData.put("timestamp", Timestamp.now());

        Map<String, Object> receivedData = new HashMap<>();
        receivedData.put("friendId", currentStudentId);
        receivedData.put("friendName", currentStudentName);
        receivedData.put("status", "pending_received");
        receivedData.put("timestamp", Timestamp.now());

        db.collection("AllStudents").document(currentStudentId)
                .collection("Friends").document(item.getStudentId())
                .set(sentData)
                .addOnSuccessListener(aVoid -> {
                    db.collection("AllStudents").document(item.getStudentId())
                            .collection("Friends").document(currentStudentId)
                            .set(receivedData);

                    item.setStatus("pending_sent");
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onAcceptRequest(FriendItem item) {
        db.collection("AllStudents").document(currentStudentId)
                .collection("Friends").document(item.getStudentId())
                .update("status", "accepted");

        db.collection("AllStudents").document(item.getStudentId())
                .collection("Friends").document(currentStudentId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    item.setStatus("friend");
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRejectRequest(FriendItem item) {
        db.collection("AllStudents").document(currentStudentId)
                .collection("Friends").document(item.getStudentId())
                .delete();

        db.collection("AllStudents").document(item.getStudentId())
                .collection("Friends").document(currentStudentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    friendList.remove(item);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Request removed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onOpenChat(FriendItem item) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("friend_id", item.getStudentId());
        intent.putExtra("friend_name", item.getFullName());
        intent.putExtra("friend_image", item.getProfileImageBase64());
        startActivity(intent);
    }
}
