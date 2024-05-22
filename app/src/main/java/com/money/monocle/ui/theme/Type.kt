package com.money.monocle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.money.monocle.R

val loraFamily = FontFamily(Font(R.font.lora_font_wght))
val AppTypography = Typography(
    labelMedium = TextStyle(
        fontFamily = loraFamily,
        fontSize = 20.sp
    )
)
