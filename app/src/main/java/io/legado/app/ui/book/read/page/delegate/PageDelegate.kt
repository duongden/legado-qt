package io.legado.app.ui.book.read.page.delegate

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.annotation.CallSuper
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import kotlin.math.abs

abstract class PageDelegate(protected val readView: ReadView) {

    protected val context: Context = readView.context

    //Start point
    protected val startX: Float get() = readView.startX
    protected val startY: Float get() = readView.startY

    //Previous touch point
    protected val lastX: Float get() = readView.lastX
    protected val lastY: Float get() = readView.lastY

    //Touch point
    protected val touchX: Float get() = readView.touchX
    protected val touchY: Float get() = readView.touchY

    protected val nextPage: PageView get() = readView.nextPage
    protected val curPage: PageView get() = readView.curPage
    protected val prevPage: PageView get() = readView.prevPage

    protected var viewWidth: Int = readView.width
    protected var viewHeight: Int = readView.height

    protected val scroller: Scroller by lazy {
        Scroller(readView.context, LinearInterpolator())
    }

    private val snackBar: Snackbar by lazy {
        Snackbar.make(readView, "", Snackbar.LENGTH_SHORT)
    }

    var isMoved = false
    var noNext = true

    //Move direction
    var mDirection = PageDirection.NONE
    var isCancel = false
    var isRunning = false
    var isStarted = false

    private var selectedOnDown = false

    init {
        curPage.resetPageOffset()
    }

    open fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int
    ) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        isRunning = true
        isStarted = true
        readView.invalidate()
    }

    protected fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, animationSpeed: Int) {
        val duration = if (dx != 0) {
            (animationSpeed * abs(dx)) / viewWidth
        } else {
            (animationSpeed * abs(dy)) / viewHeight
        }
        scroller.startScroll(startX, startY, dx, dy, duration)
        isRunning = true
        isStarted = true
        readView.invalidate()
    }

    protected fun stopScroll() {
        isStarted = false
        readView.post {
            isMoved = false
            isRunning = false
            readView.invalidate()
        }
    }

    @CallSuper
    open fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    open fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            readView.setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (isStarted) {
            onAnimStop()
            stopScroll()
        }
    }

    open fun onScroll() = Unit

    abstract fun abortAnim()

    abstract fun onAnimStart(animationSpeed: Int) //scroller start

    abstract fun onDraw(canvas: Canvas) //Draw

    abstract fun onAnimStop() //scroller finish

    abstract fun nextPageByAnim(animationSpeed: Int)

    abstract fun prevPageByAnim(animationSpeed: Int)

    open fun keyTurnPage(direction: PageDirection) {
        if (isRunning) return
        when (direction) {
            PageDirection.NEXT -> nextPageByAnim(100)
            PageDirection.PREV -> prevPageByAnim(100)
            else -> return
        }
    }

    @CallSuper
    open fun setDirection(direction: PageDirection) {
        mDirection = direction
    }

    /**
     * Touch event handling
     */
    abstract fun onTouch(event: MotionEvent)

    /**
     * Press
     */
    fun onDown() {
        //Is moved
        isMoved = false
        //Exists next chapter?
        noNext = false
        //Is animating
        isRunning = false
        //Cancel
        isCancel = false
        //Is next chapter or previous
        setDirection(PageDirection.NONE)
    }

    /**
     * Check if previous page exists
     */
    fun hasPrev(): Boolean {
        val hasPrev = readView.pageFactory.hasPrev()
        if (!hasPrev) {
            if (!snackBar.isShown) {
                snackBar.setText(R.string.no_prev_page)
                snackBar.show()
            }
        }
        return hasPrev
    }

    /**
     * Check if next page exists
     */
    fun hasNext(): Boolean {
        val hasNext = readView.pageFactory.hasNext()
        if (!hasNext) {
            readView.callBack.autoPageStop()
            if (!snackBar.isShown) {
                snackBar.setText(R.string.no_next_page)
                snackBar.show()
            }
        }
        return hasNext
    }

    fun dismissSnackBar() {
        // Check if snackBar shown, and close
        if (snackBar.isShown) {
            snackBar.dismiss()
        }
    }

    fun postInvalidate() {
        if (isStarted && isRunning && this is HorizontalPageDelegate) {
            readView.post {
                if (isStarted && isRunning) {
                    setBitmap()
                    readView.invalidate()
                }
            }
        }
    }

    open fun onDestroy() {
        // run on destroy
    }

}
