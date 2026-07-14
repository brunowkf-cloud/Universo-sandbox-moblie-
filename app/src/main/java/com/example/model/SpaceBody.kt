package com.example.model

import java.util.UUID

data class SpaceBody(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var mass: Double,
    var radius: Float,
    var color: Int, // ARGB Int
    var bodyType: BodyType,
    var isFixed: Boolean = false,
    val trail: MutableList<Pair<Float, Float>> = mutableListOf()
) {
    fun copyBody(): SpaceBody {
        return SpaceBody(
            id = id,
            name = name,
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            mass = mass,
            radius = radius,
            color = color,
            bodyType = bodyType,
            isFixed = isFixed,
            trail = ArrayList(trail)
        )
    }
}
