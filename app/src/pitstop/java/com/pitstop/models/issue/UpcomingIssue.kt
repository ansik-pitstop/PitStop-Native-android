package com.pitstop.models.issue

/**
 * Created by zohaibhussain on 2017-01-11.
 */

data class UpcomingIssue(val carId: Int, val id: Int, val priority: Int, val intervalMileage: String, val issueDetail: IssueDetail)
