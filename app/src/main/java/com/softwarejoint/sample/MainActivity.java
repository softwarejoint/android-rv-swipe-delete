package com.softwarejoint.sample;

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

        adapter = new SimpleAdapter();
        adapter.addItems(seedItemCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);

        Drawable deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete_white_24);
        swipeTouchHelper = new SwipeTouchHelper(recyclerView, deleteIcon, this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addItemBtn:
                if (adapter == null) {
                    return;
                }
                adapter.addItems(1);
                break;
            default:
                break;
        }
    }

    @Override
    public void onSwipeActionClicked(final RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onSwipeActionClicked: " + viewHolder.getItemId() + " views: " + viewHolder.hashCode());
        uiThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onSwipeActionClicked: " + viewHolder.getItemId() + " hash : " + viewHolder.hashCode());
                swipeTouchHelper.undoAction(viewHolder);
            }
        }, 2000);
    }
}
