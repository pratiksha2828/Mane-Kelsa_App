package com.manekelsa.ui.model

import androidx.annotation.StringRes
import com.manekelsa.R

object SkillOption {
    const val ALL = "All"
    const val CLEANER = "Cleaner"
    const val COOK = "Cook"
    const val GARDENER = "Gardener"
    const val DRIVER = "Driver"
    const val CARETAKER = "Caretaker"
    const val PLUMBER = "Plumber"
    const val ELECTRICIAN = "Electrician"
    const val PAINTER = "Painter"
    const val CARPENTER = "Carpenter"
    const val BABYSITTER = "Babysitter"

    val all = listOf(
        CLEANER,
        COOK,
        GARDENER,
        DRIVER,
        CARETAKER,
        PLUMBER,
        ELECTRICIAN,
        PAINTER,
        CARPENTER,
        BABYSITTER
    )

    @StringRes
    fun labelRes(skillId: String): Int? = when (skillId) {
        CLEANER -> R.string.skill_cleaner
        COOK -> R.string.skill_cook
        GARDENER -> R.string.skill_gardener
        DRIVER -> R.string.skill_driver
        CARETAKER -> R.string.skill_caretaker
        PLUMBER -> R.string.skill_plumber
        ELECTRICIAN -> R.string.skill_electrician
        PAINTER -> R.string.skill_painter
        CARPENTER -> R.string.skill_carpenter
        BABYSITTER -> R.string.skill_babysitter
        else -> null
    }

    fun normalize(skill: String): String {
        val trimmed = skill.trim()
        return when (trimmed) {
            "Cleaner", "ಕ್ಲೀನರ್", "ಸ್ವಚ್ಛತೆ" -> CLEANER
            "Cook", "ಅಡುಗೆಯವರು", "ಅಡುಗೆಗಾರ" -> COOK
            "Gardener", "ಗಾರ್ಡನರ್", "ತೋಟಗಾರ" -> GARDENER
            "Driver", "ಡ್ರೈವರ್" -> DRIVER
            "Caretaker", "Caretaking", "ಪಾಲಕ" -> CARETAKER
            "Plumber", "ಪ್ಲಂಬರ್" -> PLUMBER
            "Electrician", "ಎಲೆಕ್ಟ್ರಿಷಿಯನ್", "ಇಲೆಕ್ಟ್ರಿಷಿಯನ್" -> ELECTRICIAN
            "Painter", "ಬಣ್ಣಗಾರ" -> PAINTER
            "Carpenter", "ಕಾರ್ಪೆಂಟರ್" -> CARPENTER
            "Babysitter", "ಮಕ್ಕಳ ಪಾಲಕ" -> BABYSITTER
            else -> trimmed
        }
    }
}
