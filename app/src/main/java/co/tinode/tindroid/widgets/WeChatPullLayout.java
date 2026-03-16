package co.tinode.tindroid.widgets;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Two-layer WeChat-style home container.
 *
 * Design goals:
 * - Keep RecyclerView scrolling fully native (including fast flings).
 * - When user pulls down at the top of the list, smoothly reveal a background panel
 *   without requiring a second gesture.
 * - When panel is open, upward scroll first closes the panel, then continues scrolling the list
 *   in the same gesture.
 *
 * This is implemented as a NestedScrollingParent: the foreground remains the nested scrolling child;
 * the parent consumes unconsumed downward scroll to open the panel and consumes upward pre-scroll
 * to close it.
 */
public class WeChatPullLayout extends ViewGroup implements NestedScrollingParent3 {
    private static final float OPEN_THRESHOLD = 0.12f;
    private static final float VELOCITY_EMA_ALPHA = 0.35f;
    private static final float DIRECT_DRAG_FACTOR = 0.98f;
    private static final float PULL_START_THRESHOLD_DP = 24f;
    private static final float PULL_START_EXTRA_THRESHOLD_DP = 64f;
    private static final float PULL_SOFT_MAX_DP = 10f;

    private enum TouchRegion {
        NONE,
        PANEL,
        HEADER
    }

    private final int mTouchSlop;
    private final int mMinFlingVelocity;
    private final NestedScrollingParentHelper mParentHelper;
    private final float mPullStartThresholdPx;
    private final float mPullStartExtraThresholdPx;
    private final float mSoftPullMaxPx;

    private View mPanelView;
    private View mForegroundView;

    private float mRevealRange;
    private float mCurrentOffset;
    private boolean mPullEnabled = true;
    private boolean mOpen;

    private float mPullDownAccumulator;
    private float mActivePullStartThresholdPx;
    private boolean mGestureStartedAtTop;
    private boolean mUseSoftPull;

    private TouchRegion mTouchRegion = TouchRegion.NONE;
    private boolean mDirectDragging;
    private boolean mThresholdHapticFired;
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;
    private float mInitialDownX;
    private float mInitialDownY;
    private float mLastY;
    private float mChildScrollCarryDown;
    private float mChildScrollCarryUp;

    @Nullable
    private VelocityTracker mVelocityTracker;

    private long mLastNestedScrollTimeMs;
    private float mEstimatedVelocityY;
    private boolean mHasVelocitySample;

    @Nullable
    private SpringAnimation mSpringAnimation;
    @Nullable
    private OnPanelStateListener mListener;

    public interface OnPanelStateListener {
        void onPanelOpened();
        void onPanelClosed();
    }

    public WeChatPullLayout(@NonNull Context context) {
        this(context, null);
    }

    public WeChatPullLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeChatPullLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mParentHelper = new NestedScrollingParentHelper(this);
        float density = context.getResources().getDisplayMetrics().density;
        mPullStartThresholdPx = Math.max(mTouchSlop * 2f, PULL_START_THRESHOLD_DP * density);
        mPullStartExtraThresholdPx = PULL_START_EXTRA_THRESHOLD_DP * density;
        mSoftPullMaxPx = PULL_SOFT_MAX_DP * density;
        mActivePullStartThresholdPx = mPullStartThresholdPx;
        setClipChildren(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalStateException("WeChatPullLayout must contain exactly 2 children");
        }
        mPanelView = getChildAt(0);
        mForegroundView = getChildAt(1);
    }

    public void setOnPanelStateListener(@Nullable OnPanelStateListener listener) {
        mListener = listener;
    }

    public void setPullEnabled(boolean enabled) {
        if (mPullEnabled == enabled) {
            return;
        }
        mPullEnabled = enabled;
        if (!enabled) {
            closePanel(false);
        }
    }

    public boolean isOpen() {
        return mOpen;
    }

    public void closePanel(boolean animate) {
        if (animate) {
            settleTo(0f, false, 0f);
        } else {
            stopAnimations();
            applyForegroundOffset(0f);
            dispatchPanelState(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int exactWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int exactHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        measureChildWithMargins(mPanelView, exactWidth, 0, exactHeight, 0);
        measureChildWithMargins(mForegroundView, exactWidth, 0, exactHeight, 0);

        mRevealRange = Math.max(0, mPanelView != null ? mPanelView.getMeasuredHeight() : 0);
        if (mCurrentOffset > mRevealRange) {
            applyForegroundOffset(mRevealRange);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        mPanelView.layout(0, 0, width, mPanelView.getMeasuredHeight());
        mForegroundView.layout(0, 0, width, height);
        mForegroundView.setTranslationY(mCurrentOffset);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mPullEnabled && mCurrentOffset <= 0f) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDirectDragging = false;
                mThresholdHapticFired = false;
                mActivePointerId = ev.getPointerId(0);
                mInitialDownX = ev.getX();
                mInitialDownY = ev.getY();
                mLastY = mInitialDownY;
                mTouchRegion = resolveTouchRegion(mInitialDownY);
                mChildScrollCarryDown = 0f;
                mChildScrollCarryUp = 0f;
                ensureVelocityTracker();
                mVelocityTracker.clear();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchRegion == TouchRegion.NONE) {
                    break;
                }
                int moveIndex = ev.findPointerIndex(mActivePointerId);
                if (moveIndex < 0) {
                    break;
                }
                float x = ev.getX(moveIndex);
                float y = ev.getY(moveIndex);
                float dxTotal = x - mInitialDownX;
                float dyTotal = y - mInitialDownY;
                if (Math.abs(dyTotal) > mTouchSlop && Math.abs(dyTotal) > Math.abs(dxTotal)) {
                    mDirectDragging = true;
                    mLastY = y;
                    stopAnimations();
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                resetDirectTouch();
                break;
            default:
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mPullEnabled && mCurrentOffset <= 0f) {
            return super.onTouchEvent(event);
        }

        ensureVelocityTracker();
        mVelocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopAnimations();
                mDirectDragging = false;
                mThresholdHapticFired = false;
                mActivePointerId = event.getPointerId(0);
                mInitialDownX = event.getX();
                mInitialDownY = event.getY();
                mLastY = mInitialDownY;
                mTouchRegion = resolveTouchRegion(mInitialDownY);
                mChildScrollCarryDown = 0f;
                mChildScrollCarryUp = 0f;
                return mTouchRegion != TouchRegion.NONE;
            case MotionEvent.ACTION_MOVE: {
                if (mTouchRegion == TouchRegion.NONE) {
                    break;
                }
                int moveIndex = event.findPointerIndex(mActivePointerId);
                if (moveIndex < 0) {
                    break;
                }
                float x = event.getX(moveIndex);
                float y = event.getY(moveIndex);
                float dy = y - mLastY;
                if (!mDirectDragging) {
                    float dxTotal = x - mInitialDownX;
                    float dyTotal = y - mInitialDownY;
                    if (Math.abs(dyTotal) > mTouchSlop && Math.abs(dyTotal) > Math.abs(dxTotal)) {
                        mDirectDragging = true;
                        stopAnimations();
                    }
                }
                mLastY = y;
                if (mDirectDragging) {
                    handleDirectDragDelta(dy);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mDirectDragging) {
                    float velocityY = 0f;
                    if (mVelocityTracker != null && mActivePointerId != MotionEvent.INVALID_POINTER_ID) {
                        mVelocityTracker.computeCurrentVelocity(1000);
                        velocityY = mVelocityTracker.getYVelocity(mActivePointerId);
                    }
                    finishDirectDrag(velocityY);
                    resetDirectTouch();
                    return true;
                }
                resetDirectTouch();
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    // Nested scrolling: legacy parent (type-less) methods delegate to TYPE_TOUCH.
    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    // NestedScrollingParent3 implementation.
    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return mPullEnabled && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
        stopAnimations();
        resetVelocityTracking();
        mThresholdHapticFired = false;
        mGestureStartedAtTop = type != ViewCompat.TYPE_TOUCH || !target.canScrollVertically(-1);
        if (mGestureStartedAtTop) {
            mActivePullStartThresholdPx = 0f;
            mUseSoftPull = false;
        } else {
            mActivePullStartThresholdPx = mPullStartThresholdPx + mPullStartExtraThresholdPx;
            mUseSoftPull = true;
        }
        syncPullAccumulatorToOffset();
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mParentHelper.onStopNestedScroll(target, type);
        if (type == ViewCompat.TYPE_TOUCH) {
            settleAfterNestedScroll();
        }
        resetVelocityTracking();
        syncPullAccumulatorToOffset();
        mGestureStartedAtTop = false;
        mActivePullStartThresholdPx = mPullStartThresholdPx;
        mUseSoftPull = false;
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (!mPullEnabled) {
            return;
        }
        // Never move the foreground due to non-touch (fling) scroll. This prevents accidental panel
        // reveal/close when the list flings into an edge.
        if (type != ViewCompat.TYPE_TOUCH) {
            return;
        }
        if (dy > 0 && mCurrentOffset > 0f) {
            float closeBy = Math.min(dy, mCurrentOffset);
            moveForegroundBy(-closeBy);
            consumed[1] += (int) closeBy;
            syncPullAccumulatorToOffset();
            recordVelocity(dy, type);
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        if (!mPullEnabled) {
            return;
        }
        if (type != ViewCompat.TYPE_TOUCH) {
            return;
        }
        if (dyUnconsumed < 0 && mCurrentOffset < mRevealRange) {
            float pullDown = -dyUnconsumed;
            float oldOffset = mCurrentOffset;
            float threshold = mActivePullStartThresholdPx;
            mPullDownAccumulator = clamp(mPullDownAccumulator + pullDown, 0f, threshold + mRevealRange);
            float desiredOffset;
            if (!mUseSoftPull || threshold <= 0f) {
                desiredOffset = clamp(mPullDownAccumulator - threshold, 0f, mRevealRange);
            } else if (mPullDownAccumulator <= threshold) {
                float t = clamp(mPullDownAccumulator / threshold, 0f, 1f);
                desiredOffset = clamp(mSoftPullMaxPx * t * t * t, 0f, mRevealRange);
            } else {
                desiredOffset = clamp(mSoftPullMaxPx + (mPullDownAccumulator - threshold), 0f, mRevealRange);
            }
            float deltaOffset = desiredOffset - oldOffset;
            if (deltaOffset > 0f) {
                moveForegroundBy(deltaOffset);
                // Parent consumed downward scroll (negative dy).
                consumed[1] += (int) -deltaOffset;
                syncPullAccumulatorToOffset();
            }
            recordVelocity(dyUnconsumed, type);
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type) {
        int[] parentConsumed = new int[2];
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, parentConsumed);
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

    @NonNull
    private TouchRegion resolveTouchRegion(float downY) {
        if (!mPullEnabled && mCurrentOffset <= 0f) {
            return TouchRegion.NONE;
        }
        // When the panel is fully closed, don't intercept touches on the header to avoid accidental
        // panel reveal while the user is just scrolling the list.
        if (mCurrentOffset <= 0f) {
            return TouchRegion.NONE;
        }
        float foregroundTop = mCurrentOffset;
        if (foregroundTop > 0f && downY < foregroundTop) {
            return TouchRegion.PANEL;
        }
        int headerHeight = resolveHeaderHeight();
        if (headerHeight > 0 && downY >= foregroundTop && downY < foregroundTop + headerHeight) {
            return TouchRegion.HEADER;
        }
        return TouchRegion.NONE;
    }

    private int resolveHeaderHeight() {
        if (!(mForegroundView instanceof ViewGroup)) {
            return 0;
        }
        ViewGroup group = (ViewGroup) mForegroundView;
        if (group.getChildCount() == 0) {
            return 0;
        }
        View header = group.getChildAt(0);
        return Math.max(0, header.getHeight() > 0 ? header.getHeight() : header.getMeasuredHeight());
    }

    private void handleDirectDragDelta(float dy) {
        if (!mPullEnabled && mCurrentOffset <= 0f) {
            return;
        }

        View scrollableChild = (mTouchRegion == TouchRegion.NONE) ? null : findScrollableChild(mForegroundView);
        float scaledDy = dy * DIRECT_DRAG_FACTOR;

        if (scaledDy > 0f) {
            float remaining = scaledDy;
            if (mTouchRegion == TouchRegion.HEADER && scrollableChild != null) {
                int before = currentScrollOffsetToTop(scrollableChild);
                if (before > 0) {
                    int requested = consumeScrollCarryDown(remaining);
                    scrollScrollableChild(scrollableChild, -requested);
                    int after = currentScrollOffsetToTop(scrollableChild);
                    float consumedByChild = Math.max(0, before - after);
                    remaining = Math.max(0f, remaining - consumedByChild);
                }
            }
            if (remaining > 0f) {
                moveForegroundBy(remaining);
            }
            return;
        }

        if (scaledDy < 0f) {
            float remaining = -scaledDy;
            if (mCurrentOffset > 0f) {
                float closing = Math.min(remaining, mCurrentOffset);
                moveForegroundBy(-closing);
                remaining -= closing;
            }
            if (remaining > 0f && scrollableChild != null) {
                int requested = consumeScrollCarryUp(remaining);
                scrollScrollableChild(scrollableChild, requested);
            }
        }
    }

    private void finishDirectDrag(float velocityY) {
        if (mRevealRange <= 0f) {
            return;
        }
        if (mCurrentOffset <= 0f) {
            applyForegroundOffset(0f);
            dispatchPanelState(false);
            return;
        }
        if (mCurrentOffset >= mRevealRange) {
            applyForegroundOffset(mRevealRange);
            dispatchPanelState(true);
            return;
        }

        if (Math.abs(velocityY) > mMinFlingVelocity) {
            boolean opening = velocityY > 0f;
            settleTo(opening ? mRevealRange : 0f, opening, velocityY);
            return;
        }

        boolean opening = mCurrentOffset >= mRevealRange * OPEN_THRESHOLD;
        settleTo(opening ? mRevealRange : 0f, opening, 0f);
    }

    private void settleAfterNestedScroll() {
        if (mRevealRange <= 0f) {
            return;
        }
        if (mCurrentOffset <= 0f) {
            applyForegroundOffset(0f);
            dispatchPanelState(false);
            syncPullAccumulatorToOffset();
            return;
        }
        if (mCurrentOffset >= mRevealRange) {
            applyForegroundOffset(mRevealRange);
            dispatchPanelState(true);
            syncPullAccumulatorToOffset();
            return;
        }

        if (mHasVelocitySample && Math.abs(mEstimatedVelocityY) > mMinFlingVelocity) {
            boolean opening = mEstimatedVelocityY < 0f;
            float target = opening ? mRevealRange : 0f;
            settleTo(target, opening, -mEstimatedVelocityY);
            return;
        }

        boolean opening = mCurrentOffset >= mRevealRange * OPEN_THRESHOLD;
        settleTo(opening ? mRevealRange : 0f, opening, 0f);
    }

    private void settleTo(float targetOffset, boolean opening, float startVelocityPxPerSec) {
        stopAnimations();

        SpringForce force = new SpringForce(targetOffset)
                .setStiffness(SpringForce.STIFFNESS_LOW)
                .setDampingRatio(opening
                        ? SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        : SpringForce.DAMPING_RATIO_NO_BOUNCY);

        mSpringAnimation = new SpringAnimation(mForegroundView, DynamicAnimation.TRANSLATION_Y, targetOffset);
        mSpringAnimation.setSpring(force);
        mSpringAnimation.setStartVelocity(startVelocityPxPerSec);
        mSpringAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
        mSpringAnimation.addUpdateListener((animation, value, velocity) -> mCurrentOffset = value);
        mSpringAnimation.addEndListener((animation, canceled, value, velocity) -> {
            if (canceled) {
                return;
            }
            mCurrentOffset = targetOffset;
            dispatchPanelState(targetOffset > 0f);
            syncPullAccumulatorToOffset();
        });
        mSpringAnimation.start();
    }

    private void dispatchPanelState(boolean open) {
        boolean prevOpen = mOpen;
        mOpen = open;
        if (prevOpen == open) {
            return;
        }
        if (mListener != null) {
            if (open) {
                mListener.onPanelOpened();
            } else {
                mListener.onPanelClosed();
            }
        }
    }

    private void moveForegroundBy(float delta) {
        if (delta == 0f) {
            return;
        }
        float before = mCurrentOffset;
        float nextOffset = clamp(before + delta, 0f, mRevealRange);
        applyForegroundOffset(nextOffset);
        if (delta > 0f) {
            maybeTriggerThresholdHaptic(before, nextOffset);
        }
    }

    private void maybeTriggerThresholdHaptic(float beforeOffset, float afterOffset) {
        if (mThresholdHapticFired) {
            return;
        }
        if (mRevealRange <= 0f) {
            return;
        }
        float threshold = mRevealRange * OPEN_THRESHOLD;
        if (beforeOffset < threshold && afterOffset >= threshold) {
            int feedback = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ?
                    HapticFeedbackConstants.CONFIRM :
                    HapticFeedbackConstants.CONTEXT_CLICK;
            performHapticFeedback(feedback);
            mThresholdHapticFired = true;
        }
    }

    private void applyForegroundOffset(float offset) {
        mCurrentOffset = offset;
        if (mForegroundView != null) {
            mForegroundView.setTranslationY(offset);
        }
    }

    private void syncPullAccumulatorToOffset() {
        if (mCurrentOffset <= 0f) {
            mPullDownAccumulator = 0f;
            return;
        }

        float threshold = mActivePullStartThresholdPx;
        if (!mUseSoftPull || threshold <= 0f || mSoftPullMaxPx <= 0f) {
            mPullDownAccumulator = threshold + mCurrentOffset;
            return;
        }

        if (mCurrentOffset <= mSoftPullMaxPx) {
            float ratio = clamp(mCurrentOffset / mSoftPullMaxPx, 0f, 1f);
            mPullDownAccumulator = (float) (threshold * Math.cbrt(ratio));
        } else {
            mPullDownAccumulator = threshold + (mCurrentOffset - mSoftPullMaxPx);
        }
    }

    private void stopAnimations() {
        if (mSpringAnimation != null) {
            mSpringAnimation.cancel();
            mSpringAnimation = null;
        }
        if (mForegroundView != null) {
            mForegroundView.animate().cancel();
        }
    }

    private void resetVelocityTracking() {
        mLastNestedScrollTimeMs = 0L;
        mEstimatedVelocityY = 0f;
        mHasVelocitySample = false;
    }

    private void recordVelocity(int dy, int type) {
        if (type != ViewCompat.TYPE_TOUCH) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (mHasVelocitySample && mLastNestedScrollTimeMs > 0L) {
            long dt = now - mLastNestedScrollTimeMs;
            if (dt > 0L) {
                float v = dy * 1000f / dt;
                mEstimatedVelocityY = mEstimatedVelocityY * (1f - VELOCITY_EMA_ALPHA) + v * VELOCITY_EMA_ALPHA;
            }
        } else {
            mHasVelocitySample = true;
        }
        mLastNestedScrollTimeMs = now;
    }

    private void resetDirectTouch() {
        mTouchRegion = TouchRegion.NONE;
        mDirectDragging = false;
        mActivePointerId = MotionEvent.INVALID_POINTER_ID;
        mChildScrollCarryDown = 0f;
        mChildScrollCarryUp = 0f;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ensureVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void onSecondaryPointerUp(@NonNull MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            int newIndex = pointerIndex == 0 ? 1 : 0;
            if (newIndex < ev.getPointerCount()) {
                mActivePointerId = ev.getPointerId(newIndex);
                mLastY = ev.getY(newIndex);
                mInitialDownY = mLastY;
                mInitialDownX = ev.getX(newIndex);
            } else {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
            }
        }
    }

    @Nullable
    private View findScrollableChild(@Nullable View view) {
        if (view == null) {
            return null;
        }
        if (view != mForegroundView && (view.canScrollVertically(-1) || view.canScrollVertically(1))) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View candidate = findScrollableChild(group.getChildAt(i));
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private int currentScrollOffsetToTop(@NonNull View target) {
        if (target instanceof RecyclerView) {
            return ((RecyclerView) target).computeVerticalScrollOffset();
        }
        return Math.max(0, target.getScrollY());
    }

    private void scrollScrollableChild(@NonNull View child, int dy) {
        if (dy == 0) {
            return;
        }
        if (child instanceof RecyclerView) {
            ((RecyclerView) child).scrollBy(0, dy);
        } else {
            child.scrollBy(0, dy);
        }
    }

    private int consumeScrollCarryDown(float amount) {
        float total = amount + mChildScrollCarryDown;
        int whole = (int) total;
        mChildScrollCarryDown = total - whole;
        return whole;
    }

    private int consumeScrollCarryUp(float amount) {
        float total = amount + mChildScrollCarryUp;
        int whole = (int) total;
        mChildScrollCarryUp = total - whole;
        return whole;
    }
}
