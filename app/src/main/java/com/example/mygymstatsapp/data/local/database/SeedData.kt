package com.example.mygymstatsapp.data.local.database

import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.Equipment.BARBELL
import com.example.mygymstatsapp.domain.model.Equipment.BODYWEIGHT
import com.example.mygymstatsapp.domain.model.Equipment.CABLE
import com.example.mygymstatsapp.domain.model.Equipment.CARDIO_MACHINE
import com.example.mygymstatsapp.domain.model.Equipment.DUMBBELL
import com.example.mygymstatsapp.domain.model.Equipment.EZ_BAR
import com.example.mygymstatsapp.domain.model.Equipment.KETTLEBELL
import com.example.mygymstatsapp.domain.model.Equipment.MACHINE
import com.example.mygymstatsapp.domain.model.Equipment.OTHER
import com.example.mygymstatsapp.domain.model.Equipment.SMITH_MACHINE
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.MuscleGroup.ABS
import com.example.mygymstatsapp.domain.model.MuscleGroup.BACK
import com.example.mygymstatsapp.domain.model.MuscleGroup.BICEPS
import com.example.mygymstatsapp.domain.model.MuscleGroup.CALVES
import com.example.mygymstatsapp.domain.model.MuscleGroup.CARDIO
import com.example.mygymstatsapp.domain.model.MuscleGroup.CHEST
import com.example.mygymstatsapp.domain.model.MuscleGroup.FOREARMS
import com.example.mygymstatsapp.domain.model.MuscleGroup.GLUTES
import com.example.mygymstatsapp.domain.model.MuscleGroup.LEGS
import com.example.mygymstatsapp.domain.model.MuscleGroup.SHOULDERS
import com.example.mygymstatsapp.domain.model.MuscleGroup.TRICEPS
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.TrackingType.DURATION
import com.example.mygymstatsapp.domain.model.TrackingType.REPS_ONLY
import com.example.mygymstatsapp.domain.model.TrackingType.WEIGHT_REPS

data class SeedExercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val description: String,
    val trackingType: TrackingType = WEIGHT_REPS,
)

data class SeedTemplateExercise(
    val exerciseId: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val notes: String,
    val supersetGroup: Int? = null,
)

data class SeedTemplate(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<SeedTemplateExercise>,
)

/**
 * Data inserted when the database is created. Ids are stable so that future versions
 * can add exercises with INSERT OR IGNORE without duplicating them.
 */
object SeedData {

    val exercises: List<SeedExercise> = listOf(
        // Chest
        SeedExercise("ex_bench_press", "Bench Press", CHEST, BARBELL, "Allongé sur banc plat, descendre la barre au milieu des pectoraux puis pousser."),
        SeedExercise("ex_incline_bench_press", "Incline Bench Press", CHEST, BARBELL, "Banc incliné à 30-45°, cible le haut des pectoraux."),
        SeedExercise("ex_decline_bench_press", "Decline Bench Press", CHEST, BARBELL, "Banc décliné, cible le bas des pectoraux."),
        SeedExercise("ex_smith_incline_press", "Smith Machine Incline Press", CHEST, SMITH_MACHINE, "Développé incliné guidé, trajectoire stable."),
        SeedExercise("ex_dumbbell_bench_press", "Dumbbell Bench Press", CHEST, DUMBBELL, "Développé couché avec haltères, plus grande amplitude."),
        SeedExercise("ex_incline_dumbbell_press", "Incline Dumbbell Press", CHEST, DUMBBELL, "Développé incliné avec haltères, haut des pectoraux."),
        SeedExercise("ex_machine_chest_press", "Machine Chest Press", CHEST, MACHINE, "Développé assis à la machine."),
        SeedExercise("ex_chest_fly", "Chest Fly", CHEST, DUMBBELL, "Écarté couché avec haltères, bras légèrement fléchis."),
        SeedExercise("ex_pec_deck", "Pec Deck", CHEST, MACHINE, "Écarté à la machine, contraction en fin de mouvement."),
        SeedExercise("ex_cable_crossover", "Cable Crossover", CHEST, CABLE, "Vis-à-vis à la poulie haute, mains qui se croisent devant le buste."),
        SeedExercise("ex_low_to_high_cable_fly", "Low-to-High Cable Fly", CHEST, CABLE, "Écarté poulie basse vers le haut, cible le haut des pectoraux."),
        SeedExercise("ex_push_ups", "Push Ups", CHEST, BODYWEIGHT, "Pompes, corps gainé, poitrine près du sol.", REPS_ONLY),
        SeedExercise("ex_dips", "Dips", CHEST, BODYWEIGHT, "Dips buste penché en avant. Charge = lest ajouté (0 au poids du corps)."),

        // Back
        SeedExercise("ex_pull_ups", "Pull Ups", BACK, BODYWEIGHT, "Tractions prise pronation. Charge = lest ajouté (0 au poids du corps)."),
        SeedExercise("ex_chin_ups", "Chin Ups", BACK, BODYWEIGHT, "Tractions prise supination. Charge = lest ajouté."),
        SeedExercise("ex_lat_pulldown", "Lat Pulldown", BACK, CABLE, "Tirage vertical à la poulie haute vers le haut de la poitrine."),
        SeedExercise("ex_close_grip_pulldown", "Close-Grip Lat Pulldown", BACK, CABLE, "Tirage vertical prise serrée neutre."),
        SeedExercise("ex_barbell_row", "Barbell Row", BACK, BARBELL, "Rowing buste penché, tirer la barre vers le nombril."),
        SeedExercise("ex_dumbbell_row", "Dumbbell Row", BACK, DUMBBELL, "Rowing unilatéral appuyé sur un banc."),
        SeedExercise("ex_chest_supported_row", "Chest-Supported Row", BACK, MACHINE, "Rowing buste appuyé, élimine l'élan."),
        SeedExercise("ex_t_bar_row", "T-Bar Row", BACK, BARBELL, "Rowing à la barre T, épaisseur du dos."),
        SeedExercise("ex_seated_cable_row", "Seated Cable Row", BACK, CABLE, "Tirage horizontal à la poulie basse, omoplates serrées."),
        SeedExercise("ex_straight_arm_pulldown", "Straight-Arm Pulldown", BACK, CABLE, "Pull-over à la poulie haute, bras tendus."),
        SeedExercise("ex_deadlift", "Deadlift", BACK, BARBELL, "Soulevé de terre, dos neutre, pousser le sol avec les jambes."),
        SeedExercise("ex_back_extension", "Back Extension", BACK, BODYWEIGHT, "Extensions lombaires au banc à 45°. Charge = disque tenu."),
        SeedExercise("ex_dumbbell_shrug", "Dumbbell Shrug", BACK, DUMBBELL, "Haussements d'épaules pour les trapèzes."),

        // Shoulders
        SeedExercise("ex_overhead_press", "Overhead Press", SHOULDERS, BARBELL, "Développé militaire debout, barre au-dessus de la tête."),
        SeedExercise("ex_dumbbell_shoulder_press", "Dumbbell Shoulder Press", SHOULDERS, DUMBBELL, "Développé épaules assis avec haltères."),
        SeedExercise("ex_machine_shoulder_press", "Machine Shoulder Press", SHOULDERS, MACHINE, "Développé épaules à la machine."),
        SeedExercise("ex_arnold_press", "Arnold Press", SHOULDERS, DUMBBELL, "Développé avec rotation des poignets."),
        SeedExercise("ex_lateral_raise", "Lateral Raise", SHOULDERS, DUMBBELL, "Élévations latérales, coudes légèrement fléchis."),
        SeedExercise("ex_cable_lateral_raise", "Cable Lateral Raise", SHOULDERS, CABLE, "Élévation latérale unilatérale à la poulie basse."),
        SeedExercise("ex_front_raise", "Front Raise", SHOULDERS, DUMBBELL, "Élévations frontales jusqu'à hauteur des yeux."),
        SeedExercise("ex_rear_delt_fly", "Rear Delt Fly", SHOULDERS, DUMBBELL, "Oiseau buste penché pour l'arrière d'épaule."),
        SeedExercise("ex_face_pull", "Face Pull", SHOULDERS, CABLE, "Tirage corde vers le visage, coudes hauts."),
        SeedExercise("ex_upright_row", "Upright Row", SHOULDERS, BARBELL, "Tirage menton prise large."),

        // Biceps
        SeedExercise("ex_barbell_curl", "Barbell Curl", BICEPS, BARBELL, "Curl debout à la barre droite."),
        SeedExercise("ex_ez_bar_curl", "EZ Bar Curl", BICEPS, EZ_BAR, "Curl à la barre EZ, plus confortable pour les poignets."),
        SeedExercise("ex_dumbbell_curl", "Dumbbell Curl", BICEPS, DUMBBELL, "Curl alterné ou simultané avec haltères."),
        SeedExercise("ex_incline_dumbbell_curl", "Incline Dumbbell Curl", BICEPS, DUMBBELL, "Curl allongé sur banc incliné, étirement du biceps."),
        SeedExercise("ex_hammer_curl", "Hammer Curl", BICEPS, DUMBBELL, "Curl prise marteau, brachial et avant-bras."),
        SeedExercise("ex_preacher_curl", "Preacher Curl", BICEPS, EZ_BAR, "Curl au pupitre, sans élan."),
        SeedExercise("ex_cable_curl", "Cable Curl", BICEPS, CABLE, "Curl à la poulie basse, tension continue."),
        SeedExercise("ex_concentration_curl", "Concentration Curl", BICEPS, DUMBBELL, "Curl assis, coude contre la cuisse."),

        // Triceps
        SeedExercise("ex_triceps_pushdown", "Triceps Pushdown", TRICEPS, CABLE, "Extension à la poulie haute avec barre."),
        SeedExercise("ex_rope_pushdown", "Rope Pushdown", TRICEPS, CABLE, "Extension à la poulie haute avec corde, écarter en bas."),
        SeedExercise("ex_overhead_cable_extension", "Overhead Cable Triceps Extension", TRICEPS, CABLE, "Extension au-dessus de la tête à la poulie, longue portion."),
        SeedExercise("ex_dumbbell_overhead_extension", "Dumbbell Overhead Extension", TRICEPS, DUMBBELL, "Extension nuque avec un haltère."),
        SeedExercise("ex_skull_crusher", "Skull Crusher", TRICEPS, EZ_BAR, "Barre au front allongé sur banc."),
        SeedExercise("ex_close_grip_bench_press", "Close-Grip Bench Press", TRICEPS, BARBELL, "Développé couché prise serrée."),
        SeedExercise("ex_bench_dips", "Bench Dips", TRICEPS, BODYWEIGHT, "Dips entre deux bancs.", REPS_ONLY),

        // Legs
        SeedExercise("ex_squat", "Squat", LEGS, BARBELL, "Squat barre sur le haut du dos, descendre sous la parallèle."),
        SeedExercise("ex_front_squat", "Front Squat", LEGS, BARBELL, "Squat barre devant, buste droit."),
        SeedExercise("ex_hack_squat", "Hack Squat", LEGS, MACHINE, "Squat guidé à la machine, quadriceps."),
        SeedExercise("ex_leg_press", "Leg Press", LEGS, MACHINE, "Presse à cuisses, ne pas décoller le bassin."),
        SeedExercise("ex_leg_extension", "Leg Extension", LEGS, MACHINE, "Extension des jambes à la machine, quadriceps."),
        SeedExercise("ex_leg_curl", "Leg Curl", LEGS, MACHINE, "Flexion des jambes à la machine, ischio-jambiers."),
        SeedExercise("ex_romanian_deadlift", "Romanian Deadlift", LEGS, BARBELL, "Soulevé de terre jambes semi-tendues, ischio-jambiers et fessiers."),
        SeedExercise("ex_lunges", "Lunges", LEGS, DUMBBELL, "Fentes avant ou marchées avec haltères."),
        SeedExercise("ex_bulgarian_split_squat", "Bulgarian Split Squat", LEGS, DUMBBELL, "Fente pied arrière surélevé."),
        SeedExercise("ex_goblet_squat", "Goblet Squat", LEGS, KETTLEBELL, "Squat avec charge tenue contre la poitrine."),

        // Glutes
        SeedExercise("ex_hip_thrust", "Hip Thrust", GLUTES, BARBELL, "Extension de hanche, dos appuyé sur un banc."),
        SeedExercise("ex_glute_bridge", "Glute Bridge", GLUTES, BARBELL, "Pont fessier au sol."),
        SeedExercise("ex_cable_kickback", "Cable Kickback", GLUTES, CABLE, "Extension de hanche unilatérale à la poulie basse."),
        SeedExercise("ex_hip_abduction", "Hip Abduction", GLUTES, MACHINE, "Abduction à la machine, moyen fessier."),

        // Calves
        SeedExercise("ex_calf_raise", "Calf Raise", CALVES, MACHINE, "Extensions mollets debout, pause en étirement."),
        SeedExercise("ex_seated_calf_raise", "Seated Calf Raise", CALVES, MACHINE, "Mollets assis, cible le soléaire."),
        SeedExercise("ex_leg_press_calf_raise", "Leg Press Calf Raise", CALVES, MACHINE, "Mollets à la presse à cuisses."),

        // Abs
        SeedExercise("ex_crunch", "Crunch", ABS, BODYWEIGHT, "Enroulement du buste au sol.", REPS_ONLY),
        SeedExercise("ex_cable_crunch", "Cable Crunch", ABS, CABLE, "Crunch à genoux à la poulie haute."),
        SeedExercise("ex_hanging_leg_raise", "Hanging Leg Raise", ABS, BODYWEIGHT, "Relevé de jambes suspendu à la barre.", REPS_ONLY),
        SeedExercise("ex_ab_wheel", "Ab Wheel Rollout", ABS, OTHER, "Roulette abdominale, gainage dynamique.", REPS_ONLY),
        SeedExercise("ex_russian_twist", "Russian Twist", ABS, BODYWEIGHT, "Rotations du buste assis, pieds décollés.", REPS_ONLY),
        SeedExercise("ex_plank", "Plank", ABS, BODYWEIGHT, "Gainage ventral sur les avant-bras.", DURATION),

        // Forearms
        SeedExercise("ex_wrist_curl", "Wrist Curl", FOREARMS, BARBELL, "Flexion des poignets, avant-bras sur un banc."),
        SeedExercise("ex_reverse_wrist_curl", "Reverse Wrist Curl", FOREARMS, DUMBBELL, "Extension des poignets prise pronation."),
        SeedExercise("ex_reverse_curl", "Reverse Curl", FOREARMS, EZ_BAR, "Curl prise pronation."),

        // Cardio
        SeedExercise("ex_treadmill", "Treadmill", CARDIO, CARDIO_MACHINE, "Course ou marche inclinée sur tapis.", DURATION),
        SeedExercise("ex_stationary_bike", "Stationary Bike", CARDIO, CARDIO_MACHINE, "Vélo d'appartement.", DURATION),
        SeedExercise("ex_rowing_machine", "Rowing Machine", CARDIO, CARDIO_MACHINE, "Rameur, jambes puis dos puis bras.", DURATION),
        SeedExercise("ex_elliptical", "Elliptical", CARDIO, CARDIO_MACHINE, "Vélo elliptique.", DURATION),
        SeedExercise("ex_stair_climber", "Stair Climber", CARDIO, CARDIO_MACHINE, "Simulateur d'escaliers.", DURATION),
        SeedExercise("ex_jump_rope", "Jump Rope", CARDIO, OTHER, "Corde à sauter.", DURATION),
    )

    private const val RR = "1-2 reps en réserve"
    private const val FAIL_LAST = "1-2 reps en réserve, échec autorisé sur la dernière série"
    private const val NO_FAIL = "2 reps en réserve, jamais d'échec"

    /** Program grouping the four seeded sessions (also created for existing installs by MIGRATION_3_4). */
    const val STARTER_PROGRAM_ID = "prg_starter"
    const val STARTER_PROGRAM_NAME = "Programme 4 jours"
    const val STARTER_PROGRAM_NOTES =
        "Lundi, mardi, jeudi, vendredi ou samedi. 3 séries d'échauffement sur le premier exercice, " +
            "1 série légère sur les suivants. +2,5 kg quand toutes les séries atteignent le haut de la fourchette."

    /** The 4 sessions of the program from Programme_musculation.xlsx, in day order. */
    val templates: List<SeedTemplate> = listOf(
        SeedTemplate(
            id = "tpl_program_1",
            name = "Pecs lourd + triceps",
            notes = "Lundi · 3 séries d'échauffement sur le premier exercice",
            exercises = listOf(
                SeedTemplateExercise("ex_bench_press", 4, 5, 8, 150, RR),
                SeedTemplateExercise("ex_incline_dumbbell_press", 3, 8, 12, 120, RR),
                SeedTemplateExercise("ex_pec_deck", 3, 12, 15, 90, "Écarté poulie ou pec deck · $FAIL_LAST"),
                SeedTemplateExercise("ex_lateral_raise", 4, 12, 20, 60, FAIL_LAST),
                SeedTemplateExercise("ex_overhead_cable_extension", 3, 10, 12, 90, FAIL_LAST),
                SeedTemplateExercise("ex_rope_pushdown", 3, 12, 15, 60, FAIL_LAST),
            ),
        ),
        SeedTemplate(
            id = "tpl_program_2",
            name = "Dos + biceps",
            notes = "Mardi · 3 séries d'échauffement sur le premier exercice",
            exercises = listOf(
                SeedTemplateExercise("ex_pull_ups", 4, 6, 10, 150, "Ou tirage vertical · $RR"),
                SeedTemplateExercise("ex_chest_supported_row", 4, 8, 12, 120, "Buste appuyé ou barre · $RR"),
                SeedTemplateExercise("ex_seated_cable_row", 3, 10, 12, 90, FAIL_LAST),
                SeedTemplateExercise("ex_face_pull", 3, 15, 20, 60, RR),
                SeedTemplateExercise("ex_ez_bar_curl", 3, 8, 12, 90, FAIL_LAST),
                SeedTemplateExercise("ex_incline_dumbbell_curl", 3, 10, 12, 90, FAIL_LAST),
                SeedTemplateExercise("ex_hammer_curl", 2, 10, 12, 60, FAIL_LAST),
            ),
        ),
        SeedTemplate(
            id = "tpl_program_3",
            name = "Jambes",
            notes = "Jeudi · 3 séries d'échauffement sur le premier exercice",
            exercises = listOf(
                SeedTemplateExercise("ex_squat", 4, 6, 10, 180, "Ou hack squat · $NO_FAIL"),
                SeedTemplateExercise("ex_romanian_deadlift", 3, 8, 10, 150, NO_FAIL),
                SeedTemplateExercise("ex_leg_press", 3, 10, 15, 120, RR),
                SeedTemplateExercise("ex_leg_curl", 3, 10, 15, 90, FAIL_LAST),
                SeedTemplateExercise("ex_calf_raise", 4, 10, 15, 60, FAIL_LAST),
                SeedTemplateExercise("ex_hanging_leg_raise", 3, 10, 15, 60, "Ou crunch poulie · $RR"),
            ),
        ),
        SeedTemplate(
            id = "tpl_program_4",
            name = "Pecs volume + bras",
            notes = "Vendredi ou samedi · 3 séries d'échauffement sur le premier exercice",
            exercises = listOf(
                SeedTemplateExercise("ex_incline_bench_press", 4, 8, 10, 120, "Barre ou Smith · $RR"),
                SeedTemplateExercise("ex_dips", 3, 8, 12, 120, "Lestés, ou développé machine · $RR"),
                SeedTemplateExercise("ex_low_to_high_cable_fly", 3, 12, 15, 90, FAIL_LAST),
                SeedTemplateExercise("ex_preacher_curl", 3, 10, 12, 90, FAIL_LAST, supersetGroup = 1),
                SeedTemplateExercise("ex_overhead_cable_extension", 3, 10, 12, 90, FAIL_LAST, supersetGroup = 1),
                SeedTemplateExercise("ex_cable_curl", 3, 12, 15, 90, FAIL_LAST, supersetGroup = 2),
                SeedTemplateExercise("ex_triceps_pushdown", 3, 12, 15, 90, FAIL_LAST, supersetGroup = 2),
                SeedTemplateExercise("ex_lateral_raise", 3, 15, 20, 60, FAIL_LAST),
            ),
        ),
    )
}
