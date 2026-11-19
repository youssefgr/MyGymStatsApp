package com.example.mygymstatsapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mygymstatsapp.data.local.dao.ExerciseDao
import com.example.mygymstatsapp.data.local.dao.ProgramDao
import com.example.mygymstatsapp.data.local.dao.StatsDao
import com.example.mygymstatsapp.data.local.dao.TemplateDao
import com.example.mygymstatsapp.data.local.dao.WorkoutDao
import com.example.mygymstatsapp.data.local.entities.ExerciseEntity
import com.example.mygymstatsapp.data.local.entities.ProgramEntity
import com.example.mygymstatsapp.data.local.entities.TemplateExerciseEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutExerciseEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutSetEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutTemplateEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgramEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val DATABASE_NAME = "gym.db"

        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedDatabaseCallback())
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
