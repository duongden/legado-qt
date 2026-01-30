package io.legado.app.ui.book.search

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.R
import io.legado.app.base.adapter.DiffRecyclerAdapter
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemSearchBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import io.legado.app.utils.setTranslatedText
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope


class SearchAdapter(context: Context, val callBack: CallBack) :
    DiffRecyclerAdapter<SearchBook, ItemSearchBinding>(context) {

    override val keepScrollPosition = true

    override val diffItemCallback: DiffUtil.ItemCallback<SearchBook>
        get() = object : DiffUtil.ItemCallback<SearchBook>() {

            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return when {
                    oldItem.name != newItem.name -> false
                    oldItem.author != newItem.author -> false
                    else -> true
                }
            }

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return false
            }

            override fun getChangePayload(oldItem: SearchBook, newItem: SearchBook): Any {
                val payload = Bundle()
                payload.putInt("origins", newItem.origins.size)
                if (oldItem.coverUrl != newItem.coverUrl)
                    payload.putString("cover", newItem.coverUrl)
                if (oldItem.kind != newItem.kind)
                    payload.putString("kind", newItem.kind)
                if (oldItem.latestChapterTitle != newItem.latestChapterTitle)
                    payload.putString("last", newItem.latestChapterTitle)
                if (oldItem.intro != newItem.intro)
                    payload.putString("intro", newItem.intro)
                return payload
            }

        }

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

    override fun registerListener(holder: ItemViewHolder, binding: ItemSearchBinding) {
        binding.root.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.showBookInfo(it.name, it.author, it.bookUrl)
            }
        }
    }

    private fun bind(binding: ItemSearchBinding, searchBook: SearchBook) {
        binding.run {
            tvName.setTranslatedText(searchBook.name)
            tvAuthor.setTranslatedText(searchBook.author) { context.getString(R.string.author_show, it) }
            ivInBookshelf.isVisible = callBack.isInBookshelf(searchBook)
            bvOriginCount.setBadgeCount(searchBook.origins.size)
            upLasted(binding, searchBook.latestChapterTitle)
            tvIntroduce.setTranslatedText(searchBook.trimIntro(context))
            upKind(binding, searchBook.getKindList())
            ivCover.load(
                searchBook.coverUrl,
                searchBook.name,
                searchBook.author,
                AppConfig.loadCoverOnlyWifi,
                searchBook.origin
            )
        }
    }

    private fun bindChange(binding: ItemSearchBinding, searchBook: SearchBook, bundle: Bundle) {
        binding.run {
            bundle.keySet().forEach {
                when (it) {
                    "origins" -> bvOriginCount.setBadgeCount(searchBook.origins.size)
                    "last" -> upLasted(binding, searchBook.latestChapterTitle)
                    "intro" -> tvIntroduce.setTranslatedText(searchBook.trimIntro(context))
                    "kind" -> upKind(binding, searchBook.getKindList())
                    "isInBookshelf" -> ivInBookshelf.isVisible = callBack.isInBookshelf(searchBook)
                    "cover" -> ivCover.load(
                        searchBook.coverUrl,
                        searchBook.name,
                        searchBook.author,
                        false,
                        searchBook.origin
                    )
                }
            }
        }
    }

    private fun upLasted(binding: ItemSearchBinding, latestChapterTitle: String?) {
        binding.run {
            if (latestChapterTitle.isNullOrEmpty()) {
                tvLasted.gone()
            } else {
                tvLasted.setTranslatedText(latestChapterTitle) {
                    context.getString(R.string.lasted_show, it)
                }
                tvLasted.visible()
            }
        }
    }

    private fun upKind(binding: ItemSearchBinding, kinds: List<String>) = binding.run {
        if (kinds.isEmpty()) {
            llKind.gone()
        } else {
            llKind.visible()
            if (io.legado.app.utils.TranslateUtils.isTranslateEnabled()) {
                 CoroutineScope(Dispatchers.Main).launch {
                    val translatedKinds = kinds.map {
                        withContext(Dispatchers.IO) {
                           io.legado.app.utils.TranslateUtils.translateMeta(it)
                        }
                    }
                    llKind.setLabels(translatedKinds)
                }
            } else {
                llKind.setLabels(kinds)
            }
        }
    }

    interface CallBack {

        /**
         * Whether already added to bookshelf
         */
        fun isInBookshelf(book: SearchBook): Boolean

        /**
         * Show book details
         */
        fun showBookInfo(name: String, author: String, bookUrl: String)
    }
}