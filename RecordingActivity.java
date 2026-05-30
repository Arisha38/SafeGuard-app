package com.safeguard.womensafety;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class RecordingActivity extends AppCompatActivity {
    private static final int REQ_RECORD_AUDIO = 700;
    private MediaRecorder recorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private File currentRecordingFile;
    private File recordingsDir;
    private TextView status;
    private Button toggle;
    private final ArrayList<RecordingItem> recordings = new ArrayList<>();
    private RecordingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        status = findViewById(R.id.tvRecordingStatus);
        toggle = findViewById(R.id.btnRecordToggle);
        RecyclerView recyclerView = findViewById(R.id.recyclerRecordings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordingsAdapter(recordings, new RecordingsAdapter.RecordingActionListener() {
            @Override
            public void onPlay(RecordingItem item) {
                playRecording(item.file);
            }

            @Override
            public void onDelete(RecordingItem item) {
                deleteRecording(item.file);
            }
        });
        recyclerView.setAdapter(adapter);

        recordingsDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        loadExistingRecordings();
        updateStatusText();

        toggle.setOnClickListener(v -> {
            if (!isRecording) {
                ensurePermissionAndStartRecording();
            } else {
                stopRecording();
            }
        });
    }

    private void ensurePermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startRecording();
    }

    private void startRecording() {
        releasePlayer();
        if (recordingsDir == null) {
            Toast.makeText(this, "Storage unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
            Toast.makeText(this, "Unable to access recordings folder", Toast.LENGTH_SHORT).show();
            return;
        }

        currentRecordingFile = new File(recordingsDir,
                "safeguard_recording_" + System.currentTimeMillis() + ".3gp");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(currentRecordingFile.getAbsolutePath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            status.setText("Recording...");
            toggle.setText("Stop Recording");
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start recorder", Toast.LENGTH_SHORT).show();
            currentRecordingFile = null;
        }
    }

    private void stopRecording() {
        try {
            recorder.stop();
        } catch (Exception ignored) {
        }
        recorder.release();
        recorder = null;
        isRecording = false;
        toggle.setText("Start Recording");

        if (currentRecordingFile != null && currentRecordingFile.exists()) {
            addRecordingToList(currentRecordingFile);
            Toast.makeText(this, "Recording saved successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save recording", Toast.LENGTH_SHORT).show();
        }
        currentRecordingFile = null;
        updateStatusText();
    }

    private void loadExistingRecordings() {
        recordings.clear();
        if (recordingsDir == null) {
            adapter.notifyDataSetChanged();
            return;
        }
        if (!recordingsDir.exists()) recordingsDir.mkdirs();

        File[] files = recordingsDir.listFiles((dir, name) ->
                name.startsWith("safeguard_recording_") && name.endsWith(".3gp"));

        if (files == null || files.length == 0) {
            adapter.notifyDataSetChanged();
            return;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            String dateTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(new Date(file.lastModified()));
            recordings.add(new RecordingItem(file, file.getName(), dateTime));
        }
        adapter.notifyDataSetChanged();
    }

    private void addRecordingToList(File file) {
        if (file == null || !file.exists()) return;
        String dateTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(file.lastModified()));
        recordings.add(0, new RecordingItem(file, file.getName(), dateTime));
        adapter.notifyItemInserted(0);
    }

    private void playRecording(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            releasePlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> releasePlayer());
            mediaPlayer.start();
            Toast.makeText(this, "Playing recording", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            releasePlayer();
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecording(File file) {
        releasePlayer();
        if (file == null || !file.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean deleted = file.delete();
        if (!deleted) {
            Toast.makeText(this, "Unable to delete recording", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < recordings.size(); i++) {
            if (recordings.get(i).file.getAbsolutePath().equals(file.getAbsolutePath())) {
                recordings.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
        updateStatusText();
        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
    }

    private void updateStatusText() {
        status.setText(recordings.isEmpty() ? "Recorder Idle" : "Total recordings: " + recordings.size());
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) stopRecording();
        releasePlayer();
    }
}
