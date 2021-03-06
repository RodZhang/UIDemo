package com.rod.uidemo.flow;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.rod.uidemo.UL;

import java.util.List;

/**
 * @author Rod
 * @date 2018/8/5
 */
public class FlowLayoutManager2 extends RecyclerView.LayoutManager {
    private static final String TAG = "FlowLayoutManager2";

    private ItemRecoder mItemRecoder = new ItemRecoder();
    private LayoutState mLayoutState = new LayoutState();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout() || dy == 0 || getChildCount() == 0) {
            return 0;
        }
        UL.Companion.d(TAG, "fixDy before, dy=%d, remainingScrollVertical=%d, targetPos=%d", dy, state.getRemainingScrollVertical(), state.getTargetScrollPosition());
        dy = fixDy(dy);
        UL.Companion.d(TAG, "fixDy after, dy=%d", dy);

        updateLayoutState(dy);
        fill(recycler);
        UL.Companion.d(TAG, "scrollVerticallyBy, dy = %d", dy);
        if (mLayoutState.mRemainSpace > 0) {
            dy = (dy / mLayoutState.mAbsDy) * Math.abs(mLayoutState.mAbsDy - mLayoutState.mRemainSpace);
            UL.Companion.d(TAG, "scrollVerticallyBy, after fix dy = %d", dy);
        }
        mLayoutState.mScrollOffset += dy;
        offsetChildrenVertical(-dy);
        return dy;
    }

    private void updateLayoutState(int dy) {
        mLayoutState.mAbsDy = Math.abs(dy);
        mLayoutState.mNeedRecycle = true;
        if (dy < 0) {
            View firstChild = getChildAt(0);
            int firstChildIndex = getPosition(firstChild);
            mLayoutState.mCurrentItemPos = firstChildIndex - 1;
            ItemInfo itemInfo = mItemRecoder.getItemInfo(firstChildIndex);
            mLayoutState.mCurrentRowIndex = itemInfo == null ? -1 : itemInfo.mRowIndex - 1;
            mLayoutState.mRemainSpace = getDecoratedTop(firstChild) - mLayoutState.mTopBounds + mLayoutState.mAbsDy;
            mLayoutState.mLayoutDirection = LayoutState.LAYOUT_TO_HEAD;
        } else {
            View lastChild = getChildAt(getChildCount() - 1);
            int lastChildIndex = getPosition(lastChild);
            mLayoutState.mCurrentItemPos = lastChildIndex + 1;
            ItemInfo itemInfo = mItemRecoder.getItemInfo(lastChildIndex);
            mLayoutState.mCurrentRowIndex = itemInfo == null ? -1 : itemInfo.mRowIndex + 1;
            int lastViewBottom = getDecoratedBottom(lastChild);
            mLayoutState.mRemainSpace = mLayoutState.mBottomBounds - lastViewBottom + mLayoutState.mAbsDy;
            mLayoutState.mYOffset = lastViewBottom;
            mLayoutState.mLayoutDirection = LayoutState.LAYOUT_TO_TAIL;
        }
    }

    private int fixDy(int dy) {
        return dy < 0 ? fixDyScrollToHead(dy) : fixDyScrollToTail(dy);
    }

    private int fixDyScrollToHead(int dy) {
        return (mLayoutState.mScrollOffset + dy < 0) ? -mLayoutState.mScrollOffset : dy;
    }

    private int fixDyScrollToTail(int dy) {
        final View lastView = getChildAt(getChildCount() - 1);
        final int lastViewIndex = getPosition(lastView);
        final int viewBottom = getDecoratedBottom(lastView);
        UL.Companion.d(TAG, "fixDyScrollToTail, viewBottom=%d, lastViewIndex=%d, dy=%d, bottomBounds=%d", viewBottom, lastViewIndex, dy, mLayoutState.mBottomBounds);
        if (lastViewIndex == getItemCount() - 1) {
            if (viewBottom - dy < mLayoutState.mBottomBounds) {
                return viewBottom - mLayoutState.mBottomBounds;
            } else {
                return dy;
            }
        } else {
            if (viewBottom - dy < 0) {
                return viewBottom;
            } else {
                return dy;
            }
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            return;
        }
        removeAndRecycleAllViews(recycler);
        mItemRecoder.clear();
        resetLayoutState();
        fill(recycler);
    }

    private void resetLayoutState() {
        mLayoutState.mAbsDy = 0;
        mLayoutState.mRemainSpace = mLayoutState.mBottomBounds - mLayoutState.mTopBounds;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_TO_TAIL;
        mLayoutState.mScrollOffset = 0;
        mLayoutState.mNeedRecycle = false;
        mLayoutState.mCurrentItemPos = 0;
        mLayoutState.mCurrentRowIndex = 0;
        mLayoutState.updateBounds(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
    }

    private void fill(RecyclerView.Recycler recycler) {
        if (mLayoutState.mNeedRecycle) {
            recycle(recycler);
        }

        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_TO_TAIL) {
            fillToTail(recycler);
        } else {
            fillToHead(recycler);
        }
    }

    private void fillToTail(RecyclerView.Recycler recycler) {
        UL.Companion.d(TAG, "fillToTail, out loop, mRemainSpace=%d", mLayoutState.mRemainSpace);
        while (mLayoutState.mRemainSpace > 0 && mLayoutState.mCurrentItemPos < getItemCount()) {
            mLayoutState.mRemainSpace -= fillRowToTail(recycler);
            UL.Companion.d(TAG, "fillToTail, mRemainSpace=%d", mLayoutState.mRemainSpace);
        }
    }

    private int fillRowToTail(RecyclerView.Recycler recycler) {
        if (mLayoutState.mCurrentItemPos >= getItemCount()) {
            return 0;
        }
        ItemInfo itemInfo = mItemRecoder.getItemInfo(mLayoutState.mCurrentItemPos);
        int consumed = itemInfo == null ? fillRowToTailWithNew(recycler) : fillRowToTailWithCache(recycler, itemInfo.mRowIndex);
        mLayoutState.mYOffset += consumed;
        return consumed;
    }

    private int fillRowToTailWithNew(RecyclerView.Recycler recycler) {
        UL.Companion.d(TAG, "fillRowToTailWithNew");
        int startX = mLayoutState.mLeftBounds;
        View view = recycler.getViewForPosition(mLayoutState.mCurrentItemPos);
        addView(view);
        measureChildWithMargins(view, 0, 0);
        int viewHeight = getDecoratedMeasuredHeight(view);
        int viewWidth = getDecoratedMeasuredWidth(view);
        if (startX + viewWidth >= mLayoutState.mRightBounds) {
            layoutDecoratedWithMargins(view, mLayoutState.mLeftBounds, mLayoutState.mYOffset, mLayoutState.mRightBounds, mLayoutState.mYOffset + viewHeight);
            mLayoutState.mCurrentItemPos++;
            mLayoutState.mCurrentRowIndex++;

            addItemInfo(startX, mLayoutState.mRightBounds, viewHeight);
            return viewHeight;
        }

        int newStartX = startX + viewWidth;
        layoutDecoratedWithMargins(view, startX, mLayoutState.mYOffset, startX + viewWidth, mLayoutState.mYOffset + viewHeight);

        addItemInfo(startX, newStartX, viewHeight);
        mLayoutState.mCurrentItemPos++;
        startX = newStartX;

        int maxHeight = viewHeight;
        while (mLayoutState.mCurrentItemPos < getItemCount()) {
            view = recycler.getViewForPosition(mLayoutState.mCurrentItemPos);
            addView(view);
            measureChildWithMargins(view, 0, 0);
            viewWidth = getDecoratedMeasuredWidth(view);
            newStartX = startX + viewWidth;
            if (newStartX >= mLayoutState.mRightBounds) {
                removeAndRecycleView(view, recycler);
                break;
            }

            viewHeight = getDecoratedMeasuredHeight(view);
            addItemInfo(startX, newStartX, viewHeight);

            layoutDecoratedWithMargins(view, startX, mLayoutState.mYOffset, newStartX, mLayoutState.mYOffset + viewHeight);
            maxHeight = Math.max(maxHeight, viewHeight);
            mLayoutState.mCurrentItemPos++;
            startX = newStartX;
        }
        mLayoutState.mCurrentRowIndex++;
        return maxHeight;
    }

    private void addItemInfo(int startX, int newStartX, int viewHeight) {
        Rect rect = new Rect(startX, mLayoutState.mYOffset + mLayoutState.mScrollOffset, newStartX, mLayoutState.mYOffset + viewHeight + mLayoutState.mScrollOffset);
        ItemInfo itemInfo = new ItemInfo(rect, mLayoutState.mCurrentRowIndex, mLayoutState.mCurrentItemPos, mItemRecoder.getRowItemSize(mLayoutState.mCurrentRowIndex));
        mItemRecoder.putItemToRow(mLayoutState.mCurrentRowIndex, itemInfo);
    }

    private int fillRowToTailWithCache(RecyclerView.Recycler recycler, int rowIndex) {
        int maxRowHeight = 0;
        List<ItemInfo> infoList = mItemRecoder.getItemsOnRow(rowIndex);
        for (ItemInfo itemInfo : infoList) {
            View view = recycler.getViewForPosition(itemInfo.mIndexInAdapter);
            addView(view);
            measureChildWithMargins(view, 0, 0);
            Rect rect = itemInfo.mRect;
            maxRowHeight = Math.max(maxRowHeight, rect.bottom - rect.top);

            layoutDecoratedWithMargins(view, rect.left, rect.top - mLayoutState.mScrollOffset, rect.right, rect.bottom - mLayoutState.mScrollOffset);
            UL.Companion.d(TAG, "fillRowToTailWithCache, yOffset=%d, top=%d, mScrollOffset=%d", mLayoutState.mYOffset, rect.top, mLayoutState.mScrollOffset);
            mLayoutState.mCurrentItemPos++;
        }
        mLayoutState.mCurrentRowIndex++;
        return maxRowHeight;
    }

    private void fillToHead(RecyclerView.Recycler recycler) {
        while (mLayoutState.mRemainSpace > 0 && mLayoutState.mCurrentItemPos >= 0) {
            mLayoutState.mRemainSpace -= fillRowToHead(recycler);
        }
    }

    private int fillRowToHead(RecyclerView.Recycler recycler) {
        int maxRowHeight = 0;
        List<ItemInfo> items = mItemRecoder.getItemsOnRow(mLayoutState.mCurrentRowIndex);
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemInfo itemInfo = items.get(i);
            View view = recycler.getViewForPosition(itemInfo.mIndexInAdapter);
            addView(view, 0);
            measureChildWithMargins(view, 0, 0);
            Rect rect = itemInfo.mRect;
            maxRowHeight = Math.max(maxRowHeight, rect.bottom - rect.top);

            layoutDecoratedWithMargins(view, rect.left, rect.top - mLayoutState.mScrollOffset, rect.right, rect.bottom - mLayoutState.mScrollOffset);
            UL.Companion.d(TAG, "fillRowToHead, yOffset=%d, top=%d, mScrollOffset=%d", mLayoutState.mYOffset, rect.top, mLayoutState.mScrollOffset);
            mLayoutState.mCurrentItemPos--;
        }
        mLayoutState.mCurrentRowIndex--;
        return maxRowHeight;
    }

    private void recycle(RecyclerView.Recycler recycler) {
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_TO_TAIL) {
            for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
                View view = getChildAt(i);
                if (getDecoratedBottom(view) - mLayoutState.mAbsDy >= mLayoutState.mTopBounds) {
                    removeAndRecycleViews(recycler, 0, i);
                    return;
                }
            }
        } else {
            int lastViewIndex = getChildCount() - 1;
            for (int i = lastViewIndex; i >= 0; i--) {
                View view = getChildAt(i);
                if (getDecoratedTop(view) + mLayoutState.mAbsDy <= mLayoutState.mBottomBounds) {
                    removeAndRecycleViews(recycler, lastViewIndex, i);
                    return;
                }
            }
        }
    }

    private void removeAndRecycleViews(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }

        UL.Companion.d(TAG, "removeAndRecycleViews, startIndex=%d, endIndex=%d", startIndex, endIndex);
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }
}
