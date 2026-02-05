package com.example.test.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.test.data.LocationPoint
import org.json.JSONArray
import org.json.JSONObject

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var points: List<LocationPoint> = emptyList()
    private var isMapLoaded = false

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                if (points.isNotEmpty()) {
                    updateMap()
                }
            }
        }

        loadUrl("file:///android_asset/map.html")
    }

    fun setPath(newPoints: List<LocationPoint>) {
        this.points = newPoints
        if (isMapLoaded && points.isNotEmpty()) {
            updateMap()
        }
    }

    private fun updateMap() {
        if (points.isEmpty()) return

        val startPoint = points.first()
        val endPoint = points.last()

        // Create JSON array of all points for OSRM
        val pointsJson = JSONArray()
        points.forEach { point ->
            val pointObj = JSONObject().apply {
                put("lat", point.latitude)
                put("lon", point.longitude)
            }
            pointsJson.put(pointObj)
        }

        val javascript = """
            javascript:(function() {
                displayRoute(
                    ${startPoint.latitude},
                    ${startPoint.longitude},
                    ${endPoint.latitude},
                    ${endPoint.longitude},
                    ${pointsJson}
                );
            })()
        """.trimIndent()

        post {
            loadUrl(javascript)
        }
    }
}
