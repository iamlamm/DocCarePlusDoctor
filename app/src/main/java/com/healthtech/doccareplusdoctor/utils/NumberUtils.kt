package com.healthtech.doccareplusdoctor.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun Double.formatToVND(): String {
    return String.format("%,.0f₫", this)
}

@SuppressLint("DefaultLocale")
fun Double.formatToUSD(): String {
    return String.format("$%.2f", this)
} 