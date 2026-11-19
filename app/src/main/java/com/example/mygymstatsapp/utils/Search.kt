package com.example.mygymstatsapp.utils

import java.text.Normalizer

private val diacritics = "\\p{Mn}+".toRegex()

/** Lowercase without accents, so "developpe" matches "Développé". */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(diacritics, "")
