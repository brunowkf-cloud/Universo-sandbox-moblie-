package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.ActiveTool
import com.example.model.BodyType
import com.example.model.SpaceBody
import com.example.ui.theme.SpaceVoid
import com.example.viewmodel.UniverseViewModel
import kotlin.math.sqrt
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun SandboxCanvas(
    viewModel: UniverseViewModel,
    modifier: Modifier = Modifier
) {
    val bodies by remember { mutableStateOf(viewModel.bodiesState) }
    val activeBodies = bodies.value

    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf<Offset?>(null) }
    var isPanning by remember { mutableStateOf(false) }

    // Constants for mapping screen pixels to physical coordinates
    val velocityScale = 0.08 // Translate drag distance to velocity units

    Box(modifier = modifier.fillMaxSize().background(SpaceVoid)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewModel.activeTool, viewModel.spawningType, activeBodies) {
                    // Tap handling
                    detectTapGestures { tapOffset ->
                        val zoom = viewModel.simulator.zoom
                        val panX = viewModel.simulator.panX
                        val panY = viewModel.simulator.panY

                        // Calculate physical simulation coordinates from screen pixels
                        // ScreenX = (PhysX + panX) * zoom + CenterX
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        val physX = (((tapOffset.x - centerX) / zoom) - panX).toDouble()
                        val physY = (((tapOffset.y - centerY) / zoom) - panY).toDouble()

                        // Check if tapped on any body
                        val clickedBody = activeBodies.find { b ->
                            val dx = b.x - physX
                            val dy = b.y - physY
                            val dist = sqrt(dx * dx + dy * dy)
                            dist <= b.radius + (15.0 / zoom) // Add touch padding on high zoom outs
                        }

                        when (viewModel.activeTool) {
                            ActiveTool.ERASE -> {
                                clickedBody?.let { viewModel.deleteBody(it.id) }
                            }
                            ActiveTool.INSPECT -> {
                                viewModel.selectBody(clickedBody?.id)
                            }
                            ActiveTool.LAUNCH_ORBIT -> {
                                if (clickedBody == null) {
                                    // Spawns body in stable circular orbit around closest dominant mass
                                    val central = viewModel.simulator.findDominantBody(physX, physY)
                                    if (central != null) {
                                        val (vx, vy) = viewModel.simulator.calculateOrbitVelocity(physX, physY, central)
                                        viewModel.spawnBody(physX, physY, vx, vy)
                                    } else {
                                        // Spawn at static rest if no body exists
                                        viewModel.spawnBody(physX, physY, 0.0, 0.0)
                                    }
                                } else {
                                    // Inspect the clicked body instead
                                    viewModel.selectBody(clickedBody.id)
                                }
                            }
                            ActiveTool.LAUNCH_VECTOR -> {
                                clickedBody?.let {
                                    viewModel.selectBody(it.id)
                                    viewModel.activeTool = ActiveTool.INSPECT
                                }
                            }
                        }
                    }
                }
                .pointerInput(viewModel.activeTool, viewModel.spawningType, activeBodies) {
                    // Drag handling
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val zoom = viewModel.simulator.zoom
                            val panX = viewModel.simulator.panX
                            val panY = viewModel.simulator.panY
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            val physX = (((startOffset.x - centerX) / zoom) - panX).toDouble()
                            val physY = (((startOffset.y - centerY) / zoom) - panY).toDouble()

                            // Check if starting drag on a body for vector launching, or empty space
                            val clickedBody = activeBodies.find { b ->
                                val dx = b.x - physX
                                val dy = b.y - physY
                                val dist = sqrt(dx * dx + dy * dy)
                                dist <= b.radius + (15.0 / zoom)
                            }

                            if (viewModel.activeTool == ActiveTool.LAUNCH_VECTOR && clickedBody == null) {
                                dragStartOffset = startOffset
                                dragCurrentOffset = startOffset
                                isPanning = false
                            } else {
                                // Default to camera panning
                                isPanning = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isPanning) {
                                // Pan camera
                                val zoom = viewModel.simulator.zoom
                                val currentPanX = viewModel.simulator.panX
                                val currentPanY = viewModel.simulator.panY
                                viewModel.setPanAndZoom(
                                    currentPanX + dragAmount.x / zoom,
                                    currentPanY + dragAmount.y / zoom,
                                    zoom
                                )
                            } else {
                                // Update vector launching offsets
                                dragCurrentOffset = (dragCurrentOffset ?: Offset.Zero) + dragAmount
                            }
                        },
                        onDragEnd = {
                            if (!isPanning && dragStartOffset != null && dragCurrentOffset != null) {
                                val start = dragStartOffset!!
                                val end = dragCurrentOffset!!

                                val zoom = viewModel.simulator.zoom
                                val panX = viewModel.simulator.panX
                                val panY = viewModel.simulator.panY
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f

                                val physX = (((start.x - centerX) / zoom) - panX).toDouble()
                                val physY = (((start.y - centerY) / zoom) - panY).toDouble()

                                // Velocity is opposite of drag direction (slingshot launch!)
                                val vx = ((start.x - end.x) * velocityScale / zoom).toDouble()
                                val vy = ((start.y - end.y) * velocityScale / zoom).toDouble()

                                viewModel.spawnBody(physX, physY, vx, vy)
                            }
                            dragStartOffset = null
                            dragCurrentOffset = null
                            isPanning = false
                        }
                    )
                }
        ) {
            val zoom = viewModel.simulator.zoom
            val panX = viewModel.simulator.panX
            val panY = viewModel.simulator.panY
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // Helper to map physical space to screen pixels
            fun toScreen(x: Double, y: Double): Offset {
                val sx = ((x + panX) * zoom + centerX).toFloat()
                val sy = ((y + panY) * zoom + centerY).toFloat()
                return Offset(sx, sy)
            }

            // Draw a subtle coordinate grid for cosmic orientation
            val gridSize = 100f * zoom
            val startGridX = (panX * zoom + centerX) % gridSize
            val startGridY = (panY * zoom + centerY) % gridSize

            for (x in -1..(size.width / gridSize).toInt() + 1) {
                val gridX = startGridX + x * gridSize
                drawLine(
                    color = Color(0x156200EE),
                    start = Offset(gridX, 0f),
                    end = Offset(gridX, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in -1..(size.height / gridSize).toInt() + 1) {
                val gridY = startGridY + y * gridSize
                drawLine(
                    color = Color(0x156200EE),
                    start = Offset(0f, gridY),
                    end = Offset(size.width, gridY),
                    strokeWidth = 1f
                )
            }

            // Draw trails of all bodies
            for (b in activeBodies) {
                if (b.trail.size < 2) continue
                val trailPath = Path()
                val startS = toScreen(b.trail[0].first.toDouble(), b.trail[0].second.toDouble())
                trailPath.moveTo(startS.x, startS.y)

                for (idx in 1 until b.trail.size) {
                    val p = toScreen(b.trail[idx].first.toDouble(), b.trail[idx].second.toDouble())
                    trailPath.lineTo(p.x, p.y)
                }

                drawPath(
                    path = trailPath,
                    color = Color(b.color).copy(alpha = 0.45f),
                    style = Stroke(width = 2.5f * zoom)
                )
            }

            // Draw celestial bodies
            for (b in activeBodies) {
                val sPos = toScreen(b.x, b.y)
                val baseRadius = b.radius * zoom

                // Don't draw if outside viewport
                if (sPos.x < -baseRadius || sPos.x > size.width + baseRadius ||
                    sPos.y < -baseRadius || sPos.y > size.height + baseRadius
                ) {
                    continue
                }

                val bodyColor = Color(b.color)

                when (b.bodyType) {
                    BodyType.BLACK_HOLE -> {
                        // Drawing accretion glow & Schwarzschild radius
                        val outerGlow = baseRadius * 3.5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF8A2BE2).copy(alpha = 0.55f), Color.Transparent),
                                center = sPos,
                                radius = outerGlow
                            ),
                            center = sPos,
                            radius = outerGlow
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF007F).copy(alpha = 0.25f), Color.Transparent),
                                center = sPos,
                                radius = baseRadius * 1.8f
                            ),
                            center = sPos,
                            radius = baseRadius * 1.8f
                        )
                        // Event horizon core (Pure pitch black!)
                        drawCircle(
                            color = Color(0xFF000000),
                            center = sPos,
                            radius = baseRadius
                        )
                        drawCircle(
                            color = Color(0xFF9400D3),
                            center = sPos,
                            radius = baseRadius,
                            style = Stroke(width = 2.0f * zoom)
                        )
                    }
                    BodyType.STAR -> {
                        // Bright sun with dynamic outer glow
                        val starGlow = baseRadius * 4.0f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(bodyColor.copy(alpha = 0.6f), Color.Transparent),
                                center = sPos,
                                radius = starGlow
                            ),
                            center = sPos,
                            radius = starGlow
                        )
                        drawCircle(
                            color = Color.White,
                            center = sPos,
                            radius = baseRadius * 0.7f
                        )
                        drawCircle(
                            color = bodyColor,
                            center = sPos,
                            radius = baseRadius,
                            style = Stroke(width = 1.5f * zoom)
                        )
                    }
                    BodyType.GAS_GIANT -> {
                        // Banded textured planet (gas rings)
                        drawCircle(
                            color = bodyColor,
                            center = sPos,
                            radius = baseRadius
                        )
                        // Gas giant bands
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(sPos.x - baseRadius * 0.8f, sPos.y - baseRadius * 0.2f),
                            end = Offset(sPos.x + baseRadius * 0.8f, sPos.y - baseRadius * 0.2f),
                            strokeWidth = 2.5f * zoom
                        )
                        drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(sPos.x - baseRadius * 0.9f, sPos.y + baseRadius * 0.2f),
                            end = Offset(sPos.x + baseRadius * 0.9f, sPos.y + baseRadius * 0.2f),
                            strokeWidth = 2f * zoom
                        )
                        // Dynamic subtle atmospheric glow
                        drawCircle(
                            color = bodyColor.copy(alpha = 0.15f),
                            center = sPos,
                            radius = baseRadius * 1.3f,
                            style = Stroke(width = 2f * zoom)
                        )
                    }
                    BodyType.TERRESTRIAL -> {
                        drawCircle(
                            color = bodyColor,
                            center = sPos,
                            radius = baseRadius
                        )
                        // Add some continent-like texture highlights
                        drawCircle(
                            color = Color(0x30FFFFFF),
                            center = Offset(sPos.x - baseRadius * 0.3f, sPos.y - baseRadius * 0.2f),
                            radius = baseRadius * 0.4f
                        )
                        drawCircle(
                            color = Color(0x304CAF50),
                            center = Offset(sPos.x + baseRadius * 0.2f, sPos.y + baseRadius * 0.3f),
                            radius = baseRadius * 0.35f
                        )
                        // Blue Atmosphere outline
                        drawCircle(
                            color = Color(0x5000E5FF),
                            center = sPos,
                            radius = baseRadius + 1.5f * zoom,
                            style = Stroke(width = 1.0f * zoom)
                        )
                    }
                    BodyType.MOON -> {
                        drawCircle(
                            color = bodyColor,
                            center = sPos,
                            radius = baseRadius
                        )
                        // Crater dots
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            center = Offset(sPos.x - baseRadius * 0.3f, sPos.y),
                            radius = baseRadius * 0.25f
                        )
                    }
                    BodyType.DUST -> {
                        drawCircle(
                            color = bodyColor,
                            center = sPos,
                            radius = baseRadius.coerceAtLeast(1.5f)
                        )
                    }
                }

                // If inspected, draw selection ring
                if (b.id == viewModel.selectedBodyId) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        center = sPos,
                        radius = baseRadius + 8f * zoom,
                        style = Stroke(width = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }

                // Draw name labels for stars, planets, and black holes
                if (b.bodyType != BodyType.DUST && zoom >= 0.4f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        b.name,
                        sPos.x,
                        sPos.y - baseRadius - 10f,
                        android.graphics.Paint().apply {
                            color = AndroidColor.WHITE
                            textSize = (11f * zoom).coerceIn(10f, 14f) * density
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }

            // Draw launcher slingshot vector if dragging
            if (dragStartOffset != null && dragCurrentOffset != null) {
                val start = dragStartOffset!!
                val end = dragCurrentOffset!!

                // Slingshot logic: launching direction is opposite of drag direction
                val launchVector = start - end
                val launchTarget = start + launchVector

                // Draw aiming dotted line (drag line)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = start,
                    end = end,
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Draw powerful launching velocity arrow
                drawLine(
                    color = Color(0xFF00FFCC),
                    start = start,
                    end = launchTarget,
                    strokeWidth = 4f
                )

                // Arrow head calculations
                val arrowLen = launchVector.getDistance()
                if (arrowLen > 10f) {
                    val unit = launchVector / arrowLen
                    val leftWing = Offset(-unit.y, unit.x) * 12f - unit * 15f
                    val rightWing = Offset(unit.y, -unit.x) * 12f - unit * 15f

                    drawLine(
                        color = Color(0xFF00FFCC),
                        start = launchTarget,
                        end = launchTarget + leftWing,
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = Color(0xFF00FFCC),
                        start = launchTarget,
                        end = launchTarget + rightWing,
                        strokeWidth = 4f
                    )
                }

                // Draw orbit prediction line
                val zoomFactor = viewModel.simulator.zoom
                val panXVal = viewModel.simulator.panX
                val panYVal = viewModel.simulator.panY
                val screenCenterX = size.width / 2f
                val screenCenterY = size.height / 2f

                val physX = (((start.x - screenCenterX) / zoomFactor) - panXVal).toDouble()
                val physY = (((start.y - screenCenterY) / zoomFactor) - panYVal).toDouble()
                val vx = ((start.x - end.x) * velocityScale / zoomFactor).toDouble()
                val vy = ((start.y - end.y) * velocityScale / zoomFactor).toDouble()

                // Simulate forward in real-time to render an orbit prediction path
                val dominant = viewModel.simulator.findDominantBody(physX, physY)
                if (dominant != null) {
                    val predictPath = Path()
                    var curX = physX
                    var curY = physY
                    var curVx = vx
                    var curVy = vy

                    // Use standard 2-body Kepler integration for fast rendering of predicted orbit
                    val steps = 80
                    val simDt = 4.0
                    val pStart = toScreen(curX, curY)
                    predictPath.moveTo(pStart.x, pStart.y)

                    for (step in 0 until steps) {
                        val dx = dominant.x - curX
                        val dy = dominant.y - curY
                        val dSq = dx * dx + dy * dy + 25.0
                        val d = sqrt(dSq)

                        if (d > 0.1) {
                            val forceAcc = (viewModel.GVal * dominant.mass) / dSq
                            curVx += (forceAcc * dx / d) * simDt
                            curVy += (forceAcc * dy / d) * simDt
                        }
                        curX += curVx * simDt
                        curY += curVy * simDt

                        val pScreen = toScreen(curX, curY)
                        predictPath.lineTo(pScreen.x, pScreen.y)
                    }

                    drawPath(
                        path = predictPath,
                        color = Color(0xFF03DAC6).copy(alpha = 0.65f),
                        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                    )
                }
            }
        }
    }
}
