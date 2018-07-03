package com.pitstop.database

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Karol Zdebel on 7/3/2018.
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocalCarIssueStorageTest {

    private val tag = LocalCarIssueStorageTest::class.java.simpleName

    private var localCarIssueStorage: LocalCarIssueStorage? = null

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        localCarIssueStorage = LocalCarIssueStorage(LocalDatabaseHelper.getInstance(context))
    }

    @Test
    fun localActivityStorageTest(){
        Log.d(tag,"running localCarIssueStorageTest()")


    }
}