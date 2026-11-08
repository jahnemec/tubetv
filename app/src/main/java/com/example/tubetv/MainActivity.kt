package com.example.tubetv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import java.io.ByteArrayInputStream

class MainActivity : Activity() {

    // ---- Ayarlar: siteyi değiştirmek için sadece bu iki yeri düzenleyin ----
    private val startUrl = "https://en.yts-official.com/"

    // Sadece bu alan adlarına (ve alt alan adlarına) gidilebilir; diğer yönlendirmeler engellenir.
    private val allowedHosts = listOf("en.yts-official.com")
    // -------------------------------------------------------------------------

    // Bilinen reklam / takip alan adları (alt alan adlarıyla birlikte engellenir)
    private val blockedHosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "taboola.com", "outbrain.com",
        "popads.net", "propellerads.com", "exoclick.com", "juicyads.com",
        "adsterra.com", "popcash.net", "onclickads.com", "amazon-adsystem.com",
        "criteo.com", "pubmatic.com", "rubiconproject.com", "openx.net",
        "moatads.com", "scorecardresearch.com", "ads.yahoo.com", "adform.net"
    )

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private fun hostMatches(host: String?, list: List<String>): Boolean {
        val h = host?.lowercase() ?: return false
        return list.any { h == it || h.endsWith(".$it") }
    }

    // Kumanda ile gezinirken seçili öğe belli olsun, yeni sekme açan bağlantılar aynı sayfada açılsın
    private val pageScript = """
        (function() {
          if (!document.getElementById('tv-focus-style')) {
            var s = document.createElement('style');
            s.id = 'tv-focus-style';
            s.textContent = 'a:focus,button:focus,input:focus,[tabindex]:focus{outline:4px solid #ffb300 !important;outline-offset:2px !important;}';
            document.head.appendChild(s);
          }
          document.querySelectorAll('a[target]').forEach(function(a){ a.removeAttribute('target'); });
          window.open = function(){ return null; };
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = userAgentString.replace("; wv", "").replace(" Mobile", "")
        }

        webView.setBackgroundColor(Color.BLACK)

        webView.webViewClient = object : WebViewClient() {
            // Reklam / takip alan adlarına giden istekleri boş yanıtla düşür
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                return if (hostMatches(request.url.host, blockedHosts)) {
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                } else null
            }

            // İzin verilen siteler dışına (reklam yönlendirmeleri vb.) gitmeyi engelle
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                return !hostMatches(request.url.host, allowedHosts)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(pageScript, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Açılır pencereleri (pop-up) tamamen engelle
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean = false

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                root.addView(view)
            }

            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                webView.visibility = View.VISIBLE
            }
        }

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        if (savedInstanceState == null) {
            webView.loadUrl(startUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> webView.webChromeClient?.onHideCustomView()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
