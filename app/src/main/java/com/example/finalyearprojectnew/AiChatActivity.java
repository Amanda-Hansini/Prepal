package com.example.finalyearprojectnew;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearprojectnew.models.AiChatRequest;
import com.example.finalyearprojectnew.models.AiChatResponse;
import com.example.finalyearprojectnew.models.ChatMessage;
import com.example.finalyearprojectnew.network.RetrofitClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatActivity extends AppCompatActivity {

    private ImageView btnBack, btnClearChat;
    private RecyclerView rvAiMessages;
    private EditText etAiMessageInput;
    private FloatingActionButton btnAiSend;
    private LinearLayout layoutAiLoading;

    private Chip chipStudyPlan, chipRaiseGpa, chipManageStress, chipWeakModules;

    private FirebaseFirestore db;
    private String currentStudentId;
    private String currentStudentName;

    private List<ChatMessage> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private Map<String, Object> studentContextMap = new HashMap<>();

    private static final String AI_BOT_ID = "PREPAL_AI_BOT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("student_id", "STU-0000");
        currentStudentName = prefs.getString("student_name", "Student");

        initViews();
        setupRecyclerView();
        fetchStudentContext();
        loadChatHistory();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearChat = findViewById(R.id.btnClearChat);
        rvAiMessages = findViewById(R.id.rvAiMessages);
        etAiMessageInput = findViewById(R.id.etAiMessageInput);
        btnAiSend = findViewById(R.id.btnAiSend);
        layoutAiLoading = findViewById(R.id.layoutAiLoading);

        chipStudyPlan = findViewById(R.id.chipStudyPlan);
        chipRaiseGpa = findViewById(R.id.chipRaiseGpa);
        chipManageStress = findViewById(R.id.chipManageStress);
        chipWeakModules = findViewById(R.id.chipWeakModules);

        btnBack.setOnClickListener(v -> finish());
        btnClearChat.setOnClickListener(v -> clearChatHistory());
        btnAiSend.setOnClickListener(v -> sendMessage(etAiMessageInput.getText().toString().trim()));

        chipStudyPlan.setOnClickListener(v -> sendMessage("Can you generate a structured 7-day study plan for my upcoming university exams?"));
        chipRaiseGpa.setOnClickListener(v -> sendMessage("Based on my current performance, what specific strategies can I use to raise my GPA?"));
        chipManageStress.setOnClickListener(v -> sendMessage("I feel stressed about my studies and workload. What practical counseling advice can you give me?"));
        chipWeakModules.setOnClickListener(v -> sendMessage("How should I prioritize my weak modules and organize daily practice to improve my marks?"));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvAiMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(currentStudentId, messageList);
        rvAiMessages.setAdapter(messageAdapter);
    }

    private void fetchStudentContext() {
        db.collection("Students").document(currentStudentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double gpa = document.getDouble("gpa");
                        Double predictedGpa = document.getDouble("predictedGpa");
                        Long stress = document.getLong("stress_level");

                        if (gpa != null) studentContextMap.put("current_gpa", gpa);
                        if (predictedGpa != null) studentContextMap.put("predicted_gpa", predictedGpa);
                        if (stress != null) studentContextMap.put("stress_level", stress);
                    }
                });
    }

    private void loadChatHistory() {
        db.collection("AiChats").document(currentStudentId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messageList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }
                    } else {
                        // Add initial welcome message from AI
                        addWelcomeMessage();
                    }
                    messageAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvAiMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                })
                .addOnFailureListener(e -> addWelcomeMessage());
    }

    private void addWelcomeMessage() {
        String welcomeText = "👋 Hello " + currentStudentName + "! I am PrePal AI, your personal study planner and academic advisor.\n\nHow can I help you today? You can ask me for custom study timetables, tips to raise your GPA, or discuss any study issues!";
        ChatMessage welcomeMsg = new ChatMessage("msg_welcome", AI_BOT_ID, currentStudentId, welcomeText, Timestamp.now(), true);
        messageList.add(welcomeMsg);
        messageAdapter.notifyDataSetChanged();
    }

    private void sendMessage(String userText) {
        if (userText.isEmpty()) return;

        etAiMessageInput.setText("");

        // 1. Add user message locally and save to Firestore
        String userMsgId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.now();
        ChatMessage userMsg = new ChatMessage(userMsgId, currentStudentId, AI_BOT_ID, userText, now, true);

        messageList.add(userMsg);
        messageAdapter.notifyDataSetChanged();
        rvAiMessages.smoothScrollToPosition(messageList.size() - 1);

        saveMessageToFirestore(userMsg);

        // 2. Show loading indicator
        layoutAiLoading.setVisibility(View.VISIBLE);

        // 3. Call Retrofit API endpoint
        AiChatRequest request = new AiChatRequest(userText, studentContextMap);
        RetrofitClient.getApiService().askAiChatbot(request).enqueue(new Callback<AiChatResponse>() {
            @Override
            public void onResponse(Call<AiChatResponse> call, Response<AiChatResponse> response) {
                layoutAiLoading.setVisibility(View.GONE);

                String aiReplyText;
                if (response.isSuccessful() && response.body() != null) {
                    aiReplyText = response.body().getReply();
                } else {
                    aiReplyText = "⚠️ Unable to get a response from PrePal AI server. Please check your network or server configuration.";
                }

                // 4. Add AI response to list & Firestore
                String aiMsgId = UUID.randomUUID().toString();
                ChatMessage aiMsg = new ChatMessage(aiMsgId, AI_BOT_ID, currentStudentId, aiReplyText, Timestamp.now(), true);

                messageList.add(aiMsg);
                messageAdapter.notifyDataSetChanged();
                rvAiMessages.smoothScrollToPosition(messageList.size() - 1);

                saveMessageToFirestore(aiMsg);
            }

            @Override
            public void onFailure(Call<AiChatResponse> call, Throwable t) {
                layoutAiLoading.setVisibility(View.GONE);

                String errorText = "❌ Network error: " + t.getMessage() + ". Make sure your backend server is running.";
                ChatMessage errorMsg = new ChatMessage(UUID.randomUUID().toString(), AI_BOT_ID, currentStudentId, errorText, Timestamp.now(), true);

                messageList.add(errorMsg);
                messageAdapter.notifyDataSetChanged();
                rvAiMessages.smoothScrollToPosition(messageList.size() - 1);
            }
        });
    }

    private void saveMessageToFirestore(ChatMessage msg) {
        db.collection("AiChats").document(currentStudentId)
                .collection("Messages").document(msg.getMessageId())
                .set(msg);
    }

    private void clearChatHistory() {
        db.collection("AiChats").document(currentStudentId)
                .collection("Messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    messageList.clear();
                    addWelcomeMessage();
                    Toast.makeText(AiChatActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                });
    }
}
