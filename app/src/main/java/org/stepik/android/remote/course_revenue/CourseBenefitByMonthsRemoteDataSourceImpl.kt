package org.stepik.android.remote.course_revenue

import io.reactivex.Maybe
import org.stepik.android.data.course_revenue.source.CourseBenefitByMonthsRemoteDataSource
import org.stepik.android.domain.course_revenue.model.CourseBenefitByMonth
import org.stepik.android.remote.course_revenue.model.CourseBenefitByMonthsResponse
import org.stepik.android.remote.course_revenue.service.CourseBenefitByMonthsService
import javax.inject.Inject

class CourseBenefitByMonthsRemoteDataSourceImpl
@Inject
constructor(
    private val courseBenefitRemoteByMonthsService: CourseBenefitByMonthsService
) : CourseBenefitByMonthsRemoteDataSource {
    override fun getCourseBenefitByMonths(courseId: Long): Maybe<List<CourseBenefitByMonth>> =
        courseBenefitRemoteByMonthsService
            .getCourseBenefitByMonths(courseId)
            .map(CourseBenefitByMonthsResponse::courseBenefitByMonths)
}