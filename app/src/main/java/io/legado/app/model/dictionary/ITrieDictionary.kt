package io.legado.app.model.dictionary

interface ITrieDictionary {
    fun findLongestMatch(text: String, startIndex: Int): Pair<Int, String>?
    operator fun get(key: String): String?
}
