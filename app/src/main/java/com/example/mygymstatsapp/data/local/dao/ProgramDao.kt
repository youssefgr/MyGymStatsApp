package com.example.mygymstatsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.mygymstatsapp.data.local.entities.ProgramEntity
import com.example.mygymstatsapp.data.local.entities.ProgramWithSessions
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Transaction
    @Query("SELECT * FROM programs ORDER BY createdAt")
    fun observePrograms(): Flow<List<ProgramWithSessions>>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id")
    fun observeProgram(id: String): Flow<ProgramWithSessions?>

    @Insert
    suspend fun insert(program: ProgramEntity)

    @Query("UPDATE programs SET name = :name, notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun update(id: String, name: String, notes: String, now: Long)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun delete(id: String)
}
