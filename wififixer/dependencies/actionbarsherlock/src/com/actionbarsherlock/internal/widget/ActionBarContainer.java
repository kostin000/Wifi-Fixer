/*
 * Wifi Fixer for Android
 *     Copyright (C) 2010-2014  David Van de Ven
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see http://www.gnu.org/licenses
 */

package com.actionbarsherlock.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.nineoldandroids.widget.NineFrameLayout;

/**
 * This class acts as a container for the action bar view and action mode context views.
 * It applies special styles as needed to help handle animated transitions between them.
 *
 * @hide
 */
public class ActionBarContainer extends NineFrameLayout {
    private boolean mIsTransitioning;
    private View mTabContainer;
    private ActionBarView mActionBarView;

    private Drawable mBackground;
    private Drawable mStackedBackground;
    private Drawable mSplitBackground;
    private boolean mIsSplit;
    private boolean mIsStacked;

    public ActionBarContainer(Context context) {
        this(context, null);
    }

    public ActionBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundDrawable(null);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SherlockActionBar);
        mBackground = a.getDrawable(R.styleable.SherlockActionBar_background);
        mStackedBackground = a.getDrawable(
                R.styleable.SherlockActionBar_backgroundStacked);

        //Fix for issue #379
        if (mStackedBackground instanceof ColorDrawable && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            mStackedBackground = new IcsColorDrawable((ColorDrawable) mStackedBackground);
        }

        if (getId() == R.id.abs__split_action_bar) {
            mIsSplit = true;
            mSplitBackground = a.getDrawable(
                    R.styleable.SherlockActionBar_backgroundSplit);
        }
        a.recycle();

        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mActionBarView = (ActionBarView) findViewById(R.id.abs__action_bar);
    }

    public void setPrimaryBackground(Drawable bg) {
        mBackground = bg;
        invalidate();
    }

    public void setStackedBackground(Drawable bg) {
        mStackedBackground = bg;
        invalidate();
    }

    public void setSplitBackground(Drawable bg) {
        mSplitBackground = bg;
        invalidate();
    }

    /**
     * Set the action bar into a "transitioning" state. While transitioning
     * the bar will block focus and touch from all of its descendants. This
     * prevents the user from interacting with the bar while it is animating
     * in or out.
     *
     * @param isTransitioning true if the bar is currently transitioning, false otherwise.
     */
    public void setTransitioning(boolean isTransitioning) {
        mIsTransitioning = isTransitioning;
        setDescendantFocusability(isTransitioning ? FOCUS_BLOCK_DESCENDANTS
                : FOCUS_AFTER_DESCENDANTS);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsTransitioning || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        // An action bar always eats touch events.
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        super.onHoverEvent(ev);

        // An action bar always eats hover events.
        return true;
    }

    public void setTabContainer(ScrollingTabContainerView tabView) {
        if (mTabContainer != null) {
            removeView(mTabContainer);
        }
        mTabContainer = tabView;
        if (tabView != null) {
            addView(tabView);
            final ViewGroup.LayoutParams lp = tabView.getLayoutParams();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.WRAP_CONTENT;
            tabView.setAllowCollapse(false);
        }
    }

    public View getTabContainer() {
        return mTabContainer;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (mIsSplit) {
            if (mSplitBackground != null) mSplitBackground.draw(canvas);
        } else {
            if (mBackground != null) {
                mBackground.draw(canvas);
            }
            if (mStackedBackground != null && mIsStacked) {
                mStackedBackground.draw(canvas);
            }
        }
    }

    //This causes the animation reflection to fail on pre-HC platforms
    //@Override
    //public android.view.ActionMode startActionModeForChild(View child, android.view.ActionMode.Callback callback) {
    //    // No starting an action mode for an action bar child! (Where would it go?)
    //    return null;
    //}

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mActionBarView == null) return;

        final LayoutParams lp = (LayoutParams) mActionBarView.getLayoutParams();
        final int actionBarViewHeight = mActionBarView.isCollapsed() ? 0 :
                mActionBarView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

        if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
            final int mode = MeasureSpec.getMode(heightMeasureSpec);
            if (mode == MeasureSpec.AT_MOST) {
                final int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(getMeasuredWidth(),
                        Math.min(actionBarViewHeight + mTabContainer.getMeasuredHeight(),
                                maxHeight));
            }
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final boolean hasTabs = mTabContainer != null && mTabContainer.getVisibility() != GONE;

        if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
            final int containerHeight = getMeasuredHeight();
            final int tabHeight = mTabContainer.getMeasuredHeight();

            if ((mActionBarView.getDisplayOptions() & ActionBar.DISPLAY_SHOW_HOME) == 0) {
                // Not showing home, put tabs on top.
                final int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);

                    if (child == mTabContainer) continue;

                    if (!mActionBarView.isCollapsed()) {
                        child.offsetTopAndBottom(tabHeight);
                    }
                }
                mTabContainer.layout(l, 0, r, tabHeight);
            } else {
                mTabContainer.layout(l, containerHeight - tabHeight, r, containerHeight);
            }
        }

        boolean needsInvalidate = false;
        if (mIsSplit) {
            if (mSplitBackground != null) {
                mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                needsInvalidate = true;
            }
        } else {
            if (mBackground != null) {
                mBackground.setBounds(mActionBarView.getLeft(), mActionBarView.getTop(),
                        mActionBarView.getRight(), mActionBarView.getBottom());
                needsInvalidate = true;
            }
            if ((mIsStacked = hasTabs && mStackedBackground != null)) {
                mStackedBackground.setBounds(mTabContainer.getLeft(), mTabContainer.getTop(),
                        mTabContainer.getRight(), mTabContainer.getBottom());
                needsInvalidate = true;
            }
        }

        if (needsInvalidate) {
            invalidate();
        }
    }
}
