package com.pitstop

import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.IssueDetail
import com.pitstop.models.issue.UpcomingIssue

/**
 * Created by Karol Zdebel on 7/3/2018.
 */
class CarIssueTestUtil {

    companion object {

        fun getUpcomingCarIssue(issueId: Int, carId: Int): UpcomingIssue{
            val issue = UpcomingIssue()
            issue.carId = carId
            issue.intervalMileage = "100"
            issue.id = issueId
            val issueDetail = IssueDetail()
            issueDetail.item = "Oil"
            issueDetail.action = "Change"
            issueDetail.description = "Your oil needs to be changed."
            return issue

        }

        fun getDoneCarIssue(issueId: Int, carId: Int): CarIssue{
            val issue = CarIssue()
            issue.id = issueId
            issue.carId = carId
            issue.causes = "These are causes"
            issue.day = 1
            issue.doneAt = "50"
            issue.doneMileage = 500
            issue.issueType = CarIssue.DTC
            issue.symptoms = "These are symptoms"
            issue.status = CarIssue.ISSUE_NEW
            issue.action = "Take action"
            issue.description = "This is a description"
            issue.item = "Item"
            issue.priority = 1
            return issue
        }

        fun getCurrentIssue(issueId: Int, carId: Int): CarIssue{
            val issue = CarIssue()
            issue.id = issueId
            issue.carId = carId
            issue.causes = "These are causes"
            issue.symptoms = "These are symptoms"
            issue.issueType = CarIssue.PENDING_DTC
            issue.status = CarIssue.ISSUE_PENDING
            issue.action = "Take action"
            issue.description = "This is a description"
            issue.item = "Item"
            issue.priority = 1
            return issue
        }

        fun getCurrentCarIssue(){

        }
    }
}