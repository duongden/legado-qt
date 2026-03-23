package io.legado.app.utils

import android.widget.TextView

/**
 * Extension function to set translated text on TextView
 * @param text The text to translate and display
 * @param transform Optional transformation function, e.g. { "Author: $it" }
 */
fun TextView.setTranslatedText(
    text: String?,
    transform: ((String) -> String)? = null
) {
    TranslateUtils.translateView(this, text, transform?.let { t -> { s: String -> t(s) as CharSequence } })
}
