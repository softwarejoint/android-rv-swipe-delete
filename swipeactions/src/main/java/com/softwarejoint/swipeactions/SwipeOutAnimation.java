package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.support.v7.widget.RecyclerView;

abstract class SwipeOutAnimation extends SwipeAnimation {

    SwipeOutAnimation(RecyclerView recyclerView, RecyclerView.ViewHolder holder, float swipeDistanceX) {
        super(recyclerView, holder, swipeDistanceX);
    }

    @Override
    void update() {
        updatedX = startX - (swipeDistanceX * mFraction);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        onAnimationEnded(this);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        super.onAnimationCancel(animator);
        onAnimationCancelled(this);
    }

    public abstract void onAnimationCancelled(SwipeOutAnimation animation);

    public abstract void onAnimationEnded(SwipeOutAnimation animation);
}