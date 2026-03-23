package io.legado.app.lib.cronet

import android.os.ConditionVariable
import androidx.annotation.Keep
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.UrlRequest
import java.io.IOException

@Keep
class OldCallback(originalRequest: Request, mCall: Call, readTimeoutMillis: Int) :
    AbsCallBack(originalRequest, mCall, readTimeoutMillis) {

    private val mResponseCondition = ConditionVariable()
    private var mException: IOException? = null

    @Throws(IOException::class)
    override fun waitForDone(urlRequest: UrlRequest): Response {
        //Get okhttp call full request timeout
        val timeOutMs: Long = mCall.timeout().timeoutNanos() / 1000000
        urlRequest.start()
        startCheckCancelJob(urlRequest)
        if (timeOutMs > 0) {
            mResponseCondition.block(timeOutMs)
        } else {
            mResponseCondition.block()
        }
        //ConditionVariable after normal or timeout open, check if urlRequest complete
        if (!urlRequest.isDone) {
            urlRequest.cancel()
            mException = IOException("Cronet timeout after wait " + timeOutMs + "ms")
        }

        if (mException != null) {
            throw mException as IOException
        }
        return mResponse
    }

    /**
     * On error, notify subclass to stop blocking and throw error
     * @param error
     */
    override fun onError(error: IOException) {
        mException = error
        mResponseCondition.open()
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     * @param response
     */
    override fun onSuccess(response: Response) {
        mResponseCondition.open()
    }


}