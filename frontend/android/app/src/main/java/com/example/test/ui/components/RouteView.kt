package com.example.test.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.test.data.LocationPoint
import kotlin.math.max
import kotlin.math.min

class RouteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#00C853") // Primary Green
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val pointPaint = Paint().apply {
        color = Color.parseColor("#0288D1") // Secondary Blue
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var points: List<LocationPoint> = emptyList()
    private val drawPath = Path()

    fun setPath(newPoints: List<LocationPoint>) {
        this.points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.size < 2) return

        // Find bounds
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE

        points.forEach {
            minLat = min(minLat, it.latitude)
            maxLat = max(maxLat, it.latitude)
            minLon = min(minLon, it.longitude)
            maxLon = max(maxLon, it.longitude)
        }

        // Add padding
        val latRange = max(maxLat - minLat, 0.0001)
        val lonRange = max(maxLon - minLon, 0.0001)
        
        val padding = 50f
        val w = width - padding * 2
        val h = height - padding * 2
        
        drawPath.reset()
        
        points.forEachIndexed { index, point ->
            // Normalize 0..1
            // Note: Latitude increases upwards (Y decreases in canvas)
            // Longitude increases rightwards (X increases in canvas)
            
            val normX = (point.longitude - minLon) / lonRange
            val normY = (point.latitude - minLat) / latRange
            
            val x = (padding + normX * w).toFloat()
            val y = (height - padding - normY * h).toFloat() // Invert Y
            
            if (index == 0) {
                drawPath.moveTo(x, y)
                // Draw Start Point
                canvas.drawCircle(x, y, 15f, pointPaint)
            } else {
                drawPath.lineTo(x, y)
            }
            
            if (index == points.size - 1) {
                // Draw End Point
                canvas.drawCircle(x, y, 15f, pointPaint)
            }
        }
        
        canvas.drawPath(drawPath, paint)
    }
}