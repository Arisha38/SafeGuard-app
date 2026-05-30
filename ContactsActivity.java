package com.safeguard.womensafety;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {
    private ContactsDbHelper dbHelper;
    private final List<Contact> contacts = new ArrayList<>();
    private ContactsAdapter adapter;
    private FirebaseHelper firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        dbHelper = new ContactsDbHelper(this);
        firebase = new FirebaseHelper(this);
        RecyclerView recycler = findViewById(R.id.recyclerContacts);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactsAdapter(contacts, new ContactsAdapter.ContactActionListener() {
            @Override
            public void onEdit(Contact contact) {
                Intent i = new Intent(ContactsActivity.this, ContactFormActivity.class);
                i.putExtra("contact_id", contact.id);
                startActivity(i);
            }

            @Override
            public void onDelete(Contact contact) {
                dbHelper.deleteContact(contact.id);
                firebase.deleteEmergencyContact(contact, new FirebaseHelper.CompletionCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(@NonNull String message) {}
                });
                Toast.makeText(ContactsActivity.this, "Contact deleted", Toast.LENGTH_SHORT).show();
                loadContacts();
            }
        });
        recycler.setAdapter(adapter);

        Button add = findViewById(R.id.btnAddContact);
        add.setOnClickListener(v -> startActivity(new Intent(this, ContactFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void loadContacts() {
        contacts.clear();
        contacts.addAll(dbHelper.getAllContacts());
        adapter.notifyDataSetChanged();
    }
}
