package io.legado.app.model

/**
 * Translation data holder containing VietPhrase dictionaries using DoubleArrayTrie
 * with intelligent caching strategy for optimal performance
 * - First load: Build from text and cache to binary
 * - Subsequent loads: Load from cached binary files
 * - Custom imports: Build and cache immediately
 */
import io.legado.app.model.dictionary.ITrieDictionary

data class TranslationData(
    val names: ITrieDictionary,
    val vietPhrase: ITrieDictionary,
    val chinesePhienAm: ITrieDictionary
)
