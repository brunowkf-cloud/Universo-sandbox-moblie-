package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UniverseDao {
    @Query("SELECT * FROM saved_universes ORDER BY timestamp DESC")
    fun getAllUniverses(): Flow<List<UniverseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniverse(universe: UniverseEntity): Long

    @Query("DELETE FROM saved_universes WHERE id = :id")
    suspend fun deleteUniverseById(id: Int)
}
