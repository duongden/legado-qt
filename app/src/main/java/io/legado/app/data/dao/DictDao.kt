package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.DictEntry

/**
 * DAO for dictionary lookup operations
 */
@Dao
interface DictDao {

    @Query("SELECT value FROM dict_entries WHERE key = :key AND type = :type LIMIT 1")
    fun lookup(key: String, type: Int): String?

    @Query("SELECT * FROM dict_entries WHERE key = :key AND type = :type LIMIT 1")
    fun getEntry(key: String, type: Int): DictEntry?

    @Query("SELECT * FROM dict_entries WHERE type = :type")
    fun getAllByType(type: Int): List<DictEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: DictEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entries: List<DictEntry>)

    @Query("DELETE FROM dict_entries WHERE type = :type")
    fun deleteByType(type: Int)

    @Query("DELETE FROM dict_entries")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM dict_entries WHERE type = :type")
    fun countByType(type: Int): Int

    @Query("SELECT COUNT(*) FROM dict_entries")
    fun countAll(): Int

    companion object {
        const val TYPE_NAMES = 0
        const val TYPE_VIETPHRASE = 1
        const val TYPE_PHONETIC = 2
    }
}
