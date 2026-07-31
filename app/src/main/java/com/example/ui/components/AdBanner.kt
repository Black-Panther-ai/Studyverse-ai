package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay

private const val TAG = "AdsterraBanner"

class AdBridge(
    private val onAdSuccessCallback: () -> Unit,
    private val onAdErrorCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onAdSuccess() {
        Log.d(TAG, "Adsterra JavaScript reported: Ad loaded successfully.")
        onAdSuccessCallback()
    }

    @JavascriptInterface
    fun onAdFailure(reason: String) {
        Log.e(TAG, "Adsterra JavaScript reported failure: $reason")
        onAdErrorCallback(reason)
    }

    @JavascriptInterface
    fun logConsole(message: String) {
        Log.d(TAG, "JS [AdBridge]: $message")
    }
}

/**
 * Reusable Adsterra Banner Ad Component.
 * Encapsulates script execution safely inside an isolated, transparent Android WebView.
 * Configured with Mobile Chrome User-Agent, Mixed Content support, DomStorage,
 * JS Console message logging, JS Bridge state detection, and Fallback UI.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdBanner(
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        isAdLoaded = false
        hasError = false
        Log.d(TAG, "Initializing AdBanner load (attempt $reloadKey)...")
        delay(8000)
        if (!isAdLoaded && !hasError) {
            Log.w(TAG, "AdBanner load timed out after 8s. Displaying fallback placeholder.")
            hasError = true
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("adsterra_ad_banner"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasError) "SPONSORED OFFER" else "SPONSORED • ADSTERRA BANNER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }

                if (hasError) {
                    IconButton(
                        onClick = { reloadKey++ },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry Ad",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (hasError) {
                FallbackBannerCard(onRetry = { reloadKey++ })
            } else {
                key(reloadKey) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Log.d(TAG, "WebView page finished loading: $url")
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        Log.e(TAG, "WebView resource error: ${error?.description} (code: ${error?.errorCode})")
                                        if (request?.isForMainFrame == true) {
                                            hasError = true
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        consoleMessage?.let {
                                            Log.d(TAG, "JS Console [${it.messageLevel()}]: ${it.message()} (line ${it.lineNumber()} of ${it.sourceId()})")
                                        }
                                        return true
                                    }
                                }

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    allowFileAccess = true
                                    allowContentAccess = true

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }

                                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }

                                addJavascriptInterface(
                                    AdBridge(
                                        onAdSuccessCallback = { isAdLoaded = true },
                                        onAdErrorCallback = { reason ->
                                            Log.e(TAG, "Bridge Error: $reason")
                                            hasError = true
                                        }
                                    ),
                                    "AdBridge"
                                )

                                setBackgroundColor(Color.TRANSPARENT)

                                val htmlContent = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                      <meta charset="utf-8">
                                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                      <style>
                                        * { box-sizing: border-box; }
                                        html, body {
                                          margin: 0;
                                          padding: 0;
                                          background: transparent;
                                          display: flex;
                                          justify-content: center;
                                          align-items: center;
                                          width: 100%;
                                          height: 100%;
                                          overflow: hidden;
                                        }
                                        #ad-container {
                                          width: 100%;
                                          display: flex;
                                          justify-content: center;
                                          align-items: center;
                                        }
                                        iframe {
                                          border: none !important;
                                          max-width: 100% !important;
                                          margin: 0 auto !important;
                                        }
                                      </style>
                                    </head>
                                    <body>
                                      <div id="ad-container">
                                        <script type="text/javascript">
                                          if (window.AdBridge) window.AdBridge.logConsole("AdBanner script block executing...");
                                          atOptions = {
                                            'key' : '72ef3af51719c81243593cd39cffe6e8',
                                            'format' : 'iframe',
                                            'height' : 50,
                                            'width' : 320,
                                            'params' : {}
                                          };
                                        </script>
                                        <script type="text/javascript" 
                                                src="https://www.highperformanceformat.com/72ef3af51719c81243593cd39cffe6e8/invoke.js"
                                                onload="if(window.AdBridge){ window.AdBridge.onAdSuccess(); window.AdBridge.logConsole('invoke.js loaded successfully'); }"
                                                onerror="if(window.AdBridge){ window.AdBridge.onAdFailure('Failed to load invoke.js script'); }">
                                        </script>
                                      </div>
                                      <script type="text/javascript">
                                        setTimeout(function() {
                                          var container = document.getElementById('ad-container');
                                          var iframe = container ? container.querySelector('iframe') : null;
                                          if (iframe) {
                                            if (window.AdBridge) window.AdBridge.onAdSuccess();
                                          }
                                        }, 3000);
                                      </script>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://www.highperformanceformat.com/", htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        update = { _ -> },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackBannerCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .width(48.dp)
                        .height(60.dp)
                ) {
                    AsyncImage(
                        model = "https://landings-cdn.adsterratech.com/referralBanners/png/120%20x%20150%20px.png",
                        contentDescription = "Adsterra Referral Banner",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Monetize Your Traffic Easily",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Join Adsterra Network • High eCPM for Publishers",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Join Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
