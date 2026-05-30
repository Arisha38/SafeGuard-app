package com.safeguard.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class VideoGalleryActivity extends AppCompatActivity {
    private static final int REQ_READ_VIDEO = 990;
    private final ArrayList<VideoItem> videos = new ArrayList<>();
    private VideoListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_gallery);

        RecyclerView recycler = findViewById(R.id.recyclerVideos);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoListAdapter(videos, new VideoListAdapter.VideoActionListener() {
            @Override
            public void onPlay(VideoItem item) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(item.uri, "video/*");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(i);
            }

            @Override
            public void onDelete(VideoItem item) {
                try {
                    int deleted = getContentResolver().delete(item.uri, null, null);
                    if (deleted > 0) {
                        loadVideos();
                        Toast.makeText(VideoGalleryActivity.this, "Video deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VideoGalleryActivity.this, "Unable to delete video", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(VideoGalleryActivity.this, "Unable to delete video", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onShare(VideoItem item) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("video/mp4");
                share.putExtra(Intent.EXTRA_STREAM, item.uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Share video"));
            }
        });
        recycler.setAdapter(adapter);

        ensurePermissionThenLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensurePermissionThenLoad();
    }

    private void ensurePermissionThenLoad() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQ_READ_VIDEO);
            return;
        }
        loadVideos();
    }

    private void loadVideos() {
        videos.clear();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RELATIVE_PATH
        };
        String selection = MediaStore.Video.Media.DISPLAY_NAME + " LIKE ? AND "
                + MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
        String[] args = new String[]{"safeguard_video_%", "%Movies/SafeGuard%"};
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (android.database.Cursor c = getContentResolver().query(collection, projection, selection, args, sortOrder)) {
            if (c == null) {
                adapter.notifyDataSetChanged();
                return;
            }
            int idxId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int idxName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int idxDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            while (c.moveToNext()) {
                long id = c.getLong(idxId);
                String name = c.getString(idxName);
                long dateMs = c.getLong(idxDate) * 1000L;
                Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                videos.add(new VideoItem(id, uri, name, dateMs));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load videos", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_VIDEO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                Toast.makeText(this, "Video permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
