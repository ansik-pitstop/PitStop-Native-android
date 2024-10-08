package com.pitstop.database

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.pitstop.CarIssueTestUtil
import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.UpcomingIssue
import junit.framework.Assert
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

    private lateinit var localCarIssueStorage: LocalCarIssueStorage

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        localCarIssueStorage = LocalCarIssueStorage(LocalDatabaseHelper.getInstance(context))
    }

    @Test
    fun localCarIssueStorageTest(){
        Log.d(tag,"running localCarIssueStorageTest()")

        localCarIssueStorage.deleteAllCarIssues()

        val upcomingIssueListSize = 3
        val upcomingIssueOtherListSize = 2
        val currentIssueListSize = 1
        val doneIssueListSize = 1
        val carId = 6112
        val carIdOther = 1234

        val upcomingIssues = arrayListOf<UpcomingIssue>()
        val upcomingIssuesOther = arrayListOf<UpcomingIssue>()
        val currentIssues = arrayListOf<CarIssue>()
        val doneIssues = arrayListOf<CarIssue>()

        var index = 1
        for (i in 1..upcomingIssueListSize){
            upcomingIssues.add(CarIssueTestUtil.getUpcomingCarIssue(carId,index))
            index++
        }

        for (i in 1..upcomingIssueOtherListSize){
            upcomingIssuesOther.add(CarIssueTestUtil.getUpcomingCarIssue(carIdOther,index))
            index++
        }

        for (i in 1..currentIssueListSize){
            currentIssues.add(CarIssueTestUtil.getCurrentCarIssue(carId,index))
            index++
        }

        for (i in 1..doneIssueListSize){
            doneIssues.add(CarIssueTestUtil.getDoneCarIssue(carId,index))
            index++
        }

        //Store tests
        val currentIssuesReplacedCount = localCarIssueStorage.replaceCurrentIssues(carId,currentIssues)
        Log.d(tag,"currentIssuesReplacedCount = $currentIssuesReplacedCount")
        Assert.assertEquals(currentIssueListSize, currentIssuesReplacedCount)

        val doneIssuesReplacedCount = localCarIssueStorage.replaceDoneIssues(carId,doneIssues)
        Log.d(tag,"doneIssuesReplacedCount = $doneIssuesReplacedCount")
        Assert.assertEquals(doneIssueListSize, doneIssuesReplacedCount)

        val upcomingIssuesReplacedCount = localCarIssueStorage.replaceUpcomingIssues(carId,upcomingIssues)
        Log.d(tag,"upcomingIssuesReplacedCount = $upcomingIssuesReplacedCount")
        Assert.assertEquals(upcomingIssueListSize,upcomingIssuesReplacedCount)

        val upcomingIssuesOtherCarReplacedCount = localCarIssueStorage
                .replaceUpcomingIssues(carIdOther,upcomingIssuesOther)
        Log.d(tag,"upcomingIssuesReplacedCount = $upcomingIssuesOtherCarReplacedCount")
        Assert.assertEquals(upcomingIssueOtherListSize,upcomingIssuesOtherCarReplacedCount)

        //Retrieve tests
        val storedUpcomingIssues = localCarIssueStorage
                .getAllUpcomingCarIssues(carId).toSet()
        Log.d(tag,"storedUpcomingIssues = $storedUpcomingIssues")
        Assert.assertEquals(upcomingIssues.toSet(),storedUpcomingIssues)

        Assert.assertEquals(currentIssues.toSet(),localCarIssueStorage.getAllCurrentCarIssues(carId).toSet())
        Assert.assertEquals(doneIssues.toSet(),localCarIssueStorage.getAllDoneCarIssues(carId).toSet())

        //Delete tests
        Assert.assertEquals(upcomingIssueListSize,localCarIssueStorage.deleteAllUpcomingCarIssues(carId))
        Assert.assertEquals(doneIssueListSize,localCarIssueStorage.deleteAllDoneCarIssues(carId))
        Assert.assertEquals(upcomingIssueOtherListSize,localCarIssueStorage.deleteAllUpcomingCarIssues(carIdOther))
        Assert.assertEquals(currentIssueListSize,localCarIssueStorage.deleteAllCurrentCarIssues(carId))
    }
}