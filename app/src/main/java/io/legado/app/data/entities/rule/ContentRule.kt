package io.legado.app.data.entities.rule

import android.os.Parcelable
import com.google.gson.JsonDeserializer
import io.legado.app.utils.INITIAL_GSON
import kotlinx.parcelize.Parcelize

/**
 * 正文处理规则
 */
@Parcelize
data class ContentRule(
    var content: String? = null,
    var title: String? = null, //Some sites only get title in body
    var nextContentUrl: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null,
    var replaceRegex: String? = null, //Replace rule
    var imageStyle: String? = null,   //Default size center, FULL max width
    var imageDecode: String? = null, //Image bytes secondary decryption js, return decrypted bytes
    var payAction: String? = null,    //Purchase op, js or url with {{js}}
) : Parcelable {


    companion object {

        val jsonDeserializer = JsonDeserializer<ContentRule?> { json, _, _ ->
            when {
                json.isJsonObject -> INITIAL_GSON.fromJson(json, ContentRule::class.java)
                json.isJsonPrimitive -> INITIAL_GSON.fromJson(
                    json.asString,
                    ContentRule::class.java
                )
                else -> null
            }
        }

    }


}