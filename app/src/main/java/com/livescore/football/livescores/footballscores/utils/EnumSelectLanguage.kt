package com.livescore.football.livescores.footballscores.utils

import com.cscmobi.libraryads.data.LanguageModel
import com.livescore.football.livescores.footballscores.R

enum class EnumSelectLanguage(val id: Int, val nameLanguage: Int, val flag: Int, val code: String) {
    FRENCH(1, R.string.language_french, R.drawable.flag_fr, "fr"),
    GERMAN(2, R.string.language_german, R.drawable.flag_de, "de"),
    INDONESIAN(3, R.string.language_indonesian, R.drawable.flag_id, "id"),
    UNITED_STATES(4, R.string.language_english, R.drawable.flag_us, "en"),
    HINDI(5, R.string.language_hindi, R.drawable.flag_in, "hi"),
    SPANISH(6, R.string.language_spanish, R.drawable.flag_es, "es"),
    PORTUGUESE(7, R.string.language_portuguese, R.drawable.flag_pt, "pt"),
    VIETNAMESE(8, R.string.language_vietnamese, R.drawable.flag_vn, "vi"),
    JAPANESE(9, R.string.language_japanese, R.drawable.flag_jp, "ja");

    companion object {
        fun toLanguageModelList(): ArrayList<LanguageModel> {
            val list = arrayListOf<LanguageModel>()
            for (lang in entries) {
                list.add(
                    LanguageModel(
                        txtLanguage = lang.nameLanguage,
                        icFlag = lang.flag,
                        code = lang.code,
                        isSelect = false
                    )
                )
            }
            return list
        }
    }
}
