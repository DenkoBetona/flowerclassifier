package com.denko.flowerclassifier

import androidx.compose.ui.graphics.Color
import com.denko.flowerclassifier.ui.theme.*

fun colorForClass(bgName: String): Color = when (bgName) {
    "маргаритка" -> DaisyColor
    "глухарче" -> DandelionColor
    "роза" -> RoseColor
    "слънчоглед" -> SunflowerColor
    "лале" -> TulipColor
    else -> UnknownColor
}