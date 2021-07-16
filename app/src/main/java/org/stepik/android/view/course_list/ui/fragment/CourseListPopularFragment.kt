package org.stepik.android.view.course_list.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_course_list.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.analytic.experiments.InAppPurchaseSplitTest
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepic.droid.ui.util.CoursesSnapHelper
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course_list.model.CourseListQuery
import org.stepik.android.domain.course_payments.mapper.DefaultPromoCodeMapper
import org.stepik.android.domain.filter.model.CourseListFilterQuery
import org.stepik.android.domain.last_step.model.LastStep
import org.stepik.android.model.Course
import org.stepik.android.presentation.course_continue.model.CourseContinueInteractionSource
import org.stepik.android.presentation.course_list.CourseListQueryPresenter
import org.stepik.android.presentation.course_list.CourseListQueryView
import org.stepik.android.presentation.course_list.CourseListView
import org.stepik.android.view.base.ui.adapter.layoutmanager.TableLayoutManager
import org.stepik.android.view.course.mapper.DisplayPriceMapper
import org.stepik.android.view.course_list.delegate.CourseContinueViewDelegate
import org.stepik.android.view.course_list.delegate.CourseListViewDelegate
import org.stepik.android.view.course_list.resolver.TableLayoutHorizontalSpanCountResolver
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.core.model.PaginationDirection
import ru.nobird.android.view.base.ui.extension.setOnPaginationListener
import javax.inject.Inject

class CourseListPopularFragment : Fragment(R.layout.item_course_list), CourseListQueryView {
    companion object {
        fun newInstance(): Fragment =
            CourseListPopularFragment()
    }

    @Inject
    internal lateinit var analytic: Analytic

    @Inject
    internal lateinit var screenManager: ScreenManager

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var sharedPreferenceHelper: SharedPreferenceHelper

    @Inject
    internal lateinit var inAppPurchaseSplitTest: InAppPurchaseSplitTest

    @Inject
    internal lateinit var defaultPromoCodeMapper: DefaultPromoCodeMapper

    @Inject
    internal lateinit var displayPriceMapper: DisplayPriceMapper

    @Inject
    internal lateinit var tableLayoutHorizontalSpanCountResolver: TableLayoutHorizontalSpanCountResolver

    private lateinit var courseListViewDelegate: CourseListViewDelegate
    private val courseListQueryPresenter: CourseListQueryPresenter by viewModels { viewModelFactory }
    private lateinit var tableLayoutManager: TableLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerCarouselCount.isVisible = false
        containerTitle.text = resources.getString(R.string.course_list_popular_toolbar_title)

        val rowCount = resources.getInteger(R.integer.course_list_rows)
        val columnsCount = resources.getInteger(R.integer.course_list_columns)
        tableLayoutManager = TableLayoutManager(requireContext(), columnsCount, rowCount, RecyclerView.HORIZONTAL, false)

        with(courseListCoursesRecycler) {
            layoutManager = tableLayoutManager
            itemAnimator?.changeDuration = 0
            val snapHelper = CoursesSnapHelper(rowCount)
            snapHelper.attachToRecyclerView(this)
            setOnPaginationListener { pageDirection ->
                if (pageDirection == PaginationDirection.NEXT) {
                    courseListQueryPresenter.fetchNextPage()
                }
            }
        }

        val courseListQuery = CourseListQuery(
            page = 1,
            order = CourseListQuery.Order.ACTIVITY_DESC,
            isCataloged = true,
            filterQuery = CourseListFilterQuery(language = sharedPreferenceHelper.languageForFeatured)
        )

        catalogBlockContainer.setOnClickListener {
            screenManager.showCoursesByQuery(
                requireContext(),
                resources.getString(R.string.course_list_popular_toolbar_title),
                courseListQuery
            )
        }

        courseListPlaceholderEmpty.setOnClickListener { screenManager.showCatalog(requireContext()) }
        courseListPlaceholderEmpty.setPlaceholderText(R.string.empty_courses_popular)
        courseListPlaceholderNoConnection.setOnClickListener { courseListQueryPresenter.fetchCourses(courseListQuery = courseListQuery, forceUpdate = true) }
        courseListPlaceholderNoConnection.setText(R.string.internet_problem)

        val viewStateDelegate = ViewStateDelegate<CourseListView.State>()

        viewStateDelegate.addState<CourseListView.State.Idle>(catalogBlockContainer)
        viewStateDelegate.addState<CourseListView.State.Loading>(catalogBlockContainer, courseListCoursesRecycler)
        viewStateDelegate.addState<CourseListView.State.Content>(catalogBlockContainer, courseListCoursesRecycler)
        viewStateDelegate.addState<CourseListView.State.Empty>(courseListPlaceholderEmpty)
        viewStateDelegate.addState<CourseListView.State.NetworkError>(courseListPlaceholderNoConnection)

        courseListViewDelegate = CourseListViewDelegate(
            analytic = analytic,
            courseContinueViewDelegate = CourseContinueViewDelegate(
                activity = requireActivity(),
                analytic = analytic,
                screenManager = screenManager
            ),
            courseListTitleContainer = catalogBlockContainer,
            courseItemsRecyclerView = courseListCoursesRecycler,
            courseListViewStateDelegate = viewStateDelegate,
            onContinueCourseClicked = { courseListItem ->
                courseListQueryPresenter
                    .continueCourse(
                        course = courseListItem.course,
                        viewSource = CourseViewSource.Query(courseListQuery),
                        interactionSource = CourseContinueInteractionSource.COURSE_WIDGET
                    )
            },
            isHandleInAppPurchase = inAppPurchaseSplitTest.currentGroup.isInAppPurchaseActive,
            defaultPromoCodeMapper = defaultPromoCodeMapper,
            displayPriceMapper = displayPriceMapper
        )

        courseListQueryPresenter.fetchCourses(courseListQuery)
    }

    private fun injectComponent() {
        App.component()
            .courseListQueryComponentBuilder()
            .build()
            .inject(this)
    }

    override fun setState(state: CourseListQueryView.State) {
        val courseListState = (state as? CourseListQueryView.State.Data)?.courseListViewState ?: CourseListView.State.Idle
        if (courseListState == CourseListView.State.Empty) {
            analytic.reportEvent(Analytic.Error.FEATURED_EMPTY)
        }
        if (courseListState is CourseListView.State.Content) {
            tableLayoutHorizontalSpanCountResolver.resolveSpanCount(courseListState.courseListDataItems.size).let { resolvedSpanCount ->
                if (tableLayoutManager.spanCount != resolvedSpanCount) {
                    tableLayoutManager.spanCount = resolvedSpanCount
                }
            }
        }
        courseListViewDelegate.setState(courseListState)
    }

    override fun showCourse(course: Course, source: CourseViewSource, isAdaptive: Boolean) {
        courseListViewDelegate.showCourse(course, source, isAdaptive)
    }

    override fun showSteps(course: Course, source: CourseViewSource, lastStep: LastStep) {
        courseListViewDelegate.showSteps(course, source, lastStep)
    }

    override fun setBlockingLoading(isLoading: Boolean) {
        courseListViewDelegate.setBlockingLoading(isLoading)
    }

    override fun showNetworkError() {
        courseListViewDelegate.showNetworkError()
    }

    override fun onStart() {
        super.onStart()
        courseListQueryPresenter.attachView(this)
    }

    override fun onStop() {
        courseListQueryPresenter.detachView(this)
        super.onStop()
    }
}