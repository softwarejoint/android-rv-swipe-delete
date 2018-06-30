package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings({"WeakerAccess", "ClickableViewAccessibility"})
public final class SwipeTouchHelper extends ItemTouchHelper.SimpleCallback implements
        RecyclerView.OnChildAttachStateChangeListener, RecyclerView.OnItemTouchListener, View.OnTouchListener {

    private static final String TAG = "SwipeTouchHelper";

    private static final float MAX_ALPHA = 1.0f;
    private static final float SWIPE_THRESHOLD = 0.6f;
    private static final int PIXELS_PER_SECOND = 1000;
    private final Drawable deleteIcon;

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final float mSwipeEscapeVelocity;
    private final float mMaxSwipeVelocity;
    private final int animationDuration;

    private final ColorDrawable background;

    private float currentVelocity = 0;
    private VelocityTracker velocityTracker = null;

    private int viewHolderWidth;
    private int deleteIconLeft, deleteIconRight, deleteIconMargin, swipeVisibleMark;
    private float initialSwipeDX;
    private boolean valuesComputed;
    private boolean isSwiping;
    private float initY;
    private float mTouchYSlop;
    private boolean isTouchInvalidated;
    private long swipedItemId = RecyclerView.NO_ID;

    private SwipeOutAnimation swipeOutAnimation;
    private SwipeInAnimation swipeInAnimation;
    private OnSwipeItemClickedListener onSwipeItemClickedListener;

    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;

    private boolean isAttached;
    private boolean touchInputsViable;
    private SimpleItemDecoration itemDecoration;
    private Set<Long> items = new LinkedHashSet<>();
    private Set<Long> swipeAnimItems = new LinkedHashSet<>();

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

        ViewConfiguration configuration = ViewConfiguration.get(recyclerView.getContext());
        mTouchYSlop = configuration.getScaledTouchSlop() * 4;

        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(this);
        }

        setSwipeBackGroundColor(ContextCompat.getColor(recyclerView.getContext(), android.R.color.holo_purple));
        itemDecoration = new SimpleItemDecoration(this);
        setSwipeHelperEnabled(true);
    }

    public void setSwipeBackGroundColor(@ColorInt int resourceId) {
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
        if (!isAttached) return;

        final long itemId = viewHolder.getItemId();

        if (isItemIdValid(itemId)) return;

        viewHolder.itemView.setAlpha(1.0f);
        super.clearView(recyclerView, viewHolder);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.LEFT && onSwipeItemClickedListener != null) {
            onSwipeItemClickedListener.onSwipeActionClicked(viewHolder);
        }
    }

    private void computeValues(View itemView) {
        if (valuesComputed) return;

        final int viewHolderHeight = itemView.getBottom() - itemView.getTop();
        viewHolderWidth = itemView.getWidth();

        deleteIconMargin = (viewHolderHeight - intrinsicHeight) / 2;

        deleteIconLeft = viewHolderWidth - intrinsicWidth - deleteIconMargin;
        deleteIconRight = viewHolderWidth - deleteIconMargin;
        swipeVisibleMark = intrinsicWidth + (deleteIconMargin * 2);

        valuesComputed = true;

        mTouchYSlop = viewHolderHeight;
    }

    private boolean isItemIdValid(long itemId) {
        return items.contains(itemId);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        touchInputsViable = isCurrentlyActive;

        computeValues(viewHolder.itemView);

        final long itemId = viewHolder.getItemId();

        if (isCurrentlyActive) {
            if (!isSwiping) {
                initialSwipeDX = viewHolder.itemView.getTranslationX();
                closeAllHoldersExcept(recyclerView, itemId);
                isTouchInvalidated = false;
                swipedItemId = itemId;
            }

            isSwiping = true;
        } else {
            initialSwipeDX = 0;
            if (dX > 0 && currentVelocity == mMaxSwipeVelocity) {
                //mMaxSwipeVelocity is less than escape velocity so this means user flinged in some directions
                //we do not delete on fling, so just remove the item from being tracked
                items.remove(itemId);
            }
        }

        if (isTouchInvalidated) return;

        float absDx = Math.abs(dX) + initialSwipeDX;

        if (isCurrentlyActive) {
            if (absDx - swipeVisibleMark >= 0 && currentVelocity < mMaxSwipeVelocity) {
                if (items.add(itemId)) drawDecoration(c, viewHolder, absDx);
            } else {
                items.remove(itemId);
            }

            if (!items.contains(itemId)) {
                drawDecoration(c, viewHolder, absDx);
            }
        } else if (isItemIdValid(itemId)) {
            absDx = Math.max(absDx, swipeVisibleMark);
        }

        Log.d(TAG, "onTouch: " + itemId + " transX: " + viewHolder.itemView.getTranslationX() + " ac: " + isCurrentlyActive);

        super.onChildDraw(c, recyclerView, viewHolder, -absDx, dY, ItemTouchHelper.ACTION_STATE_SWIPE, false);
    }

    void drawDecoration(Canvas c, final RecyclerView parent) {
        RecyclerView.ViewHolder holder = null;

        ArrayList<Long> drawnItems = new ArrayList<>();

        if (swipeOutAnimation != null && !swipeOutAnimation.isEnded()) {
            holder = swipeOutAnimation.getHolder();
            drawDecoration(c, holder, Math.abs(swipeOutAnimation.getDx()));
            drawnItems.add(holder.getItemId());
        }

        if (holder == null && swipeInAnimation != null && !swipeInAnimation.isEnded()) {
            holder = swipeInAnimation.getHolder();
            drawDecoration(c, holder, Math.abs(swipeInAnimation.getDx()));
            drawnItems.add(holder.getItemId());
        }

        for (long itemId: new ArrayList<>(swipeAnimItems)) {
            if (drawnItems.contains(itemId)) continue;

            holder = parent.findViewHolderForItemId(itemId);
            if (holder == null) continue;
            float tDx = Math.abs(holder.itemView.getTranslationX());
            drawDecoration(c, holder, tDx);
            drawnItems.add(itemId);

            if (!touchInputsViable && swipedItemId == itemId) {
                isTouchInvalidated = true;
            }

            if (tDx == 0) {
                items.remove(itemId);
                swipeAnimItems.remove(itemId);
            }
        }

        for (long itemId: new ArrayList<>(items)) {
            holder = parent.findViewHolderForItemId(itemId);
            if (holder == null) continue;

            float absDx = Math.abs(holder.itemView.getTranslationX());

            if (!drawnItems.contains(itemId)) {
                drawDecoration(c, holder, absDx);
            }

            if (!touchInputsViable && swipedItemId == itemId) {
                isTouchInvalidated = true;
            }

            if (absDx == 0) {
                items.remove(itemId);
                swipeAnimItems.remove(itemId);
            }
        }
    }

    private void drawDecoration(Canvas c, RecyclerView.ViewHolder viewHolder, float absDx) {
        Log.d(TAG, "drawDecoration: itemId: " + viewHolder.getItemId() + " xTranslation: " + absDx);
        final int count = c.save();

        View itemView = viewHolder.itemView;

        final int viewHolderTop = itemView.getTop();
        final int viewHolderBottom = itemView.getBottom();

        final int deleteIconTop = viewHolderTop + deleteIconMargin;
        final int deleteIconBottom = deleteIconTop + intrinsicHeight;

        final float holderAlpha = MAX_ALPHA - (absDx / viewHolderWidth);
        itemView.setAlpha(holderAlpha);

        float swipeAlpha = MAX_ALPHA;

        if (absDx > 0) {
            swipeAlpha = MAX_ALPHA - (absDx / deleteIconLeft);
        }

        final int backGroundAlpha = (int) (255 * swipeAlpha);
        background.setAlpha(backGroundAlpha);

        // Draw the red delete background
        background.setBounds(viewHolderWidth - (int) absDx, viewHolderTop, viewHolderWidth, viewHolderBottom);
        background.draw(c);

        // Draw the delete icon
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
        deleteIcon.draw(c);

        c.restoreToCount(count);

        viewHolder.itemView.setTranslationX(-absDx);
    }

    private void handleOnClick(RecyclerView parent, MotionEvent event) {
        if (onSwipeItemClickedListener == null) {
            closeAllHoldersExcept(parent, Long.MIN_VALUE);
            return;
        }

        RecyclerView.ViewHolder clickedHolder = null;
        long matchedItemId = Long.MIN_VALUE;

        for (long itemId : new ArrayList<>(items)) {
            RecyclerView.ViewHolder holder = parent.findViewHolderForItemId(itemId);

            if (holder == null) {
                continue;
            }

            int viewHolderTop = holder.itemView.getTop();
            int viewHolderBottom = holder.itemView.getBottom();

            Rect iconRect = new Rect(viewHolderWidth - swipeVisibleMark, viewHolderTop, viewHolderWidth, viewHolderBottom);

            if (iconRect.contains((int) event.getX(), (int) event.getY())) {
                clickedHolder = holder;
                matchedItemId = itemId;
            } else {
                undoAction(holder);
            }
        }

        if (clickedHolder == null) return;

        items.remove(matchedItemId);
        swipeAnimItems.add(matchedItemId);

        int animateX = viewHolderWidth - swipeVisibleMark;

        final long finalMatchedItemId = matchedItemId;

        swipeOutAnimation = new SwipeOutAnimation(recyclerView, clickedHolder, animateX) {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                swipeAnimItems.remove(finalMatchedItemId);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                postDispatchSwipe(swipeOutAnimation, finalMatchedItemId);
            }
        };

        swipeOutAnimation.setDuration(animationDuration);
        swipeOutAnimation.start();
    }

    void postDispatchSwipe(final SwipeOutAnimation anim, final long itemId) {
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
                } else  {
                    swipeAnimItems.remove(itemId);
                }
            }
        });
    }

    public void markActionComplete(long itemId) {
        swipeAnimItems.remove(itemId);
    }

    private void closeAllHoldersExcept(RecyclerView recyclerView, long currentItemId) {
        for (long itemId : new ArrayList<>(items)) {
            if (itemId == currentItemId) continue;
            RecyclerView.ViewHolder prevHolder = recyclerView.findViewHolderForItemId(itemId);
            if (prevHolder != null) undoAction(prevHolder);
        }

        for (long itemId : new ArrayList<>(swipeAnimItems)) {
            if (itemId == currentItemId) continue;
            RecyclerView.ViewHolder prevHolder = recyclerView.findViewHolderForItemId(itemId);
            if (prevHolder != null) undoAction(prevHolder);
        }
    }

    public void undoAction(RecyclerView.ViewHolder holder) {
        float animateX = Math.abs(holder.itemView.getTranslationX());

        attachToRecyclerView(null);
        attachToRecyclerView(recyclerView);

        final long itemId = holder.getItemId();

        items.remove(itemId);
        swipeAnimItems.add(itemId);

        swipeInAnimation = new SwipeInAnimation(recyclerView, holder, animateX) {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                swipeAnimItems.remove(itemId);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                getDefaultUIUtil().clearView(holder.itemView);
                holder.itemView.setAlpha(MAX_ALPHA);
                swipeAnimItems.remove(itemId);
                super.onAnimationEnd(animation);
            }
        };

        swipeInAnimation.setDuration(animationDuration);
        swipeInAnimation.start();
    }

    private void clearSwipeTouchVars() {
        currentVelocity = 0;
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

    @SuppressWarnings("unused")
    public void setOnSwipeItemClickedListener(OnSwipeItemClickedListener onSwipeItemClickedListener) {
        this.onSwipeItemClickedListener = onSwipeItemClickedListener;
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {

    }

    public void onChildViewDetachedFromWindow(View view) {
        final RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder == null) return;

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

    private void attachToRecyclerView(RecyclerView recyclerView) {
        isAttached = recyclerView != null;
        itemTouchHelper.attachToRecyclerView(recyclerView);
        clearSwipeTouchVars();
    }

    public void setSwipeHelperEnabled(boolean enabled) {
        if (enabled) {
            attachToRecyclerView(recyclerView);
            recyclerView.addItemDecoration(itemDecoration);
            recyclerView.addOnChildAttachStateChangeListener(this);
            recyclerView.addOnItemTouchListener(this);
            recyclerView.setOnTouchListener(this);
        } else {
            attachToRecyclerView(null);
            recyclerView.removeOnItemTouchListener(this);
            recyclerView.removeItemDecoration(itemDecoration);
            recyclerView.removeOnChildAttachStateChangeListener(this);
            recyclerView.setOnTouchListener(null);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                obtainVelocityTracker();
                velocityTracker.addMovement(event);
                initY = (int) (event.getY() + 0.5f);
                break;
            case MotionEvent.ACTION_UP:
                if (!isSwiping) {
                    handleOnClick(rv, event);
                }
                break;
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        //Log.d(TAG, "onTouchEvent: " + e.getActionMasked());
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (velocityTracker == null) {
                    obtainVelocityTracker();
                }
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, mMaxSwipeVelocity);
                currentVelocity = velocityTracker.getXVelocity(pointerId);

                final int currY = (int) (event.getY() + 0.5f);

                if (!isSwiping && Math.abs(initY - currY) > mTouchYSlop) {
                    closeAllHoldersExcept(recyclerView, Long.MIN_VALUE);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, mMaxSwipeVelocity);
                currentVelocity = velocityTracker.getXVelocity(pointerId);
                releaseVelocityTracker();
                break;
            default:
                break;
        }

        return false;
    }
}