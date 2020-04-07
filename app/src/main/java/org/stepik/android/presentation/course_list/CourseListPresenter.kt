package org.stepik.android.presentation.course_list

import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepik.android.domain.course_list.interactor.CourseListInteractor
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.domain.course_list.model.CourseListQuery
import org.stepik.android.presentation.course_continue.delegate.CourseContinuePresenterDelegate
import org.stepik.android.presentation.course_continue.delegate.CourseContinuePresenterDelegateImpl
import org.stepik.android.presentation.course_list.mapper.CourseListStateMapper
import ru.nobird.android.presentation.base.PresenterBase
import ru.nobird.android.presentation.base.PresenterViewContainer
import ru.nobird.android.presentation.base.delegate.PresenterDelegate
import javax.inject.Inject

class CourseListPresenter
@Inject
constructor(
    private val courseListStateMapper: CourseListStateMapper,
    private val courseListInteractor: CourseListInteractor,
    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler,

    viewContainer: PresenterViewContainer<CourseListView>,
    continueCoursePresenterDelegate: CourseContinuePresenterDelegateImpl
) : PresenterBase<CourseListView>(viewContainer), CourseContinuePresenterDelegate by continueCoursePresenterDelegate {

    override val delegates: List<PresenterDelegate<in CourseListView>> =
        listOf(continueCoursePresenterDelegate)

    private var state: CourseListView.State = CourseListView.State.Idle
        set(value) {
            field = value
            view?.setState(value)
        }

    private var courseListQuery: CourseListQuery? = null

    private val paginationDisposable = CompositeDisposable()

    init {
        compositeDisposable += paginationDisposable
    }

    override fun attachView(view: CourseListView) {
        super.attachView(view)
        view.setState(state)
    }

    fun fetchCourses(courseListQuery: CourseListQuery, forceUpdate: Boolean = false) {
        if (state != CourseListView.State.Idle && !forceUpdate) return

        paginationDisposable.clear()

        val oldState = state

        state = CourseListView.State.Loading()
        this.courseListQuery = courseListQuery

        paginationDisposable += courseListInteractor
            .getCourseListItems(courseListQuery)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onSuccess = {
                    state = if (it.isNotEmpty()) {
                        CourseListView.State.Content(
                            courseListDataItems = it,
                            courseListItems = it
                        )
                    } else {
                        CourseListView.State.Empty
                    }
                },
                onError = {
                    when (oldState) {
                        is CourseListView.State.Content -> {
                            state = oldState
                            view?.showNetworkError()
                        }
                        else ->
                            state = CourseListView.State.NetworkError
                    }
                }
            )
    }

    fun fetchNextPage() {
        val oldState = state as? CourseListView.State.Content
            ?: return

        val oldCourseListQuery = courseListQuery ?: return

        if (oldState.courseListItems.last() is CourseListItem.PlaceHolder ||
            !oldState.courseListDataItems.hasNext) {
            return
        }

        val nextPage = oldState.courseListDataItems.page + 1

        val courseListQuery = oldCourseListQuery.copy(page = nextPage)

        state = courseListStateMapper.mapToLoadMoreState(oldState)
        paginationDisposable += courseListInteractor
            .getCourseListItems(courseListQuery)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onSuccess = {
                    state = courseListStateMapper.mapFromLoadMoreToSuccess(state, it)
                },
                onError = {
                    state = courseListStateMapper.mapFromLoadMoreToError(state)
                    view?.showNetworkError()
                }
            )
    }
}