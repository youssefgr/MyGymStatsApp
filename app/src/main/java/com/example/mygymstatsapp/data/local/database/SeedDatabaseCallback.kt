package com.example.mygymstatsapp.data.local.database

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Fills a freshly created database with the exercise library and the starter program.
 * Runs inside the database creation transaction, before any query can observe the tables.
 */
class SeedDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        SeedData.exercises.forEach { exercise ->
            db.insert(
                "exercises",
                SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to exercise.id,
                    "name" to exercise.name,
                    "muscleGroup" to exercise.muscleGroup.name,
                    "equipment" to exercise.equipment.name,
                    "trackingType" to exercise.trackingType.name,
                    "description" to exercise.description,
                    "isCustom" to 0,
                    "isFavorite" to 0,
                    "isArchived" to 0,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            )
        }

        db.insert(
            "programs",
            SQLiteDatabase.CONFLICT_IGNORE,
            contentValuesOf(
                "id" to SeedData.STARTER_PROGRAM_ID,
                "name" to SeedData.STARTER_PROGRAM_NAME,
                "notes" to SeedData.STARTER_PROGRAM_NOTES,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        )

        SeedData.templates.forEachIndexed { templateIndex, template ->
            // Offset timestamps so templates keep the program order (sorted by createdAt).
            val createdAt = now + templateIndex
            db.insert(
                "workout_templates",
                SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to template.id,
                    "name" to template.name,
                    "notes" to template.notes,
                    "createdAt" to createdAt,
                    "updatedAt" to createdAt,
                    "lastUsedAt" to null,
                    "programId" to SeedData.STARTER_PROGRAM_ID,
                    "programPosition" to templateIndex,
                ),
            )
            template.exercises.forEachIndexed { position, item ->
                db.insert(
                    "template_exercises",
                    SQLiteDatabase.CONFLICT_IGNORE,
                    contentValuesOf(
                        "id" to "${template.id}_$position",
                        "templateId" to template.id,
                        "exerciseId" to item.exerciseId,
                        "position" to position,
                        "targetSets" to item.sets,
                        "targetRepsMin" to item.repsMin,
                        "targetRepsMax" to item.repsMax,
                        "restSeconds" to item.restSeconds,
                        "notes" to item.notes,
                        "supersetGroup" to item.supersetGroup,
                    ),
                )
            }
        }
    }
}
