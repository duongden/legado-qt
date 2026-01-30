package io.legado.app.data.entities

import android.text.TextUtils
import androidx.room.DatabaseView
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.utils.splitNotBlank


@DatabaseView(
    """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore, 
    (loginUrl is not null and trim(loginUrl) <> '') hasLoginUrl, lastUpdateTime, respondTime, weight, 
    (exploreUrl is not null and trim(exploreUrl) <> '') hasExploreUrl 
    from book_sources""",
    viewName = "book_sources_part"
)
data class BookSourcePart(
    // Address, including http/https
    var bookSourceUrl: String = "",
    // Name
    var bookSourceName: String = "",
    // Group
    var bookSourceGroup: String? = null,
    // Manual sort number
    var customOrder: Int = 0,
    // Is Enabled
    var enabled: Boolean = true,
    // Enable Explore
    var enabledExplore: Boolean = true,
    // Has Login URL
    var hasLoginUrl: Boolean = false,
    // Last update time, for sorting
    var lastUpdateTime: Long = 0,
    // Response time, for sorting
    var respondTime: Long = 180000L,
    // Smart sort weight
    var weight: Int = 0,
    // Has Explore URL
    var hasExploreUrl: Boolean = false
) {

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BookSourcePart) other.bookSourceUrl == bookSourceUrl else false
    }

    fun getDisPlayNameGroup(): String {
        return if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            String.format("%s (%s)", bookSourceName, bookSourceGroup)
        }
    }

    fun getBookSource(): BookSource? {
        return appDb.bookSourceDao.getBookSource(bookSourceUrl)
    }

    fun addGroup(groups: String) {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            bookSourceGroup = TextUtils.join(",", it)
        }
        if (bookSourceGroup.isNullOrBlank()) bookSourceGroup = groups
    }

    fun removeGroup(groups: String) {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            bookSourceGroup = TextUtils.join(",", it)
        }
    }

}

fun List<BookSourcePart>.toBookSource(): List<BookSource> {
    return mapNotNull { it.getBookSource() }
}
