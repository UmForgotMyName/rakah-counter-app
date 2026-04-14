package com.example.rakah.android.ui.guidance

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.rakah.android.R
import com.example.rakah.model.PrayerStep
import com.example.rakah.model.PrayerType
import com.example.rakah.model.spec

private data class GuidanceItem(
    val title: String,
    val step: PrayerStep?
)

@Composable
fun StepGuidanceStrip(
    previous: PrayerStep?,
    current: PrayerStep,
    next: PrayerStep?,
    prayer: PrayerType,
    currentRakah: Int,
    totalRakah: Int
) {
    val then = stepAfter(next, prayer, currentRakah, totalRakah)
    val items = listOf(
        GuidanceItem("Previous", previous),
        GuidanceItem("Current", current),
        GuidanceItem("Next", next),
        GuidanceItem("Then", then)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            StepCard(item)
        }
    }
}

@Composable
private fun StepCard(item: GuidanceItem) {
    Card {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            if (item.step != null) {
                Image(
                    painter = painterResource(id = item.step.drawableRes()),
                    contentDescription = item.step.pretty(),
                    modifier = Modifier.size(width = 140.dp, height = 90.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
                Text(item.step.pretty(), style = MaterialTheme.typography.labelSmall)
            } else {
                Row(modifier = Modifier.size(width = 140.dp, height = 90.dp)) {}
                Spacer(Modifier.height(4.dp))
                Text("-", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun PrayerStep.drawableRes(): Int = when (this) {
    PrayerStep.QIYAM -> R.drawable.step_qiyam
    PrayerStep.RUKU -> R.drawable.step_ruku
    PrayerStep.QAWMAH -> R.drawable.step_qawmah
    PrayerStep.SUJOOD -> R.drawable.step_sujood
    PrayerStep.JALSA_BETWEEN_SUJOOD -> R.drawable.step_jalsa_between_sujood
    PrayerStep.SECOND_SUJOOD -> R.drawable.step_second_sujood
    PrayerStep.TASHAHHUD -> R.drawable.step_tashahhud
    PrayerStep.SALAAM_RIGHT -> R.drawable.step_salaam_right
    PrayerStep.SALAAM_LEFT -> R.drawable.step_salaam_left
    PrayerStep.DONE -> R.drawable.step_done
}

private fun stepAfter(
    step: PrayerStep?,
    prayer: PrayerType,
    currentRakah: Int,
    totalRakah: Int
): PrayerStep? = when (step) {
    null -> null
    PrayerStep.QIYAM -> PrayerStep.RUKU
    PrayerStep.RUKU -> PrayerStep.QAWMAH
    PrayerStep.QAWMAH -> PrayerStep.SUJOOD
    PrayerStep.SUJOOD -> PrayerStep.JALSA_BETWEEN_SUJOOD
    PrayerStep.JALSA_BETWEEN_SUJOOD -> PrayerStep.SECOND_SUJOOD
    PrayerStep.SECOND_SUJOOD -> {
        if (currentRakah in prayer.spec().tashahhudAfterRakah) PrayerStep.TASHAHHUD else PrayerStep.QIYAM
    }
    PrayerStep.TASHAHHUD -> {
        if (currentRakah >= totalRakah) PrayerStep.SALAAM_RIGHT else PrayerStep.QIYAM
    }
    PrayerStep.SALAAM_RIGHT -> PrayerStep.SALAAM_LEFT
    PrayerStep.SALAAM_LEFT -> PrayerStep.DONE
    PrayerStep.DONE -> null
}

private fun PrayerStep.pretty(): String =
    name.lowercase().split("_").joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }

