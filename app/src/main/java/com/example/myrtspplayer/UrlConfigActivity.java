package com.example.myrtspplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class UrlConfigActivity extends AppCompatActivity {
    private EditText editTextUrl;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_config);

        editTextUrl = findViewById(R.id.editTextUrl);
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Retrieve URL from SharedPreferences or use a default value
        String defaultUrl = sharedPreferences.getString("streamUrl", "rtsp://192.168.10.75:8554/stream");

        // Set the retrieved URL as the text of EditText
        editTextUrl.setText(defaultUrl);

        Button buttonSave = findViewById(R.id.buttonSave);
        buttonSave.setOnClickListener(v -> {
            // Save the entered URL to SharedPreferences
            String enteredUrl = editTextUrl.getText().toString().trim();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("streamUrl", enteredUrl);
            editor.apply();

            // Return the new URL to MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("newUrl", enteredUrl);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }
}