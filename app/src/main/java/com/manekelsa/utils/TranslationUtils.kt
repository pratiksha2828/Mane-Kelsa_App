package com.manekelsa.utils

import com.manekelsa.ui.model.SkillOption

object TranslationUtils {
    private val kannadaRegex = Regex("[\\u0C80-\\u0CFF]")

    private val nameTranslations = mapOf(
        "Ananya Rao" to "ಅನನ್ಯಾ ರಾವ್",
        "Ravi Kumar" to "ರವಿ ಕುಮಾರ್",
        "Lakshmi Devi" to "ಲಕ್ಷ್ಮಿ ದೇವಿ",
        "Suresh Nair" to "ಸುರೇಶ್ ನಾಯರ್",
        "Geetha Iyer" to "ಗೀತಾ ಅಯ್ಯರ್",
        "Manjunath Gowda" to "ಮಂಜುನಾಥ್ ಗೌಡ",
        "Kavitha Reddy" to "ಕವಿತಾ ರೆಡ್ಡಿ",
        "Venkatesh Murthy" to "ವೆಂಕಟೇಶ್ ಮೂರ್ತಿ",
        "Shanthi Pillai" to "ಶಾಂತಿ ಪಿಳ್ಳೈ",
        "Harish Bhat" to "ಹರೀಶ್ ಭಟ್",
        "Divya Shetty" to "ದಿವ್ಯಾ ಶೆಟ್ಟಿ",
        "Naveen Patil" to "ನವೀನ್ ಪಾಟೀಲ್",
        "Meena Krishnan" to "ಮೀನಾ ಕೃಷ್ಣನ್",
        "Arun Joseph" to "ಅರುಣ್ ಜೋಸೆಫ್"
    )

    private val skillKannada = mapOf(
        SkillOption.CLEANER to "ಸ್ವಚ್ಛತೆ",
        SkillOption.COOK to "ಅಡುಗೆಗಾರ",
        SkillOption.GARDENER to "ತೋಟಗಾರ",
        SkillOption.DRIVER to "ಚಾಲಕ",
        SkillOption.CARETAKER to "ಪಾಲಕ",
        SkillOption.PLUMBER to "ಪ್ಲಂಬರ್",
        SkillOption.ELECTRICIAN to "ವಿದ್ಯುತ್ ತಂತ್ರಜ್ಞ",
        SkillOption.PAINTER to "ಬಣ್ಣಗಾರ",
        SkillOption.CARPENTER to "ಮರಗೆಲಸಗಾರ",
        SkillOption.BABYSITTER to "ಮಕ್ಕಳ ಪಾಲಕ",
        SkillOption.NURSE to "ನರ್ಸ್"
    )

    private val areaTranslations = mapOf(
        "bangalore" to "ಬೆಂಗಳೂರು",
        "bengaluru" to "ಬೆಂಗಳೂರು",
        "vijayanagar" to "ವಿಜಯನಗರ",
        "vijay nagar" to "ವಿಜಯನಗರ",
        "jayanagar" to "ಜಯನಗರ",
        "jaya nagar" to "ಜಯನಗರ",
        "rajajinagar" to "ರಾಜಾಜಿನಗರ",
        "raja ji nagar" to "ರಾಜಾಜಿನಗರ",
        "malleshwaram" to "ಮಲ್ಲೇಶ್ವರಂ",
        "malleswaram" to "ಮಲ್ಲೇಶ್ವರಂ",
        "koramangala" to "ಕೋರಮಂಗಲ",
        "indiranagar" to "ಇಂದಿರಾನಗರ",
        "indira nagar" to "ಇಂದಿರಾನಗರ",
        "whitefield" to "ವೈಟ್‌ಫೀಲ್ಡ್",
        "white field" to "ವೈಟ್‌ಫೀಲ್ಡ್",
        "btm" to "ಬಿಟಿಎಂ",
        "btm layout" to "ಬಿಟಿಎಂ ಲೇಔಟ್",
        "hsr" to "ಎಚ್‌ಎಸ್‌ಆರ್",
        "hsr layout" to "ಎಚ್‌ಎಸ್‌ಆರ್ ಲೇಔಟ್",
        "electronic city" to "ಎಲೆಕ್ಟ್ರಾನಿಕ್ ಸಿಟಿ",
        "mg road" to "ಎಂ.ಜಿ. ರಸ್ತೆ"
    )

    private val areaWordTranslations = mapOf(
        "road" to "ರಸ್ತೆ",
        "street" to "ರಸ್ತೆ",
        "layout" to "ಲೇಔಟ್",
        "main" to "ಮುಖ್ಯ",
        "cross" to "ಕ್ರಾಸ್",
        "phase" to "ಹಂತ",
        "block" to "ಬ್ಲಾಕ್",
        "sector" to "ವಲಯ",
        "nagar" to "ನಗರ",
        "colony" to "ವಸತಿ",
        "extension" to "ವಿಸ್ತರಣೆ"
    )

    private val skillKeywordMap = mapOf(
        SkillOption.CLEANER to listOf(
            "clean", "cleaning", "cleaner", "swachate", "swachh", "sweep", "sweeping",
            "house cleaning", "housework", "house work",
            "mane vorsakke", "mane vorasakke", "mane vorsake", "mane swachate", "mane orsakke",
            "batte ogiyakke", "batte ogiayakke", "batte ogiyake", "batte ogiyakke",
            "laundry", "washing clothes", "wash clothes",
            "ಮನೆ ಸ್ವಚ್ಛತೆ", "ಮನೆ ಕೆಲಸ", "ಬಟ್ಟೆ ಒಗೆಯ", "ಸ್ವಚ್ಛತೆ"
        ),
        SkillOption.COOK to listOf(
            "cook", "cooking", "chef", "adige", "aduge", "adugey", "aduge",
            "ಅಡುಗೆ", "ಅಡಿಗೆ", "ಪಾಕ", "ಅಡುಗೆಗಾರ", "ಅಡಿಗೆ ಮಾಡುವ"
        ),
        SkillOption.GARDENER to listOf(
            "gardener", "garden", "gardening", "thotagara", "thota", "lawn",
            "ತೋಟ", "ತೋಟಗಾರ"
        ),
        SkillOption.DRIVER to listOf(
            "driver", "driving", "chalak", "chalaak", "chalaaka", "drive",
            "ಚಾಲಕ", "ಡ್ರೈವರ್", "ವಾಹನ"
        ),
        SkillOption.BABYSITTER to listOf(
            "babysitter", "baby sitter", "child care", "childcare", "nanny",
            "shishu pakala", "shishu paalaka", "shishu paalak", "shishu palaka",
            "ಶಿಶು ಪಾಲಕ", "ಮಕ್ಕಳ ಪಾಲಕ", "ಮಕ್ಕಳ ಕಾಳಜಿ"
        ),
        SkillOption.CARETAKER to listOf(
            "caretaker", "caretaking", "patient care", "elder care", "care taker",
            "palaka", "paalaka", "ಪಾಲಕ", "ರೋಗಿಯ"
        ),
        SkillOption.PLUMBER to listOf(
            "plumber", "plumbing", "plumb", "ನಳಿ", "ಪ್ಲಂಬರ್"
        ),
        SkillOption.ELECTRICIAN to listOf(
            "electrician", "electrical", "wiring", "ವಿದ್ಯುತ್", "ಇಲೆಕ್ಟ್ರಿಷಿಯನ್"
        ),
        SkillOption.PAINTER to listOf(
            "painter", "painting", "paint", "ಬಣ್ಣ", "ಬಣ್ಣಗಾರ"
        ),
        SkillOption.CARPENTER to listOf(
            "carpenter", "carpentry", "woodwork", "ಮರಗೆಲಸ", "ಕಾರ್ಪೆಂಟರ್"
        ),
        SkillOption.NURSE to listOf(
            "nurse", "nursing", "care nurse", "ನರ್ಸ್"
        )
    )

    fun getTranslatedName(name: String): String {
        if (!isKannadaEnabled() || containsKannada(name)) return name
        return nameTranslations[name] ?: name
    }

    fun getTranslatedSkill(skill: String): String {
        if (!isKannadaEnabled()) return skill
        val normalized = SkillOption.normalize(skill)
        return skillKannada[normalized] ?: skill
    }

    fun getTranslatedArea(area: String): String {
        if (!isKannadaEnabled()) return area
        if (containsKannada(area)) return area
        val normalized = normalizeAreaForSearch(area)
        areaTranslations[normalized]?.let { return it }

        var translated = normalized
        areaWordTranslations.forEach { (english, kannada) ->
            translated = translated.replace(Regex("\\b$english\\b"), kannada)
        }
        return if (translated == normalized) area else translated
    }

    fun normalizeAreaForSearch(area: String): String = normalizeSearchText(area)

    fun normalizeSearchText(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\u0C80-\\u0CFF ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun extractSkillIds(text: String): Set<String> {
        if (text.isBlank()) return emptySet()
        val normalized = normalizeSearchText(text)
        val joined = normalized.replace(" ", "")
        val hits = mutableSetOf<String>()
        skillKeywordMap.forEach { (skillId, keywords) ->
            if (keywords.any { keyword ->
                    val key = normalizeSearchText(keyword)
                    normalized.contains(key) || joined.contains(key.replace(" ", ""))
                }) {
                hits.add(skillId)
            }
        }
        
        // Handle "shishu pakala" overlap: if babysitter is matched, drop caretaker unless explicitly requested
        if (hits.contains(SkillOption.BABYSITTER)) {
            val isExplicitCaretaker = normalized.contains("rogi") || normalized.contains("patient") || normalized.contains("elder") || normalized.contains("vridha")
            if (!isExplicitCaretaker) {
                hits.remove(SkillOption.CARETAKER)
            }
        }
        
        return hits
    }

    fun areaKeywordsMatch(workerArea: String, searchText: String): Boolean {
        val w = normalizeAreaForSearch(workerArea).replace(" ", "")
        val t = normalizeSearchText(searchText).replace(" ", "")
        if (t.isBlank() || w.isBlank()) return false
        areaTranslations.keys.forEach { key ->
            val compact = key.replace(" ", "")
            if (t.contains(compact) && w.contains(compact)) return true
        }
        return false
    }

    private fun isKannadaEnabled(): Boolean = LocalizationManager.getLanguage().startsWith("kn")

    private fun containsKannada(text: String): Boolean = kannadaRegex.containsMatchIn(text)
}
