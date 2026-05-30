package com.safeguard.womensafety;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FakeCallActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        EditText etCallerName = findViewById(R.id.etCallerName);
        EditText etDelay = findViewById(R.id.etDelay);
        Button btnStart = findViewById(R.id.btnStartFakeCall);

        btnStart.setOnClickListener(v -> {
            String caller = etCallerName.getText().toString().trim();
            String delayInput = etDelay.getText().toString().trim();
            if (caller.isEmpty()) caller = "Unknown Caller";
            int seconds = 5;
            if (!delayInput.isEmpty()) seconds = Integer.parseInt(delayInput);

            String finalCaller = caller;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent i = new Intent(this, IncomingCallActivity.class);
                i.putExtra("caller_name", finalCaller);
                startActivity(i);
            }, seconds * 1000L);
            Toast.makeText(this, "Fake call scheduled", Toast.LENGTH_SHORT).show();
        });
    }
}
