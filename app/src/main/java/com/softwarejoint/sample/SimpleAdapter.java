package com.softwarejoint.sample;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

    private ArrayList<RowItem> items;
    private int itemId = 0;

    SimpleAdapter() {
        items = new ArrayList<>();
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RowItem item = getItem(position);
        holder.configure(item);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    private RowItem getItem(int position) {
        return items.get(position);
    }

    @SuppressWarnings("SameParameterValue")
    void addItems(int itemCount) {
        int startPosition = items.size();
        for (int i = 0; i < itemCount; i++) {
            String item = "Item: " + UUID.randomUUID().toString().substring(0, 5);
            items.add(new RowItem(item, itemId++));
        }

        notifyItemRangeInserted(startPosition, itemCount);
    }

    void removeAt(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.rowName);
        }

        void configure(RowItem item) {
            tvName.setText(item.text);
        }
    }
}
