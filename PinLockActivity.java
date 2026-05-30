package com.safeguard.womensafety;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinLockActivity extends AppCompatActivity {
    private static final String KEY_INPUT = "key_input";
    private static final String KEY_FIRST_PIN = "key_first_pin";
    private static final String KEY_MODE = "key_mode";
    private static final String KEY_ATTEMPTS = "key_attempts";

    private TextView tvTitle;
    private TextView tvDots;
    private StringBuilder currentInput = new StringBuilder();
    private String firstPin = "";
    private int attempts = 0;
    private Mode mode;

    private enum Mode {
        SETUP_CREATE,
        SETUP_CONFIRM,
        UNLOCK
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        tvTitle = findViewById(R.id.tvPinTitle);
        tvDots = findViewById(R.id.tvPinDots);
        Button forgotPin = findViewById(R.id.btnForgotPin);
        Button clear = findViewById(R.id.btnClearPin);
        Button delete = findViewById(R.id.btnDeletePin);

        if (savedInstanceState != null) {
            currentInput = new StringBuilder(savedInstanceState.getString(KEY_INPUT, ""));
            firstPin = savedInstanceState.getString(KEY_FIRST_PIN, "");
            attempts = savedInstanceState.getInt(KEY_ATTEMPTS, 0);
            mode = Mode.valueOf(savedInstanceState.getString(KEY_MODE, Mode.UNLOCK.name()));
        } else {
            mode = AppLockManager.hasPin(this) ? Mode.UNLOCK : Mode.SETUP_CREATE;
        }

        int[] digitIds = new int[]{
                R.id.btnDigit0, R.id.btnDigit1, R.id.btnDigit2, R.id.btnDigit3, R.id.btnDigit4,
                R.id.btnDigit5, R.id.btnDigit6, R.id.btnDigit7, R.id.btnDigit8, R.id.btnDigit9
        };
        for (int id : digitIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> appendDigit(b.getText().toString()));
        }

        clear.setOnClickListener(v -> {
            currentInput.setLength(0);
            updateDots();
        });
        delete.setOnClickListener(v -> {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
                updateDots();
            }
        });
        forgotPin.setOnClickListener(v -> {
            AppLockManager.clearPin(this);
            mode = Mode.SETUP_CREATE;
            firstPin = "";
            currentInput.setLength(0);
            attempts = 0;
            Toast.makeText(this, "PIN reset. Create a new PIN.", Toast.LENGTH_SHORT).show();
            refreshUi();
        });

        refreshUi();
    }

    private void appendDigit(String digit) {
        if (currentInput.length() >= 4) return;
        currentInput.append(digit);
        updateDots();
        if (currentInput.length() == 4) {
            processPin(currentInput.toString());
        }
    }

    private void processPin(String pin) {
        switch (mode) {
            case SETUP_CREATE:
                firstPin = pin;
                currentInput.setLength(0);
                mode = Mode.SETUP_CONFIRM;
                refreshUi();
                break;
            case SETUP_CONFIRM:
                if (firstPin.equals(pin)) {
                    AppLockManager.savePin(this, pin);
                    AppLockManager.setLocked(this, false);
                    openMain();
                } else {
                    Toast.makeText(this, "PINs do not match. Try again.", Toast.LENGTH_SHORT).show();
                    firstPin = "";
                    currentInput.setLength(0);
                    mode = Mode.SETUP_CREATE;
                    refreshUi();
                }
                break;
            case UNLOCK:
                if (AppLockManager.verifyPin(this, pin)) {
                    AppLockManager.setLocked(this, false);
                    openMainIfNeeded();
                } else {
                    attempts++;
                    currentInput.setLength(0);
                    updateDots();
                    if (attempts >= 3) {
                        Toast.makeText(this, "Too many wrong attempts. Be careful.", Toast.LENGTH_LONG).show();
                        attempts = 0;
                    } else {
                        Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    private void openMainIfNeeded() {
        if (getIntent().getBooleanExtra("from_background_lock", false)) {
            finish();
            return;
        }
        openMain();
    }

    private void openMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void refreshUi() {
        if (mode == Mode.UNLOCK) {
            tvTitle.setText("Enter your 4-digit PIN");
            findViewById(R.id.btnForgotPin).setVisibility(android.view.View.VISIBLE);
        } else if (mode == Mode.SETUP_CREATE) {
            tvTitle.setText("Create a 4-digit PIN");
            findViewById(R.id.btnForgotPin).setVisibility(android.view.View.GONE);
        } else {
            tvTitle.setText("Confirm your 4-digit PIN");
            findViewById(R.id.btnForgotPin).setVisibility(android.view.View.GONE);
        }
        updateDots();
    }

    private void updateDots() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            dots.append(i < currentInput.length() ? "\u25cf " : "\u25cb ");
        }
        tvDots.setText(dots.toString().trim());
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing lock screen.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_INPUT, currentInput.toString());
        outState.putString(KEY_FIRST_PIN, firstPin);
        outState.putString(KEY_MODE, mode.name());
        outState.putInt(KEY_ATTEMPTS, attempts);
    }
}
