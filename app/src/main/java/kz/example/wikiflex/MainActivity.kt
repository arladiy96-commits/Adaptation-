package kz.example.wikiflex

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val HOME = "https://ru.m.wikipedia.org/wiki/Заглавная_страница"
        private const val DARK = "#101722"
        private const val BLUE = "#418AF7"
    }

    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor(DARK)
        window.navigationBarColor = Color.parseColor(DARK)
        makeUi()
        configureWeb()
        web.loadUrl(HOME)
    }

    private fun dp(n: Int): Int = (resources.displayMetrics.density * n + 0.5f).toInt()

    private fun makeUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(DARK))
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(12), dp(8))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = "WIKIFLEX"
            textSize = 18f
            letterSpacing = 0.1f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        subtitle = TextView(this).apply {
            text = "Настоящая Википедия"
            textSize = 11f
            setTextColor(Color.parseColor("#ABB9C8"))
        }
        titles.addView(subtitle)
        top.addView(titles, LinearLayout.LayoutParams(0, dp(54), 1f))
        top.addView(TextView(this).apply {
            text = "⟳"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Обновить страницу"
            setOnClickListener { web.reload() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(top)
        val frame = FrameLayout(this)
        web = WebView(this)
        frame.addView(web, FrameLayout.LayoutParams(-1, -1))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(BLUE))
        }
        frame.addView(progress, FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP))
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(6), dp(7), dp(9))
        }
        nav(bottom, "⌂", "Главная") { web.loadUrl(HOME) }
        nav(bottom, "⌕", "Поиск") { clickSearch() }
        nav(bottom, "☰", "Меню") { clickMenu() }
        nav(bottom, "‹", "Назад") { if (web.canGoBack()) web.goBack() else toast("Это первая страница") }
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(73)))
        setContentView(root)
    }

    private fun nav(bar: LinearLayout, icon: String, title: String, click: () -> Unit) {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = title
            background = GradientDrawable().apply {
                cornerRadius = dp(13).toFloat()
                setColor(Color.parseColor("#1A2839"))
            }
            setOnClickListener { click() }
        }
        tile.addView(TextView(this).apply {
            text = icon
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(BLUE))
        })
        tile.addView(TextView(this).apply {
            text = title
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        bar.addView(tile, LinearLayout.LayoutParams(0, dp(58), 1f).apply {
            leftMargin = dp(3)
            rightMargin = dp(3)
        })
    }

    private fun configureWeb() {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.setBackgroundColor(Color.WHITE)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" &&
                    (uri.host == "ru.m.wikipedia.org" || uri.host == "ru.wikipedia.org")) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) { toast("Не удалось открыть ссылку") }
                return true
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                subtitle.text = "Загрузка..."
                progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView, url: String?) {
                subtitle.text = "Википедия · оригинальный сайт"
                web.evaluateJavascript("""
                    (function() {
                        var s = document.getElementById('wikiflex-style');
                        if (!s && document.head) {
                            s = document.createElement('style');
                            s.id = 'wikiflex-style';
                            document.head.appendChild(s);
                        }
                        if (s) s.textContent =
                            '#mw-mf-main-menu-button {opacity:0!important;pointer-events:none!important;}';
                    })();
                """.trimIndent(), null)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, n: Int) {
                progress.progress = n
                progress.visibility = if (n >= 100) View.GONE else View.VISIBLE
            }
        }
    }

    private fun clickMenu() {
        web.evaluateJavascript("""
            (function() {
                var b = document.querySelector('#mw-mf-main-menu-button') ||
                    document.querySelector('.mw-ui-icon-minerva-mainmenu') ||
                    document.querySelector('[aria-controls="mw-mf-page-left"]') ||
                    document.querySelector('#main-menu-input');
                if (!b) return 'missing';
                b.click();
                return 'clicked';
            })();
        """.trimIndent()) { result ->
            if (result?.contains("missing") == true) toast("Не найдена кнопка меню сайта")
        }
    }

    private fun clickSearch() {
        web.evaluateJavascript("""
            (function() {
                var b = document.querySelector('#searchIcon') ||
                    document.querySelector('.skin-minerva-search-trigger');
                if (b) { b.click(); return 'clicked'; }
                var input = document.querySelector('input[name="search"]');
                if (input) { input.focus(); return 'clicked'; }
                return 'missing';
            })();
        """.trimIndent()) { result ->
            if (result?.contains("missing") == true) {
                web.loadUrl("https://ru.m.wikipedia.org/wiki/Служебная:Поиск")
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Используем историю WebView для кнопки назад")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
