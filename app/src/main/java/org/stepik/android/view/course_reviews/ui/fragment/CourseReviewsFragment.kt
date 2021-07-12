package org.stepik.android.view.course_reviews.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.empty_default.*
import kotlinx.android.synthetic.main.empty_default.view.*
import kotlinx.android.synthetic.main.error_no_connection.*
import kotlinx.android.synthetic.main.fragment_course_reviews.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.ui.util.snackbar
import org.stepik.android.domain.course_reviews.model.CourseReview
import org.stepik.android.domain.course_reviews.model.CourseReviewItem
import org.stepik.android.presentation.course_reviews.CourseReviewsPresenter
import org.stepik.android.presentation.course_reviews.CourseReviewsView
import org.stepik.android.view.course_reviews.ui.adapter.delegates.CourseReviewDataDelegate
import org.stepik.android.view.course_reviews.ui.adapter.delegates.CourseReviewPlaceholderDelegate
import org.stepik.android.view.course_reviews.ui.adapter.delegates.CourseReviewSummaryDelegate
import org.stepik.android.view.course_reviews.ui.adapter.delegates.CourseReviewsComposeBannerDelegate
import org.stepik.android.view.course_reviews.ui.dialog.ComposeCourseReviewDialogFragment
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.ui.adapters.DefaultDelegateAdapter
import ru.nobird.android.view.base.ui.extension.argument
import ru.nobird.android.view.base.ui.extension.showIfNotExists
import javax.inject.Inject

class CourseReviewsFragment : Fragment(), CourseReviewsView {
    companion object {
        fun newInstance(courseId: Long): Fragment =
            CourseReviewsFragment().apply {
                this.courseId = courseId
            }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var screenManager: ScreenManager

    @Inject
    internal lateinit var analytic: Analytic

    private var courseId: Long by argument()

    private lateinit var courseReviewsAdapter: DefaultDelegateAdapter<CourseReviewItem>
    private lateinit var viewStateDelegate: ViewStateDelegate<CourseReviewsView.State>

    private val courseReviewsPresenter: CourseReviewsPresenter by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent(courseId)

        courseReviewsAdapter = DefaultDelegateAdapter()
        courseReviewsAdapter +=
            CourseReviewDataDelegate(
                onUserClicked = { screenManager.openProfile(requireContext(), it.id) },
                onEditReviewClicked = ::showCourseReviewEditDialog,
                onRemoveReviewClicked = courseReviewsPresenter::removeCourseReview
            )
        courseReviewsAdapter += CourseReviewPlaceholderDelegate()
        courseReviewsAdapter += CourseReviewSummaryDelegate()
        courseReviewsAdapter +=
            CourseReviewsComposeBannerDelegate { showCourseReviewEditDialog(null) }
    }

    private fun injectComponent(courseId: Long) {
        App.componentManager()
            .courseComponent(courseId)
            .inject(this)
    }

    private fun releaseComponent(courseId: Long) {
        App.componentManager()
            .releaseCourseComponent(courseId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_course_reviews, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(courseReviewsRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = courseReviewsAdapter

            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(context, R.drawable.bg_divider_vertical)?.let(::setDrawable)
            })

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager)
                        ?: return

                    if (dy > 0) {
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                        if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                            post {
                                courseReviewsPresenter.fetchNextPageFromRemote()
                            }
                        }
                    }
                }
            })
        }

        report_empty
            .placeholderMessage
            .setText(R.string.course_reviews_empty)

        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<CourseReviewsView.State.Idle>(courseReviewsPlaceholder)
        viewStateDelegate.addState<CourseReviewsView.State.Loading>(courseReviewsPlaceholder)
        viewStateDelegate.addState<CourseReviewsView.State.CourseReviews>(courseReviewsRecycler)
        viewStateDelegate.addState<CourseReviewsView.State.CourseReviewsLoading>(courseReviewsRecycler)
        viewStateDelegate.addState<CourseReviewsView.State.NetworkError>(reportProblem)
        viewStateDelegate.addState<CourseReviewsView.State.EmptyContent>(report_empty)
    }

    override fun onStart() {
        super.onStart()
        courseReviewsPresenter.attachView(this)
    }

    override fun onResume() {
        super.onResume()
        courseReviewsPresenter.fetchNextPageFromRemote(isFromOnResume = true)
    }

    override fun onStop() {
        courseReviewsPresenter.detachView(this)
        super.onStop()
    }

    override fun setState(state: CourseReviewsView.State) {
        viewStateDelegate.switchState(state)
        when (state) {
            is CourseReviewsView.State.CourseReviews ->
                courseReviewsAdapter.items = state.courseReviewItems

            is CourseReviewsView.State.CourseReviewsLoading ->
                courseReviewsAdapter.items = state.courseReviewItems + CourseReviewItem.Placeholder()
        }
    }

    override fun showNetworkError() {
        view?.snackbar(messageRes = R.string.connectionProblems)
    }

    private fun showCourseReviewEditDialog(courseReview: CourseReview?) {
        val supportFragmentManager = activity
            ?.supportFragmentManager
            ?: return

        val requestCode =
            if (courseReview == null) {
                ComposeCourseReviewDialogFragment.CREATE_REVIEW_REQUEST_CODE
            } else {
                ComposeCourseReviewDialogFragment.EDIT_REVIEW_REQUEST_CODE
            }

        val dialog = ComposeCourseReviewDialogFragment.newInstance(courseId, courseReview)
        dialog.setTargetFragment(this, requestCode)
        dialog.showIfNotExists(supportFragmentManager, ComposeCourseReviewDialogFragment.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ComposeCourseReviewDialogFragment.CREATE_REVIEW_REQUEST_CODE ->
                data?.takeIf { resultCode == Activity.RESULT_OK }
                    ?.getParcelableExtra<CourseReview>(ComposeCourseReviewDialogFragment.ARG_COURSE_REVIEW)
                    ?.let(courseReviewsPresenter::onCourseReviewCreated)

            ComposeCourseReviewDialogFragment.EDIT_REVIEW_REQUEST_CODE ->
                data?.takeIf { resultCode == Activity.RESULT_OK }
                    ?.getParcelableExtra<CourseReview>(ComposeCourseReviewDialogFragment.ARG_COURSE_REVIEW)
                    ?.let(courseReviewsPresenter::onCourseReviewUpdated)

            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        releaseComponent(courseId)
        super.onDestroy()
    }
}