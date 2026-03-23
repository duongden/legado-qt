package io.legado.app.model

/**
 * Translation data holder containing VietPhrase dictionaries using DoubleArrayTrie
 * with intelligent caching strategy for optimal performance
 * - First load: Build from text and cache to binary
 * - Subsequent loads: Load from cached binary files
 * - Custom imports: Build and cache immediately
 */
data class TranslationData(
    val names: DoubleArrayTrie,
    val vietPhrase: DoubleArrayTrie,
    val chinesePhienAm: Map<String, String>
)
