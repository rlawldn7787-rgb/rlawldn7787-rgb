package com.woohaeng.board.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

data class BoardFields(
    val workName: String,
    val workType: String,
    val location: String,
    val content: String,
    val workDate: String
)

data class BoardLayout(
    /** 가용 영역 기준 가로 위치 0(왼쪽) ~ 1(오른쪽) */
    val offsetX: Float = 0f,
    /** 가용 영역 기준 세로 위치 0(위) ~ 1(아래) */
    val offsetY: Float = 1f,
    /** 사진 너비 대비 보드판 비율 (0.2 ~ 0.7) */
    val widthRatio: Float = 0.42f
)

object BoardCompositor {
    fun compose(
        photo: Bitmap,
        fields: BoardFields,
        layout: BoardLayout = BoardLayout()
    ): Bitmap {
        val result = photo.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val ratio = layout.widthRatio.coerceIn(0.2f, 0.7f)
        val boardWidth = (result.width * ratio).coerceAtLeast(200f)
        val margin = (result.width * 0.02f).coerceAtLeast(12f)
        val padding = boardWidth * 0.06f
        val lineHeight = boardWidth * 0.085f
        val rows = listOf(
            "공사명" to fields.workName,
            "공종" to fields.workType,
            "위치" to fields.location,
            "내용" to fields.content,
            "일자" to fields.workDate
        )
        val boardHeight = padding * 2 + lineHeight * rows.size + padding

        val travelX = (result.width - boardWidth - 2 * margin).coerceAtLeast(0f)
        val travelY = (result.height - boardHeight - 2 * margin).coerceAtLeast(0f)
        val left = margin + layout.offsetX.coerceIn(0f, 1f) * travelX
        val top = margin + layout.offsetY.coerceIn(0f, 1f) * travelY
        val rect = RectF(left, top, left + boardWidth, top + boardHeight)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 80, 55)
            style = Paint.Style.STROKE
            strokeWidth = (boardWidth * 0.01f).coerceAtLeast(3f)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 60, 50)
            textSize = lineHeight * 0.55f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = lineHeight * 0.58f
        }

        canvas.drawRoundRect(rect, 16f, 16f, bg)
        canvas.drawRoundRect(rect, 16f, 16f, border)

        var y = top + padding + lineHeight * 0.7f
        rows.forEach { (label, value) ->
            canvas.drawText(label, left + padding, y, labelPaint)
            canvas.drawText(
                value.ifBlank { "-" },
                left + padding + boardWidth * 0.28f,
                y,
                valuePaint
            )
            y += lineHeight
        }
        return result
    }
}
