package com.safeguard.womensafety;

import android.graphics.Bitmap;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoVH> {
    public interface VideoActionListener {
        void onPlay(VideoItem item);
        void onDelete(VideoItem item);
        void onShare(VideoItem item);
    }

    private final List<VideoItem> items;
    private final VideoActionListener listener;

    public VideoListAdapter(List<VideoItem> items, VideoActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoVH holder, int position) {
        VideoItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(item.dateMs));
        holder.btnPlay.setOnClickListener(v -> listener.onPlay(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        holder.btnShare.setOnClickListener(v -> listener.onShare(item));

        try {
            Bitmap bmp;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                bmp = holder.itemView.getContext().getContentResolver()
                        .loadThumbnail(item.uri, new android.util.Size(180, 180), null);
            } else {
                bmp = MediaStore.Video.Thumbnails.getThumbnail(
                        holder.itemView.getContext().getContentResolver(),
                        item.id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                );
            }
            holder.ivThumb.setImageBitmap(bmp);
        } catch (Exception e) {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VideoVH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvDate;
        Button btnPlay, btnDelete, btnShare;

        VideoVH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivVideoThumb);
            tvName = itemView.findViewById(R.id.tvVideoName);
            tvDate = itemView.findViewById(R.id.tvVideoDate);
            btnPlay = itemView.findViewById(R.id.btnPlayVideo);
            btnDelete = itemView.findViewById(R.id.btnDeleteVideo);
            btnShare = itemView.findViewById(R.id.btnShareVideo);
        }
    }
}
