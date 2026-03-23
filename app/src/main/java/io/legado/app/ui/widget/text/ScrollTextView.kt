package io.legado.app.ui.widget.text

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * 嵌套惯性滚动 TextView
 */
class ScrollTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    //Flag reached top or bottom
    private var disallowIntercept = true

    private val scrollStateIdle = 0
    private val scrollStateDragging = 1
    val scrollStateSettling = 2

    private val mViewFling: ViewFling by lazy { ViewFling() }
    private val velocityTracker: VelocityTracker by lazy { VelocityTracker.obtain() }
    private var mScrollState = scrollStateIdle
    private var mLastTouchY: Int = 0
    private var mTouchSlop: Int = 0
    private var mMinFlingVelocity: Int = 0
    private var mMaxFlingVelocity: Int = 0

    //Max swipe distance boundary
    private var mOffsetHeight: Int = 0

    //f(x) = (x-1)^5 + 1
    private val sQuinticInterpolator = Interpolator {
        var t = it
        t -= 1.0f
        t * t * t * t * t + 1.0f
    }

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                disallowIntercept = true
                return super.onDown(e)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val y = scrollY + distanceY
                if (y < 0 || y > mOffsetHeight) {
                    disallowIntercept = false
                    //Trigger parent/grandparent swipe event
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    disallowIntercept = true
                }
                return true
            }

        })

    init {
        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop
        mMinFlingVelocity = vc.scaledMinimumFlingVelocity
        mMaxFlingVelocity = vc.scaledMaximumFlingVelocity
        movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        initOffsetHeight()
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        initOffsetHeight()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (lineCount > maxLines) {
            gestureDetector.onTouchEvent(event)
        }
        velocityTracker.addMovement(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setScrollState(scrollStateIdle)
                mLastTouchY = (event.y + 0.5f).toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val y = (event.y + 0.5f).toInt()
                var dy = mLastTouchY - y
                if (mScrollState != scrollStateDragging) {
                    var startScroll = false

                    if (abs(dy) > mTouchSlop) {
                        if (dy > 0) {
                            dy -= mTouchSlop
                        } else {
                            dy += mTouchSlop
                        }
                        startScroll = true
                    }
                    if (startScroll) {
                        setScrollState(scrollStateDragging)
                    }
                }
                if (mScrollState == scrollStateDragging) {
                    mLastTouchY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity.toFloat())
                val yVelocity = velocityTracker.yVelocity
                if (abs(yVelocity) > mMinFlingVelocity) {
                    mViewFling.fling(-yVelocity.toInt())
                } else {
                    setScrollState(scrollStateIdle)
                }
                resetTouch()
            }
            MotionEvent.ACTION_CANCEL -> {
                resetTouch()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        //Intercept if needed, this method called again after onScrollChanged
        if (disallowIntercept && lineCount > maxLines) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return result
    }

    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, min(y, mOffsetHeight))
    }

    private fun initOffsetHeight() {
        val mLayoutHeight: Int

        //Get content panel
        val mLayout = layout ?: return
        //Get content panel height
        mLayoutHeight = mLayout.height
        //Get top padding
        val paddingTop: Int = totalPaddingTop
        //Get bottom padding
        val paddingBottom: Int = totalPaddingBottom

        //Get widget actual height
        val mHeight: Int = measuredHeight

        //Calc swipe distance boundary
        mOffsetHeight = mLayoutHeight + paddingTop + paddingBottom - mHeight
        if (mOffsetHeight <= 0) {
            scrollTo(0, 0)
        }
    }

    private fun resetTouch() {
        velocityTracker.clear()
    }

    private fun setScrollState(state: Int) {
        if (state == mScrollState) {
            return
        }
        mScrollState = state
        if (state != scrollStateSettling) {
            mViewFling.stop()
        }
    }

    /**
     * Inertial scrolling
     */
    private inner class ViewFling : Runnable {

        private var mLastFlingY = 0
        private val mScroller: OverScroller = OverScroller(context, sQuinticInterpolator)
        private var mEatRunOnAnimationRequest = false
        private var mReSchedulePostAnimationCallback = false

        override fun run() {
            disableRunOnAnimationRequests()
            val scroller = mScroller
            if (scroller.computeScrollOffset()) {
                val y = scroller.currY
                val dy = y - mLastFlingY
                mLastFlingY = y
                if (dy < 0 && scrollY > 0) {
                    scrollBy(0, max(dy, -scrollY))
                } else if (dy > 0 && scrollY < mOffsetHeight) {
                    scrollBy(0, min(dy, mOffsetHeight - scrollY))
                }
                postOnAnimation()
            }
            enableRunOnAnimationRequests()
        }

        fun fling(velocityY: Int) {
            mLastFlingY = 0
            setScrollState(scrollStateSettling)
            mScroller.fling(
                0,
                0,
                0,
                velocityY,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            )
            postOnAnimation()
        }

        fun stop() {
            removeCallbacks(this)
            mScroller.abortAnimation()
        }

        private fun disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false
            mEatRunOnAnimationRequest = true
        }

        private fun enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation()
            }
        }

        @Suppress("DEPRECATION")
        fun postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true
            } else {
                removeCallbacks(this)
                ViewCompat.postOnAnimation(this@ScrollTextView, this)
            }
        }
    }

}