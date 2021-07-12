package org.stepik.android.data.course_revenue.repository

import io.reactivex.Maybe
import org.stepik.android.data.course_revenue.source.CourseBenefitByMonthsRemoteDataSource
import org.stepik.android.domain.course_revenue.model.CourseBenefitByMonth
import org.stepik.android.domain.course_revenue.repository.CourseBenefitByMonthsRepository
import javax.inject.Inject

class CourseBenefitByMonthsRepositoryImpl
@Inject
constructor(
    private val courseBenefitByMonthsRemoteDataSource: CourseBenefitByMonthsRemoteDataSource
) : CourseBenefitByMonthsRepository {
    override fun getCourseBenefitByMonths(courseId: Long): Maybe<List<CourseBenefitByMonth>> =
        courseBenefitByMonthsRemoteDataSource.getCourseBenefitByMonths(courseId)
}