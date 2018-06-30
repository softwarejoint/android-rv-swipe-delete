package com.softwarejoint.sample;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

    private static final String TAG = "SimpleAdapter";

    private static final int nextBound = 10 * 365 * 24 * 3600;

    private CoordinatorLayout mLayout;
    private ArrayList<RowItem> items;
    private int itemId = 0;
    private @ColorInt int colorInt = -1;

    SimpleAdapter(RecyclerView recyclerView) {
        items = new ArrayList<>();
        setHasStableIds(true);
        mLayout = (CoordinatorLayout) recyclerView.getParent();
    }

    public void setSnackBarColor(@ColorInt int color) {
        colorInt = color;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
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
            items.add(new RowItem(itemId++));
        }

        notifyItemRangeInserted(startPosition, itemCount);
    }

    public RowItem removeAt(final int position) {
        final RowItem rowItem = items.remove(position);
        notifyItemRemoved(position);
        Snackbar snackbar = Snackbar.make(mLayout, "rowItem: " + rowItem.text + " : " + position + " removed.", Snackbar.LENGTH_LONG);
        if (colorInt != -1) {
            snackbar.setActionTextColor(colorInt);
        }

        snackbar.setAction("Undo", view -> {
            undoLastDelete(rowItem, position);
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                onItemDeleteConfirmed(rowItem, position);
            }
        });

        snackbar.show();

        return rowItem;
    }

    protected void onItemDeleteConfirmed(RowItem rowItem, int position) {
        Log.d(TAG, "onItemDeleteConfirmed: " + position);
    }

    private void undoLastDelete(RowItem item, int position) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvName;
        final TextView tvDate;
        final TextView tvLastMsg;
        final TextView tvBadge;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_row_username);
            tvDate = itemView.findViewById(R.id.tv_chat_row_time);
            tvLastMsg = itemView.findViewById(R.id.tv_chat_row_last_message);
            tvBadge = itemView.findViewById(R.id.tv_chat_row_badge);
        }

        void configure(RowItem item) {
            tvName.setText(item.text);
            tvDate.setText(createDate());
            tvLastMsg.setText(UUID.randomUUID().toString().substring(0, 5));

            final int badge = new Random().nextInt(2);

            if (badge == 0) {
                tvBadge.setVisibility(View.GONE);
            } else {
                tvBadge.setVisibility(View.VISIBLE);
            }

            tvBadge.setText(String.format("(%s)", badge));
        }

        String createDate() {
            final long randomTime = System.currentTimeMillis() + new Random().nextInt(nextBound);
            DateFormat format = DateFormat.getDateTimeInstance();
            return format.format(new Date(randomTime));
        }
    }
}