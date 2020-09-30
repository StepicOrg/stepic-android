package org.stepik.android.view.step_quiz_review.ui.delegate

import android.view.View
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_step_quiz_review_peer.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_footer.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_header.*
import org.stepic.droid.R
import org.stepik.android.model.ReviewStrategyType
import org.stepik.android.model.Submission
import org.stepik.android.presentation.step_quiz.StepQuizView
import org.stepik.android.presentation.step_quiz_review.StepQuizReviewView
import org.stepik.android.view.progress.ui.mapper.ProgressTextMapper
import org.stepik.android.view.step_quiz_review.ui.widget.ReviewStatusView
import ru.nobird.android.core.model.safeCast


class StepQuizReviewDelegate(
    override val containerView: View,
    private val instructionType: ReviewStrategyType
) : LayoutContainer {
    private val resources = containerView.resources

    fun render(state: StepQuizReviewView.State) {
        renderStep1(state)
        renderStep2(state)

        if (instructionType == ReviewStrategyType.PEER) {
            renderStep3(state)
            renderStep4(state)
        }

        renderStep5(state)
    }

    private fun renderStep1(state: StepQuizReviewView.State) {
        // todo work with quiz
        when (state) {
            is StepQuizReviewView.State.SubmissionNotMade -> {
                val submissionStatus = state.quizState.safeCast<StepQuizView.State.AttemptLoaded>()
                    ?.submissionState
                    ?.safeCast<StepQuizView.SubmissionState.Loaded>()
                    ?.submission
                    ?.status

                reviewStep1Status.status =
                    if (submissionStatus == Submission.Status.WRONG) {
                        ReviewStatusView.Status.ERROR
                    } else {
                        ReviewStatusView.Status.IN_PROGRESS
                    }

                reviewStep1Title.isEnabled = true
            }

            else -> {
                reviewStep1Title.isEnabled = false
                reviewStep1Status.status = ReviewStatusView.Status.COMPLETED
            }
        }
    }

    private fun renderStep2(state: StepQuizReviewView.State) {
        // todo work with quiz
        when (state) {
            is StepQuizReviewView.State.SubmissionNotMade -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_pending)
                reviewStep2Title.isEnabled = false
                reviewStep2Link.isEnabled = false
                reviewStep2Status.status = ReviewStatusView.Status.PENDING
            }
            is StepQuizReviewView.State.SubmissionNotSelected -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_in_progress)
                reviewStep2Title.isEnabled = true
                reviewStep2Link.isEnabled = true
                reviewStep2Status.status = ReviewStatusView.Status.IN_PROGRESS
            }
            else -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_completed)
                reviewStep2Title.isEnabled = false
                reviewStep2Link.isEnabled = true
                reviewStep2Status.status = ReviewStatusView.Status.COMPLETED
            }
        }
    }

    private fun renderStep3(state: StepQuizReviewView.State) {
        // todo reduce duplication
        val reviewCount = state.safeCast<StepQuizReviewView.State.WithInstruction>()?.instruction?.minReviews ?: 0

        when (state) {
            is StepQuizReviewView.State.SubmissionNotMade,
            is StepQuizReviewView.State.SubmissionNotSelected,
            is StepQuizReviewView.State.SubmissionSelectedLoading -> {
                reviewStep3Title.setText(R.string.step_quiz_review_given_pending_zero)
                reviewStep3Title.isEnabled = false
                reviewStep3Link.isEnabled = false
                reviewStep3Status.status = ReviewStatusView.Status.PENDING
            }
            is StepQuizReviewView.State.SubmissionSelected -> {
                val givenReviewCount = state.session.givenReviews.size
                val remainingReviewCount = reviewCount - givenReviewCount

                val text =
                    buildString {
                        if (remainingReviewCount > 0) {
                            @PluralsRes
                            val pluralRes =
                                if (givenReviewCount > 0) {
                                    R.plurals.step_quiz_review_given_in_progress
                                } else {
                                    R.plurals.step_quiz_review_given_pending
                                }
                            append(resources.getQuantityString(pluralRes, remainingReviewCount, remainingReviewCount))
                        }

                        if (givenReviewCount > 0) {
                            if (isNotEmpty()) {
                                append(" ")
                            }
                            append(resources.getQuantityString(R.plurals.step_quiz_review_given_completed, givenReviewCount, givenReviewCount))
                        }
                    }

                reviewStep3Title.text = text
                reviewStep3Title.isEnabled = true
                reviewStep3Link.isEnabled = true
                reviewStep3Status.status = ReviewStatusView.Status.IN_PROGRESS
            }
            is StepQuizReviewView.State.Completed -> {
                val givenReviewCount = state.session.givenReviews.size

                reviewStep3Title.text = resources.getQuantityString(R.plurals.step_quiz_review_given_completed, givenReviewCount, givenReviewCount)
                reviewStep3Title.isEnabled = false
                reviewStep3Link.isEnabled = true
                reviewStep3Status.status = ReviewStatusView.Status.COMPLETED
            }
        }
    }

    private fun renderStep4(state: StepQuizReviewView.State) {
        val reviewCount = state.safeCast<StepQuizReviewView.State.WithInstruction>()?.instruction?.minReviews ?: 0

        when (state) {
            is StepQuizReviewView.State.SubmissionNotMade,
            is StepQuizReviewView.State.SubmissionNotSelected,
            is StepQuizReviewView.State.SubmissionSelectedLoading -> {
                reviewStep3Title.setText(R.string.step_quiz_review_taken_pending_zero)
                reviewStep4Title.isEnabled = false
                reviewStep4Link.isEnabled = false
                reviewStep4Status.status = ReviewStatusView.Status.PENDING
            }
            is StepQuizReviewView.State.SubmissionSelected -> {
                val takenReviewCount = state.session.takenReviews.size
                val remainingReviewCount = reviewCount - takenReviewCount

                val text =
                    buildString {
                        if (remainingReviewCount > 0) {
                            @PluralsRes
                            val pluralRes =
                                if (takenReviewCount > 0) {
                                    R.plurals.step_quiz_review_taken_in_progress
                                } else {
                                    R.plurals.step_quiz_review_taken_pending
                                }
                            append(resources.getQuantityString(pluralRes, remainingReviewCount, remainingReviewCount))
                        }

                        if (takenReviewCount > 0) {
                            if (isNotEmpty()) {
                                append(" ")
                            }
                            append(resources.getQuantityString(R.plurals.step_quiz_review_taken_completed, takenReviewCount, takenReviewCount))
                        }
                    }

                reviewStep4Title.text = text
                reviewStep4Title.isEnabled = true
                reviewStep4Link.isEnabled = true
                reviewStep4Status.status = ReviewStatusView.Status.IN_PROGRESS
            }
            is StepQuizReviewView.State.Completed -> {
                val takenReviewCount = state.session.takenReviews.size
                reviewStep4Title.text = resources.getQuantityString(R.plurals.step_quiz_review_taken_completed, takenReviewCount, takenReviewCount)
                reviewStep4Title.isEnabled = false
                reviewStep4Link.isEnabled = true
                reviewStep4Status.status = ReviewStatusView.Status.COMPLETED
            }
        }
    }

    private fun renderStep5(state: StepQuizReviewView.State) {
        reviewStep5Status.position =
            when (instructionType) {
                ReviewStrategyType.PEER -> 5
                ReviewStrategyType.INSTRUCTOR -> 3
            }

        when (state) {
            is StepQuizReviewView.State.Completed -> {
                val receivedPoints = state.progress?.score?.toFloatOrNull() ?: 0f

                reviewStep5Title.text = ProgressTextMapper
                    .mapProgressToText(
                        containerView.context,
                        receivedPoints,
                        state.progress?.cost ?: 0,
                        R.string.step_quiz_review_peer_completed,
                        R.string.step_quiz_review_peer_completed,
                        R.plurals.points
                    )
                reviewStep5Title.isEnabled = true
                reviewStep5Link.isEnabled = true
                reviewStep5Status.status = ReviewStatusView.Status.COMPLETED
            }
            else -> {
                val cost = state.safeCast<StepQuizReviewView.State.WithProgress>()?.progress?.cost ?: 0L

                @StringRes
                val stringRes =
                    when (instructionType) {
                        ReviewStrategyType.PEER ->
                            R.string.step_quiz_review_peer_pending

                        ReviewStrategyType.INSTRUCTOR ->
                            R.string.step_quiz_review_instructor_pending
                    }

                reviewStep5Title.text = resources.getString(stringRes, resources.getQuantityString(R.plurals.points, cost.toInt(), cost))

                val inProgress =
                    state is StepQuizReviewView.State.SubmissionSelected && instructionType == ReviewStrategyType.INSTRUCTOR

                reviewStep5Title.isEnabled = inProgress
                reviewStep5Link.isEnabled = inProgress
                reviewStep5Status.status = if (inProgress) ReviewStatusView.Status.IN_PROGRESS else ReviewStatusView.Status.PENDING
            }
        }
    }
}