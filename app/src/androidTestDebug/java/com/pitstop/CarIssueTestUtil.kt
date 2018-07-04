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
            val issueDetail = IssueDetail("Oil","Change","Your oil needs to be changed.")
            return UpcomingIssue(carId, issueId, 1, "100",issueDetail)

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
            issue.status = CarIssue.ISSUE_DONE
            issue.issueDetail = IssueDetail("Item","Take action"
                    ,"This is a description")
            issue.priority = 1
            return issue
        }

        fun getCurrentCarIssue(issueId: Int, carId: Int): CarIssue{
            val issue = CarIssue()
            issue.id = issueId
            issue.carId = carId
            issue.causes = "These are causes"
            issue.symptoms = "These are symptoms"
            issue.issueType = CarIssue.PENDING_DTC
            issue.status = CarIssue.ISSUE_NEW
            issue.issueDetail = IssueDetail("Item","Take action"
                    ,"This is a description")
            issue.priority = 1
            return issue
        }

    }
}