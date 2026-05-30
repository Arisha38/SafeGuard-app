package com.safeguard.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.safeguard.womensafety.databinding.ActivityCommunityAlertBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityAlertActivity extends AppCompatActivity {

    public static final String ACTION_COMMUNITY_ALERT = "com.safeguard.womensafety.ALERT";

    private static final int REQ_SMS_SHARE = 904;

    private ActivityCommunityAlertBinding binding;
    private CommunityAlertAdapter adapter;
    private ContactsDbHelper contactsDb;
    private String pendingSmsBody;
    private final DateFormat shareTimeFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        contactsDb = new ContactsDbHelper(this);

        binding.toolbarCommunityAlert.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        adapter = new CommunityAlertAdapter();
        binding.recyclerCommunityAlerts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCommunityAlerts.setAdapter(adapter);
        binding.recyclerCommunityAlerts.setNestedScrollingEnabled(false);

        List<CommunityAlert> saved = CommunityAlertStorage.load(this);
        adapter.setItems(saved);
        updateEmptyState();

        binding.btnSendCommunityAlert.setOnClickListener(v -> sendAlert());
    }

    private void sendAlert() {
        String message = binding.etCommunityAlert.getText().toString().trim();
        if (message.isEmpty()) {
            binding.tilCommunityAlert.setError(getString(R.string.community_alert_validation_empty));
            return;
        }
        binding.tilCommunityAlert.setError(null);

        long now = System.currentTimeMillis();
        CommunityAlert item = new CommunityAlert(now, message);
        CommunityAlertStorage.prepend(this, item);
        adapter.addAtTop(item);
        binding.recyclerCommunityAlerts.scrollToPosition(0);

        Intent intent = new Intent(ACTION_COMMUNITY_ALERT);
        intent.putExtra("alert_message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        binding.etCommunityAlert.setText("");
        updateEmptyState();
        Toast.makeText(this, R.string.community_alert_send_success, Toast.LENGTH_SHORT).show();

        final String shareBody = getString(
                R.string.community_alert_share_body,
                message,
                shareTimeFormat.format(new Date(now)));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.community_alert_share_title)
                .setMessage(R.string.community_alert_share_message)
                .setPositiveButton(R.string.community_alert_share_sms_contacts, (d, w) -> shareSmsToAllContacts(shareBody))
                .setNeutralButton(R.string.community_alert_share_apps, (d, w) -> openShareChooser(shareBody))
                .setNegativeButton(R.string.community_alert_share_not_now, (d, w) -> d.dismiss())
                .show();
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        binding.tvCommunityStatus.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.recyclerCommunityAlerts.setVisibility(empty ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.tvCommunityAlertsSectionLabel.setVisibility(empty ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void shareSmsToAllContacts(@NonNull String body) {
        List<Contact> contacts = contactsDb.getAllContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(this, R.string.community_alert_no_contacts, Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingSmsBody = body;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_SMS_SHARE);
            return;
        }
        dispatchSmsToContacts(contacts, body);
    }

    private void dispatchSmsToAllWithPending() {
        if (pendingSmsBody == null) {
            return;
        }
        String body = pendingSmsBody;
        pendingSmsBody = null;
        List<Contact> contacts = contactsDb.getAllContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(this, R.string.community_alert_no_contacts, Toast.LENGTH_LONG).show();
            return;
        }
        dispatchSmsToContacts(contacts, body);
    }

    private void dispatchSmsToContacts(@NonNull List<Contact> contacts, @NonNull String body) {
        SmsManager smsManager = SmsManager.getDefault();
        for (Contact c : contacts) {
            ArrayList<String> parts = smsManager.divideMessage(body);
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(c.phone, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(c.phone, null, body, null, null);
            }
        }
        Toast.makeText(this, getString(R.string.community_alert_sms_sent, contacts.size()), Toast.LENGTH_LONG).show();
    }

    private void openShareChooser(@NonNull String body) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(send, getString(R.string.community_alert_share_chooser_title)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_SMS_SHARE) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchSmsToAllWithPending();
        } else {
            pendingSmsBody = null;
            Toast.makeText(this, R.string.community_alert_sms_permission_denied, Toast.LENGTH_LONG).show();
        }
    }
}
