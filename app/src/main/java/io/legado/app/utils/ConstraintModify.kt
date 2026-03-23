package io.legado.app.utils

import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager

@Suppress("unused")
fun ConstraintLayout.modifyBegin(withAnim: Boolean = false): ConstraintModify.ConstraintBegin {
    val begin = ConstraintModify(this).begin
    if (withAnim) {
        TransitionManager.beginDelayedTransition(this)
    }
    return begin
}

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ConstraintModify(private val constraintLayout: ConstraintLayout) {

    val begin: ConstraintBegin by lazy {
        applyConstraintSet.clone(constraintLayout)
        ConstraintBegin(constraintLayout, applyConstraintSet)
    }
    private val applyConstraintSet = ConstraintSet()
    private val resetConstraintSet = ConstraintSet()

    init {
        resetConstraintSet.clone(constraintLayout)
    }

    /**
     * Modify with animation
     * @return
     */
    fun beginWithAnim(): ConstraintBegin {
        TransitionManager.beginDelayedTransition(constraintLayout)
        return begin
    }

    /**
     * Reset
     */
    fun reSet() {
        resetConstraintSet.applyTo(constraintLayout)
    }

    /**
     * Reset with animation
     */
    fun reSetWidthAnim() {
        TransitionManager.beginDelayedTransition(constraintLayout)
        resetConstraintSet.applyTo(constraintLayout)
    }


    @Suppress("unused", "MemberVisibilityCanBePrivate")
    class ConstraintBegin(
        private val constraintLayout: ConstraintLayout,
        private val applyConstraintSet: ConstraintSet
    ) {

        /**
         * Clear relationship, this not only clears relationship but also clears corresponding view width/height to w:0, h:0
         * @param viewId View ID
         * @return
         */
        fun clear(viewId: Int): ConstraintBegin {
            applyConstraintSet.clear(viewId)
            return this
        }

        /**
         * Clear a relationship of a view
         * @param viewId View ID
         * @param anchor Relationship to remove
         * @return
         */
        fun clear(viewId: Int, anchor: Anchor): ConstraintBegin {
            applyConstraintSet.clear(viewId, anchor.toInt())
            return this
        }

        fun setHorizontalWeight(viewId: Int, weight: Float): ConstraintBegin {
            applyConstraintSet.setHorizontalWeight(viewId, weight)
            return this
        }

        fun setVerticalWeight(viewId: Int, weight: Float): ConstraintBegin {
            applyConstraintSet.setVerticalWeight(viewId, weight)
            return this
        }

        /**
         * Set margin for a view
         * @param viewId View ID
         * @param left marginLeft
         * @param top   marginTop
         * @param right marginRight
         * @param bottom marginBottom
         * @return
         */
        fun setMargin(
            @IdRes viewId: Int,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ): ConstraintBegin {
            setMarginLeft(viewId, left)
            setMarginTop(viewId, top)
            setMarginRight(viewId, right)
            setMarginBottom(viewId, bottom)
            return this
        }

        /**
         * Set marginLeft for a view
         * @param viewId View ID
         * @param left marginLeft
         * @return
         */
        fun setMarginLeft(@IdRes viewId: Int, left: Int): ConstraintBegin {
            applyConstraintSet.setMargin(viewId, ConstraintSet.LEFT, left)
            return this
        }

        /**
         * Set marginRight for a view
         * @param viewId View ID
         * @param right marginRight
         * @return
         */
        fun setMarginRight(@IdRes viewId: Int, right: Int): ConstraintBegin {
            applyConstraintSet.setMargin(viewId, ConstraintSet.RIGHT, right)
            return this
        }

        /**
         * Set marginTop for a view
         * @param viewId View ID
         * @param top marginTop
         * @return
         */
        fun setMarginTop(@IdRes viewId: Int, top: Int): ConstraintBegin {
            applyConstraintSet.setMargin(viewId, ConstraintSet.TOP, top)
            return this
        }

        /**
         * Set marginBottom for a view
         * @param viewId View ID
         * @param bottom marginBottom
         * @return
         */
        fun setMarginBottom(@IdRes viewId: Int, bottom: Int): ConstraintBegin {
            applyConstraintSet.setMargin(viewId, ConstraintSet.BOTTOM, bottom)
            return this
        }

        /**
         * Set constraint left_to_left_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun leftToLeftOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.LEFT, endId, ConstraintSet.LEFT)
            return this
        }

        /**
         * Set constraint left_to_right_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun leftToRightOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.LEFT, endId, ConstraintSet.RIGHT)
            return this
        }

        /**
         * Set constraint top_to_top_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun topToTopOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.TOP, endId, ConstraintSet.TOP)
            return this
        }

        /**
         * Set constraint top_to_bottom_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun topToBottomOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.TOP, endId, ConstraintSet.BOTTOM)
            return this
        }

        /**
         * Set constraint right_to_left_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun rightToLeftOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.RIGHT, endId, ConstraintSet.LEFT)
            return this
        }

        /**
         * Set constraint right_to_right_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun rightToRightOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.RIGHT, endId, ConstraintSet.RIGHT)
            return this
        }

        /**
         * Set constraint bottom_to_bottom_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun bottomToBottomOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.BOTTOM, endId, ConstraintSet.BOTTOM)
            return this
        }

        /**
         * Set constraint bottom_to_top_of for a view
         * @param startId
         * @param endId
         * @return
         */
        fun bottomToTopOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
            applyConstraintSet.connect(startId, ConstraintSet.BOTTOM, endId, ConstraintSet.TOP)
            return this
        }

        /**
         * Set width for a view
         * @param viewId
         * @param width
         * @return
         */
        fun setWidth(@IdRes viewId: Int, width: Int): ConstraintBegin {
            applyConstraintSet.constrainWidth(viewId, width)
            return this
        }

        /**
         * Set height for a view
         * @param viewId
         * @param height
         * @return
         */
        fun setHeight(@IdRes viewId: Int, height: Int): ConstraintBegin {
            applyConstraintSet.constrainHeight(viewId, height)
            return this
        }

        /**
         * Commit to apply
         */
        fun commit() {
            constraintLayout.post {
                applyConstraintSet.applyTo(constraintLayout)
            }
        }
    }

    enum class Anchor {
        LEFT, RIGHT, TOP, BOTTOM, BASELINE, START, END, CIRCLE_REFERENCE;

        fun toInt(): Int {
            return when (this) {
                LEFT -> ConstraintSet.LEFT
                RIGHT -> ConstraintSet.RIGHT
                TOP -> ConstraintSet.TOP
                BOTTOM -> ConstraintSet.BOTTOM
                BASELINE -> ConstraintSet.BASELINE
                START -> ConstraintSet.START
                END -> ConstraintSet.END
                CIRCLE_REFERENCE -> ConstraintSet.CIRCLE_REFERENCE
            }
        }

    }

}
