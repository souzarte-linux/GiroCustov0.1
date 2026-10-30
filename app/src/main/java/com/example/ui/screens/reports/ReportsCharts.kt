package com.example.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.data.DailyRecord
import com.example.util.CurrencyUtils

// Custom SVG line-style graph with Bezier curve
@Composable
fun NetProfitCurveChart(records: List<DailyRecord>) {
    if (records.isEmpty()) return

    val maxProfit = (records.maxOfOrNull { CurrencyUtils.centsToDouble(it.netProfit) } ?: 100.0).coerceAtLeast(10.0)
    val minProfit = (records.minOfOrNull { CurrencyUtils.centsToDouble(it.netProfit) } ?: 0.0).coerceAtMost(0.0)
    val profitRange = (maxProfit - minProfit).coerceAtLeast(10.0)

    val emerald = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val pointBorderColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        // Desenhar grades horizontais simples
        val gridLines = 3
        for (i in 0..gridLines) {
            val yGrid = height * i / gridLines
            drawLine(
                color = outlineColor,
                start = Offset(0f, yGrid),
                end = Offset(width, yGrid),
                strokeWidth = 1f
            )
        }

        if (records.size == 1) {
            // Renderização elegante e estável para turno único (evita Bézier degenerado)
            val record = records.first()
            val yPct = ((CurrencyUtils.centsToDouble(record.netProfit) - minProfit) / profitRange).coerceIn(0.0, 1.0)
            val y = (height - (yPct * height).toFloat()).coerceIn(0f, height)
            val safeEndY = height.coerceAtLeast(y + 1f)

            // Preenchimento gradiente abaixo da linha
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(emerald.copy(alpha = 0.25f), Color.Transparent),
                    startY = y,
                    endY = safeEndY
                ),
                topLeft = Offset(0f, y),
                size = Size(width, (height - y).coerceAtLeast(0f))
            )

            // Linha contínua horizontal de referência do turno único
            drawLine(
                color = emerald,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Ponto central em destaque
            val centerOffset = Offset(width / 2f, y)
            drawCircle(
                color = pointBorderColor,
                radius = 6.dp.toPx(),
                center = centerOffset
            )
            drawCircle(
                color = emerald,
                radius = 4.dp.toPx(),
                center = centerOffset
            )
            return@Canvas
        }

        val stepX = width / (records.size - 1).coerceAtLeast(1)

        val points = records.mapIndexed { idx, record ->
            val x = idx * stepX
            val yPct = ((CurrencyUtils.centsToDouble(record.netProfit) - minProfit) / profitRange).coerceIn(0.0, 1.0)
            val y = (height - (yPct * height).toFloat()).coerceIn(0f, height)
            Offset(x, y)
        }

        // Criar caminho do gráfico de lucros
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, height)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                // Curva de Bézier suave
                val p0 = points[i - 1]
                val p1 = points[i]
                val controlX = (p0.x + p1.x) / 2
                path.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlX, p0.y,
                    controlX, p1.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            // Desenhar preenchimento de gradiente verde esmeralda
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(emerald.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height.coerceAtLeast(1f)
                )
            )

            // Desenhar traçado da linha
            drawPath(
                path = path,
                color = emerald,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Desenhar pontos de destaque (Dots com borda da superfície e centro primário)
            points.forEachIndexed { _, point ->
                drawCircle(
                    color = pointBorderColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = emerald,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

// Custom bar-based comparison chart (Faturamento vs Lucro)
@Composable
fun ProductivityComparisonChart(records: List<DailyRecord>) {
    if (records.isEmpty()) return

    val maxVal = records.maxOfOrNull { 
        CurrencyUtils.centsToDouble(it.grossEarnings).coerceAtLeast(CurrencyUtils.centsToDouble(it.netProfit)) 
    } ?: 100.0
    val maxScale = (maxVal * 1.15).coerceAtLeast(10.0)

    val colorGross = MaterialTheme.colorScheme.secondary
    val colorNet = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        val totalBars = records.size.coerceAtLeast(1)
        val sectionWidth = width / totalBars
        val barWidth = (sectionWidth * 0.35f).coerceIn(4f, (sectionWidth * 0.45f).coerceAtLeast(4f))
        val spacing = (sectionWidth * 0.08f).coerceAtLeast(2f)

        records.forEachIndexed { idx, record ->
            val sectionX = idx * sectionWidth

            // Faturamento Bar
            val grossDouble = CurrencyUtils.centsToDouble(record.grossEarnings).coerceAtLeast(0.0)
            val grossH = ((grossDouble / maxScale) * height).toFloat().coerceIn(0f, height)
            val grossX = sectionX + (sectionWidth / 2f) - barWidth - spacing
            val grossY = height - grossH
            drawRect(
                color = colorGross,
                topLeft = Offset(grossX, grossY),
                size = Size(barWidth, grossH)
            )

            // Lucro Líquido Bar
            val netDouble = CurrencyUtils.centsToDouble(record.netProfit).coerceAtLeast(0.0)
            val netH = ((netDouble / maxScale) * height).toFloat().coerceIn(0f, height)
            val netX = sectionX + (sectionWidth / 2f) + spacing
            val netY = height - netH
            drawRect(
                color = colorNet,
                topLeft = Offset(netX, netY),
                size = Size(barWidth, netH)
            )
        }
    }
}
