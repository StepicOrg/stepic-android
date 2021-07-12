package org.stepik.android.presentation.course_benefits

interface CourseBenefitsFeature {
    data class State(
        val courseBenefitState: CourseBenefitState,
        val courseBenefitSummaryState: CourseBenefitSummaryFeature.State
    )

    sealed class CourseBenefitState {
        object Idle : CourseBenefitState()
        object Loading : CourseBenefitState()
        object Error : CourseBenefitState()
        object Content : CourseBenefitState()
    }

    sealed class Message {
        data class InitMessage(val courseId: Long, val forceUpdate: Boolean = false) : Message()
        /**
         * Message Wrappers
         */
        data class CourseBenefitSummaryMessage(val message: CourseBenefitSummaryFeature.Message) : Message()
    }

    sealed class Action {
        sealed class ViewAction : Action()
        /**
         * Action Wrappers
         */
        data class CourseBenefitSummaryAction(val action: CourseBenefitSummaryFeature.Action) : Action()
    }
}