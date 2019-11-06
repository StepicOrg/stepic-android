package org.stepik.android.view.submission.ui.adapter.delegate

import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.android.synthetic.main.item_submission_data.view.*
import org.stepic.droid.R
import org.stepic.droid.ui.util.setCompoundDrawables
import org.stepic.droid.ui.util.wrapWithGlide
import org.stepic.droid.util.DateTimeHelper
import org.stepik.android.domain.submission.model.SubmissionItem
import org.stepik.android.model.Submission
import org.stepik.android.model.user.User
import org.stepik.android.view.base.ui.mapper.DateMapper
import ru.nobird.android.ui.adapterdelegates.AdapterDelegate
import ru.nobird.android.ui.adapterdelegates.DelegateViewHolder

class SubmissionDataAdapterDelegate(
    private val actionListener: ActionListener
) : AdapterDelegate<SubmissionItem, DelegateViewHolder<SubmissionItem>>() {
    override fun onCreateViewHolder(parent: ViewGroup): DelegateViewHolder<SubmissionItem> =
        ViewHolder(createView(parent, R.layout.item_submission_data))

    override fun isForViewType(position: Int, data: SubmissionItem): Boolean =
        data is SubmissionItem.Data

    private inner class ViewHolder(root: View) : DelegateViewHolder<SubmissionItem>(root), View.OnClickListener {
        private val submissionUserIcon = root.submissionUserIcon
        private val submissionUserIconWrapper = submissionUserIcon.wrapWithGlide()
        private val submissionUserName = root.submissionUserName

        private val submissionTime = root.submissionTime
        private val submissionSolution = root.submissionSolution

        private val submissionUserIconPlaceholder = with(context.resources) {
            val coursePlaceholderBitmap = BitmapFactory.decodeResource(this, R.drawable.general_placeholder)
            val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(this, coursePlaceholderBitmap)
            circularBitmapDrawable.cornerRadius = getDimension(R.dimen.course_image_radius)
            circularBitmapDrawable
        }

        init {
            submissionSolution.setOnClickListener(this)

            submissionUserIcon.setOnClickListener(this)
            submissionUserName.setOnClickListener(this)
        }
        override fun onBind(data: SubmissionItem) {
            data as SubmissionItem.Data

            submissionUserName.text = data.user.fullName
            submissionUserIconWrapper.setImagePath(data.user.avatar ?: "", submissionUserIconPlaceholder)
            submissionSolution.text = context.getString(R.string.comment_solution_pattern, data.submission.id)

            submissionTime.text = DateMapper.mapToRelativeDate(context, DateTimeHelper.nowUtc(), data.submission.time?.time ?: 0)

            @DrawableRes
            val compoundDrawableRes =
                when (data.submission.status) {
                    Submission.Status.CORRECT ->
                        R.drawable.ic_step_quiz_correct

                    Submission.Status.WRONG ->
                        R.drawable.ic_step_quiz_wrong_wide

                    else ->
                        -1
                }
            submissionSolution.setCompoundDrawables(start = compoundDrawableRes)
        }

        override fun onClick(view: View) {
            val dataItem = itemData as? SubmissionItem.Data
                ?: return

            when (view.id) {
                R.id.submissionUserIcon,
                R.id.submissionUserName ->
                    actionListener.onUserClicked(dataItem.user)

                R.id.submissionSolution ->
                    actionListener.onSubmissionClicked(dataItem)
            }
        }
    }

    interface ActionListener {
        fun onUserClicked(user: User)
        fun onSubmissionClicked(data: SubmissionItem.Data)
    }
}