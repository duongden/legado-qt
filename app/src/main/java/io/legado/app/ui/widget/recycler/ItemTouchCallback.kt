package io.legado.app.ui.widget.recycler


import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Created by GKF on 2018/3/16.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ItemTouchCallback(private val callback: Callback) : ItemTouchHelper.Callback() {

    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    /**
     * Can drag
     */
    var isCanDrag = false

    /**
     * Can swipe
     */
    var isCanSwipe = false

    /**
     * Whether Item can be dragged on long press
     */
    override fun isLongPressDragEnabled(): Boolean {
        return isCanDrag
    }

    /**
     * Can Item be swiped (H: Horizontal, V: Vertical)
     */
    override fun isItemViewSwipeEnabled(): Boolean {
        return isCanSwipe
    }

    /**
     * Tell system drag/swipe direction when user drags/swipes Item
     */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {// GridLayoutManager
            // If flag is 0, this feature is disabled
            val dragFlag =
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag = 0
            // create make
            return makeMovementFlags(dragFlag, swipeFlag)
        } else if (layoutManager is LinearLayoutManager) {// linearLayoutManager
            val linearLayoutManager = layoutManager as LinearLayoutManager?
            val orientation = linearLayoutManager!!.orientation

            var dragFlag = 0
            var swipeFlag = 0

            // For easier understanding, equivalent to horizontal ListView and vertical ListView
            if (orientation == LinearLayoutManager.HORIZONTAL) {// If horizontal layout
                swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            } else if (orientation == LinearLayoutManager.VERTICAL) {// If vertical layout, equivalent to ListView
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                swipeFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            }
            return makeMovementFlags(dragFlag, swipeFlag)
        }
        return 0
    }

    /**
     * Called when Item is dragged
     *
     * @param recyclerView     recyclerView
     * @param srcViewHolder    Dragged ViewHolder
     * @param targetViewHolder Destination ViewHolder
     */
    override fun onMove(
        recyclerView: RecyclerView,
        srcViewHolder: RecyclerView.ViewHolder,
        targetViewHolder: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition: Int = srcViewHolder.bindingAdapterPosition
        val toPosition: Int = targetViewHolder.bindingAdapterPosition
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                callback.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                callback.swap(i, i - 1)
            }
        }
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        callback.onSwiped(viewHolder.bindingAdapterPosition)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        val swiping = actionState == ItemTouchHelper.ACTION_STATE_DRAG
        swipeRefreshLayout?.isEnabled = !swiping
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        callback.onClearView(recyclerView, viewHolder)
    }

    interface Callback {

        /**
         * When an Item is swiped to delete
         *
         * @param adapterPosition item position
         */
        fun onSwiped(adapterPosition: Int) {

        }

        /**
         * Called when two Items swap positions
         *
         * @param srcPosition    Dragged item position
         * @param targetPosition Destination item position
         * @return Return true if developer handled operation, false otherwise
         */
        fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            return true
        }

        /**
         * Finger release
         */
        fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {

        }

    }
}
