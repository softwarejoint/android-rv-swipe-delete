package com.softwarejoint.swipeactions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
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

@SuppressWarnings("WeakerAccess")
public final class SwipeTouchHelper extends ItemTouchHelper.SimpleCallback implements
        RecyclerView.OnChildAttachStateChangeListener, RecyclerView.OnItemTouchListener, View.OnTouchListener {

    private static final String TAG = "SwipeTouchHelper";

    private static final float MAX_ALPHA = 1.0f;
    private static final float SWIPE_THRESHOLD = 0.6f;
    private static final int PIXELS_PER_SECOND = 1000;
    private final Drawable deleteIcon;

    private final int mTouchSlop;
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
    private boolean valuesComputed;
    private boolean isSwiping;

    private SwipeOutAnimation swipeOutAnimation;
    private SwipeInAnimation swipeInAnimation;
    private OnSwipeItemClickedListener onSwipeItemClickedListener;

    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;

    private boolean isAttached;
    private SimpleItemDecoration itemDecoration;
    private Set<Long> items = new LinkedHashSet<>();

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

        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mTouchSlop = vc.getScaledTouchSlop();

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

        long itemId = viewHolder.getItemId();
        if (isItemIdValid(itemId)) {
            return;
        }

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

        int viewHolderHeight = itemView.getBottom() - itemView.getTop();
        viewHolderWidth = itemView.getWidth();

        deleteIconMargin = (viewHolderHeight - intrinsicHeight) / 2;

        deleteIconLeft = viewHolderWidth - intrinsicWidth - deleteIconMargin;
        deleteIconRight = viewHolderWidth - deleteIconMargin;
        swipeVisibleMark = intrinsicWidth + (deleteIconMargin * 2);

        valuesComputed = true;
    }

    private boolean isItemIdValid(long itemId) {
        return items.contains(itemId);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (isCurrentlyActive) {
            if (!isSwiping) {
                dX = dX + viewHolder.itemView.getTranslationX();
            }

            isSwiping = true;
        }

        computeValues(viewHolder.itemView);

        final long itemId = viewHolder.getItemId();

        if (isCurrentlyActive) {
            closeAllHoldersExcept(recyclerView, itemId);
        } else if (currentVelocity == mMaxSwipeVelocity) {
            items.remove(itemId);
        }

        onChildDraw(c, recyclerView, viewHolder, dX, dY, itemId, isCurrentlyActive);
    }

    private void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                             float dX, float dY, long itemId, boolean isCurrentlyActive) {
        float absDx = Math.abs(dX);

        if (!isCurrentlyActive && swipeVisibleMark > absDx && isItemIdValid(viewHolder.getItemId())) {
            absDx = swipeVisibleMark;
        }

        float paintTillX = onChildDraw(c, viewHolder, absDx, itemId, isCurrentlyActive);
        Log.d(TAG, "paintTillX: " + paintTillX + " absDx: " + absDx);
        super.onChildDraw(c, recyclerView, viewHolder, paintTillX, dY, ItemTouchHelper.ACTION_STATE_SWIPE, false);
    }

    private float onChildDraw(Canvas c, RecyclerView.ViewHolder viewHolder, float absDx, long itemId, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        final int viewHolderTop = itemView.getTop();
        final int viewHolderBottom = itemView.getBottom();

        final int deleteIconTop = viewHolderTop + deleteIconMargin;
        final int deleteIconBottom = deleteIconTop + intrinsicHeight;

        final float swipedBackGroundX = absDx - swipeVisibleMark + deleteIconMargin;
        float paintTillX = absDx;

        if (isCurrentlyActive) {
            if (swipedBackGroundX >= 0 && currentVelocity < mSwipeEscapeVelocity) {
                items.add(itemId);
            } else {
                items.remove(itemId);
            }
        } else if (isItemIdValid(viewHolder.getItemId())) {
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
                closeHolder(holder, itemId);
            }
        }

        if (clickedHolder == null) {
            return;
        }

        items.remove(matchedItemId);

        int animateX = viewHolderWidth - swipeVisibleMark;

        swipeOutAnimation = new SwipeOutAnimation(recyclerView, clickedHolder, animateX) {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                postDispatchSwipe(swipeOutAnimation);
            }
        };

        swipeOutAnimation.setDuration(animationDuration);
        swipeOutAnimation.start();
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

    private void closeAllHoldersExcept(RecyclerView recyclerView, long currentItemId) {
        for (long itemId : new ArrayList<>(items)) {
            if (itemId == currentItemId) {
                continue;
            }
            RecyclerView.ViewHolder prevHolder = recyclerView.findViewHolderForItemId(itemId);
            if (prevHolder != null) closeHolder(prevHolder, itemId);
        }
    }

    private void closeHolder(final RecyclerView.ViewHolder holder, long itemId) {
        if (holder.itemView.getTranslationX() < 0) {
            holder.itemView.animate().alpha(1)
                    .translationX(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            getDefaultUIUtil().clearView(holder.itemView);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            super.onAnimationCancel(animation);
                        }
                    }).setDuration(animationDuration).start();
        }
        items.remove(itemId);
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

    void drawDecoration(Canvas c, final RecyclerView parent) {
        RecyclerView.ViewHolder holder = null;

        if (swipeOutAnimation != null && !swipeOutAnimation.isEnded()) {
            holder = swipeOutAnimation.getHolder();
            drawDecoration(c, parent, holder, swipeOutAnimation.getDx(), holder.getItemId());
        }

        if (holder == null && swipeInAnimation != null && !swipeInAnimation.isEnded()) {
            holder = swipeInAnimation.getHolder();
            drawDecoration(c, parent, holder, swipeInAnimation.getDx(), holder.getItemId());
        }

        float dx = -swipeVisibleMark;

        for (long itemId : new ArrayList<>(items)) {
            holder = parent.findViewHolderForItemId(itemId);
            if (holder != null && holder.itemView.getTranslationX() == dx) {
                drawDecoration(c, parent, holder, dx, holder.getItemId());
            }
        }
    }

    private void drawDecoration(Canvas c, final RecyclerView parent, RecyclerView.ViewHolder holder, float dx, long itemId) {
        final float transY = holder.itemView.getTranslationY();

        final int count = c.save();
        onChildDraw(c, parent, holder, dx, transY, itemId, false);
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

    public void undoAction(RecyclerView.ViewHolder holder) {
        float animateX = Math.abs(holder.itemView.getTranslationX());

        attachToRecyclerView(null);
        attachToRecyclerView(recyclerView);

        items.remove(holder.getItemId());
        swipeInAnimation = new SwipeInAnimation(recyclerView, holder, animateX) {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                getDefaultUIUtil().clearView(holder.itemView);
                super.onAnimationEnd(animation);
            }
        };
        swipeInAnimation.setDuration(animationDuration);
        swipeInAnimation.start();
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
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                obtainVelocityTracker();
                velocityTracker.addMovement(event);
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