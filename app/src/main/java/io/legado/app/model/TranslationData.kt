package io.legado.app.model

import io.legado.app.model.dictionary.BinaryDictionary

/**
 * Translation data holder containing VietPhrase dictionaries
 */
data class TranslationData(
    val names: BinaryDictionary,
    val vietPhrase: BinaryDictionary,
    val chinesePhienAm: BinaryDictionary
)
