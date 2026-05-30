package com.safeguard.womensafety;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ContactFormActivity extends AppCompatActivity {
    private ContactsDbHelper dbHelper;
    private EditText etName, etPhone;
    private int contactId = -1;
    private FirebaseHelper firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_form);

        dbHelper = new ContactsDbHelper(this);
        firebase = new FirebaseHelper(this);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        Button btnSave = findViewById(R.id.btnSave);

        contactId = getIntent().getIntExtra("contact_id", -1);
        if (contactId != -1) {
            Contact c = dbHelper.getContactById(contactId);
            if (c != null) {
                etName.setText(c.name);
                etPhone.setText(c.phone);
            }
        }

        btnSave.setOnClickListener(v -> saveContact());
    }

    private void saveContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contactId == -1) {
            long newId = dbHelper.addContact(name, phone);
            Contact c = new Contact((int) newId, name, phone);
            firebase.upsertEmergencyContact(c, new FirebaseHelper.CompletionCallback() {
                @Override public void onSuccess() {}
                @Override public void onError(@NonNull String message) {}
            });
            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateContact(contactId, name, phone);
            Contact c = new Contact(contactId, name, phone);
            firebase.upsertEmergencyContact(c, new FirebaseHelper.CompletionCallback() {
                @Override public void onSuccess() {}
                @Override public void onError(@NonNull String message) {}
            });
            Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
