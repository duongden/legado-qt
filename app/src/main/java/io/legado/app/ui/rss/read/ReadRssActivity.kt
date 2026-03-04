package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityRssReadBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.favorites.RssFavoritesDialog
import io.legado.app.utils.ACache
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.isTrue
import io.legado.app.utils.keepScreenOn
import io.legado.app.utils.longSnackbar
import io.legado.app.utils.openUrl
import io.legado.app.utils.setDarkeningAllowed
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.setTintMutate
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.textArray
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import splitties.views.bottomPadding
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.util.regex.PatternSyntaxException

/**
 * rss阅读界面
 */
class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(),
    RssFavoritesDialog.Callback {

    override val binding by viewBinding(ActivityRssReadBinding::inflate)
    override val viewModel by viewModels<ReadRssViewModel>()

    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var isTranslated = false
    private val translateSeparator = "=|==|="

    inner class TranslateInterface {
        @JavascriptInterface
        fun translateBatch(joined: String): String {
            val parts = joined.split(translateSeparator)
            val results = mutableListOf<String>()
            kotlinx.coroutines.runBlocking {
                for (part in parts) results.add(io.legado.app.utils.TranslateUtils.translateCode(part))
            }
            return results.joinToString(translateSeparator)
        }
    }
    private val selectImageDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(it.value, uri)
        }
    }
    private val rssJsExtensions by lazy { RssJsExtensions(this) }

    fun getSource(): RssSource? {
        return viewModel.rssSource
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.upStarMenuData.observe(this) { upStarMenu() }
        viewModel.upTtsMenuData.observe(this) { upTtsMenu(it) }
        binding.titleBar.title = intent.getStringExtra("title")
        initView()
        initWebView()
        initLiveData()
        viewModel.initData(intent)
        onBackPressedDispatcher.addCallback(this) {
            if (binding.customWebView.size > 0) {
                customWebViewCallback?.onCustomViewHidden()
                return@addCallback
            } else if (binding.webView.canGoBack()
                && binding.webView.copyBackForwardList().size > 1
            ) {
                binding.webView.goBack()
                return@addCallback
            }
            finish()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_read, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        ttsMenuItem = menu.findItem(R.id.menu_aloud)
        upStarMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.rssSource?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_translate_page -> translatePage()
            R.id.menu_restore_original -> restoreOriginal()
            R.id.menu_rss_refresh -> viewModel.refresh {
                binding.webView.reload()
            }

            R.id.menu_rss_star -> {
                viewModel.addFavorite()
                viewModel.rssArticle?.let {
                    showDialogFragment(RssFavoritesDialog(it))
                }
            }

            R.id.menu_share_it -> {
                binding.webView.url?.let {
                    share(it)
                } ?: viewModel.rssArticle?.let {
                    share(it.link)
                } ?: toastOnUi(R.string.null_url)
            }

            R.id.menu_aloud -> readAloud()
            R.id.menu_login -> startActivity<SourceLoginActivity> {
                putExtra("type", "rssSource")
                putExtra("key", viewModel.rssSource?.sourceUrl)
            }

            R.id.menu_browser_open -> binding.webView.url?.let {
                openUrl(it)
            } ?: toastOnUi(R.string.url_is_null)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun updateFavorite(title: String?, group: String?) {
        viewModel.rssArticle?.let {
            if (title != null) {
                it.title = title
            }
            if (group != null) {
                it.group = group
            }
        }
        viewModel.updateFavorite()
    }

    override fun deleteFavorite() {
        viewModel.delFavorite()
    }

    @JavascriptInterface
    fun isNightTheme(): Boolean {
        return AppConfig.isNightTheme
    }

    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { view, windowInsets ->
            val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val insets = windowInsets.getInsets(typeMask)
            view.bottomPadding = insets.bottom
            windowInsets
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initWebView() {
        binding.progressBar.fontColor = accentColor
        binding.webView.webChromeClient = CustomWebChromeClient()
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            setDarkeningAllowed(AppConfig.isNightTheme)
        }
        binding.webView.addJavascriptInterface(this, "thisActivity")
        binding.webView.addJavascriptInterface(TranslateInterface(), "translateInterface")
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                hitTestResult.extra?.let { webPic ->
                    selector(
                        arrayListOf(
                            SelectItem(getString(R.string.action_save), "save"),
                            SelectItem(getString(R.string.select_folder), "selectFolder")
                        )
                    ) { _, charSequence, _ ->
                        when (charSequence.value) {
                            "save" -> saveImage(webPic)
                            "selectFolder" -> selectSaveFolder(null)
                        }
                    }
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { url, _, contentDisposition, _, _ ->
            var fileName = URLUtil.guessFileName(url, contentDisposition, null)
            fileName = URLDecoder.decode(fileName, "UTF-8")
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, url, fileName)
            }
        }

    }

    private fun saveImage(webPic: String) {
        val path = ACache.get().getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder(webPic)
        } else {
            viewModel.saveImage(webPic, Uri.parse(path))
        }
    }

    private fun selectSaveFolder(webPic: String?) {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        selectImageDir.launch {
            otherActions = default
            value = webPic
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this) { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                binding.webView.settings.userAgentString =
                    viewModel.headerMap[AppConst.UA_NAME] ?: AppConfig.userAgent
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    binding.webView
                        .loadDataWithBaseURL(url, html, "text/html", "utf-8", url)//Don't want to use baseUrl enter else
                } else {
                    binding.webView
                        .loadDataWithBaseURL(null, html, "text/html;charset=utf-8", "utf-8", url)
                }
            }
        }
        viewModel.urlLiveData.observe(this) {
            upJavaScriptEnable()
            CookieManager.applyToWebView(it.url)
            binding.webView.settings.userAgentString = it.getUserAgent()
            binding.webView.loadUrl(it.url, it.headerMap)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            binding.webView.settings.javaScriptEnabled = true
        }
    }

    private fun upStarMenu() {
        starMenuItem?.isVisible = viewModel.rssArticle != null
        if (viewModel.rssStar != null) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.in_favorites)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.out_favorites)
        }
        starMenuItem?.icon?.setTintMutate(primaryTextColor)
    }

    private fun upTtsMenu(isPlaying: Boolean) {
        lifecycleScope.launch {
            if (isPlaying) {
                ttsMenuItem?.setIcon(R.drawable.ic_stop_black_24dp)
                ttsMenuItem?.setTitle(R.string.aloud_stop)
            } else {
                ttsMenuItem?.setIcon(R.drawable.ic_volume_up)
                ttsMenuItem?.setTitle(R.string.read_aloud)
            }
            ttsMenuItem?.icon?.setTintMutate(primaryTextColor)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun readAloud() {
        if (viewModel.tts?.isSpeaking == true) {
            viewModel.tts?.stop()
            upTtsMenu(false)
        } else {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                    .replace("^\"|\"$".toRegex(), "")
                viewModel.readAloud(
                    Jsoup.parse(html)
                        .textArray()
                        .joinToString("\n")
                )
            }
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
                    if (realtimeTranslateLock) { deferredCheck = true; return; }
                    doTranslate();
                    if (deferredCheck) { deferredCheck = false; setTimeout(scheduleTranslate, deferDelay); }
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
        binding.webView.evaluateJavascript(js, null)
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
        binding.webView.evaluateJavascript(js, null)
        isTranslated = false
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.setDurProgress(newProgress)
            binding.progressBar.gone(newProgress == 100)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            binding.llView.invisible()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
            keepScreenOn(true)
            toggleSystemBar(false)
        }

        override fun onHideCustomView() {
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            keepScreenOn(false)
            toggleSystemBar(true)
        }
    }

    inner class CustomWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(request.url)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return shouldOverrideUrlLoading(Uri.parse(url))
        }

        /**
         * If there is blacklist, blacklist match returns blank,
         * If no blacklist then check whitelist, only whitelist passes,
         * If neither, no processing
         */
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()
            val source = viewModel.rssSource ?: return super.shouldInterceptRequest(view, request)
            val blacklist = source.contentBlacklist?.splitNotBlank(",")
            if (!blacklist.isNullOrEmpty()) {
                blacklist.forEach {
                    try {
                        if (url.startsWith(it) || url.matches(it.toRegex())) {
                            return createEmptyResource()
                        }
                    } catch (e: PatternSyntaxException) {
                        AppLog.put(getString(R.string.error_blacklist_regex, source.sourceName, it), e)
                    }
                }
            } else {
                val whitelist = source.contentWhitelist?.splitNotBlank(",")
                if (!whitelist.isNullOrEmpty()) {
                    whitelist.forEach {
                        try {
                            if (url.startsWith(it) || url.matches(it.toRegex())) {
                                return super.shouldInterceptRequest(view, request)
                            }
                        } catch (e: PatternSyntaxException) {
                            val msg = getString(R.string.error_whitelist_regex, source.sourceName, it)
                            AppLog.put(msg, e)
                        }
                    }
                    return createEmptyResource()
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            isTranslated = false  // Reset translate state when new page loads
            view.title?.let { title ->
                if (title != url
                    && title != view.url
                    && title.isNotBlank()
                    && url != "about:blank"
                    && !url.contains(title)
                ) {
                    binding.titleBar.title = title
                } else {
                    binding.titleBar.title = intent.getStringExtra("title")
                }
            }
            viewModel.rssSource?.injectJs?.let {
                if (it.isNotBlank()) {
                    view.evaluateJavascript(it, null)
                }
            }
        }

        private fun createEmptyResource(): WebResourceResponse {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream("".toByteArray())
            )
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            val source = viewModel.rssSource
            val js = source?.shouldOverrideUrlLoading
            if (!js.isNullOrBlank()) {
                val t = SystemClock.uptimeMillis()
                val result = kotlin.runCatching {
                    runScriptWithContext(lifecycleScope.coroutineContext) {
                        source.evalJS(js) {
                            put("java", rssJsExtensions)
                            put("url", url.toString())
                        }.toString()
                    }
                }.onFailure {
                    AppLog.put(getString(R.string.error_url_redirect_js, source.getTag()), it)
                }.getOrNull()
                if (SystemClock.uptimeMillis() - t > 30) {
                    AppLog.put(getString(R.string.error_url_redirect_timeout, source.getTag()))
                }
                if (result.isTrue()) {
                    return true
                }
            }
            when (url.scheme) {
                "http", "https", "jsbridge" -> {
                    return false
                }

                "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> {
                        data = url
                    }
                    return true
                }

                else -> {
                    binding.root.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                        openUrl(url)
                    }
                    return true
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
