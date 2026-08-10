package com.xianming.watch4sat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransmitterDao {

    @Query("SELECT * FROM transmitters ORDER BY description COLLATE NOCASE ASC")
    abstract fun observeAllTransmitters(): Flow<List<TransmitterEntity>>

    @Query("SELECT * FROM transmitters ORDER BY description COLLATE NOCASE ASC")
    abstract suspend fun getAllTransmitters(): List<TransmitterEntity>

    @Query("SELECT * FROM transmitters WHERE catalogNumber = :catalogNumber ORDER BY description COLLATE NOCASE ASC")
    abstract fun observeTransmittersForSatellite(catalogNumber: Int): Flow<List<TransmitterEntity>>

    @Query("SELECT * FROM transmitters WHERE catalogNumber = :catalogNumber ORDER BY description COLLATE NOCASE ASC")
    abstract suspend fun getTransmittersForSatellite(catalogNumber: Int): List<TransmitterEntity>

    @Query("SELECT COUNT(*) FROM transmitters")
    abstract suspend fun transmitterCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransmitters(entities: List<TransmitterEntity>)

    @Query("DELETE FROM transmitters")
    abstract suspend fun clearTransmitters()

    @Transaction
    open suspend fun replaceAllTransmitters(entities: List<TransmitterEntity>) {
        clearTransmitters()
        if (entities.isNotEmpty()) {
            insertTransmitters(entities)
        }
    }
}
