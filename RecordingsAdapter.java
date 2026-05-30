package com.safeguard.womensafety;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.RecordingVH> {
    public interface RecordingActionListener {
        void onPlay(RecordingItem item);
        void onDelete(RecordingItem item);
    }

    private final List<RecordingItem> items;
    private final RecordingActionListener listener;

    public RecordingsAdapter(List<RecordingItem> items, RecordingActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recording, parent, false);
        return new RecordingVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingVH holder, int position) {
        RecordingItem item = items.get(position);
        holder.tvName.setText(item.displayName);
        holder.tvTime.setText(item.dateTime);
        holder.btnPlay.setOnClickListener(v -> listener.onPlay(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecordingVH extends RecyclerView.ViewHolder {
        TextView tvName, tvTime;
        Button btnPlay, btnDelete;

        RecordingVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecordingName);
            tvTime = itemView.findViewById(R.id.tvRecordingTime);
            btnPlay = itemView.findViewById(R.id.btnPlayItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
