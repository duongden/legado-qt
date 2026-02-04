package io.legado.app.data.entities.rule

import android.os.Parcelable
import com.google.gson.JsonDeserializer
import io.legado.app.utils.INITIAL_GSON
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReviewRule(
    var reviewUrl: String? = null,          // Paragraph comment URL
    var avatarRule: String? = null,         // Paragraph comment publisher avatar
    var contentRule: String? = null,        // Paragraph comment content
    var postTimeRule: String? = null,       // Paragraph comment publish time
    var reviewQuoteUrl: String? = null,     // Get paragraph comment reply URL

    // These features implemented after above features
    var voteUpUrl: String? = null,          // Like URL
    var voteDownUrl: String? = null,        // Dislike URL
    var postReviewUrl: String? = null,      // Send reply URL
    var postQuoteUrl: String? = null,       // Send paragraph comment reply URL
    var deleteUrl: String? = null,          // Delete paragraph comment URL
) : Parcelable {

    companion object {

        val jsonDeserializer = JsonDeserializer<ReviewRule?> { json, _, _ ->
            when {
                json.isJsonObject -> INITIAL_GSON.fromJson(json, ReviewRule::class.java)
                json.isJsonPrimitive -> INITIAL_GSON.fromJson(json.asString, ReviewRule::class.java)
                else -> null
            }
        }

    }

}
