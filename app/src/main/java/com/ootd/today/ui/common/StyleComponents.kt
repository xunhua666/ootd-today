package com.ootd.today.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ootd.today.data.model.ClothingCategory

val StudioInk = Color(0xFF263C32)
val StudioLime = Color(0xFFD9EC9C)

enum class StudioSymbol { TODAY, WARDROBE, CALENDAR, PERSON, ADD, SEARCH, SORT, CLOSE, CHECK, HEART, ARROW, REFRESH, SHARE, SPARK }

/** Small native line icons keep navigation and controls visually consistent. */
@Composable
fun StudioIcon(symbol: StudioSymbol, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface, description: String? = null) {
    Canvas(modifier.size(22.dp).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.6f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x, y), Offset(x2, y2), strokeWidth = 1.6f, cap = StrokeCap.Round)
            fun path(vararg points: Float) { val p = Path(); p.moveTo(points[0], points[1]); for (i in 2 until points.size step 2) p.lineTo(points[i], points[i + 1]); drawPath(p, color, style = stroke) }
            when(symbol) {
                StudioSymbol.TODAY -> { drawCircle(color, 4.5f, Offset(12f,12f), style=stroke); for (i in 0..7) { val a=i*Math.PI/4; line(12+(8*kotlin.math.cos(a)).toFloat(),12+(8*kotlin.math.sin(a)).toFloat(),12+(10*kotlin.math.cos(a)).toFloat(),12+(10*kotlin.math.sin(a)).toFloat()) } }
                StudioSymbol.WARDROBE -> { path(9f,6f,9f,4f,11f,3f,14f,4f,14f,6f,12f,8f,12f,10f,3f,17f,3f,19f,21f,19f,21f,17f,12f,10f) }
                StudioSymbol.CALENDAR -> { drawRoundRect(color,Offset(3f,5f),Size(18f,16f),androidx.compose.ui.geometry.CornerRadius(3f),style=stroke); line(3f,10f,21f,10f); line(8f,3f,8f,7f); line(16f,3f,16f,7f); line(8f,14f,10f,14f); line(14f,14f,16f,14f); line(8f,17f,10f,17f) }
                StudioSymbol.PERSON -> { drawCircle(color,3.5f,Offset(12f,7f),style=stroke); val p=Path(); p.moveTo(4f,21f); p.cubicTo(4f,11f,20f,11f,20f,21f); drawPath(p,color,style=stroke) }
                StudioSymbol.ADD -> { line(5f,12f,19f,12f); line(12f,5f,12f,19f) }
                StudioSymbol.SEARCH -> { drawCircle(color,6.5f,Offset(10.5f,10.5f),style=stroke); line(16f,16f,21f,21f) }
                StudioSymbol.SORT -> { line(4f,6f,20f,6f); line(7f,12f,17f,12f); line(10f,18f,14f,18f) }
                StudioSymbol.CLOSE -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
                StudioSymbol.CHECK -> path(5f,12f,10f,17f,20f,6f)
                StudioSymbol.ARROW -> { line(4f,12f,20f,12f); path(14f,6f,20f,12f,14f,18f) }
                StudioSymbol.HEART -> { val p=Path(); p.moveTo(12f,21f); p.cubicTo(-9f,7f,7f,-3f,12f,6f); p.cubicTo(17f,-3f,33f,7f,12f,21f); drawPath(p,color,style=stroke) }
                StudioSymbol.REFRESH -> { drawArc(color,40f,285f,false,Offset(4f,4f),Size(16f,16f),style=stroke); path(20f,4f,20f,10f,14f,10f) }
                StudioSymbol.SHARE -> { line(12f,15f,12f,2f); path(7f,7f,12f,2f,17f,7f); path(5f,12f,5f,21f,19f,21f,19f,12f) }
                StudioSymbol.SPARK -> { path(12f,2f,15f,9f,22f,12f,15f,15f,12f,22f,9f,15f,2f,12f,9f,9f,12f,2f) }
            }
        }
    }
}

@Composable
fun EditorialHeader(eyebrow: String, title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow, fontSize=10.sp, letterSpacing=2.4.sp, fontWeight=FontWeight.SemiBold, color=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(title, style=MaterialTheme.typography.headlineMedium, color=MaterialTheme.colorScheme.onBackground)
            subtitle?.let { Spacer(Modifier.height(5.dp)); Text(it, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing?.invoke()
    }
}

/** Vector garments provide a color-accurate fallback for items without a photograph. */
@Composable
fun ClothingArt(category: ClothingCategory, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val factor = minOf(size.width, size.height) / 100f
        val ox = (size.width - 100f * factor) / 2
        val oy = (size.height - 100f * factor) / 2
        drawContext.transform.translate(ox, oy)
        scale(factor, factor, pivot=Offset.Zero) {
            drawOval(Color.Black.copy(alpha=.05f), Offset(22f,86f), Size(56f,5f))
            val p=Path()
            fun polygon(vararg points: Float) { p.moveTo(points[0],points[1]); for(i in 2 until points.size step 2) p.lineTo(points[i],points[i+1]); p.close() }
            when(category) {
                ClothingCategory.TOP -> polygon(35f,19f,44f,16f,56f,16f,65f,19f,85f,36f,74f,49f,65f,42f,66f,82f,34f,82f,35f,42f,26f,49f,15f,36f)
                ClothingCategory.OUTERWEAR -> polygon(36f,16f,46f,13f,54f,13f,64f,16f,78f,34f,85f,75f,72f,78f,63f,44f,67f,85f,33f,85f,37f,44f,28f,78f,15f,75f,22f,34f)
                ClothingCategory.BOTTOM -> polygon(29f,15f,71f,15f,73f,44f,68f,86f,52f,86f,50f,45f,48f,86f,32f,86f,27f,44f)
                ClothingCategory.ONE_PIECE -> polygon(40f,15f,46f,15f,47f,24f,53f,24f,54f,15f,60f,15f,66f,37f,60f,45f,78f,84f,22f,84f,40f,45f,34f,37f)
                ClothingCategory.SHOES -> { p.moveTo(19f,44f); p.lineTo(34f,39f); p.lineTo(52f,53f); p.lineTo(74f,58f); p.cubicTo(89f,61f,88f,76f,77f,77f); p.lineTo(19f,77f); p.quadraticTo(10f,64f,19f,44f); p.close() }
                ClothingCategory.BAG -> { polygon(27f,37f,73f,37f,80f,81f,20f,81f); val h=Path(); h.moveTo(37f,39f); h.cubicTo(32f,8f,68f,8f,63f,39f); drawPath(h,color,style=Stroke(5f)) }
                ClothingCategory.ACCESSORY -> { p.moveTo(22f,57f); p.cubicTo(18f,16f,78f,16f,76f,57f); p.lineTo(90f,66f); p.quadraticTo(56f,82f,15f,68f); p.close() }
            }
            drawPath(p,color)
            drawPath(p,Color(0xFF263C32).copy(alpha=.3f),style=Stroke(1f))
            val seam = if(color.luminanceCompat() > .7f) Color.Black.copy(alpha=.16f) else Color.White.copy(alpha=.3f)
            when(category) {
                ClothingCategory.TOP -> { drawArc(seam,0f,180f,false,Offset(43f,12f),Size(14f,13f),style=Stroke(2f)); drawLine(seam,Offset(37f,76f),Offset(63f,76f),1f) }
                ClothingCategory.BOTTOM -> { drawLine(seam,Offset(30f,23f),Offset(70f,23f),1.2f); drawLine(seam,Offset(50f,24f),Offset(50f,42f),1.2f) }
                ClothingCategory.OUTERWEAR -> { drawLine(seam,Offset(50f,24f),Offset(50f,84f),1.5f); drawLine(seam,Offset(38f,17f),Offset(48f,38f),2f); drawLine(seam,Offset(62f,17f),Offset(52f,38f),2f) }
                ClothingCategory.SHOES -> { drawLine(seam,Offset(18f,70f),Offset(82f,70f),2f); for(i in 0..2) drawLine(seam,Offset(37f+i*7,53f+i*2),Offset(33f+i*7,60f+i*2),2f) }
                else -> drawLine(seam,Offset(35f,48f),Offset(65f,48f),1.5f)
            }
        }
    }
}

private fun Color.luminanceCompat() = .299f*red + .587f*green + .114f*blue
