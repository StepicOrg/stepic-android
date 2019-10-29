package org.stepik.android.view.step_quiz_choice.ui.delegate

import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_step_quiz.view.*
import kotlinx.android.synthetic.main.layout_step_quiz_choice.view.*
import org.stepic.droid.R
import org.stepik.android.model.Reply
import org.stepik.android.presentation.step_quiz.StepQuizView
import org.stepik.android.presentation.step_quiz.model.ReplyResult
import org.stepik.android.view.step_quiz.resolver.StepQuizFormResolver
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFormDelegate
import org.stepik.android.view.step_quiz_choice.mapper.ChoiceStepQuizOptionsMapper
import org.stepik.android.view.step_quiz_choice.model.Choice
import org.stepik.android.view.step_quiz_choice.ui.adapter.ChoicesAdapterDelegate
import ru.nobird.android.ui.adapters.DefaultDelegateAdapter
import ru.nobird.android.ui.adapters.selection.MultipleChoiceSelectionHelper
import ru.nobird.android.ui.adapters.selection.SelectionHelper
import ru.nobird.android.ui.adapters.selection.SingleChoiceSelectionHelper

class ChoiceStepQuizFormDelegate(
    containerView: View
) : StepQuizFormDelegate {
    private val context = containerView.context

    private val quizDescription = containerView.stepQuizDescription
    private val choiceStepQuizOptionsMapper = ChoiceStepQuizOptionsMapper()
    private var choicesAdapter: DefaultDelegateAdapter<Choice> = DefaultDelegateAdapter()
    private lateinit var selectionHelper: SelectionHelper

    init {
        containerView.choicesRecycler.apply {
            itemAnimator = null
            adapter = choicesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
        }
    }

    override fun setState(state: StepQuizView.State.AttemptLoaded) {
        val dataset = state.attempt.dataset ?: return

        @StringRes
        val descriptionRes =
            if (dataset.isMultipleChoice) {
                R.string.step_quiz_choice_description_multiple
            } else {
                R.string.step_quiz_choice_description_single
            }
        quizDescription.setText(descriptionRes)

        val submission = (state.submissionState as? StepQuizView.SubmissionState.Loaded)
                ?.submission

        val reply = submission?.reply

        if (!::selectionHelper.isInitialized) {
            selectionHelper =
                if (dataset.isMultipleChoice) {
                    MultipleChoiceSelectionHelper(choicesAdapter)
                } else {
                    SingleChoiceSelectionHelper(choicesAdapter)
                }
            choicesAdapter += ChoicesAdapterDelegate(selectionHelper, onClick = ::handleChoiceClick)
        }

        choicesAdapter.items = choiceStepQuizOptionsMapper.mapChoices(
            dataset.options ?: emptyList(),
            reply?.choices,
            submission,
            StepQuizFormResolver.isQuizEnabled(state)
        )

        selectionHelper.reset()
        reply?.choices?.let {
            it.forEachIndexed { index, choice ->
                if (choice) {
                    selectionHelper.select(index)
                }
            }
        }
    }

    override fun createReply(): ReplyResult {
        val choices = (0 until choicesAdapter.itemCount).map { selectionHelper.isSelected(it) }
        return if ((true !in choices && selectionHelper is SingleChoiceSelectionHelper)) {
            ReplyResult.Error(context.getString(R.string.step_quiz_choice_empty_reply))
        } else {
            ReplyResult.Success(Reply(choices = choices))
        }
    }

    private fun handleChoiceClick(choice: Choice) {
        when (selectionHelper) {
            is SingleChoiceSelectionHelper ->
                selectionHelper.select(choicesAdapter.items.indexOf(choice))

            is MultipleChoiceSelectionHelper ->
                selectionHelper.toggle(choicesAdapter.items.indexOf(choice))
        }
    }
}