package com.amicus.testapk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<ChatMessage> messageList;
    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(ChatMessage message);
        void onMessageLongClick(ChatMessage message);
    }

    public HistoryAdapter(OnMessageClickListener listener) {
        this.listener = listener;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView userMessageText;
        private TextView aiResponseText;
        private TextView timeText;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            userMessageText = itemView.findViewById(R.id.userMessageText);
            aiResponseText = itemView.findViewById(R.id.aiResponseText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(ChatMessage message) {
            userMessageText.setText("Вы: " + message.getUserMessage());
            aiResponseText.setText("ИИ: " + message.getAiResponse());

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            timeText.setText(sdf.format(new Date(message.getTimestamp())));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(message);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message);
                }
                return true;
            });
        }
    }
}