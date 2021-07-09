package org.stepik.android.view.course_benefits.ui.delegate

import android.view.View
import kotlinx.android.synthetic.main.view_course_benefit_summary.view.*
import org.stepic.droid.R
import org.stepic.droid.ui.util.collapse
import org.stepic.droid.ui.util.expand
import org.stepic.droid.util.DateTimeHelper
import org.stepik.android.presentation.course_benefits.CourseBenefitSummaryFeature
import org.stepik.android.view.course.mapper.DisplayPriceMapper
import ru.nobird.android.view.base.ui.delegate.ViewStateDelegate
import java.util.TimeZone
import java.util.Locale

class CourseBenefitSummaryViewDelegate(
    containerView: View,
    private val displayPriceMapper: DisplayPriceMapper,
    private val onCourseSummaryClicked: (Boolean) -> Unit
) {
    private val context = containerView.context

    private val courseBenefitsSummaryLoading = containerView.courseBenefitSummaryLoading
    private val courseBenefitSummaryEmpty = containerView.courseBenefitSummaryEmpty

    private val courseBenefitSummaryContainer = containerView.courseBenefitSummaryInformation
    private val courseBenefitSummaryInformationExpansion = containerView.courseBenefitSummaryInformationExpansion

    private val courseBenefitSummaryArrow = containerView.courseBenefitSummaryArrow
    private val courseBenefitOperationDisclaimer = containerView.courseBenefitOperationDisclaimer

    private val courseBenefitCurrentEarningsTitle = containerView.courseBenefitSummaryEarningsCurrentMonthText
    private val courseBenefitCurrentEarningsValue = containerView.courseBenefitSummaryEarningsCurrentMonthValue

    private val courseBenefitCurrentTurnoverTitle = containerView.courseBenefitSummaryTurnoverCurrentMonthText
    private val courseBenefitCurrentTurnoverValue = containerView.courseBenefitSummaryTurnoverCurrentMonthValue

    private val courseBenefitTotalEarningsTitle = containerView.courseBenefitSummaryEarningsTotalText
    private val courseBenefitTotalEarningsValue = containerView.courseBenefitSummaryEarningsTotalValue

    private val courseBenefitTotalTurnoverTitle = containerView.courseBenefitSummaryTurnoverTotalText
    private val courseBenefitTotalTurnoverValue = containerView.courseBenefitSummaryTurnoverTotalValue

    private val viewStateDelegate = ViewStateDelegate<CourseBenefitSummaryFeature.State>()

    init {
        viewStateDelegate.addState<CourseBenefitSummaryFeature.State.Loading>(courseBenefitsSummaryLoading)
        viewStateDelegate.addState<CourseBenefitSummaryFeature.State.Empty>(courseBenefitSummaryEmpty, courseBenefitOperationDisclaimer)
        viewStateDelegate.addState<CourseBenefitSummaryFeature.State.Content>(courseBenefitSummaryContainer, courseBenefitOperationDisclaimer)

        courseBenefitSummaryContainer.setOnClickListener {
            courseBenefitSummaryArrow.changeState()
            val isExpanded = courseBenefitSummaryArrow.isExpanded()
            onCourseSummaryClicked(isExpanded)
            if (isExpanded) {
                courseBenefitSummaryInformationExpansion.expand()
            } else {
                courseBenefitSummaryInformationExpansion.collapse()
            }
        }
    }

    fun render(state: CourseBenefitSummaryFeature.State) {
        viewStateDelegate.switchState(state)
        if (state is CourseBenefitSummaryFeature.State.Content) {
            val currentMonthDate = DateTimeHelper.getPrintableDate(
                state.courseBenefitSummary.currentDate,
                DateTimeHelper.DISPLAY_MONTH_YEAR_NOMINAL_PATTERN,
                TimeZone.getDefault()
            ).capitalize(Locale.ROOT)

            val totalDate = DateTimeHelper.getPrintableDate(
                state.courseBenefitSummary.beginPaymentDate,
                DateTimeHelper.DISPLAY_MONTH_YEAR_GENITIVE_PATTERN,
                TimeZone.getDefault()
            ).capitalize(Locale.ROOT)

            courseBenefitCurrentEarningsTitle.text = context.getString(R.string.course_benefits_earning_current_month, currentMonthDate)
            courseBenefitCurrentEarningsValue.text = displayPriceMapper.mapToDisplayPrice(state.courseBenefitSummary.currencyCode, state.courseBenefitSummary.monthUserIncome)

            courseBenefitCurrentTurnoverTitle.text = context.getString(R.string.course_benefits_turnover_current_month, currentMonthDate)
            courseBenefitCurrentTurnoverValue.text = displayPriceMapper.mapToDisplayPrice(state.courseBenefitSummary.currencyCode, state.courseBenefitSummary.monthTurnover)

            courseBenefitTotalEarningsTitle.text = context.getString(R.string.course_benefits_earnings_total, totalDate)
            courseBenefitTotalEarningsValue.text = displayPriceMapper.mapToDisplayPrice(state.courseBenefitSummary.currencyCode, state.courseBenefitSummary.totalUserIncome)

            courseBenefitTotalTurnoverTitle.text = context.getString(R.string.course_beneifts_turnover_total, totalDate)
            courseBenefitTotalTurnoverValue.text = displayPriceMapper.mapToDisplayPrice(state.courseBenefitSummary.currencyCode, state.courseBenefitSummary.totalTurnover)
        }
    }
}