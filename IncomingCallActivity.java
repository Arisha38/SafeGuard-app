package com.safeguard.womensafety;

import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IncomingCallActivity extends AppCompatActivity {
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        setVolumeControlStream(AudioManager.STREAM_RING);
        String caller = getIntent().getStringExtra("caller_name");
        TextView tv = findViewById(R.id.tvIncomingCaller);
        tv.setText("Incoming call from\n" + (caller == null ? "Unknown Caller" : caller));

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone != null) ringtone.play();

        Button end = findViewById(R.id.btnEndFakeCall);
        end.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }
}
