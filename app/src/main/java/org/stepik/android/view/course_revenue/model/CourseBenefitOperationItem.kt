package org.stepik.android.view.course_revenue.model

import org.stepik.android.presentation.course_revenue.CourseBenefitsFeature
import org.stepik.android.presentation.course_revenue.CourseBenefitsMonthlyFeature
import ru.nobird.android.core.model.Identifiable

sealed class CourseBenefitOperationItem {
    class CourseBenefits(val state: CourseBenefitsFeature.State) : CourseBenefitOperationItem(), Identifiable<String> {
        override val id: String =
            "course_benefits"
    }

    class CourseBenefitsMonthly(val state: CourseBenefitsMonthlyFeature.State) : CourseBenefitOperationItem(), Identifiable<String> {
        override val id: String =
            "course_benefits_monthly"
    }
}