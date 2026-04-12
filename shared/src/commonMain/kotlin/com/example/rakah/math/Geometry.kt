// shared/src/commonMain/kotlin/com/example/rakah/math/Geometry.kt
package com.example.rakah.math

import com.example.rakah.model.Keypoint
import kotlin.math.*

data class Pt(val x: Float, val y: Float)

fun angleAt(b: Pt, a: Pt, c: Pt): Float {
    val abx = a.x - b.x; val aby = a.y - b.y
    val cbx = c.x - b.x; val cby = c.y - b.y
    val dot = (abx * cbx + aby * cby)
    val mag1 = sqrt(abx*abx + aby*aby).coerceAtLeast(1e-3f)
    val mag2 = sqrt(cbx*cbx + cby*cby).coerceAtLeast(1e-3f)
    val cosT = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
    return acos(cosT) * 180f / PI.toFloat()
}

fun inclineFromVertical(a: Pt, b: Pt): Float {
    // angle between vector a->b and vertical axis
    val vx = b.x - a.x
    val vy = b.y - a.y
    val mag = sqrt(vx*vx + vy*vy).coerceAtLeast(1e-3f)
    val cos = (abs(vy) / mag).coerceIn(0f, 1f) // vertical as reference
    return acos(cos) * 180f / PI.toFloat()
}

fun mid(p: Pt, q: Pt) = Pt((p.x + q.x)/2f, (p.y + q.y)/2f)

fun get(name: String, map: Map<String, Keypoint>): Pt? =
    map[name]?.let { Pt(it.x, it.y) }
