package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.exception.NoStackTraceException
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import splitties.init.appCtx
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Parcelize
@Entity(
    tableName = "replace_rules",
    indices = [(Index(value = ["id"]))]
)
data class ReplaceRule(
    @PrimaryKey(autoGenerate = true)
    var id: Long = System.currentTimeMillis(),
    //Name
    @ColumnInfo(defaultValue = "")
    var name: String = "",
    //Group
    var group: String? = null,
    //Replace content
    @ColumnInfo(defaultValue = "")
    var pattern: String = "",
    //Replace with
    @ColumnInfo(defaultValue = "")
    var replacement: String = "",
    //Scope
    var scope: String? = null,
    //Apply to title
    @ColumnInfo(defaultValue = "0")
    var scopeTitle: Boolean = false,
    //Apply to body
    @ColumnInfo(defaultValue = "1")
    var scopeContent: Boolean = true,
    //Exclude range
    var excludeScope: String? = null,
    //Is enabled
    @ColumnInfo(defaultValue = "1")
    var isEnabled: Boolean = true,
    //Is Regex
    @ColumnInfo(defaultValue = "1")
    var isRegex: Boolean = true,
    //Timeout
    @ColumnInfo(defaultValue = "3000")
    var timeoutMillisecond: Long = 3000L,
    //Sort
    @ColumnInfo(name = "sortOrder", defaultValue = "0")
    var order: Int = Int.MIN_VALUE
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is ReplaceRule) {
            return other.id == id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    val regex: Regex by lazy {
        pattern.toRegex()
    }

    fun getDisplayNameGroup(): String {
        return if (group.isNullOrBlank()) {
            name
        } else {
            String.format("%s (%s)", name, group)
        }
    }

    fun isValid(): Boolean {
        if (TextUtils.isEmpty(pattern)) {
            return false
        }
        //Check if regex correct
        if (isRegex) {
            try {
                Pattern.compile(pattern)
            } catch (ex: PatternSyntaxException) {
                AppLog.put("正则语法错误或不支持：${ex.localizedMessage}", ex)
                return false
            }
            // Pattern.compile passed test, but sometimes replacement times out, error, usually happens when modifying expression and missed deletion
            if (pattern.endsWith('|') && !pattern.endsWith("\\|")) {
                return false
            }
        }
        return true
    }

    @Throws(NoStackTraceException::class)
    fun checkValid() {
        if (!isValid()) {
            throw NoStackTraceException(appCtx.getString(R.string.replace_rule_invalid))
        }
    }

    fun getValidTimeoutMillisecond(): Long {
        if (timeoutMillisecond <= 0) {
            return 3000L
        }
        return timeoutMillisecond
    }
}