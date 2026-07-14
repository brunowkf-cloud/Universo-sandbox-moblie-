package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_universes")
data class UniverseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val serializedBodies: String, // CSV/String-serialized representation
    val G: Double = 1.0,
    val timeScale: Double = 1.0
)
