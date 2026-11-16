package com.amicus.testapk;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    long insert(ChatMessage message);

    @Delete
    void delete(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    LiveData<List<ChatMessage>> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}