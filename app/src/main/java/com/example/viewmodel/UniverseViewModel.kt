package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UniverseDatabase
import com.example.data.UniverseEntity
import com.example.data.UniverseRepository
import com.example.model.ActiveTool
import com.example.model.BodyType
import com.example.model.SpaceBody
import com.example.physics.GravitySimulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class UniverseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UniverseRepository
    val savedUniverses: StateFlow<List<UniverseEntity>>

    val simulator = GravitySimulator()

    // UI exposed state of active bodies
    private val _bodiesState = MutableStateFlow<List<SpaceBody>>(emptyList())
    val bodiesState: StateFlow<List<SpaceBody>> = _bodiesState.asStateFlow()

    // Simulation states
    var isPaused by mutableStateOf(false)
    var GVal by mutableStateOf(1.0)
    var timeScaleVal by mutableStateOf(1.0)

    // Tool states
    var activeTool by mutableStateOf(ActiveTool.LAUNCH_VECTOR)
    var spawningType by mutableStateOf(BodyType.TERRESTRIAL)

    // Selected / followed bodies
    var selectedBodyId by mutableStateOf<String?>(null)
    var followedBodyId by mutableStateOf<String?>(null)

    // Stats
    var kineticEnergy by mutableStateOf(0.0)
    var potentialEnergy by mutableStateOf(0.0)
    var centerOfMassX by mutableStateOf(0.0)
    var centerOfMassY by mutableStateOf(0.0)

    init {
        val database = UniverseDatabase.getDatabase(application)
        repository = UniverseRepository(database.universeDao())
        savedUniverses = repository.allUniverses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load default solar system preset initially
        loadPreset("Solar System")

        // Start Physics simulation loop
        viewModelScope.launch(Dispatchers.Default) {
            val fixedDt = 0.5 // Stable physics step size
            while (true) {
                if (!isPaused) {
                    simulator.step(fixedDt)
                    updateStats()
                    // Apply camera tracking if following an object
                    followedBodyId?.let { id ->
                        val b = simulator.getBodies().find { it.id == id }
                        if (b != null) {
                            simulator.panX = -b.x.toFloat()
                            simulator.panY = -b.y.toFloat()
                        } else {
                            followedBodyId = null
                            simulator.followedBodyId = null
                        }
                    }
                }
                _bodiesState.value = simulator.getBodies()
                delay(16) // Target ~60 physics ticks per second
            }
        }
    }

    private fun updateStats() {
        val (k, p) = simulator.calculateTotalEnergy()
        kineticEnergy = k
        potentialEnergy = p

        val (comX, comY) = simulator.calculateCenterOfMass()
        centerOfMassX = comX
        centerOfMassY = comY
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun updateG(newG: Double) {
        GVal = newG
        simulator.G = newG
    }

    fun updateTimeScale(newTimeScale: Double) {
        timeScaleVal = newTimeScale
        simulator.timeScale = newTimeScale
    }

    fun setPanAndZoom(px: Float, py: Float, z: Float) {
        if (followedBodyId == null) {
            simulator.panX = px
            simulator.panY = py
        }
        simulator.zoom = z
    }

    fun resetCamera() {
        followedBodyId = null
        simulator.followedBodyId = null
        simulator.panX = 0.0f
        simulator.panY = 0.0f
        simulator.zoom = 1.0f
    }

    fun clearSimulation() {
        simulator.clear()
        selectedBodyId = null
        followedBodyId = null
        _bodiesState.value = emptyList()
        updateStats()
    }

    fun loadPreset(presetName: String) {
        selectedBodyId = null
        followedBodyId = null
        simulator.followedBodyId = null
        when (presetName) {
            "Solar System" -> simulator.loadPresetSolarSystem()
            "Galaxy Collision" -> simulator.loadPresetGalaxyCollision()
            "Binary Stars" -> simulator.loadPresetBinaryStars()
            "Accretion Disk" -> simulator.loadPresetAccretionDisk()
            else -> simulator.clear()
        }
        _bodiesState.value = simulator.getBodies()
        updateStats()
        resetCamera()
    }

    // Helper to fetch properties for a spawning type
    fun getDefaultSpawningProps(type: BodyType): SpawningProperties {
        return when (type) {
            BodyType.BLACK_HOLE -> SpawningProperties(
                name = "Black Hole",
                mass = 200000.0,
                radius = 16f,
                color = 0xFF4A148C.toInt() // Deep Violet
            )
            BodyType.STAR -> SpawningProperties(
                name = "Sun",
                mass = 30000.0,
                radius = 24f,
                color = 0xFFFFD54F.toInt() // Radiant Gold
            )
            BodyType.GAS_GIANT -> SpawningProperties(
                name = "Gas Giant",
                mass = 800.0,
                radius = 16f,
                color = 0xFFFF7043.toInt() // Deep Orange
            )
            BodyType.TERRESTRIAL -> SpawningProperties(
                name = "Earth",
                mass = 100.0,
                radius = 10f,
                color = 0xFF29B6F6.toInt() // Sky Blue
            )
            BodyType.MOON -> SpawningProperties(
                name = "Moon",
                mass = 1.0,
                radius = 4.5f,
                color = 0xFFB0BEC5.toInt() // Cool Gray
            )
            BodyType.DUST -> SpawningProperties(
                name = "Space Dust",
                mass = 0.02,
                radius = 2f,
                color = 0xFFE040FB.toInt() // Neon Magenta
            )
        }
    }

    // Spawn body at target coordinate
    fun spawnBody(
        x: Double,
        y: Double,
        vx: Double,
        vy: Double,
        customMass: Double? = null,
        customName: String? = null,
        customColor: Int? = null,
        customRadius: Float? = null
    ) {
        val defaultProps = getDefaultSpawningProps(spawningType)
        val mass = customMass ?: defaultProps.mass
        val name = customName ?: (defaultProps.name + " " + UUID.randomUUID().toString().take(4))
        val color = customColor ?: defaultProps.color
        val radius = customRadius ?: defaultProps.radius

        val body = SpaceBody(
            name = name,
            x = x, y = y,
            vx = vx, vy = vy,
            mass = mass, radius = radius,
            color = color,
            bodyType = spawningType
        )
        simulator.addBody(body)
        _bodiesState.value = simulator.getBodies()
        updateStats()
    }

    fun deleteBody(id: String) {
        simulator.removeBody(id)
        if (selectedBodyId == id) {
            selectedBodyId = null
        }
        if (followedBodyId == id) {
            followedBodyId = null
            simulator.followedBodyId = null
        }
        _bodiesState.value = simulator.getBodies()
        updateStats()
    }

    fun selectBody(id: String?) {
        selectedBodyId = id
    }

    fun followBody(id: String?) {
        followedBodyId = id
        simulator.followedBodyId = id
        if (id == null) {
            // keep current panning where it is, just unlock
        } else {
            selectedBodyId = id
        }
    }

    fun updateSelectedBodyProperties(
        name: String,
        mass: Double,
        radius: Float,
        color: Int,
        isFixed: Boolean
    ) {
        selectedBodyId?.let { id ->
            simulator.updateBody(id) { body ->
                body.name = name
                body.mass = mass
                body.radius = radius
                body.color = color
                body.isFixed = isFixed
            }
            _bodiesState.value = simulator.getBodies()
            updateStats()
        }
    }

    // Room persistence integrations
    fun saveUniverse(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bodies = simulator.getBodies()
            val serialized = serializeBodies(bodies)
            val entity = UniverseEntity(
                name = name,
                description = description,
                serializedBodies = serialized,
                G = GVal,
                timeScale = timeScaleVal
            )
            repository.insert(entity)
        }
    }

    fun loadUniverse(entity: UniverseEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            selectedBodyId = null
            followedBodyId = null
            simulator.followedBodyId = null

            val bodies = deserializeBodies(entity.serializedBodies)
            simulator.setBodies(bodies)
            updateG(entity.G)
            updateTimeScale(entity.timeScale)

            _bodiesState.value = simulator.getBodies()
            updateStats()
            resetCamera()
        }
    }

    fun deleteSavedUniverse(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    // Serializer helper (compact, lightning-fast, highly robust CSV-like serialization)
    private fun serializeBodies(bodies: List<SpaceBody>): String {
        return bodies.joinToString("|") { b ->
            "${b.id};${b.name};${b.x};${b.y};${b.vx};${b.vy};${b.mass};${b.radius};${b.color};${b.bodyType.name};${b.isFixed}"
        }
    }

    private fun deserializeBodies(data: String): List<SpaceBody> {
        if (data.isBlank()) return emptyList()
        return data.split("|").mapNotNull { line ->
            try {
                val parts = line.split(";")
                if (parts.size < 11) return@mapNotNull null
                SpaceBody(
                    id = parts[0],
                    name = parts[1],
                    x = parts[2].toDouble(),
                    y = parts[3].toDouble(),
                    vx = parts[4].toDouble(),
                    vy = parts[5].toDouble(),
                    mass = parts[6].toDouble(),
                    radius = parts[7].toFloat(),
                    color = parts[8].toInt(),
                    bodyType = BodyType.valueOf(parts[9]),
                    isFixed = parts[10].toBoolean()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class SpawningProperties(
    val name: String,
    val mass: Double,
    val radius: Float,
    val color: Int
)
