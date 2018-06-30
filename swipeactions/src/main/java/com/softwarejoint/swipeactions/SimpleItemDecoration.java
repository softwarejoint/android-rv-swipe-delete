package com.softwarejoint.swipeactions;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

public class SimpleItemDecoration extends RecyclerView.ItemDecoration {

    private static final String TAG = "SimpleItemDecoration";

    private SwipeTouchHelper itemTouchHelper;

    SimpleItemDecoration(SwipeTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        itemTouchHelper.drawDecoration(c, parent);
    }
}