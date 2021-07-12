package org.stepik.android.view.injection.course_benefits

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.stepik.android.presentation.base.injection.ViewModelKey
import org.stepik.android.presentation.course_benefits.CourseBenefitSummaryFeature
import org.stepik.android.presentation.course_benefits.CourseBenefitsFeature
import org.stepik.android.presentation.course_benefits.CourseBenefitsViewModel
import org.stepik.android.presentation.course_benefits.dispatcher.CourseBenefitSummaryActionDispatcher
import org.stepik.android.presentation.course_benefits.reducer.CourseBenefitsReducer
import ru.nobird.android.core.model.safeCast
import ru.nobird.android.presentation.redux.container.wrapWithViewContainer
import ru.nobird.android.presentation.redux.dispatcher.transform
import ru.nobird.android.presentation.redux.dispatcher.wrapWithActionDispatcher
import ru.nobird.android.presentation.redux.feature.ReduxFeature

@Module
object CourseBenefitsPresentationModule {
    /**
     * Presentation
     */
    @Provides
    @IntoMap
    @ViewModelKey(CourseBenefitsViewModel::class)
    internal fun provideCourseBenefitsPresenter(
        courseBenefitsReducer: CourseBenefitsReducer,
        courseBenefitSummaryActionDispatcher: CourseBenefitSummaryActionDispatcher
    ): ViewModel =
        CourseBenefitsViewModel(
            ReduxFeature(CourseBenefitsFeature.State(
                courseBenefitState = CourseBenefitsFeature.CourseBenefitState.Idle,
                courseBenefitSummaryState = CourseBenefitSummaryFeature.State.Loading
            ), courseBenefitsReducer)
                .wrapWithActionDispatcher(
                    courseBenefitSummaryActionDispatcher.transform(
                        transformAction = { it.safeCast<CourseBenefitsFeature.Action.CourseBenefitSummaryAction>()?.action },
                        transformMessage = CourseBenefitsFeature.Message::CourseBenefitSummaryMessage
                    )
                )
                .wrapWithViewContainer()
        )
}