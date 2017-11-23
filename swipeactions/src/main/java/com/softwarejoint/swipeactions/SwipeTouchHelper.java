package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

public final class SwipeTouchHelper extends ItemTouchHelper.SimpleCallback implements
        View.OnTouchListener, RecyclerView.OnChildAttachStateChangeListener {

    private static final String TAG = "SwipeTouchHelper";

    private static final float MAX_ALPHA = 1.0f;
    private static final float SWIPE_THRESHOLD = 0.6f;
    private static final int PIXELS_PER_SECOND = 1000;
    private final Drawable deleteIcon;

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final ColorDrawable background;
    private final float mSwipeEscapeVelocity;
    private final float mMaxSwipeVelocity;
    private final int animationDuration;

    private float currentVelocity = 0;
    private VelocityTracker velocityTracker = null;
    private long itemId = Long.MIN_VALUE;
    private boolean swipedActionEnabled;

    private int viewHolderWidth;
    private int deleteIconLeft, deleteIconRight, deleteIconMargin, swipeVisibleMark;
    private boolean valuesComputed;
    private boolean isSwiping;
    private Rect iconRect;

    private SwipeOutAnimation swipeOutAnimation;
    private SwipeInAnimation swipeInAnimation;
    private OnSwipeItemClickedListener onSwipeItemClickedListener;

    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;

    private boolean isAttached;
    private SimpleItemDecoration itemDecoration;

    public SwipeTouchHelper(RecyclerView recyclerView, Drawable icon, OnSwipeItemClickedListener listener) {
        super(0, ItemTouchHelper.LEFT);

        onSwipeItemClickedListener = listener;

        deleteIcon = icon;
        intrinsicWidth = icon.getIntrinsicWidth();
        intrinsicHeight = icon.getIntrinsicHeight();
        background = new ColorDrawable();

        this.recyclerView = recyclerView;
        final Resources resources = recyclerView.getResources();

        mSwipeEscapeVelocity = resources.getDimension(R.dimen.swipe_escape_velocity);
        mMaxSwipeVelocity = resources.getDimension(R.dimen.swipe_max_velocity);
        animationDuration = resources.getInteger(android.R.integer.config_shortAnimTime);

        recyclerView.setOnTouchListener(this);

        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(this);
        }

        setSwipeBackGroundColor(Color.parseColor("#f44336"));
        itemDecoration = new SimpleItemDecoration(this);
        setSwipeHelperEnabled(true);
    }

    public void setSwipeHelperEnabled(boolean enabled) {
        if (enabled) {
            attachToRecyclerView(recyclerView);
            recyclerView.addItemDecoration(itemDecoration);
            recyclerView.addOnChildAttachStateChangeListener(this);
        } else {
            attachToRecyclerView(null);
            recyclerView.removeItemDecoration(itemDecoration);
            recyclerView.removeOnChildAttachStateChangeListener(this);
        }
    }

    public void setSwipeBackGroundColor(@ColorRes int resourceId) {
        background.setColor(resourceId);
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return mSwipeEscapeVelocity;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return mMaxSwipeVelocity;
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getMovementFlags(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (!isAttached) {
            return;
        }

        boolean holdPosition = swipedActionEnabled && isHoldVelocity();

        if (holdPosition) {
            itemId = viewHolder.getItemId();
        } else {
            clearItemHolder();
            viewHolder.itemView.setAlpha(1.0f);
            super.clearView(recyclerView, viewHolder);
        }
    }

    private boolean isItemIdValid() {
        return itemId != Long.MIN_VALUE;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.LEFT && onSwipeItemClickedListener != null) {
            onSwipeItemClickedListener.onSwipeActionClicked(viewHolder);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                obtainVelocityTracker();
                velocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, mMaxSwipeVelocity);
                currentVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
                break;
            case MotionEvent.ACTION_UP:
                if (!isSwiping) {
                    handleOnClick((RecyclerView) view, event);
                }
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, mMaxSwipeVelocity);
                currentVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
                releaseVelocityTracker();
                break;
            default:
                break;
        }
        return false;
    }

    private void computeValues(View itemView) {
        if (valuesComputed) return;

        int viewHolderHeight = itemView.getBottom() - itemView.getTop();
        viewHolderWidth = itemView.getWidth();

        deleteIconMargin = (viewHolderHeight - intrinsicHeight) / 2;

        deleteIconLeft = viewHolderWidth - intrinsicWidth - deleteIconMargin;
        deleteIconRight = viewHolderWidth - deleteIconMargin;
        swipeVisibleMark = intrinsicWidth + (deleteIconMargin * 2);

        valuesComputed = true;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (isCurrentlyActive) {
            isSwiping = true;
        }

        computeValues(viewHolder.itemView);

        if (isCurrentlyActive && isItemIdValid() && viewHolder.getItemId() != itemId) {
            closePreviousHolder(recyclerView);
        }

        onChildDraw(c, recyclerView, viewHolder, dX, dY, isCurrentlyActive);
    }

    private void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, boolean isCurrentlyActive) {
        float absDx = Math.abs(dX);

        if (swipeVisibleMark > absDx && viewHolder.getItemId() == itemId) {
            absDx = swipeVisibleMark;
        }

        float paintTillX = onChildDraw(c, viewHolder, absDx, isCurrentlyActive);
        super.onChildDraw(c, recyclerView, viewHolder, paintTillX, dY, ItemTouchHelper.ACTION_STATE_SWIPE, isCurrentlyActive);
    }

    private float onChildDraw(Canvas c, RecyclerView.ViewHolder viewHolder, float absDx, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        final int viewHolderTop = itemView.getTop();
        final int viewHolderBottom = itemView.getBottom();

        final int deleteIconTop = viewHolderTop + deleteIconMargin;
        final int deleteIconBottom = deleteIconTop + intrinsicHeight;

        final float swipedBackGroundX = absDx - swipeVisibleMark + deleteIconMargin;
        float paintTillX = absDx;

        if (isCurrentlyActive) {
            swipedActionEnabled = swipedBackGroundX >= 0 && isHoldVelocity();
            iconRect = new Rect(viewHolderWidth - swipeVisibleMark, viewHolderTop, viewHolderWidth, viewHolderBottom);
        } else if (swipedActionEnabled) {
            paintTillX = Math.max(absDx, swipeVisibleMark);
        }

        final float holderAlpha = MAX_ALPHA - (absDx / viewHolderWidth);
        itemView.setAlpha(holderAlpha);

        float swipeAlpha = MAX_ALPHA;

        if (swipedBackGroundX > 0) {
            swipeAlpha = MAX_ALPHA - (swipedBackGroundX / deleteIconLeft);
        }

        final int backGroundAlpha = (int) (255 * swipeAlpha);
        background.setAlpha(backGroundAlpha);

        // Draw the red delete background
        background.setBounds(viewHolderWidth - (int) paintTillX, viewHolderTop, viewHolderWidth, viewHolderBottom);
        background.draw(c);

        // Draw the delete icon
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
        deleteIcon.draw(c);

        return -paintTillX;
    }

    void handleOnClick(RecyclerView parent, MotionEvent event) {
        if (!isItemIdValid()) {
            return;
        }

        if (onSwipeItemClickedListener == null || iconRect == null || !iconRect.contains((int) event.getX(), (int) event.getY())) {
            closePreviousHolder(parent);
        } else {
            final RecyclerView.ViewHolder holder = parent.findViewHolderForItemId(itemId);

            if (holder == null) {
                closePreviousHolder(parent);
                return;
            }

            clearItemHolder();

            int animateX = viewHolderWidth - swipeVisibleMark;

            swipeOutAnimation = new SwipeOutAnimation(recyclerView, holder, animateX) {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    postDispatchSwipe(swipeOutAnimation);
                }
            };

            swipeOutAnimation.setDuration(animationDuration);
            swipeOutAnimation.start();
        }
    }

    void postDispatchSwipe(final SwipeOutAnimation anim) {
        // wait until animations are complete.
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (recyclerView.isAttachedToWindow()
                        && anim.getHolder().getAdapterPosition() != RecyclerView.NO_POSITION) {

                    final RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();

                    if ((animator == null || !animator.isRunning(null))) {
                        if (onSwipeItemClickedListener != null) {
                            onSwipeItemClickedListener.onSwipeActionClicked(anim.getHolder());
                        }
                    } else {
                        recyclerView.post(this);
                    }
                }
            }
        });
    }

    private void closePreviousHolder(RecyclerView recyclerView) {
        RecyclerView.ViewHolder prevHolder = recyclerView.findViewHolderForItemId(itemId);
        if (prevHolder == null) {
            return;
        }
        prevHolder.itemView.animate().alpha(1)
                .translationX(0)
                .setDuration(animationDuration).start();

        clearItemHolder();
    }

    private void clearItemHolder() {
        itemId = Long.MIN_VALUE;
        iconRect = null;
    }

    private boolean isHoldVelocity() {
        return currentVelocity < mSwipeEscapeVelocity;
    }

    private void clearSwipeTouchVars() {
        currentVelocity = 0;
        swipedActionEnabled = false;
        isSwiping = false;
    }

    private void obtainVelocityTracker() {
        clearSwipeTouchVars();
        if (velocityTracker != null) {
            velocityTracker.recycle();
        }
        velocityTracker = VelocityTracker.obtain();
    }

    private void releaseVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    void drawDecoration(Canvas c, final RecyclerView parent) {
        RecyclerView.ViewHolder holder = null;

        float dx = 0;

        if (swipeOutAnimation != null && !swipeOutAnimation.isEnded()) {
            holder = swipeOutAnimation.getHolder();
            dx = swipeOutAnimation.getDx();
        }

        if (holder == null && swipeInAnimation != null && !swipeInAnimation.isEnded()) {
            holder = swipeInAnimation.getHolder();
            dx = swipeInAnimation.getDx();
        }

        if (holder == null && isItemIdValid()) {
            holder = parent.findViewHolderForItemId(itemId);
            dx = -swipeVisibleMark;
        }

        if (holder == null) {
            return;
        }

        final float transY = holder.itemView.getTranslationY();

        final int count = c.save();
        onChildDraw(c, parent, holder, dx, transY, false);
        c.restoreToCount(count);
    }

    public void setOnSwipeItemClickedListener(OnSwipeItemClickedListener onSwipeItemClickedListener) {
        this.onSwipeItemClickedListener = onSwipeItemClickedListener;
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {

    }

    public void onChildViewDetachedFromWindow(View view) {
        final RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }

        if (swipeOutAnimation != null && swipeOutAnimation.getHolder() == holder) {
            if (!swipeOutAnimation.isEnded()) {
                swipeOutAnimation.cancel();
            }

            clearSwipeTouchVars();
            clearView(recyclerView, holder);
        }

        if (swipeInAnimation != null && swipeInAnimation.getHolder() == holder) {
            if (!swipeInAnimation.isEnded()) {
                swipeInAnimation.cancel();
            }

            clearSwipeTouchVars();
            clearView(recyclerView, holder);
        }
    }

    public void undoAction(RecyclerView.ViewHolder holder) {
        float animateX = Math.abs(holder.itemView.getTranslationX());
        attachToRecyclerView(null);
        swipeInAnimation = new SwipeInAnimation(recyclerView, holder, animateX) {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                attachToRecyclerView(recyclerView);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                attachToRecyclerView(recyclerView);
            }
        };
        swipeInAnimation.setDuration(animationDuration);
        swipeInAnimation.start();
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        isAttached = recyclerView != null;
        itemTouchHelper.attachToRecyclerView(recyclerView);
        clearSwipeTouchVars();
        clearItemHolder();
    }
}