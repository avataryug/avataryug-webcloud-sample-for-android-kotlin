package com.example.webviewandroid

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var showButton: Button
    private var webViewURL: String? = null
    private val projectTitle = "y2eew"//"project_title" //Get from https://portal.avataryug.com
    private val apiKey = "23d0cbf8817e4a3794e99e8853fbd4d9"//"api_primary_key" //Get from https://portal.avataryug.com

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_activity_view)

        webView = findViewById(R.id.WebView)
        showButton = findViewById(R.id.closeBtn)

        val action = intent.getStringExtra("ACTION")
        when (action) {
            "LoginWithServer" -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val response = createAndLoadUser("UserID")
                    onLoadServerLoginWebView(response)
                }
            }
            "LoginWithCustom" -> loginWithCustom()
        }

        showButton.setOnClickListener {
            val intent = Intent(this, MyLoginView::class.java)
            intent.putExtra("ACTION", "LoginWithServer")
            startActivity(intent)
        }
    }

    private suspend fun createAndLoadUser(userid: String): JSONObject? {
        val request = JSONObject().apply {
            put("CustomID", userid)
            put("CreateAccount", true)
        }

        val headers = Headers.Builder()
            .add("X-Forwarded-Host", "${projectTitle}.local.host")
            .add("X-API-Key", apiKey)
            .build()

        val url = "https://${projectTitle}.avataryugapi.com/server/LoginWithServerCustomID"

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), request.toString())
        val httpRequest = Request.Builder().url(url).post(body).headers(headers).build()
        return try {
            val response = withContext(Dispatchers.IO) {
                OkHttpClient().newCall(httpRequest).execute()
            }
            JSONObject(response.body?.string())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun onLoadServerLoginWebView(data: JSONObject?) {
        data?.let {
            val queryParams = mapOf(
                "AccessToken" to it.getJSONObject("Data").getString("AccessToken"),
                "UserID" to it.getJSONObject("Data").getJSONObject("User").getString("UserID")
            )

            val unsafeUrl = "https://${projectTitle}.avataredge.net"
            webViewURL = "$unsafeUrl?${toQueryString(queryParams)}"

            configureWebViewSettings()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectJavaScriptIntoWebView(view)
                }
            }

            clearWebViewCache()

            webView.loadUrl(webViewURL!!)
            webView.addJavascriptInterface(WebInterface(this) { message ->  getWebMessage(message) }, "WebView")
        }
    }

    private fun configureWebViewSettings() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.databaseEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    private fun injectJavaScriptIntoWebView(view: WebView?) {
        view?.evaluateJavascript(
            """
                function subscribe(event) {  WebView.getData(event.data) }
                window.removeEventListener('message', subscribe);
                window.addEventListener('message', subscribe);
            """.trimIndent(), null
        )
    }

    private fun clearWebViewCache() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        webView.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    private fun toQueryString(params: Map<String, String>): String {
        return params.map { "${it.key}=${it.value}" }.joinToString("&")
    }

    private fun loginWithCustom() {
        configureWebViewSettings()
        webView.addJavascriptInterface(WebInterface(this@MainActivity){message-> getWebMessage(message)}, "WebView")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectJavaScriptIntoWebView(view)
            }
        }

        clearWebViewCache()

        val url = "https://$projectTitle.avataredge.net/"
        webView.loadUrl(url)
    }

    private fun getWebMessage(messages: String) {
        println("Web msg--->>$messages")
    }
}

class WebInterface(private val context: Context, private val callback: (String) -> Unit) {
    @JavascriptInterface
    fun getData(data: String) {
        callback(data)
    }
}
