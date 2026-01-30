package io.legado.app.utils

import io.legado.app.help.config.AppConfig

// Match "input chapter" string
private val regexEpisode = Regex("\\d+(-\\d+)?(,\\d+(-\\d+)?)*")

/**
 * Whether to enable custom export
 *
 * @author Discut
 */
fun enableCustomExport(): Boolean {
    return AppConfig.enableCustomExport && AppConfig.exportType == 1
}

/**
 * Verify if input range is correct
 *
 * @since 1.0.0
 * @author Discut
 * @param text Input range string
 * @return Is correct
 */
fun verificationField(text: String): Boolean {
    return text.matches(regexEpisode)
}