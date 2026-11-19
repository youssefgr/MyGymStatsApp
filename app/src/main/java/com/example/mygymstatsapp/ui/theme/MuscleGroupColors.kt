package com.example.mygymstatsapp.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.SetType

/** Slightly muted hues so they sit on the warm cast-iron surfaces without fighting the orange brand color. */
val MuscleGroup.color: Color
    get() = when (this) {
        MuscleGroup.CHEST -> Color(0xFFF2766B)
        MuscleGroup.BACK -> Color(0xFF5FA8D3)
        MuscleGroup.SHOULDERS -> Color(0xFFF2A65A)
        MuscleGroup.BICEPS -> Color(0xFFA68BEB)
        MuscleGroup.TRICEPS -> Color(0xFFD98AD6)
        MuscleGroup.LEGS -> Color(0xFF7DC87A)
        MuscleGroup.GLUTES -> Color(0xFFEF8FB2)
        MuscleGroup.CALVES -> Color(0xFF4FC4A8)
        MuscleGroup.ABS -> Color(0xFFE9C46A)
        MuscleGroup.FOREARMS -> Color(0xFFB5CC6A)
        MuscleGroup.CARDIO -> Color(0xFF6CCFE0)
    }

val SetType.color: Color
    get() = when (this) {
        SetType.WARMUP -> Steel
        SetType.NORMAL -> Color.Unspecified
        SetType.DROP -> Plum
        SetType.FAILURE -> Danger
    }
