package com.thunderboarsolution.MVVMretrofiltrequest;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.viewmodel.AddSessionViewModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Study Assistant
 * - Subject selector (from Subjects API)
 * - Simple chat UI
 * - Calls Gemini generateContent via OkHttp
 *
 * NOTE: The API key is injected as BuildConfig.GEMINI_API_KEY (from gradle.properties).
 */
public class ChatbotActivity extends AppCompatActivity {

    private Spinner spSubjects;
    private Button btnClear;
    private RecyclerView rvChat;
    private EditText etMessage;
    private Button btnSend;

    private AddSessionViewModel subjectsViewModel;

    private final List<SubjectOption> subjectOptions = new ArrayList<>();
    private ArrayAdapter<SubjectOption> subjectAdapter;

    private final ChatAdapter chatAdapter = new ChatAdapter();

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    // Gemini REST base and model names
    private static final String GEMINI_API_BASE =
            "https://generativelanguage.googleapis.com/v1/models/";
    // Primary: Flash 2.x generation
    private static final String GEMINI_MODEL_PRIMARY = "gemini-2.0-flash";
    // Fallback: widely available 1.5 Flash latest alias
    private static final String GEMINI_MODEL_FALLBACK = "gemini-1.5-flash-latest";
       
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        bindViews();
        setupSubjectsSpinner();
        setupRecycler();

        // Reuse AddSessionViewModel to get subjects map (it already refreshes subjects)
        subjectsViewModel = new ViewModelProvider(this).get(AddSessionViewModel.class);
        subjectsViewModel.getSubjectsMap().observe(this, this::rebuildSubjectSpinner);

        btnSend.setOnClickListener(v -> onSend());
        btnClear.setOnClickListener(v -> chatAdapter.clear());
    }

    private void bindViews() {
        spSubjects = findViewById(R.id.spSubjects);
        btnClear = findViewById(R.id.btnClear);
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
    }

    private void setupRecycler() {
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
    }

    private void setupSubjectsSpinner() {
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectOptions);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubjects.setAdapter(subjectAdapter);
    }

    private void rebuildSubjectSpinner(@Nullable Map<String, Subject> map) {
        subjectOptions.clear();
        subjectOptions.add(new SubjectOption("__GENERAL__", "General")); // default "no specific subject"
        if (map != null) {
            for (Map.Entry<String, Subject> e : map.entrySet()) {
                Subject s = e.getValue();
                String name = (s != null && s.getName() != null && !s.getName().trim().isEmpty())
                        ? s.getName() : e.getKey();
                subjectOptions.add(new SubjectOption(e.getKey(), name));
            }
        }
        subjectAdapter.notifyDataSetChanged();
        if (!subjectOptions.isEmpty()) spSubjects.setSelection(0);
    }

    private void onSend() {
        String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(msg)) return;

        String subject = getSelectedSubjectName();
        chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.USER, msg));
        etMessage.setText("");

        String prompt = buildPrompt(subject, msg);
        callGemini(prompt);
    }

    private String getSelectedSubjectName() {
        int idx = spSubjects.getSelectedItemPosition();
        if (idx < 0 || idx >= subjectOptions.size()) return "General";
        SubjectOption opt = subjectOptions.get(idx);
        return opt != null ? opt.name : "General";
    }

    private String buildPrompt(String subjectName, String userMsg) {
        return String.format(Locale.getDefault(),
                "You are a helpful study assistant for the subject \"%s\". " +
                "Answer concisely and clearly with helpful examples when appropriate.\n" +
                "Question: %s", subjectName, userMsg);
    }

    private void callGemini(String prompt) {
        // Try primary model first; auto-fallback to 1.5 flash if 404 (model not found)
        callGeminiWithModel(prompt, GEMINI_MODEL_PRIMARY, true);
    }

    private void callGeminiWithModel(String prompt, String model, boolean allowFallback) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("null")) {
            chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT,
                    "Gemini API key is missing. Please configure BuildConfig.GEMINI_API_KEY."));
            return;
        }

        String url = GEMINI_API_BASE + model + ":generateContent?key=" + apiKey;

        // Request body for Gemini generateContent
        GenerateContentRequest bodyObj = new GenerateContentRequest();
        bodyObj.contents = new ArrayList<>();
        Content content = new Content();
        content.parts = new ArrayList<>();
        Part p = new Part();
        p.text = prompt;
        content.parts.add(p);
        bodyObj.contents.add(content);

        String json = gson.toJson(bodyObj);
        RequestBody rb = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url(url)
                .post(rb)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT,
                        "Network error: " + e.getMessage())));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                String body;
                try {
                    body = response.body() != null ? new String(response.body().bytes(), StandardCharsets.UTF_8) : "";
                } catch (IOException e) {
                    body = "";
                }

                if (!response.isSuccessful()) {
                    if (response.code() == 404 && allowFallback) {
                        runOnUiThread(() -> chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT,
                                "Selected model '" + model + "' not found. Retrying with '" + GEMINI_MODEL_FALLBACK + "'...")));
                        callGeminiWithModel(prompt, GEMINI_MODEL_FALLBACK, false);
                        return;
                    }
                    String msg = "API error (" + model + "): " + response.code() + " " + body;
                    final String finalMsg = msg;
                    runOnUiThread(() -> chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, finalMsg)));
                    return;
                }
                try {
                    GenerateContentResponse resp = gson.fromJson(body, GenerateContentResponse.class);
                    String text = extractText(resp);
                    if (TextUtils.isEmpty(text)) text = "No answer received.";
                    final String ans = text;
                    runOnUiThread(() -> chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, ans)));
                } catch (Exception ex) {
                    final String err = "Parse error: " + ex.getMessage();
                    runOnUiThread(() -> chatAdapter.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, err)));
                }
            }
        });
    }

    // Extract assistant text from Gemini response
    private String extractText(@Nullable GenerateContentResponse resp) {
        if (resp == null || resp.candidates == null || resp.candidates.isEmpty()) return null;
        Candidate c = resp.candidates.get(0);
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return null;
        Part part = c.content.parts.get(0);
        return part != null ? part.text : null;
    }

    // --- Simple in-activity RecyclerView adapter for chat ---

    static class ChatMessage {
        enum Role { USER, ASSISTANT }
        final Role role;
        final String text;
        ChatMessage(Role role, String text) { this.role = role; this.text = text; }
    }

    static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatVH> {
        private final List<ChatMessage> items = new ArrayList<>();

        void addMessage(ChatMessage m) {
            items.add(m);
            notifyItemInserted(items.size() - 1);
        }

        void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ChatVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(15f);
            int pad = (int) (parent.getResources().getDisplayMetrics().density * 12);
            tv.setPadding(pad, pad, pad, pad);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            return new ChatVH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatVH holder, int position) {
            ChatMessage m = items.get(position);
            TextView tv = (TextView) holder.itemView;
            if (m.role == ChatMessage.Role.USER) {
                tv.setText("You: " + m.text);
                tv.setBackgroundColor(0xFFE3F2FD); // light blue
                setMargins(tv, 48, 12, 12, 12);
                tv.setGravity(Gravity.START);
            } else {
                tv.setText("AI: " + m.text);
                tv.setBackgroundColor(0xFFE8F5E9); // light green
                setMargins(tv, 12, 12, 48, 12);
                tv.setGravity(Gravity.START);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private void setMargins(TextView tv, int left, int top, int right, int bottom) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tv.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            tv.setLayoutParams(p);
        }

        static class ChatVH extends RecyclerView.ViewHolder {
            ChatVH(@NonNull View itemView) { super(itemView); }
        }
    }

    // --- Models for Gemini REST ---

    static class GenerateContentRequest {
        List<Content> contents;
    }
    static class Content {
        List<Part> parts;
    }
    static class Part {
        @SerializedName("text") String text;
    }

    static class GenerateContentResponse {
        List<Candidate> candidates;
    }
    static class Candidate {
        Content content;
    }

    // Subject spinner option
    private static class SubjectOption {
        final String id;
        final String name;
        SubjectOption(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}