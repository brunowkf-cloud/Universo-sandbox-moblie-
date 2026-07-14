package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UniverseEntity
import com.example.model.ActiveTool
import com.example.model.BodyType
import com.example.model.SpaceBody
import com.example.ui.theme.CelestialGold
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CosmicNeonPurple
import com.example.ui.theme.DarkGreySpace
import com.example.ui.theme.GlassCard
import com.example.ui.theme.PlasmaOrange
import com.example.viewmodel.UniverseViewModel
import java.util.Locale

@Composable
fun TelemetryPanel(
    viewModel: UniverseViewModel,
    modifier: Modifier = Modifier
) {
    val bodies by viewModel.bodiesState.collectAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        modifier = modifier
            .widthIn(max = 240.dp)
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "COSMIC TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CelestialGold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            TelemetryRow(label = "Total Bodies", value = "${bodies.size}")

            val kineticFormatted = String.format(Locale.US, "%.1e", viewModel.kineticEnergy)
            val potentialFormatted = String.format(Locale.US, "%.1e", viewModel.potentialEnergy)
            val totalFormatted = String.format(Locale.US, "%.1e", viewModel.kineticEnergy + viewModel.potentialEnergy)

            TelemetryRow(label = "Kinetic Energy (K)", value = kineticFormatted)
            TelemetryRow(label = "Potential Energy (U)", value = potentialFormatted)
            TelemetryRow(label = "Total Energy (E)", value = totalFormatted, valueColor = CosmicCyan)

            HorizontalDivider(color = Color(0x20FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

            val baryX = String.format(Locale.US, "%.1f", viewModel.centerOfMassX)
            val baryY = String.format(Locale.US, "%.1f", viewModel.centerOfMassY)
            TelemetryRow(label = "Barycenter", value = "($baryX, $baryY)", valueColor = Color.LightGray)
            TelemetryRow(label = "Time Step dt", value = "${viewModel.timeScaleVal}x", valueColor = CosmicNeonPurple)
        }
    }
}

@Composable
fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopControlBar(
    viewModel: UniverseViewModel,
    onOpenSaveDialog: () -> Unit,
    onOpenLoadDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var presetsExpanded by remember { mutableStateOf(false) }
    val presets = listOf("Solar System", "Galaxy Collision", "Binary Stars", "Accretion Disk")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play / Pause
            IconButton(
                onClick = { viewModel.togglePause() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (viewModel.isPaused) PlasmaOrange.copy(alpha = 0.2f) else CosmicCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Play/Pause",
                    tint = if (viewModel.isPaused) PlasmaOrange else CosmicCyan
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Parameters and Zoom Controls
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Gravity Icon and label
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Gravity Strength",
                    tint = CelestialGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.width(75.dp)) {
                    Text("Gravity G", fontSize = 9.sp, color = Color.LightGray)
                    Slider(
                        value = viewModel.GVal.toFloat(),
                        onValueChange = { viewModel.updateG(it.toDouble()) },
                        valueRange = 0.1f..5.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CelestialGold,
                            activeTrackColor = CelestialGold,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.height(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Speed Icon and label
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Sim Speed",
                    tint = CosmicNeonPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.width(75.dp)) {
                    Text("Time Step", fontSize = 9.sp, color = Color.LightGray)
                    Slider(
                        value = viewModel.timeScaleVal.toFloat(),
                        onValueChange = { viewModel.updateTimeScale(it.toDouble()) },
                        valueRange = 0.1f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CosmicNeonPurple,
                            activeTrackColor = CosmicNeonPurple,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.height(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Preset Dropdown
            Box {
                OutlinedButton(
                    onClick = { presetsExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(34.dp),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
                ) {
                    Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = presetsExpanded,
                    onDismissRequest = { presetsExpanded = false },
                    modifier = Modifier.background(DarkGreySpace)
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset, color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                viewModel.loadPreset(preset)
                                presetsExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Camera Focus
            IconButton(
                onClick = { viewModel.resetCamera() },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x15FFFFFF)),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Reset Camera",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Save / Load Action triggers
            IconButton(
                onClick = onOpenSaveDialog,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x15FFFFFF)),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Universe",
                    tint = CosmicCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Load trigger
            IconButton(
                onClick = onOpenLoadDialog,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x15FFFFFF)),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Load Universe",
                    tint = CelestialGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SpawningTray(
    viewModel: UniverseViewModel,
    modifier: Modifier = Modifier
) {
    val celestialTypes = listOf(
        BodyType.BLACK_HOLE,
        BodyType.STAR,
        BodyType.GAS_GIANT,
        BodyType.TERRESTRIAL,
        BodyType.MOON,
        BodyType.DUST
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeButton(
                    label = "Launch Vector",
                    isActive = viewModel.activeTool == ActiveTool.LAUNCH_VECTOR,
                    onClick = { viewModel.activeTool = ActiveTool.LAUNCH_VECTOR }
                )
                ModeButton(
                    label = "Orbital stable",
                    isActive = viewModel.activeTool == ActiveTool.LAUNCH_ORBIT,
                    onClick = { viewModel.activeTool = ActiveTool.LAUNCH_ORBIT }
                )
                ModeButton(
                    label = "Erase",
                    isActive = viewModel.activeTool == ActiveTool.ERASE,
                    onClick = { viewModel.activeTool = ActiveTool.ERASE }
                )
                ModeButton(
                    label = "Inspector",
                    isActive = viewModel.activeTool == ActiveTool.INSPECT,
                    onClick = { viewModel.activeTool = ActiveTool.INSPECT }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Celestial Spawner list
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(celestialTypes) { type ->
                    val props = viewModel.getDefaultSpawningProps(type)
                    val isSelected = viewModel.spawningType == type && viewModel.activeTool != ActiveTool.ERASE && viewModel.activeTool != ActiveTool.INSPECT

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0x25FFFFFF) else Color.Transparent)
                            .border(1.dp, if (isSelected) Color(props.color) else Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.spawningType = type
                                if (viewModel.activeTool == ActiveTool.ERASE || viewModel.activeTool == ActiveTool.INSPECT) {
                                    viewModel.activeTool = ActiveTool.LAUNCH_VECTOR
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(props.color), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = props.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = formatMassString(type, props.mass),
                                    fontSize = 8.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatMassString(type: BodyType, mass: Double): String {
    return when (type) {
        BodyType.BLACK_HOLE -> "${mass / 10000.0}k M☉"
        BodyType.STAR -> "${mass / 10000.0} M☉"
        BodyType.GAS_GIANT -> "${mass / 10.0} M⊕"
        BodyType.TERRESTRIAL -> "${mass / 10.0} M⊕"
        else -> "${mass} GU"
    }
}

@Composable
fun ModeButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFF6200EE) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color.LightGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorPanel(
    viewModel: UniverseViewModel,
    modifier: Modifier = Modifier
) {
    val bodies by viewModel.bodiesState.collectAsState()
    val selectedBody = bodies.find { it.id == viewModel.selectedBodyId }

    AnimatedVisibility(
        visible = selectedBody != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (selectedBody != null) {
            // Temporary editing states
            var editName by remember(selectedBody.id) { mutableStateOf(selectedBody.name) }
            var editMass by remember(selectedBody.id) { mutableStateOf(selectedBody.mass) }
            var editRadius by remember(selectedBody.id) { mutableStateOf(selectedBody.radius) }
            var editIsFixed by remember(selectedBody.id) { mutableStateOf(selectedBody.isFixed) }

            val themeColor = Color(selectedBody.color)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(themeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OBJECT INSPECTOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CelestialGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = { viewModel.selectBody(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("×", color = Color.White, fontSize = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable properties
                    TextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            viewModel.updateSelectedBodyProperties(it, editMass, editRadius, selectedBody.color, editIsFixed)
                        },
                        label = { Text("Body Name", fontSize = 10.sp, color = Color.LightGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x10FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = CosmicCyan,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mass Slider
                    val massMin = 0.1
                    val massMax = when (selectedBody.bodyType) {
                        BodyType.BLACK_HOLE -> 1000000.0
                        BodyType.STAR -> 150000.0
                        BodyType.GAS_GIANT -> 5000.0
                        BodyType.TERRESTRIAL -> 1000.0
                        else -> 50.0
                    }

                    Text(
                        text = "Mass: ${formatMassString(selectedBody.bodyType, editMass)}",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = editMass.toFloat(),
                        onValueChange = {
                            editMass = it.toDouble()
                            viewModel.updateSelectedBodyProperties(editName, editMass, editRadius, selectedBody.color, editIsFixed)
                        },
                        valueRange = massMin.toFloat()..massMax.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = themeColor,
                            activeTrackColor = themeColor
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Radius Slider
                    Text(
                        text = "Visual Size: ${String.format(Locale.US, "%.1f", editRadius)}px",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = editRadius,
                        onValueChange = {
                            editRadius = it
                            viewModel.updateSelectedBodyProperties(editName, editMass, editRadius, selectedBody.color, editIsFixed)
                        },
                        valueRange = 2f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = themeColor,
                            activeTrackColor = themeColor
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fixed Position Anchor Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editIsFixed,
                            onCheckedChange = {
                                editIsFixed = it
                                viewModel.updateSelectedBodyProperties(editName, editMass, editRadius, selectedBody.color, it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CosmicCyan,
                                uncheckedColor = Color.LightGray
                            )
                        )
                        Column {
                            Text("Fixed Position Anchor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Immune to orbital drag & gravity pull", fontSize = 8.sp, color = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Track Follow & Destroy Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isFollowing = viewModel.followedBodyId == selectedBody.id
                        Button(
                            onClick = {
                                if (isFollowing) viewModel.followBody(null) else viewModel.followBody(selectedBody.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) PlasmaOrange else Color(0xFF6200EE)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Camera else Icons.Default.MyLocation,
                                contentDescription = "Follow",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFollowing) "Unfollow" else "Follow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.deleteBody(selectedBody.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Destroy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveUniverseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkGreySpace,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "SAVE CURRENT UNIVERSE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CelestialGold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Universe Name") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, description)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Sandbox")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadUniverseDialog(
    savedUniverses: List<UniverseEntity>,
    onDismiss: () -> Unit,
    onLoad: (UniverseEntity) -> Unit,
    onDelete: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkGreySpace,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "LOAD SAVED UNIVERSE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CelestialGold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (savedUniverses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved sandboxes found.\nBuild galaxies and save your work!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedUniverses) { entity ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0x10FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLoad(entity)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entity.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        if (entity.description.isNotBlank()) {
                                            Text(
                                                text = entity.description,
                                                fontSize = 10.sp,
                                                color = Color.LightGray,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date(entity.timestamp)),
                                            fontSize = 8.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(entity.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Save",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
