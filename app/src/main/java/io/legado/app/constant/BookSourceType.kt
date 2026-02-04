package io.legado.app.constant

import androidx.annotation.IntDef

@Suppress("ConstPropertyName")
object BookSourceType {

    const val default = 0           // 0 Text
    const val audio = 1             // 1 Audio
    const val image = 2            // 2 Image
    const val file = 3               // 3 Site providing download service only

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(default, audio, image, file)
    annotation class Type

}