package com.safeguard.womensafety;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactVH> {
    public interface ContactActionListener {
        void onEdit(Contact contact);
        void onDelete(Contact contact);
    }

    private final List<Contact> contacts;
    private final ContactActionListener listener;

    public ContactsAdapter(List<Contact> contacts, ContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactVH holder, int position) {
        Contact c = contacts.get(position);
        holder.tvName.setText(c.name);
        holder.tvPhone.setText(c.phone);
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(c));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(c));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;
        Button btnEdit, btnDelete;

        ContactVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
