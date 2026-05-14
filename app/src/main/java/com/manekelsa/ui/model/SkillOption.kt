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
    const val NURSE = "Nurse"

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
        BABYSITTER,
        NURSE
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
        NURSE -> R.string.skill_nurse
        else -> null
    }

    fun normalize(skill: String): String {
        val trimmed = skill.trim()
        return when (trimmed) {
            "Cleaner", "ಕ್ಲೀನರ್", "ಸ್ವಚ್ಛತೆ", "Cleaning", "House cleaning", "Housekeeping", "Maid" -> CLEANER
            "Cook", "ಅಡುಗೆಯವರು", "ಅಡುಗೆಗಾರ", "Chef" -> COOK
            "Gardener", "ಗಾರ್ಡನರ್", "ತೋಟಗಾರ", "Gardening", "Garden" -> GARDENER
            "Driver", "ಡ್ರೈವರ್", "Driving" -> DRIVER
            "Caretaker", "Caretaking", "ಪಾಲಕ", "Patient care", "Elder care" -> CARETAKER
            "Plumber", "ಪ್ಲಂಬರ್", "Plumbing" -> PLUMBER
            "Electrician", "ಎಲೆಕ್ಟ್ರಿಷಿಯನ್", "ಇಲೆಕ್ಟ್ರಿಷಿಯನ್", "Electrical" -> ELECTRICIAN
            "Painter", "ಬಣ್ಣಗಾರ", "Painting" -> PAINTER
            "Carpenter", "ಕಾರ್ಪೆಂಟರ್", "Carpentry" -> CARPENTER
            "Babysitter", "ಮಕ್ಕಳ ಪಾಲಕ", "Child care", "Childcare", "Nanny" -> BABYSITTER
            "Nurse", "ನರ್ಸ್", "Nursing" -> NURSE
            else -> trimmed
        }
    }

    fun getAliases(skillId: String): List<String> {
        return when (skillId) {
            CLEANER -> listOf(CLEANER, "ಕ್ಲೀನರ್", "ಸ್ವಚ್ಛತೆ")
            COOK -> listOf(COOK, "ಅಡುಗೆಯವರು", "ಅಡುಗೆಗಾರ")
            GARDENER -> listOf(GARDENER, "ಗಾರ್ಡನರ್", "ತೋಟಗಾರ")
            DRIVER -> listOf(DRIVER, "ಡ್ರೈವರ್")
            CARETAKER -> listOf(CARETAKER, "Caretaking", "ಪಾಲಕ")
            PLUMBER -> listOf(PLUMBER, "ಪ್ಲಂಬರ್")
            ELECTRICIAN -> listOf(ELECTRICIAN, "ಎಲೆಕ್ಟ್ರಿಷಿಯನ್", "ಇಲೆಕ್ಟ್ರಿಷಿಯನ್")
            PAINTER -> listOf(PAINTER, "ಬಣ್ಣಗಾರ")
            CARPENTER -> listOf(CARPENTER, "ಕಾರ್ಪೆಂಟರ್")
            BABYSITTER -> listOf(BABYSITTER, "ಮಕ್ಕಳ ಪಾಲಕ")
            NURSE -> listOf(NURSE, "ನರ್ಸ್")
            else -> listOf(skillId)
        }
    }
}
