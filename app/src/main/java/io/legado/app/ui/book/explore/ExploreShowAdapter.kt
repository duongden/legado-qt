package io.legado.app.ui.book.explore

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemSearchBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.setTranslatedText
import io.legado.app.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.legado.app.utils.TranslateUtils


class ExploreShowAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<SearchBook, ItemSearchBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemSearchBinding {
        return ItemSearchBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemSearchBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            bind(binding, item)
        } else {
            for (i in payloads.indices) {
                val bundle = payloads[i] as Bundle
                bindChange(binding, item, bundle)
            }
        }

    }

    private fun bind(binding: ItemSearchBinding, item: SearchBook) {
        binding.run {
            tvName.setTranslatedText(item.name)
            tvAuthor.setTranslatedText(item.author) { context.getString(R.string.author_show, it) }
            ivInBookshelf.isVisible = callBack.isInBookshelf(item)
            if (item.latestChapterTitle.isNullOrEmpty()) {
                tvLasted.gone()
            } else {
                tvLasted.setTranslatedText(item.latestChapterTitle) {
                    context.getString(R.string.lasted_show, it)
                }
                tvLasted.visible()
            }
            tvIntroduce.setTranslatedText(item.trimIntro(context))
            val kinds = item.getKindList()
            if (kinds.isEmpty()) {
                llKind.gone()
            } else {
                llKind.visible()
                if (TranslateUtils.isTranslateEnabled()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val translatedKinds = kinds.map {
                            withContext(Dispatchers.IO) {
                                TranslateUtils.translateMeta(it)
                            }
                        }
                        llKind.setLabels(translatedKinds)
                    }
                } else {
                    llKind.setLabels(kinds)
                }
            }
            ivCover.load(
                item,
                AppConfig.loadCoverOnlyWifi
            )
        }
    }

    private fun bindChange(binding: ItemSearchBinding, item: SearchBook, bundle: Bundle) {
        binding.run {
            bundle.keySet().forEach {
                when (it) {
                    "isInBookshelf" -> ivInBookshelf.isVisible =
                        callBack.isInBookshelf(item)
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemSearchBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.bindingAdapterPosition - getHeaderCount())?.let {
                callBack.showBookInfo(it)
            }
        }
    }

    interface CallBack {
        /**
         * 是否已经加入书架
         */
        fun isInBookshelf(book: SearchBook): Boolean

        fun showBookInfo(book: SearchBook)
    }
}