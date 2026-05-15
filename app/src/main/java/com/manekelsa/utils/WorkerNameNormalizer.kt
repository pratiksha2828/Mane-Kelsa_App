package com.manekelsa.utils

object WorkerNameNormalizer {
    fun normalize(name: String?, id: String?, phoneNumber: String?): String {
        val raw = name?.trim().orEmpty()
        if (raw.isNotEmpty()) {
            return raw.replace(Regex("\\s+"), " ")
        }
        val phone = phoneNumber?.filter { it.isDigit() }.orEmpty()
        if (phone.isNotEmpty()) {
            return "Worker $phone"
        }
        val safeId = id?.trim().orEmpty()
        return if (safeId.isNotEmpty()) {
            "Worker ${safeId.take(6)}"
        } else {
            "Worker"
        }
    }
}
