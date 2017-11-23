package com.softwarejoint.swipeactions;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;

public class SimpleItemDecoration extends RecyclerView.ItemDecoration {

    private SwipeTouchHelper itemTouchHelper;

    SimpleItemDecoration(SwipeTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        itemTouchHelper.drawDecoration(c, parent);
    }
}