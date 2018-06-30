package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.support.v7.widget.RecyclerView;

abstract class SwipeInAnimation extends SwipeAnimation {

    SwipeInAnimation(RecyclerView recyclerView, RecyclerView.ViewHolder holder, float swipeDistanceX) {
        super(recyclerView, holder, swipeDistanceX);
    }

    @Override
    void update() {
        updatedX = startX + (swipeDistanceX * mFraction);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        onAnimationEnded(this);
        super.onAnimationEnd(animation);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        super.onAnimationCancel(animator);
        onAnimationCancelled(this);
    }

    public abstract void onAnimationCancelled(SwipeInAnimation animation);

    public abstract void onAnimationEnded(SwipeInAnimation animation);
}
