package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CelestialGold
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.GlassCard
import com.example.viewmodel.UniverseViewModel

@Composable
fun MainScreen(
    viewModel: UniverseViewModel,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showOnboardingInfo by remember { mutableStateOf(true) }

    val savedUniverses by viewModel.savedUniverses.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) // Full bleed canvas!
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Fullscreen interactive Physics Canvas
            SandboxCanvas(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // 2. HUD & Overlays Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .statusBarsPadding()
            ) {
                // Top control center
                TopControlBar(
                    viewModel = viewModel,
                    onOpenSaveDialog = { showSaveDialog = true },
                    onOpenLoadDialog = { showLoadDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Telemetry dashboard floated on top-left
                Row(modifier = Modifier.fillMaxWidth()) {
                    TelemetryPanel(viewModel = viewModel)

                    Spacer(modifier = Modifier.weight(1f))

                    // Floating help/onboarding toggle
                    IconButton(
                        onClick = { showOnboardingInfo = !showOnboardingInfo },
                        modifier = Modifier.background(GlassCard, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show Sandbox Guide",
                            tint = CelestialGold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Responsive bottom sections (Inspector & Spawner Tray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left: Spawning panel
                    Box(modifier = Modifier.weight(1.3f)) {
                        SpawningTray(viewModel = viewModel)
                    }

                    // Right: Inspector panel (visible if a body is inspected)
                    val activeBodies by viewModel.bodiesState.collectAsState()
                    val hasSelection = activeBodies.any { it.id == viewModel.selectedBodyId }
                    if (hasSelection) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            InspectorPanel(viewModel = viewModel)
                        }
                    }
                }
            }

            // 3. Quick Sandbox Onboarding Overlay
            AnimatedVisibility(
                visible = showOnboardingInfo,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.2.dp, CelestialGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COSMIC SANDBOX BUILDER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CelestialGold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Text(
                            text = "Welcome to your gravity sandbox. Experiment with orbital mechanics in real-time:\n\n" +
                                    "🌌 Drag & Fling: Swipe anywhere on empty space to pan. Choose Launch Vector tool, tap-drag and slingshot to launch a planet with dynamic speed!\n\n" +
                                    "💫 Circular Orbits: Choose Orbital Stable tool. Tap to instantly spawn planets in perfect, balanced orbits relative to nearby stars!\n\n" +
                                    "🔭 Inspector: Tap any body to rename, scale mass, lock positions, or track/follow camera focus.",
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showOnboardingInfo = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Initialize Simulation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // 4. Save/Load Dialog boxes
            if (showSaveDialog) {
                SaveUniverseDialog(
                    onDismiss = { showSaveDialog = false },
                    onSave = { name, desc -> viewModel.saveUniverse(name, desc) }
                )
            }

            if (showLoadDialog) {
                LoadUniverseDialog(
                    savedUniverses = savedUniverses,
                    onDismiss = { showLoadDialog = false },
                    onLoad = { entity -> viewModel.loadUniverse(entity) },
                    onDelete = { id -> viewModel.deleteSavedUniverse(id) }
                )
            }
        }
    }
}
