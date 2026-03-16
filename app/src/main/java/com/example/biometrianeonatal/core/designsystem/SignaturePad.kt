package com.example.biometrianeonatal.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import java.io.ByteArrayOutputStream

private data class SignatureStroke(
    val points: MutableList<Offset> = mutableListOf(),
)

/**
 * Funcao de topo `SignaturePad` usada como parte do fluxo principal do arquivo.
 */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 5f,
    onSignatureChanged: (String?) -> Unit,
) {
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentStroke by remember { mutableStateOf<SignatureStroke?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .background(Color.White)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stroke = SignatureStroke(mutableListOf(offset))
                        currentStroke = stroke
                        strokes.add(stroke)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentStroke?.points?.add(change.position)
                    },
                    onDragEnd = {
                        currentStroke = null
                        if (size.width > 0 && size.height > 0) {
                            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                            val canvas = AndroidCanvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            val paint = AndroidPaint().apply {
                                color = android.graphics.Color.BLACK
                                style = AndroidPaint.Style.STROKE
                                this.strokeWidth = strokeWidth
                                strokeCap = AndroidPaint.Cap.ROUND
                                strokeJoin = AndroidPaint.Join.ROUND
                                isAntiAlias = true
                            }

                            strokes.forEach { stroke ->
                                if (stroke.points.size == 1) {
                                    val point = stroke.points.first()
                                    canvas.drawPoint(point.x, point.y, paint)
                                } else {
                                    stroke.points.zipWithNext { start, end ->
                                        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                                    }
                                }
                            }

                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            onSignatureChanged(base64)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            strokes.forEach { stroke ->
                if (stroke.points.size == 1) {
                    drawCircle(
                        color = strokeColor,
                        radius = strokeWidth / 2,
                        center = stroke.points.first(),
                    )
                } else {
                    stroke.points.zipWithNext { start, end ->
                        drawLine(
                            color = strokeColor,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

