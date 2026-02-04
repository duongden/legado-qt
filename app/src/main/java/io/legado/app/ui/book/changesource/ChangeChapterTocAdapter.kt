package io.legado.app.ui.book.changesource

import android.content.Context
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemChapterListBinding
import io.legado.app.lib.theme.ThemeUtils
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.visible

class ChangeChapterTocAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    var durChapterIndex = 0

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = durChapterIndex == item.index
            if (isDur) {
                tvChapterName.setTextColor(context.accentColor)
            } else {
                tvChapterName.setTextColor(context.getCompatColor(R.color.primaryText))
            }
            tvChapterName.text = item.title
            if (item.isVolume) {
                //Volume name, e.g. Volume 1 Highlight
                tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
            } else {
                //Normal chapter keep unchanged
                tvChapterItem.background =
                    ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            }
            if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                //Volume name tag not shown (update time rule)
                tvTag.text = item.tag
                tvTag.visible()
            } else {
                tvTag.gone()
            }
            ivChecked.setImageResource(R.drawable.ic_check)
            ivChecked.visible(isDur)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.clickChapter(it, getItem(holder.layoutPosition + 1)?.url)
            }
        }
    }

    interface Callback {
        fun clickChapter(bookChapter: BookChapter, nextChapterUrl: String?)
    }
}