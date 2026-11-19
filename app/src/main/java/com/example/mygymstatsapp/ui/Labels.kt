package com.example.mygymstatsapp.ui

import android.content.Context
import androidx.annotation.StringRes
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.GeneratedSession
import com.example.mygymstatsapp.domain.model.GeneratorOptions
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.ProgramGenerator
import com.example.mygymstatsapp.domain.model.RecordType
import com.example.mygymstatsapp.domain.model.SessionKind
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.ThemeMode
import com.example.mygymstatsapp.domain.model.TrackingType

/* Display names of domain enums. The domain stays free of UI text; translations live in strings.xml. */

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.LEGS -> R.string.muscle_legs
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
        MuscleGroup.ABS -> R.string.muscle_abs
        MuscleGroup.FOREARMS -> R.string.muscle_forearms
        MuscleGroup.CARDIO -> R.string.muscle_cardio
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.EZ_BAR -> R.string.equipment_ez_bar
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.BAND -> R.string.equipment_band
        Equipment.CARDIO_MACHINE -> R.string.equipment_cardio_machine
        Equipment.OTHER -> R.string.equipment_other
    }

@get:StringRes
val TrackingType.labelRes: Int
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
        TrackingType.REPS_ONLY -> R.string.tracking_reps_only
        TrackingType.DURATION -> R.string.tracking_duration
    }

@get:StringRes
val SetType.labelRes: Int
    get() = when (this) {
        SetType.WARMUP -> R.string.set_type_warmup
        SetType.NORMAL -> R.string.set_type_normal
        SetType.DROP -> R.string.set_type_drop
        SetType.FAILURE -> R.string.set_type_failure
    }

/** One-letter badge shown instead of the set number ("" for normal sets). */
val SetType.badge: String
    get() = when (this) {
        SetType.WARMUP -> "W"
        SetType.NORMAL -> ""
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
    }

@get:StringRes
val Goal.labelRes: Int
    get() = when (this) {
        Goal.MUSCLE_GAIN -> R.string.goal_muscle_gain
        Goal.STRENGTH -> R.string.goal_strength
        Goal.FAT_LOSS -> R.string.goal_fat_loss
        Goal.GENERAL_FITNESS -> R.string.goal_general_fitness
    }

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.SYSTEM -> R.string.theme_system
    }

@get:StringRes
val RecordType.labelRes: Int
    get() = when (this) {
        RecordType.MAX_WEIGHT -> R.string.record_max_weight
        RecordType.ONE_RM -> R.string.record_one_rm
        RecordType.MAX_REPS -> R.string.record_max_reps
        RecordType.VOLUME -> R.string.record_volume
    }

@get:StringRes
val SessionKind.labelRes: Int
    get() = when (this) {
        SessionKind.FULL_BODY -> R.string.session_kind_full_body
        SessionKind.UPPER -> R.string.session_kind_upper
        SessionKind.LOWER -> R.string.session_kind_lower
        SessionKind.PUSH -> R.string.session_kind_push
        SessionKind.PULL -> R.string.session_kind_pull
        SessionKind.LEGS -> R.string.session_kind_legs
        // Generated focus sessions are named after their muscle groups (LabelResolver.sessionName).
        SessionKind.FOCUS -> R.string.session_kind_focus
    }

/** Resolves labels outside composition (search in ViewModels, default and generated names). */
class LabelResolver(private val context: Context) {
    fun muscleGroup(group: MuscleGroup): String = context.getString(group.labelRes)
    fun equipment(equipment: Equipment): String = context.getString(equipment.labelRes)

    /** "Push", "Haut du corps B", "Biceps + Triceps". */
    fun sessionName(session: GeneratedSession): String {
        val base = if (session.kind == SessionKind.FOCUS) {
            session.groups.joinToString(" + ") { muscleGroup(it) }
        } else {
            context.getString(session.kind.labelRes)
        }
        return if (session.repeated) {
            context.getString(R.string.generated_session_variant, base, ('A' + session.variant).toString())
        } else {
            base
        }
    }

    /** Muscle groups trained: "Pectoraux, épaules, triceps". */
    fun sessionNotes(session: GeneratedSession): String = sentence(session.groups)

    /** "Push Pull Legs · priorité pectoraux + bras", "Spécialisation Biceps + Triceps". */
    fun generatedProgramName(options: GeneratorOptions): String {
        val split = context.resources.getStringArray(R.array.generated_split_names)[
            options.sessionsPerWeek.coerceIn(ProgramGenerator.MIN_SESSIONS, ProgramGenerator.MAX_SESSIONS) - 1
        ]
        val focus = options.focus.sortedBy { it.ordinal }.joinToString(" + ") { muscleGroup(it) }
        return when {
            options.focus.isEmpty() -> split
            options.focusOnly -> context.getString(R.string.generated_program_specialization, focus)
            else -> context.getString(R.string.generated_program_focus, split, focus.lowercase())
        }
    }

    /** "Prise de muscle · 4 séances par semaine. Priorité : pectoraux, biceps." */
    fun generatedProgramNotes(options: GeneratorOptions): String {
        val base = context.resources.getQuantityString(
            R.plurals.generated_program_notes,
            options.sessionsPerWeek,
            context.getString(options.goal.labelRes),
            options.sessionsPerWeek,
        )
        if (options.focus.isEmpty()) return base
        return base + " " + context.getString(R.string.generated_program_priority, sentence(options.focus.sortedBy { it.ordinal }).lowercase())
    }

    private fun sentence(groups: List<MuscleGroup>): String =
        groups.joinToString(", ") { muscleGroup(it) }.lowercase().replaceFirstChar { it.titlecase() }

    fun defaultWorkoutName(hour: Int): String = context.getString(
        when (hour) {
            in 5..11 -> R.string.workout_name_morning
            in 12..17 -> R.string.workout_name_afternoon
            else -> R.string.workout_name_evening
        }
    )
}
