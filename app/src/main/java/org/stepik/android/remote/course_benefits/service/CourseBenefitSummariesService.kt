package org.stepik.android.remote.course_benefits.service

import io.reactivex.Single
import org.stepik.android.remote.course_benefits.model.CourseBenefitSummariesResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface CourseBenefitSummariesService {
    @GET("api/course-benefit-summaries/{id}")
    fun getCourseBenefitSummaries(@Path("id") courseId: Long): Single<CourseBenefitSummariesResponse>
}