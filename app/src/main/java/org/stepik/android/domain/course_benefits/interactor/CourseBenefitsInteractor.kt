package org.stepik.android.domain.course_benefits.interactor

import io.reactivex.Maybe
import org.stepik.android.domain.course_benefits.model.CourseBenefitSummary
import org.stepik.android.domain.course_benefits.repository.CourseBenefitSummariesRepository
import ru.nobird.android.domain.rx.maybeFirst
import javax.inject.Inject

class CourseBenefitsInteractor
@Inject
constructor(
    private val courseBenefitSummariesRepository: CourseBenefitSummariesRepository
) {
    fun getCourseBenefitSummary(courseId: Long): Maybe<CourseBenefitSummary> =
        courseBenefitSummariesRepository
            .getCourseBenefitSummaries(courseId)
            .maybeFirst()
}