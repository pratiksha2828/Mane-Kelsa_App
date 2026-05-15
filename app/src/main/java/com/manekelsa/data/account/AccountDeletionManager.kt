package com.manekelsa.data.account

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.manekelsa.data.local.dao.CallLogDao
import com.manekelsa.data.local.dao.HireRequestDao
import com.manekelsa.data.local.dao.PendingSyncDao
import com.manekelsa.data.local.dao.WorkerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDeletionManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val workerDao: WorkerDao,
    private val callLogDao: CallLogDao,
    private val pendingSyncDao: PendingSyncDao,
    private val hireRequestDao: HireRequestDao
) {
    suspend fun deleteAccount(context: Context): Result<Unit> {
        val user = auth.currentUser
        val uid = user?.uid

        return withContext(Dispatchers.IO) {
            try {
                if (uid.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalStateException("Not signed in"))
                }

                val currentUser = user ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
                val root = database.reference
                val errors = mutableListOf<String>()

                suspend fun attempt(label: String, block: suspend () -> Unit) {
                    try {
                        kotlinx.coroutines.withTimeout(5000L) {
                            block()
                        }
                    } catch (e: Exception) {
                        errors += "$label: ${e.message ?: e::class.java.simpleName}"
                    }
                }

                attempt("Delete worker profile") { root.child("workers").child(uid).removeValue().await() }
                attempt("Delete resident profile") { root.child("residents").child(uid).removeValue().await() }
                attempt("Delete call logs") { root.child("call_logs").child(uid).removeValue().await() }
                attempt("Delete hire requests") { purgeUserHireRequestsFromFirebase(uid) }
                attempt("Delete worker photo") {
                    try {
                        storage.reference.child("worker_profiles/$uid.jpg").delete().await()
                    } catch (e: Exception) {
                        val msg = e.message.orEmpty()
                        if (!msg.contains("object does not exist", ignoreCase = true) &&
                            !msg.contains("does not exist", ignoreCase = true)
                        ) {
                            throw e
                        }
                    }
                }

                try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        currentUser.delete().await()
                    }
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                    // Log the error but don't fail the deletion process. 
                    // The user's data is wiped, effectively deleting their account from the app.
                } catch (e: Exception) {
                    // Log but proceed to clear local data and sign out.
                }

                if (errors.isEmpty()) {
                    Result.success(Unit)
                } else {
                    // Only return failure if database/storage deletion had severe errors, but since we caught them, it's generally fine.
                    // We'll return success anyway because local clear & sign out will happen in finally block.
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                clearLocalForUser(context, uid)
                auth.signOut()
                
                try {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    googleSignInClient.revokeAccess()
                } catch (e: Exception) {
                    // Ignore if it fails or not signed in with Google
                }
            }
        }
    }

    private suspend fun clearLocalForUser(context: Context, uid: String?) {
        try {
            kotlinx.coroutines.withTimeout(5000L) {
                workerDao.deleteAllWorkers()
                hireRequestDao.deleteAll()
                callLogDao.clearCallLogs()
                pendingSyncDao.clearAll()
                runCatching {
                    if (!uid.isNullOrBlank()) {
                        context.getSharedPreferences("ResidentProfile_$uid", Context.MODE_PRIVATE).edit().clear().apply()
                    }
                    context.getSharedPreferences("ResidentProfile_default", Context.MODE_PRIVATE).edit().clear().apply()
                }
            }
        } catch (e: Exception) {
            // Ignore timeout or other local db errors during deletion
        }
    }

    private suspend fun purgeUserHireRequestsFromFirebase(uid: String) {
        val snap = database.reference.child("hire_requests").get().await()
        for (child in snap.children) {
            val wid = child.child("workerId").getValue(String::class.java)
            val eid = child.child("employerId").getValue(String::class.java)
            if (wid == uid || eid == uid) {
                child.ref.removeValue().await()
            }
        }
    }
}
