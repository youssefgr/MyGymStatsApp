package com.example.mygymstatsapp.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.RecordType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeight
import com.example.mygymstatsapp.utils.fromKg

/** "67,5 kg × 8 (+5 kg)", "1RM ≈ 86,7 kg (+2,5 kg)", "15 reps (+2)", "Volume 2 450 kg (+150 kg)". */
@Composable
fun recordDescription(record: NewPersonalRecord, unit: WeightUnit): String {
    val delta = record.value - record.previousValue
    val weightDelta = "${formatDecimal(unit.fromKg(delta), 1)} ${unit.label}"
    return when (record.type) {
        RecordType.MAX_WEIGHT ->
            stringResource(R.string.record_desc_weight, formatWeight(record.value, unit), record.reps ?: 0, weightDelta)
        RecordType.ONE_RM -> if (record.weightKg != null && record.reps != null) {
            stringResource(R.string.record_desc_one_rm_set, formatWeight(record.weightKg, unit), record.reps, formatWeight(record.value, unit), weightDelta)
        } else {
            stringResource(R.string.record_desc_one_rm, formatWeight(record.value, unit), weightDelta)
        }
        RecordType.MAX_REPS -> stringResource(R.string.record_desc_reps, record.value.toInt(), delta.toInt())
        RecordType.VOLUME -> stringResource(R.string.record_desc_volume, formatVolume(record.value, unit), formatVolume(delta, unit))
    }
}
