package com.example.data

import kotlinx.coroutines.flow.Flow

class UniverseRepository(private val universeDao: UniverseDao) {
    val allUniverses: Flow<List<UniverseEntity>> = universeDao.getAllUniverses()

    suspend fun insert(universe: UniverseEntity): Long {
        return universeDao.insertUniverse(universe)
    }

    suspend fun deleteById(id: Int) {
        universeDao.deleteUniverseById(id)
    }
}
