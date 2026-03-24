package io.legado.app.ui.browser

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.databinding.ActivityWebViewBinding
import io.legado.app.help.http.CookieStore
import io.legado.app.help.source.SourceVerificationHelp
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.ACache
import io.legado.app.utils.TranslateUtils
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.keepScreenOn
import io.legado.app.utils.longSnackbar
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.startActivity
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import io.legado.app.constant.AppLog
import io.legado.app.help.webView.WebJsExtensions
import io.legado.app.help.webView.WebJsExtensions.Companion.basicJs
import io.legado.app.help.webView.WebJsExtensions.Companion.nameBasic
import io.legado.app.help.webView.WebJsExtensions.Companion.nameJava
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import io.legado.app.help.http.CookieManager as AppCookieManager
import java.net.URLDecoder
import androidx.core.net.toUri
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.webView.PooledWebView
import io.legado.app.help.webView.WebViewPool
import io.legado.app.help.webView.WebViewPool.BLANK_HTML
import io.legado.app.help.webView.WebViewPool.DATA_HTML
import io.legado.app.model.Download
import splitties.systemservices.powerManager
import java.lang.ref.WeakReference

import androidx.core.graphics.createBitmap
import io.legado.app.help.WebCacheManager
import io.legado.app.help.webView.WebJsExtensions.Companion.nameCache

class WebViewActivity : VMBaseActivity<ActivityWebViewBinding, WebViewModel>() {
    companion object {
        // 是否输出日志
        var sessionShowWebLog = false
    }

    private lateinit var pooledWebView: PooledWebView
    private lateinit var currentWebView: WebView

    override val binding by viewBinding(ActivityWebViewBinding::inflate)
    override val viewModel by viewModels<WebViewModel>()
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null
    private var isCloudflareChallenge = false
    private var isFullScreen = false
    private var isfullscreen = false
    private var wasScreenOff = false
    private var needClearHistory = true
    private var isTranslated = false
    private val translateSeparator = "=|==|="

    inner class TranslateInterface {
        @android.webkit.JavascriptInterface
        fun translateBatch(joined: String): String {
            val parts = joined.split(translateSeparator)
            val results = mutableListOf<String>()
            kotlinx.coroutines.runBlocking {
                for (part in parts) results.add(TranslateUtils.translateCode(part))
            }
            return results.joinToString(translateSeparator)
        }
    }
    private val saveImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(webPic, uri.toString())
        }
    }

    private fun refresh() {
        currentWebView.reload()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        pooledWebView = WebViewPool.acquire(this)
        currentWebView = pooledWebView.realWebView
        binding.webViewContainer.addView(currentWebView)
        currentWebView.post {
            currentWebView.clearHistory()
        }
        binding.titleBar.title = intent.getStringExtra("title") ?: getString(R.string.loading)
        binding.titleBar.subtitle = intent.getStringExtra("sourceName")
        viewModel.initData(intent) {
            val url = viewModel.baseUrl
            val headerMap = viewModel.headerMap
            initWebView(url, headerMap)
            val html = viewModel.html
            if (html.isNullOrEmpty()) {
                currentWebView.loadUrl(url, headerMap)
            } else {
                if (viewModel.localHtml) {
                    viewModel.source?.let {
                        val webJsExtensions = WebJsExtensions(it, this, currentWebView)
                        currentWebView.addJavascriptInterface(webJsExtensions, nameJava)
                    }
                    currentWebView.addJavascriptInterface(WebCacheManager, nameCache)
                }
                currentWebView.loadDataWithBaseURL(url, html, "text/html", "utf-8", url)
            }
        }
        currentWebView.clearHistory()
        onBackPressedDispatcher.addCallback(this) {
            if (binding.customWebView.size > 0) { //网页全屏
                customWebViewCallback?.onCustomViewHidden()
                return@addCallback
            }
            if (isFullScreen) { //按钮全屏
                toggleFullScreen()
                return@addCallback
            }
            if (currentWebView.canGoBack()) {
                val list = currentWebView.copyBackForwardList()
                val size = list.size
                if (size == 1) {
                    finish()
                    return@addCallback
                }
                val currentIndex = list.currentIndex
                val currentItem = list.currentItem
                val currentUrl = currentItem?.originalUrl ?: BLANK_HTML
                val currentTitle = currentItem?.title
                var steps = 1
                for (i in currentIndex - 1 downTo 0) {
                    val item = list.getItemAtIndex(i)
                    val itemUrl = item.originalUrl
                    if (itemUrl == BLANK_HTML) {
                        finish()
                        return@addCallback
                    }
                    if (itemUrl != currentUrl || currentTitle != item.title) {
                        break
                    }
                    if (currentUrl == DATA_HTML) {
                        break
                    }
                    steps++
                }
                if (steps == size) {
                    finish()
                    return@addCallback
                }
                currentWebView.goBackOrForward(-steps)
                return@addCallback
            }
            finish()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_view, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (viewModel.sourceOrigin.isNotEmpty()) {
            menu.findItem(R.id.menu_disable_source)?.isVisible = true
            menu.findItem(R.id.menu_delete_source)?.isVisible = true
        }
        menu.findItem(R.id.menu_show_web_log)?.isChecked = sessionShowWebLog
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_web_refresh -> refresh()
            R.id.menu_open_in_browser -> openUrl(viewModel.baseUrl)
            R.id.menu_copy_url -> sendToClip(viewModel.baseUrl)
            R.id.menu_ok -> {
                if (viewModel.sourceVerificationEnable) {
                    viewModel.saveVerificationResult(currentWebView) {
                        finish()
                    }
                } else {
                    finish()
                }
            }

            R.id.menu_full_screen -> toggleFullScreen()
            R.id.menu_show_web_log -> {
                sessionShowWebLog = !sessionShowWebLog
                item.isChecked = sessionShowWebLog
            }
            R.id.menu_translate_page -> translatePage()
            R.id.menu_restore_original -> restoreOriginal()
            R.id.menu_disable_source -> {
                viewModel.disableSource {
                    finish()
                }
            }

            R.id.menu_delete_source -> {
                alert(R.string.draw) {
                    setMessage(getString(R.string.sure_del) + "\n" + viewModel.sourceName)
                    noButton()
                    yesButton {
                        viewModel.deleteSource {
                            finish()
                        }
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    //实现starBrowser调起页面全屏
    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        toggleSystemBar(!isFullScreen)
        if (isFullScreen) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(url: String, headerMap: HashMap<String, String>) {
        binding.progressBar.fontColor = accentColor
        currentWebView.webChromeClient = CustomWebChromeClient()
        // 添加 JavaScript 接口
        currentWebView.addJavascriptInterface(JSInterface(this), nameBasic)
        currentWebView.webViewClient = CustomWebViewClient()
        currentWebView.settings.apply {
            useWideViewPort = true
            loadWithOverviewMode = true
            headerMap[AppConst.UA_NAME]?.let {
                userAgentString = it
            }
        }
        AppCookieManager.applyToWebView(url)
        currentWebView.addJavascriptInterface(TranslateInterface(), "translateInterface")
        currentWebView.setOnLongClickListener {
            val hitTestResult = currentWebView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                hitTestResult.extra?.let { webPic ->
                    selector(
                        arrayListOf(
                            SelectItem(getString(R.string.action_save), "save"),
                            SelectItem(getString(R.string.select_folder), "selectFolder")
                        )
                    ) { _, charSequence, _ ->
                        when (charSequence.value) {
                            "save" -> saveImage(webPic)
                            "selectFolder" -> selectSaveFolder()
                        }
                    }
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        currentWebView.setDownloadListener { url, _, contentDisposition, _, _ ->
            var fileName = URLUtil.guessFileName(url, contentDisposition, null)
            fileName = URLDecoder.decode(fileName, "UTF-8")
            currentWebView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, url, fileName)
            }
        }
    }

    private fun saveImage(webPic: String) {
        this.webPic = webPic
        val path = ACache.get().getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder()
        } else {
            viewModel.saveImage(webPic, path)
        }
    }

    private fun selectSaveFolder() {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        saveImage.launch {
            otherActions = default
        }
    }

    private fun translatePage() {
        if (isTranslated) return
        val sep = translateSeparator
        val js = """
            (function() {
                if (window.stvObserver) return;
                var SEP = "$sep";
                var chineseRegex = /[\u3400-\u9FBF]/;
                var deferDelay = 400;
                var translateDelay = 800;
                var realtimeTranslateLock = false;
                var deferredCheck = false;

                function recurTraver(node, arr, tarr) {
                    if (!node) return;
                    for (var i = 0; i < node.childNodes.length; i++) {
                        var child = node.childNodes[i];
                        if (child.nodeType === 3) {
                            if (chineseRegex.test(child.nodeValue)) {
                                arr.push(child);
                                tarr.push(child.nodeValue);
                            }
                        } else if (child.nodeName !== 'SCRIPT' && child.nodeName !== 'STYLE') {
                            recurTraver(child, arr, tarr);
                        }
                    }
                }

                function doTranslate() {
                    realtimeTranslateLock = true;
                    setTimeout(function() { realtimeTranslateLock = false; }, translateDelay);

                    var totranslist = [];
                    var transtext = [];
                    recurTraver(document.title ? document.querySelector('title') : null, totranslist, transtext);
                    recurTraver(document.body, totranslist, transtext);

                    if (totranslist.length === 0) return;

                    var joined = transtext.join(SEP);
                    var result = window.translateInterface.translateBatch(joined);
                    if (!result) return;
                    var translateds = result.split(SEP);
                    for (var i = 0; i < totranslist.length; i++) {
                        if (translateds[i]) {
                            if (!totranslist[i].orgn) totranslist[i].orgn = totranslist[i].nodeValue;
                            totranslist[i].nodeValue = translateds[i];
                        }
                    }

                    if (!window.stvStyleInjected) {
                        var styleEl = document.createElement('style');
                        styleEl.id = 'stv-word-break';
                        styleEl.textContent = ':not(i){word-break:break-word;text-overflow:ellipsis;overflow-wrap:break-word;}';
                        document.head.appendChild(styleEl);
                        window.stvStyleInjected = true;
                    }

                    var inputs = document.querySelectorAll("input[type='submit'],[placeholder],[title]");
                    var inpNodes = [], inpTexts = [], inpMeta = [];
                    for (var i = 0; i < inputs.length; i++) {
                        var el = inputs[i];
                        if (el.type === 'submit' && chineseRegex.test(el.value)) {
                            if (!el.orgnValue) el.orgnValue = el.value;
                            inpNodes.push(el); inpTexts.push(el.value); inpMeta.push('val');
                        }
                        if (el.placeholder && chineseRegex.test(el.placeholder)) {
                            if (!el.orgnPlaceholder) el.orgnPlaceholder = el.placeholder;
                            inpNodes.push(el); inpTexts.push(el.placeholder); inpMeta.push('ph');
                        }
                        if (el.title && chineseRegex.test(el.title)) {
                            if (!el.orgnTitle) el.orgnTitle = el.title;
                            inpNodes.push(el); inpTexts.push(el.title); inpMeta.push('ti');
                        }
                    }
                    if (inpTexts.length > 0) {
                        var inpResult = window.translateInterface.translateBatch(inpTexts.join(SEP));
                        if (inpResult) {
                            var inpTrans = inpResult.split(SEP);
                            for (var i = 0; i < inpNodes.length; i++) {
                                if (!inpTrans[i]) continue;
                                if (inpMeta[i] === 'val') inpNodes[i].value = inpTrans[i];
                                else if (inpMeta[i] === 'ph') inpNodes[i].placeholder = inpTrans[i];
                                else if (inpMeta[i] === 'ti') inpNodes[i].title = inpTrans[i];
                            }
                        }
                    }
                }

                function scheduleTranslate() {
                    if (realtimeTranslateLock) {
                        deferredCheck = true;
                        return;
                    }
                    doTranslate();
                    if (deferredCheck) {
                        deferredCheck = false;
                        setTimeout(scheduleTranslate, deferDelay);
                    }
                }

                scheduleTranslate();

                window.stvObserver = new MutationObserver(function(mutations) {
                    var needsTranslation = false;
                    for (var i = 0; i < mutations.length; i++) {
                        if (mutations[i].addedNodes.length > 0) { needsTranslation = true; break; }
                    }
                    if (needsTranslation) {
                        if (realtimeTranslateLock) { deferredCheck = true; return; }
                        realtimeTranslateLock = true;
                        setTimeout(function() {
                            realtimeTranslateLock = false;
                            if (deferredCheck) { deferredCheck = false; doTranslate(); }
                        }, deferDelay);
                        doTranslate();
                    }
                });
                window.stvObserver.observe(document.body, { childList: true, subtree: true });

                if (!window.origXHRSend) {
                    window.origXHRSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send = function() {
                        this.addEventListener('loadend', function() { setTimeout(scheduleTranslate, 300); });
                        window.origXHRSend.apply(this, arguments);
                    };
                }
                if (window.fetch && !window.origFetch) {
                    window.origFetch = window.fetch;
                    window.fetch = function() {
                        return window.origFetch.apply(this, arguments).then(function(res) {
                            setTimeout(scheduleTranslate, 300); return res;
                        });
                    };
                }
            })();
        """.trimIndent()
        currentWebView.evaluateJavascript(js, null)
        isTranslated = true
    }

    private fun restoreOriginal() {
        if (!isTranslated) return
        val js = """
            (function() {
                if (window.stvObserver) { window.stvObserver.disconnect(); window.stvObserver = null; }
                var all = document.querySelectorAll('*');
                for (var i = 0; i < all.length; i++) {
                    for (var j = 0; j < all[i].childNodes.length; j++) {
                        var n = all[i].childNodes[j];
                        if (n.nodeType === 3 && n.orgn) { n.nodeValue = n.orgn; delete n.orgn; }
                    }
                }
                var inputs = document.querySelectorAll("input[type='submit'],[placeholder],[title]");
                for (var i = 0; i < inputs.length; i++) {
                    if (inputs[i].orgnValue) { inputs[i].value = inputs[i].orgnValue; delete inputs[i].orgnValue; }
                    if (inputs[i].orgnPlaceholder) { inputs[i].placeholder = inputs[i].orgnPlaceholder; delete inputs[i].orgnPlaceholder; }
                    if (inputs[i].orgnTitle) { inputs[i].title = inputs[i].orgnTitle; delete inputs[i].orgnTitle; }
                }
            })();
        """.trimIndent()
        currentWebView.evaluateJavascript(js, null)
        isTranslated = false
    }

    override fun finish() {
        SourceVerificationHelp.checkResult(viewModel.sourceOrigin)
        super.finish()
    }

    private fun close() {
        if (!isCloudflareChallenge) {
            if (viewModel.sourceVerificationEnable) {
                viewModel.saveVerificationResult(currentWebView) {
                    finish()
                }
            }
            else {
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (powerManager.isInteractive) {
            wasScreenOff = false
            currentWebView.onPause()
        } else {
            wasScreenOff = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!wasScreenOff) {
            currentWebView.onResume()
        }
    }

    override fun onDestroy() {
        WebViewPool.release(pooledWebView)
        super.onDestroy()
    }

    @Suppress("unused")
    private class JSInterface(activity: WebViewActivity) {
        private val activityRef: WeakReference<WebViewActivity> = WeakReference(activity)
        @JavascriptInterface
        fun lockOrientation(orientation: String) {
            val ctx = activityRef.get()
            if (ctx != null && ctx.isfullscreen  && !ctx.isFinishing && !ctx.isDestroyed) {
                ctx.runOnUiThread {
                    ctx.requestedOrientation = when (orientation) {
                        "portrait", "portrait-primary" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        "portrait-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE //横屏且受重力控制正反
                        "landscape-primary" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE //正向横屏
                        "landscape-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE //反向横屏
                        "any", "unspecified" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }

        @JavascriptInterface
        fun onCloseRequested() {
            val ctx = activityRef.get()
            if (ctx != null && !ctx.isFinishing && !ctx.isDestroyed) {
                ctx.runOnUiThread {
                    ctx.close()
                }
            }
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {
        override fun getDefaultVideoPoster(): Bitmap {
            return super.getDefaultVideoPoster() ?: createBitmap(100, 100)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.setDurProgress(newProgress)
            binding.progressBar.gone(newProgress == 100)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            isfullscreen = true
            binding.llView.invisible()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
            keepScreenOn(true)
            toggleSystemBar(false)
        }

        override fun onHideCustomView() {
            isfullscreen = false
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            keepScreenOn(false)
            toggleSystemBar(true)
        }

        /* 覆盖window.close() */
        override fun onCloseWindow(window: WebView?) {
            close()
        }

        /* 监听网页日志 */
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            viewModel.source?.let { source ->
                if (sessionShowWebLog) {
                    val messageLevel = consoleMessage.messageLevel().name
                    val message = consoleMessage.message()
                    AppLog.put("${source.getTag()}${messageLevel}: $message",
                        NoStackTraceException("\n${message}\n- Line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"))
                    return true
                }
            }
            return false
        }
        
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                return shouldOverrideUrlLoading(it.url)
            }
            return true
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let {
                return shouldOverrideUrlLoading(it.toUri())
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (needClearHistory) {
                needClearHistory = false
                currentWebView.clearHistory() //清除历史
            }
            super.onPageStarted(view, url, favicon)
            currentWebView.evaluateJavascript(basicJs, null)
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            isTranslated = false  // Reset translate state when new page loads
            val cookieManager = CookieManager.getInstance()
            url?.let {
                CookieStore.setCookie(it, cookieManager.getCookie(it))
            }
            view?.title?.let { title ->
                if (title != url && title != view.url && title.isNotBlank()) {
                    binding.titleBar.title = title
                } else {
                    binding.titleBar.title = intent.getStringExtra("title")
                }
                view.evaluateJavascript("!!window._cf_chl_opt") {
                    if (it == "true") {
                        isCloudflareChallenge = true
                    } else if (isCloudflareChallenge && viewModel.sourceVerificationEnable) {
                        viewModel.saveVerificationResult(currentWebView) {
                            finish()
                        }
                    }
                }
            }
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            return when (url.scheme) {
                "http", "https" -> false
                "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> {
                        data = url
                    }
                    true
                }

                else -> {
                    binding.root.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                        openUrl(url)
                    }
                    true
                }
            }
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.proceed()
        }

    }

}