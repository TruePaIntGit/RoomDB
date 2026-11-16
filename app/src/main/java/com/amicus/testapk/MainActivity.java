package com.amicus.testapk;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements HistoryAdapter.OnMessageClickListener {

    private EditText editText;
    private Button button;
    private TextView textView;
    private RecyclerView historyRecyclerView;
    private Button clearHistoryButton;

    private static final String API_KEY = "sk-bd3d6422f39e4ca5a51c309f3ec82e38";
    private final String BASE_URL = "https://api.deepseek.com/";
    private static final String MODEL = "deepseek-flash";
    private final String SYSTEM_PROMPT = "Ты должен отвечать в стиле Ивана Грозного.Используй характерную для эпохи лексику но сохраняй понятность и точность ответов";

    private DeepSeekApi api;
    private ChatMessageDao messageDao;
    private HistoryAdapter historyAdapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.redactHeader("Authorization");
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(DeepSeekApi.class);

        AppDatabase database = AppDatabase.getInstance(this);
        messageDao = database.chatMessageDao();
        executorService = Executors.newSingleThreadExecutor();

        historyAdapter = new HistoryAdapter(this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);

        loadHistory();

        button.setOnClickListener(v -> sendMessage());

        clearHistoryButton.setOnClickListener(v -> showClearDialog());
    }

    private void loadHistory() {
        messageDao.getAllMessages().observe(this, messages -> {
            historyAdapter.setMessages(messages);
        });
    }

    private void sendMessage() {
        String userMessage = editText.getText().toString().trim();

        if (userMessage.isEmpty()) {
            editText.setError("Введите сообщение");
            return;
        }

        textView.setText("DeepSeek думает...");
        button.setEnabled(false);

        ChatRequest request = new ChatRequest(SYSTEM_PROMPT, userMessage);
        request.model = MODEL;

        api.sendMessage("Bearer " + API_KEY, request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                button.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().choices != null
                        && !response.body().choices.isEmpty()
                        && response.body().choices.get(0).message != null) {

                    String answer = response.body().choices.get(0).message.content;

                    if (answer != null && !answer.isEmpty()) {
                        textView.setText(answer);
                        saveMessage(userMessage, answer);
                    } else {
                        textView.setText("Сервер вернул пустой ответ");
                    }

                } else {
                    String error = "Неизвестная ошибка";
                    if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (IOException e) {
                            error = e.getMessage();
                        }
                    }
                    textView.setText("Ошибка API: " + response.code() + "\n" + error);
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                button.setEnabled(true);
                textView.setText("Ошибка соединения: " + t.getMessage());
            }
        });
    }

    private void saveMessage(String userMessage, String aiResponse) {
        ChatMessage message = new ChatMessage(userMessage, aiResponse);

        executorService.execute(() -> {
            messageDao.insert(message);
            runOnUiThread(() -> {
                editText.setText("");
                Toast.makeText(MainActivity.this, "Сообщение сохранено", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onMessageClick(ChatMessage message) {
        textView.setText(message.getAiResponse());
        editText.setText(message.getUserMessage());
    }

    @Override
    public void onMessageLongClick(ChatMessage message) {
        showDeleteDialog(message);
    }

    private void showDeleteDialog(ChatMessage message) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить сообщение")
                .setMessage("Удалить этот диалог?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    executorService.execute(() -> {
                        messageDao.delete(message);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Сообщение удалено", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Очистить историю")
                .setMessage("Удалить все сообщения?")
                .setPositiveButton("Удалить все", (dialog, which) -> {
                    executorService.execute(() -> {
                        messageDao.deleteAll();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "История очищена", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}