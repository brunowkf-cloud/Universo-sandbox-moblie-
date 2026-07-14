package com.example.physics

import android.graphics.Color
import com.example.model.BodyType
import com.example.model.SpaceBody
import java.util.UUID
import kotlin.math.sqrt

class GravitySimulator {
    var G: Double = 1.0
    var timeScale: Double = 1.0 // speed of simulation
    private val bodies = mutableListOf<SpaceBody>()

    // Viewport properties
    var zoom: Float = 1.0f
    var panX: Float = 0.0f
    var panY: Float = 0.0f
    var followedBodyId: String? = null

    // Trails length limit
    private val maxTrailPoints = 80

    // Lock for multi-threaded access
    private val lock = Any()

    fun getBodies(): List<SpaceBody> {
        return synchronized(lock) {
            bodies.map { it.copyBody() }
        }
    }

    fun clear() {
        synchronized(lock) {
            bodies.clear()
            followedBodyId = null
        }
    }

    fun addBody(body: SpaceBody) {
        synchronized(lock) {
            bodies.add(body)
        }
    }

    fun removeBody(id: String) {
        synchronized(lock) {
            bodies.removeAll { it.id == id }
            if (followedBodyId == id) {
                followedBodyId = null
            }
        }
    }

    fun updateBody(id: String, block: (SpaceBody) -> Unit) {
        synchronized(lock) {
            bodies.find { it.id == id }?.let { block(it) }
        }
    }

    fun setBodies(newBodies: List<SpaceBody>) {
        synchronized(lock) {
            bodies.clear()
            bodies.addAll(newBodies)
        }
    }

    // Step physics forward by dt
    fun step(dt: Double) {
        val scaledDt = dt * timeScale
        if (scaledDt <= 0.0) return

        synchronized(lock) {
            val n = bodies.size
            if (n == 0) return

            // 1. Reset forces
            val fx = DoubleArray(n)
            val fy = DoubleArray(n)

            // 2. Compute gravitational forces (pairwise N-body)
            val softeningSq = 25.0 // prevent division by zero / infinite forces
            for (i in 0 until n) {
                val b1 = bodies[i]
                for (j in i + 1 until n) {
                    val b2 = bodies[j]

                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distSq = dx * dx + dy * dy + softeningSq
                    val dist = sqrt(distSq)

                    if (dist > 0.1) {
                        // F = G * m1 * m2 / distSq
                        val force = (G * b1.mass * b2.mass) / distSq
                        val forceX = force * dx / dist
                        val forceY = force * dy / dist

                        fx[i] += forceX
                        fy[i] += forceY

                        fx[j] -= forceX
                        fy[j] -= forceY
                    }
                }
            }

            // 3. Update velocities and positions
            for (i in 0 until n) {
                val b = bodies[i]
                if (!b.isFixed) {
                    val ax = fx[i] / b.mass
                    val ay = fy[i] / b.mass

                    b.vx += ax * scaledDt
                    b.vy += ay * scaledDt

                    b.x += b.vx * scaledDt
                    b.y += b.vy * scaledDt
                }

                // Add to trail (record every few ticks, or just simple decimation)
                b.trail.add(Pair(b.x.toFloat(), b.y.toFloat()))
                if (b.trail.size > maxTrailPoints) {
                    b.trail.removeAt(0)
                }
            }

            // 4. Handle collisions and mergers
            val mergedIds = mutableSetOf<String>()
            val toRemove = mutableListOf<SpaceBody>()

            for (i in 0 until n) {
                val b1 = bodies[i]
                if (b1.id in mergedIds) continue

                for (j in i + 1 until n) {
                    val b2 = bodies[j]
                    if (b2.id in mergedIds) continue

                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val dist = sqrt(dx * dx + dy * dy)
                    val collisionDist = b1.radius + b2.radius

                    if (dist < collisionDist) {
                        // Collision detected! Merge smaller into larger
                        val (larger, smaller) = if (b1.mass >= b2.mass) Pair(b1, b2) else Pair(b2, b1)

                        // Conserve momentum: m1*v1 + m2*v2 = M*V
                        val totalMass = larger.mass + smaller.mass
                        if (totalMass > 0) {
                            if (!larger.isFixed) {
                                larger.vx = (larger.mass * larger.vx + smaller.mass * smaller.vx) / totalMass
                                larger.vy = (larger.mass * larger.vy + smaller.mass * smaller.vy) / totalMass
                                larger.x = (larger.mass * larger.x + smaller.mass * smaller.x) / totalMass
                                larger.y = (larger.mass * larger.y + smaller.mass * smaller.y) / totalMass
                            }

                            // Scale radius realistically based on volume conservation in 3D: R = (R1^3 + R2^3)^(1/3)
                            // Or in 2D: R = sqrt(R1^2 + R2^2)
                            larger.radius = sqrt(larger.radius * larger.radius + smaller.radius * smaller.radius)

                            // Blend colors proportionally to mass
                            val r1 = Color.red(larger.color)
                            val g1 = Color.green(larger.color)
                            val b1Color = Color.blue(larger.color)

                            val r2 = Color.red(smaller.color)
                            val g2 = Color.green(smaller.color)
                            val b2Color = Color.blue(smaller.color)

                            val w1 = larger.mass / totalMass
                            val w2 = smaller.mass / totalMass

                            val finalR = (r1 * w1 + r2 * w2).toInt().coerceIn(0, 255)
                            val finalG = (g1 * w1 + g2 * w2).toInt().coerceIn(0, 255)
                            val finalB = (b1Color * w1 + b2Color * w2).toInt().coerceIn(0, 255)

                            larger.color = Color.rgb(finalR, finalG, finalB)
                            larger.mass = totalMass

                            // Append absorption tag to name if it doesn't already have one
                            if (!larger.name.contains("+")) {
                                larger.name = "${larger.name}+"
                            }
                        }

                        mergedIds.add(smaller.id)
                        toRemove.add(smaller)
                    }
                }
            }

            bodies.removeAll(toRemove)

            // If followed body was merged/removed, shift focus to the larger absorber
            followedBodyId?.let { fid ->
                if (fid in mergedIds) {
                    // find a candidate that has "+" in its name and closest to the crash
                    // or just default to the most massive remaining body.
                    followedBodyId = bodies.maxByOrNull { it.mass }?.id
                }
            }
        }
    }

    // Telemetry stats
    fun calculateTotalEnergy(): Pair<Double, Double> {
        return synchronized(lock) {
            var kinetic = 0.0
            var potential = 0.0
            val n = bodies.size

            for (i in 0 until n) {
                val b1 = bodies[i]
                kinetic += 0.5 * b1.mass * (b1.vx * b1.vx + b1.vy * b1.vy)

                for (j in i + 1 until n) {
                    val b2 = bodies[j]
                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > 0.1) {
                        potential -= (G * b1.mass * b2.mass) / dist
                    }
                }
            }
            Pair(kinetic, potential)
        }
    }

    fun calculateCenterOfMass(): Pair<Double, Double> {
        return synchronized(lock) {
            var totalMass = 0.0
            var sumX = 0.0
            var sumY = 0.0
            for (b in bodies) {
                totalMass += b.mass
                sumX += b.x * b.mass
                sumY += b.y * b.mass
            }
            if (totalMass > 0.0) {
                Pair(sumX / totalMass, sumY / totalMass)
            } else {
                Pair(0.0, 0.0)
            }
        }
    }

    // Calculate circular orbit velocity for body relative to central body
    fun calculateOrbitVelocity(
        posX: Double,
        posY: Double,
        central: SpaceBody
    ): Pair<Double, Double> {
        val dx = posX - central.x
        val dy = posY - central.y
        val r = sqrt(dx * dx + dy * dy)
        if (r < 1.0) return Pair(0.0, 0.0)

        // Orbital speed v = sqrt(G * M / r)
        val v = sqrt((G * central.mass) / r)

        // Tangent vector counter-clockwise perpendicular to radial vector (dx/r, dy/r)
        // Tangent vector is (-dy/r, dx/r)
        val vx = -v * (dy / r) + central.vx
        val vy = v * (dx / r) + central.vy

        return Pair(vx, vy)
    }

    // Find the most gravitationally dominant body near position
    fun findDominantBody(x: Double, y: Double): SpaceBody? {
        return synchronized(lock) {
            bodies.maxByOrNull { b ->
                val dx = b.x - x
                val dy = b.y - y
                val distSq = dx * dx + dy * dy
                if (distSq < 100.0) 0.0 else b.mass / distSq
            }
        }
    }

    // Load presets
    fun loadPresetSolarSystem() {
        synchronized(lock) {
            clear()
            // Sun (Yellow Star)
            val sun = SpaceBody(
                name = "Sol",
                x = 0.0, y = 0.0,
                vx = 0.0, vy = 0.0,
                mass = 30000.0, radius = 28f,
                color = 0xFFFFD700.toInt(), // Golden Yellow
                bodyType = BodyType.STAR,
                isFixed = true
            )
            bodies.add(sun)

            // Mercury
            addOrbitingPlanet("Mercury", 85.0, 0.2, 5f, 0xFF8A8D8F.toInt(), BodyType.TERRESTRIAL, sun)
            // Venus
            addOrbitingPlanet("Venus", 130.0, 3.5, 8f, 0xFFE3BB76.toInt(), BodyType.TERRESTRIAL, sun)
            // Earth
            val earth = addOrbitingPlanet("Earth", 190.0, 10.0, 10f, 0xFF2F80ED.toInt(), BodyType.TERRESTRIAL, sun)
            // Moon
            addOrbitingPlanet("Moon", 20.0, 0.1, 4f, 0xFFCCCCCC.toInt(), BodyType.MOON, earth)
            // Mars
            addOrbitingPlanet("Mars", 260.0, 1.2, 7f, 0xFFC1440E.toInt(), BodyType.TERRESTRIAL, sun)
            // Jupiter
            addOrbitingPlanet("Jupiter", 380.0, 120.0, 18f, 0xFFD8CA9D.toInt(), BodyType.GAS_GIANT, sun)
            // Saturn
            addOrbitingPlanet("Saturn", 500.0, 80.0, 15f, 0xFFE2BF7D.toInt(), BodyType.GAS_GIANT, sun)
        }
    }

    fun loadPresetGalaxyCollision() {
        synchronized(lock) {
            clear()

            // Galaxy A Black Hole
            val bhA = SpaceBody(
                id = "bhA",
                name = "Sagittarius A*",
                x = -250.0, y = -120.0,
                vx = 8.0, vy = 4.0,
                mass = 120000.0, radius = 18f,
                color = 0xFF8A2BE2.toInt(), // Dark violet
                bodyType = BodyType.BLACK_HOLE
            )
            bodies.add(bhA)

            // Galaxy B Black Hole
            val bhB = SpaceBody(
                id = "bhB",
                name = "Andromeda Core",
                x = 250.0, y = 120.0,
                vx = -8.0, vy = -4.0,
                mass = 140000.0, radius = 19f,
                color = 0xFF4B0082.toInt(), // Indigo
                bodyType = BodyType.BLACK_HOLE
            )
            bodies.add(bhB)

            // Dust rings around Sagittarius A*
            val particlesA = 35
            for (i in 0 until particlesA) {
                val r = 50.0 + (i * 5.0) + Math.random() * 8.0
                val angle = Math.random() * 2.0 * Math.PI
                val px = bhA.x + r * Math.cos(angle)
                val py = bhA.y + r * Math.sin(angle)

                val (vx, vy) = calculateOrbitVelocity(px, py, bhA)
                // Add minor random noise to make orbits irregular
                val jitter = 0.15
                bodies.add(
                    SpaceBody(
                        name = "Dust A-$i",
                        x = px, y = py,
                        vx = vx + (Math.random() - 0.5) * jitter,
                        vy = vy + (Math.random() - 0.5) * jitter,
                        mass = 0.1, radius = 2f,
                        color = 0xFF00FFFF.toInt(), // Neon cyan
                        bodyType = BodyType.DUST
                    )
                )
            }

            // Dust rings around Andromeda Core
            val particlesB = 35
            for (i in 0 until particlesB) {
                val r = 50.0 + (i * 5.0) + Math.random() * 8.0
                val angle = Math.random() * 2.0 * Math.PI
                val px = bhB.x + r * Math.cos(angle)
                val py = bhB.y + r * Math.sin(angle)

                val (vx, vy) = calculateOrbitVelocity(px, py, bhB)
                val jitter = 0.15
                bodies.add(
                    SpaceBody(
                        name = "Dust B-$i",
                        x = px, y = py,
                        vx = vx + (Math.random() - 0.5) * jitter,
                        vy = vy + (Math.random() - 0.5) * jitter,
                        mass = 0.1, radius = 2f,
                        color = 0xFFFF007F.toInt(), // Pink/rose neon
                        bodyType = BodyType.DUST
                    )
                )
            }
        }
    }

    fun loadPresetBinaryStars() {
        synchronized(lock) {
            clear()

            // Two co-orbiting stars of equal mass
            // Center of mass (0,0), distance = 240, r = 120
            // v = sqrt(G * M_total / (4 * r)) = sqrt(1.0 * 100000 / 480) = sqrt(208.33) ~ 14.43
            val m = 50000.0
            val r = 120.0
            val v = sqrt((G * (m + m)) / (4.0 * r))

            val star1 = SpaceBody(
                name = "Alpha",
                x = -r, y = 0.0,
                vx = 0.0, vy = -v,
                mass = m, radius = 20f,
                color = 0xFFFF4500.toInt(), // Orange Red
                bodyType = BodyType.STAR
            )
            val star2 = SpaceBody(
                name = "Beta",
                x = r, y = 0.0,
                vx = 0.0, vy = v,
                mass = m, radius = 20f,
                color = 0xFFFFD700.toInt(), // Golden Yellow
                bodyType = BodyType.STAR
            )
            bodies.add(star1)
            bodies.add(star2)

            // Add circumbinary gas giant orbiting far away
            // Total central mass = 100000 at (0,0)
            val orbitR = 340.0
            val pX = 0.0
            val pY = orbitR
            val orbitalSpeed = sqrt((G * 100000.0) / orbitR)

            val planet = SpaceBody(
                name = "Tatooine",
                x = pX, y = pY,
                vx = orbitalSpeed, vy = 0.0,
                mass = 150.0, radius = 10f,
                color = 0xFF20B2AA.toInt(), // Light Sea Green
                bodyType = BodyType.GAS_GIANT
            )
            bodies.add(planet)

            // Add some ring dust around the planet
            for (i in 0 until 12) {
                val ringR = 22.0 + i * 1.5
                val angle = Math.random() * 2.0 * Math.PI
                val px = planet.x + ringR * Math.cos(angle)
                val py = planet.y + ringR * Math.sin(angle)
                val (rvx, rvy) = calculateOrbitVelocity(px, py, planet)
                bodies.add(
                    SpaceBody(
                        name = "Ring-$i",
                        x = px, y = py,
                        vx = rvx, vy = rvy,
                        mass = 0.05, radius = 1.5f,
                        color = 0xAAFFFAF0.toInt(), // Floral white translucent
                        bodyType = BodyType.DUST
                    )
                )
            }
        }
    }

    fun loadPresetAccretionDisk() {
        synchronized(lock) {
            clear()

            // Supermassive Black Hole
            val centralBH = SpaceBody(
                name = "Gargantua",
                x = 0.0, y = 0.0,
                vx = 0.0, vy = 0.0,
                mass = 600000.0, radius = 15f,
                color = 0xFF000000.toInt(), // Pure black center, but we'll draw customized neon aura
                bodyType = BodyType.BLACK_HOLE,
                isFixed = true
            )
            bodies.add(centralBH)

            // 110 Accretion particles
            val count = 110
            for (i in 0 until count) {
                // Distribute distances logarithmically or with a curve to have higher density near the BH
                val norm = i.toDouble() / count
                val r = 40.0 + (norm * norm * 450.0) + Math.random() * 15.0
                val angle = Math.random() * 2.0 * Math.PI

                val px = r * Math.cos(angle)
                val py = r * Math.sin(angle)

                val (vx, vy) = calculateOrbitVelocity(px, py, centralBH)

                // Hot colors closer to BH, cooler colors further out
                val color = when {
                    r < 100.0 -> 0xFFFF4500.toInt() // OrangeRed
                    r < 220.0 -> 0xFFFF8C00.toInt() // DarkOrange
                    r < 350.0 -> 0xFFFFD700.toInt() // Gold
                    else -> 0xFFFFE4B5.toInt() // Moccasin
                }

                bodies.add(
                    SpaceBody(
                        name = "Disk dust $i",
                        x = px, y = py,
                        vx = vx, vy = vy,
                        mass = 0.01, radius = 1.5f,
                        color = color,
                        bodyType = BodyType.DUST
                    )
                )
            }
        }
    }

    private fun addOrbitingPlanet(
        name: String,
        distance: Double,
        mass: Double,
        radius: Float,
        color: Int,
        type: BodyType,
        central: SpaceBody
    ): SpaceBody {
        val angle = Math.random() * 2.0 * Math.PI
        val px = central.x + distance * Math.cos(angle)
        val py = central.y + distance * Math.sin(angle)

        val (vx, vy) = calculateOrbitVelocity(px, py, central)

        val body = SpaceBody(
            name = name,
            x = px, y = py,
            vx = vx, vy = vy,
            mass = mass, radius = radius,
            color = color,
            bodyType = type
        )
        bodies.add(body)
        return body
    }
}
