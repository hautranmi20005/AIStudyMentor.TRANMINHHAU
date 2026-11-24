package com.aistudymentor.ui.ai

import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.aistudymentor.R
import org.json.JSONObject

@Composable
fun FormulaText(
    formula: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val html = remember(formula) {
        context.assets.open("katex/formula.html").bufferedReader().use { it.readText() }
            .replace("__FORMULA_JSON__", JSONObject.quote(formula))
    }

    AndroidView(
        factory = { viewContext ->
            WebView(viewContext).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                webViewClient = WebViewClient()
                tag = html
                loadDataWithBaseURL(
                    "file:///android_asset/katex/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    "file:///android_asset/katex/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height((72 * formula.lines().size.coerceAtLeast(1)).dp)
            .semantics {
                this.contentDescription = contentDescription ?: context.getString(R.string.formula_accessibility, formula)
            }
    )
}
