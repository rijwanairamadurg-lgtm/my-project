package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.ui.NotebookPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canSaveAndReadOwnNotebookPage() = runBlocking {
        signInTestUser("alice@example.com")
        val repo = NotebookCloudRepository(firestore, auth)

        val page = NotebookPage(
            id = 101L,
            title = "ಅಂಗಡಿ ಲೆಕ್ಕ (Shop Ledger)",
            content = "1. ಅಕ್ಕಿ: 1200\n2. ಬೇಳೆ: 800",
            updatedAt = System.currentTimeMillis()
        )

        repo.savePage(page)

        val pages = repo.observeNotebookPages().first { it.isNotEmpty() }
        assertNotNull(pages)
        assertEquals(1, pages.size)
        assertEquals("ಅಂಗಡಿ ಲೆಕ್ಕ (Shop Ledger)", pages[0].title)
        assertEquals(page.content, pages[0].content)
    }

    @Test
    fun unauthenticatedUser_throwsExceptionWhenWriting() = runBlocking {
        auth.signOut()
        val repo = NotebookCloudRepository(firestore, auth)

        val page = NotebookPage(
            id = 202L,
            title = "Unauthed",
            content = "Test content",
            updatedAt = System.currentTimeMillis()
        )

        var errorReported = false
        repo.savePage(page, onError = { errorReported = true })
        assertTrue("Error should be reported when unauthenticated", errorReported)
    }
}
