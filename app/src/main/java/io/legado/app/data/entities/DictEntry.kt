package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Dictionary entry for translation lookup
 * Composite primary key: (key, type) to allow same key in different dict types
 */
@Entity(
    tableName = "dict_entries",
    primaryKeys = ["key", "type"],
    indices = [Index(value = ["key", "type"])]
)
data class DictEntry(
    val key: String,
    val value: String,
    val type: Int // 0=names, 1=vietphrase, 2=phonetic
)
