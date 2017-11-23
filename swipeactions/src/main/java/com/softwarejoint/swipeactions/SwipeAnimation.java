package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

abstract class SwipeAnimation implements Animator.AnimatorListener {

    private static final String TAG = "SimpleAnimation";

    final float startX;
    float swipeDistanceX;
    float mFraction;
    float updatedX;

    private RecyclerView recyclerView;
    private RecyclerView.ViewHolder holder;
    private ValueAnimator mValueAnimator;
    private boolean mEnded;

    SwipeAnimation(RecyclerView recyclerView, RecyclerView.ViewHolder holder, float swipeDistanceX) {
        this.recyclerView = recyclerView;
        this.holder = holder;
        this.swipeDistanceX = swipeDistanceX;
        mValueAnimator = ValueAnimator.ofFloat(0f, 1f);
        mValueAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        setFraction(animation.getAnimatedFraction());
                    }
                });
        mValueAnimator.setTarget(holder.itemView);
        mValueAnimator.addListener(this);
        startX = holder.itemView.getTranslationX();
        setFraction(0f);
    }

    private void setFraction(float fraction) {
        mFraction = fraction;
        update();
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.invalidate();
            }
        });
    }

    void start() {
        holder.setIsRecyclable(false);
        mValueAnimator.start();
    }

    boolean isEnded() {
        return mEnded;
    }

    @Override
    public void onAnimationStart(Animator animator) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (!mEnded) {
            holder.setIsRecyclable(true);
        }
        mEnded = true;
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        setFraction(0f);
        Log.d(TAG, "onAnimationCancel");
    }

    @Override
    public void onAnimationRepeat(Animator animator) {

    }

    void setDuration(long duration) {
        mValueAnimator.setDuration(duration);
    }

    RecyclerView.ViewHolder getHolder() {
        return holder;
    }

    abstract void update();

    float getDx() {
        return updatedX;
    }

    void cancel() {
        mValueAnimator.cancel();
    }
}
