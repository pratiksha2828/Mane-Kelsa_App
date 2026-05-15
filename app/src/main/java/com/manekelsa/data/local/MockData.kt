package com.manekelsa.data.local

import com.manekelsa.data.local.entity.WorkerEntity

/**
 * Demo workers merged into search/home when the local DB has fewer listings.
 * Excludes known test accounts the user asked to remove from discovery.
 */
object MockData {

    fun shouldHideWorker(worker: WorkerEntity): Boolean {
        val n = worker.name.lowercase()
        if (n.contains("pratiksha") && n.contains("bhat")) return true
        if (n.contains("pratiksh") && n.contains("bhat")) return true
        return false
    }

    val fallbackWorkers: List<WorkerEntity> = listOf(
        w("01", "Ananya Rao", "https://images.unsplash.com/photo-1618245472177-2a74ad3b994a?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Cook", "Caretaker"), 3200.0, "Jayanagar", "9876543210", 4.6f, 12),
        w("02", "Ravi Kumar", "https://plus.unsplash.com/premium_photo-1689977871600-e755257fb5f8?q=80&w=1170&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Driver"), 2800.0, "Indiranagar", "9876543211", 4.4f, 8, isAvailable = false),
        w("03", "Lakshmi Devi", "https://plus.unsplash.com/premium_photo-1661964243697-734d7bd664ff?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Cleaner", "Cook"), 2500.0, "Malleshwaram", "9876543212", 4.8f, 20),
        w("04", "Suresh Nair", "https://images.unsplash.com/photo-1534339480783-6816b68be29c?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Electrician", "Plumber"), 3500.0, "Koramangala", "9876543213", 4.5f, 15),
        w("05", "Geetha Iyer", "https://images.unsplash.com/photo-1533128361669-69c065857a13?q=80&w=736&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Babysitter", "Caretaker"), 3000.0, "Whitefield", "9876543214", 4.9f, 9, isAvailable = false),
        w("06", "Manjunath Gowda", "https://plus.unsplash.com/premium_photo-1689838026921-c09632fd77ff?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Gardener", "Driver"), 2600.0, "Rajajinagar", "9876543215", 4.2f, 6),
        w("07", "Kavitha Reddy", "https://plus.unsplash.com/premium_photo-1682092039530-584ae1d9da7f?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Cook"), 3100.0, "BTM Layout", "9876543216", 4.7f, 11),
        w("08", "Venkatesh Murthy", "https://images.unsplash.com/photo-1607346256330-dee7af15f7c5?q=80&w=1206&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Carpenter", "Painter"), 2900.0, "HSR Layout", "9876543217", 4.3f, 7),
        w("09", "Shanthi Pillai", "https://images.unsplash.com/photo-1496813146940-1601b02f81a4?q=80&w=1149&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Cleaner"), 2200.0, "Electronic City", "9876543218", 4.5f, 14, isAvailable = false),
        w("10", "Harish Bhat", "https://images.unsplash.com/photo-1694871420373-e9c1031b91ee?q=80&w=1170&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Driver", "Caretaker"), 3300.0, "Vijayanagar", "9876543219", 4.6f, 10),
        w("11", "Divya Shetty", "https://images.unsplash.com/photo-1552113125-81af17f36b57?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Nurse", "Caretaker"), 4000.0, "Jayanagar", "9876543220", 4.9f, 18),
        w("12", "Naveen Patil", "https://images.unsplash.com/photo-1508341591423-4347099e1f19?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Plumber"), 2700.0, "Indiranagar", "9876543221", 4.4f, 5),
        w("13", "Meena Krishnan", "https://images.unsplash.com/photo-1463335361701-e90f4c5045d0?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Cook", "Cleaner"), 2400.0, "Koramangala", "9876543222", 4.7f, 16),
        w("14", "Arun Joseph", "https://plus.unsplash.com/premium_photo-1682089869602-2ec199cc501a?q=80&w=1170&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", listOf("Electrician"), 3000.0, "Whitefield", "9876543223", 4.5f, 9)
    )

    private fun w(
        suffix: String,
        name: String,
        photoUrl: String?,
        skills: List<String>,
        wage: Double,
        area: String,
        phone: String,
        rating: Float,
        ratingsCount: Int,
        isAvailable: Boolean = true
    ) = WorkerEntity(
        id = "demo_worker_$suffix",
        name = name,
        photoUrl = photoUrl,
        skillsList = skills,
        dailyWage = wage,
        area = area,
        experience = 3,
        phoneNumber = phone,
        averageRating = rating,
        totalRatings = ratingsCount,
        likes = ratingsCount / 2,
        isAvailable = isAvailable,
        lastUpdated = System.currentTimeMillis()
    )
}
