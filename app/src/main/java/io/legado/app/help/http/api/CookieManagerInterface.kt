package io.legado.app.help.http.api

interface CookieManagerInterface {

    /**
     * Save cookie
     */
    fun setCookie(url: String, cookie: String?)

    /**
     * Replace cookie
     */
    fun replaceCookie(url: String, cookie: String)

    /**
     * Get cookie
     */
    fun getCookie(url: String): String

    /**
     * Remove cookie
     */
    fun removeCookie(url: String)

    fun cookieToMap(cookie: String): MutableMap<String, String>

    fun mapToCookie(cookieMap: Map<String, String>?): String?
}