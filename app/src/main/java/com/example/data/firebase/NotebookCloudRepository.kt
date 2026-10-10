package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.ui.NotebookPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotebookCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun getCurrentUserName(): String? = auth.currentUser?.displayName

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Cloud Notebook.")
    }

    /**
     * Realtime observation of the user's notebook pages from Firestore.
     */
    fun observeNotebookPages(): Flow<List<NotebookPage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionPath = "users/$uid/notebook_pages"
        val collectionRef = db.collection("users").document(uid).collection("notebook_pages")

        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionPath)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val pages = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: "Note"
                    val content = doc.getString("content") ?: ""
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    NotebookPage(
                        id = id,
                        title = title,
                        content = content,
                        updatedAt = updatedAt
                    )
                }.sortedBy { it.id }
                trySend(pages)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Sync/save a single notebook page to Firestore.
     */
    suspend fun savePage(page: NotebookPage, onError: (String) -> Unit = {}) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            onError(e.message ?: "Not signed in")
            return
        }

        val docPath = "users/$uid/notebook_pages/${page.id}"
        val docRef = db.collection("users").document(uid).collection("notebook_pages").document(page.id.toString())

        val payload = hashMapOf<String, Any>(
            "id" to page.id,
            "title" to page.title,
            "content" to page.content,
            "updatedAt" to page.updatedAt,
            "userId" to uid
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            onError(e.localizedMessage ?: "Failed to save note to cloud")
        }
    }

    /**
     * Backup a list of notebook pages to cloud.
     */
    suspend fun syncAllPages(pages: List<NotebookPage>, onError: (String) -> Unit = {}) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            onError(e.message ?: "Not signed in")
            return
        }

        for (page in pages) {
            val docPath = "users/$uid/notebook_pages/${page.id}"
            val docRef = db.collection("users").document(uid).collection("notebook_pages").document(page.id.toString())
            val payload = hashMapOf<String, Any>(
                "id" to page.id,
                "title" to page.title,
                "content" to page.content,
                "updatedAt" to page.updatedAt,
                "userId" to uid
            )
            try {
                docRef.set(payload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docPath)
                onError(e.localizedMessage ?: "Failed to backup page to cloud")
            }
        }
    }

    /**
     * Delete a notebook page from cloud.
     */
    suspend fun deletePage(pageId: Long, onError: (String) -> Unit = {}) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return
        }

        val docPath = "users/$uid/notebook_pages/$pageId"
        val docRef = db.collection("users").document(uid).collection("notebook_pages").document(pageId.toString())

        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            onError(e.localizedMessage ?: "Failed to delete page from cloud")
        }
    }
}
