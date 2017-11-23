package com.softwarejoint.swipeactions;

import android.support.v7.widget.RecyclerView;

class SwipeInAnimation extends SwipeAnimation {

    SwipeInAnimation(RecyclerView recyclerView, RecyclerView.ViewHolder holder, float swipeDistanceX) {
        super(recyclerView, holder, swipeDistanceX);
    }

    @Override
    void update() {
        updatedX = startX + (swipeDistanceX * mFraction);
    }
}
