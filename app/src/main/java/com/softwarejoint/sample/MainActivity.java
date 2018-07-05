package com.softwarejoint.sample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.softwarejoint.swipeactions.OnSwipeItemClickedListener;
import com.softwarejoint.swipeactions.SwipeTouchHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnSwipeItemClickedListener {

    private static final int seedItemCount = 5;
    private static final String TAG = "MainActivity";

    private SimpleAdapter adapter;
    private SwipeTouchHelper swipeTouchHelper;
    private RecyclerView recyclerView;
    private Handler uiThreadHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiThreadHandler = new Handler();
        initializeView();
    }

    private void initializeView() {
        recyclerView = findViewById(R.id.recyclerView);
        View addItemBtn = findViewById(R.id.addItemBtn);

        addItemBtn.setOnClickListener(this);

        adapter = new SimpleAdapter(recyclerView);
        adapter.addItems(seedItemCount);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        Drawable deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete_white_24);
        //noinspection ConstantConditions
        swipeTouchHelper = new SwipeTouchHelper(recyclerView, deleteIcon, this);
        swipeTouchHelper.setSwipeBackGroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
    }

    @Override
    protected void onStart() {
        super.onStart();
        uiThreadHandler.postDelayed(this::loopUpdates, 5000L);
    }

    //Refresh adapter contents after 5 seconds
    private void loopUpdates() {
        if (isFinishing() || isDestroyed()) return;
        uiThreadHandler.postDelayed(this::loopUpdates, 5000L);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addItemBtn:
                if (adapter == null) return;
                adapter.addItems(1);
                break;
            default:
                break;
        }
    }

    int randomVar = 0;

    @Override
    public void onSwipeActionClicked(final RecyclerView.ViewHolder viewHolder) {
        final long itemId = viewHolder.getItemId();

        randomVar = randomVar + 1;

        if (randomVar % 2 == 0) {
            //Delete row & present snackbar to undo
            uiThreadHandler.postDelayed(() -> swipeTouchHelper.undoAction(viewHolder), 200L);
        } else {
            //Undo delete row swipe action
            final int position = viewHolder.getAdapterPosition();
            uiThreadHandler.postDelayed(() -> {
                adapter.removeAt(position);
                swipeTouchHelper.markActionComplete(itemId);
            }, 200L);
        }
    }
}