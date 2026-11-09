package com.amicus.testapk;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.List;

import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText ageEditText;
    private Button addButton;
    private RecyclerView usersRecyclerView;

    private UserDao userDao;
    private UserAdapter userAdapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        ageEditText = findViewById(R.id.ageEditText);
        addButton = findViewById(R.id.addButton);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);

        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();

        userAdapter = new UserAdapter(this);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        loadUsers();

        addButton.setOnClickListener(v -> addUser());
    }

    private void loadUsers() {
        userDao.getAllUsers().observe(this, users -> {
            userAdapter.setUsers(users);
        });
    }

    private void addUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(name, email, age);

        executorService.execute(() -> {
            userDao.insert(user);
            runOnUiThread(() -> {
                clearFields();
                Toast.makeText(MainActivity.this, "User added", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void clearFields() {
        nameEditText.setText("");
        emailEditText.setText("");
        ageEditText.setText("");
    }

    @Override
    public void onUserClick(User user) {
        showEditDialog(user);
    }

    @Override
    public void onUserLongClick(User user) {
        showDeleteDialog(user);
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editEmail = dialogView.findViewById(R.id.editEmail);
        EditText editAge = dialogView.findViewById(R.id.editAge);

        editName.setText(user.getName());
        editEmail.setText(user.getEmail());
        editAge.setText(String.valueOf(user.getAge()));

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = editName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            String newAgeStr = editAge.getText().toString().trim();

            if (!newName.isEmpty() && !newEmail.isEmpty() && !newAgeStr.isEmpty()) {
                user.setName(newName);
                user.setEmail(newEmail);
                user.setAge(Integer.parseInt(newAgeStr));

                executorService.execute(() -> {
                    userDao.update(user);
                });

                Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executorService.execute(() -> {
                        userDao.delete(user);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
}