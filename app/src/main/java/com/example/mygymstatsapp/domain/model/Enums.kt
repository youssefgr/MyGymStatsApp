package com.example.mygymstatsapp.domain.model

/* Display names live in the UI layer (ui/Labels.kt + strings.xml), not in the domain. */

enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, LEGS, GLUTES, CALVES, ABS, FOREARMS, CARDIO }

enum class Equipment {
    BARBELL, DUMBBELL, EZ_BAR, CABLE, MACHINE, SMITH_MACHINE, KETTLEBELL, BODYWEIGHT, BAND, CARDIO_MACHINE, OTHER,
}

/** What a set of this exercise records. */
enum class TrackingType { WEIGHT_REPS, REPS_ONLY, DURATION }

enum class SetType { WARMUP, NORMAL, DROP, FAILURE }

enum class WorkoutStatus { IN_PROGRESS, COMPLETED }

/** [label] is the unit symbol, identical in every language. */
enum class WeightUnit(val label: String) { KG("kg"), LB("lb") }

enum class Goal { MUSCLE_GAIN, STRENGTH, FAT_LOSS, GENERAL_FITNESS }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class RecordType { MAX_WEIGHT, ONE_RM, MAX_REPS, VOLUME }
