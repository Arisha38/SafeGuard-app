package com.safeguard.womensafety;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityAlertAdapter extends RecyclerView.Adapter<CommunityAlertAdapter.Holder> {

    private final List<CommunityAlert> items = new ArrayList<>();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault());

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_alert, parent, false);
        return new Holder(card);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CommunityAlert item = items.get(position);
        holder.message.setText(item.message);
        holder.time.setText(dateFormat.format(new Date(item.sentAtMillis)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(@NonNull List<CommunityAlert> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addAtTop(@NonNull CommunityAlert item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;

        Holder(@NonNull MaterialCardView card) {
            super(card);
            message = card.findViewById(R.id.tvCommunityAlertItemMessage);
            time = card.findViewById(R.id.tvCommunityAlertItemTime);
        }
    }
}
