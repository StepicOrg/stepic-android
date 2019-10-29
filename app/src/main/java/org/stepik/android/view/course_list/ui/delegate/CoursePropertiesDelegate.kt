package org.stepik.android.view.course_list.ui.delegate

import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.layout_course_properties.view.*
import org.stepic.droid.R
import org.stepic.droid.util.safeDiv
import org.stepik.android.model.Course
import java.util.Locale

class CoursePropertiesDelegate(
    private val view: ViewGroup
) {
    private val learnersCountImage = view.learnersCountImage
    private val learnersCountText = view.learnersCountText

    private val courseItemProgress = view.courseItemProgressView
    private val courseItemProgressTitle = view.courseItemProgressTitle

    private val courseRatingImage = view.courseRatingImage
    private val courseRatingText = view.courseRatingText

    fun setTextColor(@ColorInt color: Int) {
        learnersCountText.setTextColor(color)
        learnersCountImage.setColorFilter(color)
        courseRatingText.setTextColor(color)
        courseRatingImage.setColorFilter(color)
        courseItemProgress.backgroundPaintColor = color
        courseItemProgressTitle.setTextColor(color)
    }

    fun setStats(course: Course) {
        setLearnersCount(course.learnersCount)
        setProgress(course)
        setRating(course)

        view.isVisible = view.children.any(View::isVisible)
    }

    private fun setLearnersCount(learnersCount: Long) {
        val needShowLearners = learnersCount > 0
        if (needShowLearners) {
            learnersCountText.text = String.format(Locale.getDefault(), "%d", learnersCount)
        }
        learnersCountImage.isVisible = needShowLearners
        learnersCountText.isVisible = needShowLearners
    }

    private fun setProgress(course: Course) {
        val progress = course.progressObject
        val needShow = if (progress != null && progress.cost > 0) {
            val score = progress
                .score
                ?.toFloatOrNull()
                ?.toLong()
                ?: 0L

            prepareViewForProgress(score, progress.cost)
            true
        } else {
            false
        }
        courseItemProgress.isVisible = needShow
        courseItemProgressTitle.isVisible = needShow
    }

    private fun prepareViewForProgress(score: Long, cost: Long) {
        courseItemProgress.progress = (score * 100 safeDiv cost) / 100f
        courseItemProgressTitle.text = view
            .resources
            .getString(R.string.course_content_text_progress, score, cost)
    }

    private fun setRating(course: Course) {
        val needShow = course.rating > 0
        if (needShow) {
            courseRatingText.text = String.format(Locale.ROOT, view.resources.getString(R.string.course_rating_value), course.rating)
        }
        courseRatingImage.isVisible = needShow
        courseRatingText.isVisible = needShow
    }
}