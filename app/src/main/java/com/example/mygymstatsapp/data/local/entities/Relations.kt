package com.example.mygymstatsapp.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/** WorkoutExercise → Exercise + its sets. */
data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>,
)

/** Workout → WorkoutExercise → (Exercise, WorkoutSet). */
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val exercises: List<WorkoutExerciseWithSets>,
)

/** Program → sessions (TemplateWithExercises), unordered: sort by programPosition. */
data class ProgramWithSessions(
    @Embedded val program: ProgramEntity,
    @Relation(
        entity = WorkoutTemplateEntity::class,
        parentColumn = "id",
        entityColumn = "programId",
    )
    val sessions: List<TemplateWithExercises>,
)

data class TemplateExerciseWithExercise(
    @Embedded val templateExercise: TemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        entity = TemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "templateId",
    )
    val exercises: List<TemplateExerciseWithExercise>,
)
