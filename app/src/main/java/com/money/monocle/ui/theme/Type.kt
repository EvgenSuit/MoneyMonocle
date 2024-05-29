package com.money.monocle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.money.monocle.R

val montserratExtraBold = FontFamily(Font(R.font.montserrat_extra_bold))
val manropeMedium = FontFamily(Font(R.font.manrope_medium))
val lora = FontFamily(Font(R.font.lora_font_wght))

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = montserratExtraBold,
        fontSize = 45.sp,
        textAlign = TextAlign.Center
    ),
    titleMedium = TextStyle(
        fontFamily = lora,
        fontSize = 30.sp,
        textAlign = TextAlign.Center
    ),
    labelSmall = TextStyle(
        fontFamily = lora,
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    ),
    labelMedium = TextStyle(
        fontFamily = manropeMedium,
        fontSize = 23.sp,
        textAlign = TextAlign.Center
    )
)

