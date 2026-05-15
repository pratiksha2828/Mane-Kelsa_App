package com.manekelsa.utils

import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.ui.model.SkillOption

object SearchMatcher {

    fun normalizeQuery(query: String): String = query.trim().lowercase()

    fun normalizeText(text: String): String = text.trim().lowercase()

    fun matchesSkill(worker: WorkerEntity, selectedSkill: String): Boolean {
        if (selectedSkill == SkillOption.ALL) return true
        val aliases = SkillOption.getAliases(selectedSkill)
        return worker.skillsList.any { skill ->
            aliases.any { alias -> skill.equals(alias, ignoreCase = true) } ||
                skill.equals(selectedSkill, ignoreCase = true)
        }
    }

    fun matchesQuery(worker: WorkerEntity, normalizedQuery: String): Boolean {
        val q = normalizedQuery.trim()
        if (q.isBlank()) return true
        val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (tokens.size <= 1) {
            matchesSingleToken(worker, q)
        } else {
            tokens.all { token -> matchesSingleToken(worker, token) }
        }
    }

    private fun matchesSingleToken(worker: WorkerEntity, token: String): Boolean {
        val nameMatch = normalizeText(worker.name).contains(token)
        val areaMatch = normalizeText(worker.area).contains(token) ||
            TranslationUtils.areaKeywordsMatch(worker.area, token)
        val extractedSkills = TranslationUtils.extractSkillIds(token)
        val skillMatch = worker.skillsList.any { skill ->
            val normSkill = normalizeText(skill)
            normSkill.contains(token) ||
                SkillOption.getAliases(SkillOption.normalize(skill)).any { alias ->
                    normalizeText(alias).contains(token)
                } ||
                (extractedSkills.isNotEmpty() && extractedSkills.contains(SkillOption.normalize(skill)))
        }
        val phoneMatch = worker.phoneNumber.contains(token)
        return nameMatch || areaMatch || skillMatch || phoneMatch
    }

    fun similarityScore(worker: WorkerEntity, normalizedQuery: String): Int {
        if (normalizedQuery.isBlank()) return 0
        var score = 0
        val q = normalizedQuery.trim()
        val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return 0
        val nameText = normalizeText(worker.name)
        val areaText = normalizeText(worker.area)
        val skillsText = worker.skillsList.joinToString(" ") { normalizeText(it) }
        for (t in tokens) {
            if (!nameText.contains(t)) score += 2
            if (!areaText.contains(t) && !TranslationUtils.areaKeywordsMatch(worker.area, t)) score += 2
            if (!skillsText.contains(t) && TranslationUtils.extractSkillIds(t).isEmpty()) score += 1
        }
        return score
    }
}
