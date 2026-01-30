package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "search_keywords", indices = [(Index(value = ["word"], unique = true))])
data class SearchKeyword(
    /** Search keyword */
    @PrimaryKey
    var word: String = "",
    /** Usage count */
    var usage: Int = 1,
    /** Last used time */
    var lastUseTime: Long = System.currentTimeMillis()
) : Parcelable
