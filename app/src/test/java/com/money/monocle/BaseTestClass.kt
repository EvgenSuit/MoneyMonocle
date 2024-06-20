package com.money.monocle

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.unmockkAll
import kotlinx.coroutines.test.TestScope
import org.junit.After

open class BaseTestClass {
    val testScope = TestScope()
    val snackbarScope = TestScope()
    open lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    @After
    fun clean() = unmockkAll()
}