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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Refresh
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
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay

private const val TAG = "AdsterraNative"

class AdNativeBridge(
    private val onAdSuccessCallback: () -> Unit,
    private val onAdErrorCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onAdSuccess() {
        Log.d(TAG, "Adsterra Native JS reported: Ad loaded successfully.")
        onAdSuccessCallback()
    }

    @JavascriptInterface
    fun onAdFailure(reason: String) {
        Log.e(TAG, "Adsterra Native JS reported failure: $reason")
        onAdErrorCallback(reason)
    }

    @JavascriptInterface
    fun logConsole(message: String) {
        Log.d(TAG, "JS [NativeBridge]: $message")
    }
}

/**
 * Reusable Adsterra Native Ad Component (Ad Unit 2).
 * Renders Effective CPM Network native dynamic ads inside an isolated Android WebView.
 * Fits seamlessly into feeds, recommended sections, and product listing streams.
 * Includes console logging, JS callbacks, and a smart fallback placeholder.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdNative(
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        isAdLoaded = false
        hasError = false
        Log.d(TAG, "Initializing AdNative load (attempt $reloadKey)...")
        delay(8000)
        if (!isAdLoaded && !hasError) {
            Log.w(TAG, "AdNative load timed out after 8s. Displaying fallback placeholder.")
            hasError = true
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .testTag("adsterra_native_ad"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
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
                        text = if (hasError) "SPONSORED RECOMMENDATION" else "SPONSORED RECOMMENDATION • ADSTERRA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
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
                            contentDescription = "Retry Native Ad",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (hasError) {
                FallbackNativeCard(onRetry = { reloadKey++ })
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
                                        Log.d(TAG, "WebView Native page finished loading: $url")
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        Log.e(TAG, "WebView Native resource error: ${error?.description}")
                                        if (request?.isForMainFrame == true) {
                                            hasError = true
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        consoleMessage?.let {
                                            Log.d(TAG, "JS Console Native [${it.messageLevel()}]: ${it.message()} (line ${it.lineNumber()} of ${it.sourceId()})")
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
                                    AdNativeBridge(
                                        onAdSuccessCallback = { isAdLoaded = true },
                                        onAdErrorCallback = { reason ->
                                            Log.e(TAG, "Native Bridge Error: $reason")
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
                                          min-height: 140px;
                                          overflow: hidden;
                                        }
                                        #container-b4b6bff2415bc18ef1a66ebf09fb6829 {
                                          width: 100% !important;
                                          margin: 0 auto !important;
                                          text-align: center;
                                        }
                                        iframe {
                                          border: none !important;
                                          max-width: 100% !important;
                                        }
                                      </style>
                                    </head>
                                    <body>
                                      <div id="container-b4b6bff2415bc18ef1a66ebf09fb6829"></div>
                                      <script type="text/javascript">
                                        if (window.AdBridge) window.AdBridge.logConsole("AdNative script initializing...");
                                      </script>
                                      <script async="async" data-cfasync="false" 
                                              src="https://pl30508878.effectivecpmnetwork.com/b4b6bff2415bc18ef1a66ebf09fb6829/invoke.js"
                                              onload="if(window.AdBridge){ window.AdBridge.onAdSuccess(); window.AdBridge.logConsole('Native script loaded successfully'); }"
                                              onerror="if(window.AdBridge){ window.AdBridge.onAdFailure('Failed to load Native invoke.js'); }">
                                      </script>
                                      <script type="text/javascript">
                                        setTimeout(function() {
                                          var container = document.getElementById('container-b4b6bff2415bc18ef1a66ebf09fb6829');
                                          if (container && container.children.length > 0) {
                                            if (window.AdBridge) window.AdBridge.onAdSuccess();
                                          }
                                        }, 3500);
                                      </script>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://pl30508878.effectivecpmnetwork.com/", htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        update = { _ -> },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackNativeCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .width(50.dp)
                        .height(62.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Join Adsterra Network • Instant Approval & High CPMs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exclusive Publisher Offer • Adsterra Monetization",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Join Adsterra", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
