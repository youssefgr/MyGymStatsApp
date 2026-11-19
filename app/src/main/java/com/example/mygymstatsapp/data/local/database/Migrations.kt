package com.example.mygymstatsapp.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: supersets. Also links the two supersets of the starter program "Pecs volume + bras". */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN supersetGroup INTEGER")
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN supersetGroup INTEGER")
        // Seeded rows keep stable ids ("<template>_<position>") until the user edits the program.
        db.execSQL("UPDATE template_exercises SET supersetGroup = 1 WHERE id IN ('tpl_program_4_3', 'tpl_program_4_4')")
        db.execSQL("UPDATE template_exercises SET supersetGroup = 2 WHERE id IN ('tpl_program_4_5', 'tpl_program_4_6')")
    }
}

/** v2 → v3: per-exercise progressive overload (on by default, profile load step). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN progressionEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE exercises ADD COLUMN progressionIncrementKg REAL")
    }
}

/**
 * v3 → v4: programs (groups of sessions). The four seeded sessions, if still there,
 * are grouped into the starter program they come from.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `programs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("ALTER TABLE workout_templates ADD COLUMN programId TEXT")
        db.execSQL("ALTER TABLE workout_templates ADD COLUMN programPosition INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_templates_programId` ON `workout_templates` (`programId`)")

        val starterSessions = SeedData.templates.map { it.id }
        val placeholders = starterSessions.joinToString { "?" }
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT INTO programs (id, name, notes, createdAt, updatedAt) SELECT ?, ?, ?, ?, ? " +
                "WHERE EXISTS (SELECT 1 FROM workout_templates WHERE id IN ($placeholders))",
            arrayOf<Any>(
                SeedData.STARTER_PROGRAM_ID, SeedData.STARTER_PROGRAM_NAME, SeedData.STARTER_PROGRAM_NOTES, now, now,
                *starterSessions.toTypedArray(),
            ),
        )
        starterSessions.forEachIndexed { position, id ->
            db.execSQL(
                "UPDATE workout_templates SET programId = ?, programPosition = ? WHERE id = ?",
                arrayOf<Any>(SeedData.STARTER_PROGRAM_ID, position, id),
            )
        }
    }
}

/** v4 → v5: average and max heart rate of a workout, measured by the watch. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workouts ADD COLUMN avgHeartRate INTEGER")
        db.execSQL("ALTER TABLE workouts ADD COLUMN maxHeartRate INTEGER")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
