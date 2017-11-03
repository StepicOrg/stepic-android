package org.stepic.droid.core.presenters

import android.support.annotation.WorkerThread
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.concurrency.MainHandler
import org.stepic.droid.concurrency.SingleThreadExecutor
import org.stepic.droid.core.FilterApplicator
import org.stepic.droid.core.presenters.contracts.CoursesView
import org.stepic.droid.di.course_list.CourseListScope
import org.stepic.droid.model.Course
import org.stepic.droid.model.Progress
import org.stepic.droid.model.StepikFilter
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepic.droid.storage.operations.DatabaseFacade
import org.stepic.droid.storage.operations.Table
import org.stepic.droid.util.RWLocks
import org.stepic.droid.web.Api
import org.stepic.droid.web.CoursesStepicResponse
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@CourseListScope
class PersistentCourseListPresenter
@Inject constructor(
        private val analytic: Analytic,
        private val databaseFacade: DatabaseFacade,
        private val singleThreadExecutor: SingleThreadExecutor,
        private val mainHandler: MainHandler,
        private val api: Api,
        private val filterApplicator: FilterApplicator,
        private val sharedPreferenceHelper: SharedPreferenceHelper
) : PresenterBase<CoursesView>() {

    companion object {
        //if hasNextPage & <MIN_COURSES_ON_SCREEN -> load next page
        private const val MIN_COURSES_ON_SCREEN = 5
        private const val MAX_CURRENT_NUMBER_OF_TASKS = 2
    }

    private val currentPage = AtomicInteger(1);
    private val hasNextPage = AtomicBoolean(true)
    private var currentNumberOfTasks: Int = 0 //only main thread
    private val isEmptyCourses = AtomicBoolean(false)

    fun restoreState() {
        if (isEmptyCourses.get() && !hasNextPage.get()) {
            view?.showEmptyCourses()
        }
    }

    /**
     * 1) Show from cache, if not empty (hide progress).
     * 2) Load from internet (if fail -> show)
     * 3) Save to db
     * 4) show from cache. (all states)
     */
    fun downloadData(courseType: Table, applyFilter: Boolean) {
        downloadData(courseType, applyFilter, isRefreshing = false)
    }

    private fun downloadData(courseType: Table, applyFilter: Boolean, isRefreshing: Boolean, isLoadMore: Boolean = false) {
        if (currentNumberOfTasks >= MAX_CURRENT_NUMBER_OF_TASKS) {
            return
        }
        currentNumberOfTasks++
        Timber.d("load more tasks = $currentNumberOfTasks") //here 1 or 2, not more
        if (hasNextPage.get()) {
            view?.showLoading()
        }
        singleThreadExecutor.execute {
            try {
                Timber.d("load more start downloading ${Thread.currentThread()}")
                downloadDataPlain(isRefreshing, isLoadMore, applyFilter, courseType)
                Timber.d("load more end downloading ${Thread.currentThread()}")
            } finally {
                mainHandler.post {
                    currentNumberOfTasks--
                }
            }
        }
    }

    @WorkerThread
    private fun downloadDataPlain(isRefreshing: Boolean, isLoadMore: Boolean, applyFilter: Boolean, courseType: Table) {
        if (!isRefreshing && !isLoadMore) {
            getFromDatabaseAndShow(applyFilter, courseType)
        } else if (hasNextPage.get()) {
            mainHandler.post {
                view?.showLoading()
            }
        }

        while (hasNextPage.get()) {
            val coursesFromInternet: List<Course>? = try {
                if (courseType == Table.featured) {
                    val response = api.getPopularCourses(currentPage.get()).blockingGet()
                    handleMeta(response)
                    response.courses
                } else {
                    val allMyCourses = arrayListOf<Course>()
                    while (hasNextPage.get()) {
                        val originalResponse = api.getEnrolledCourses(currentPage.get()).blockingGet()
                        allMyCourses.addAll(originalResponse.courses)
                        handleMeta(originalResponse)
                    }
                    allMyCourses
                }
            } catch (ex: Exception) {
                null
            }?.distinctBy { it.courseId }

            if (coursesFromInternet == null) {
                mainHandler.post {
                    view?.showConnectionProblem()
                }
                break
            }

            if (courseType == Table.enrolled) {
                val progressIds = coursesFromInternet.map { it.progress }.toTypedArray()
                val progresses: List<Progress>? = try {
                    api.getProgresses(progressIds).execute().body()?.progresses
                } catch (exception: Exception) {
                    //ok show without progresses
                    null
                }
                progresses?.forEach {
                    databaseFacade.addProgress(progress = it)
                }
            }

            try {
                //this lock need for not saving enrolled courses to database after user click logout
                RWLocks.ClearEnrollmentsLock.writeLock().lock()
                if (sharedPreferenceHelper.authResponseFromStore != null || courseType == Table.featured) {
                    if (isRefreshing && currentPage.get() == 1) {
                        if (courseType == Table.featured) {
                            databaseFacade.dropFeaturedCourses()
                        } else if (courseType == Table.enrolled) {
                            databaseFacade.dropEnrolledCourses()
                        }
                    }

                    coursesFromInternet.forEach {
                        databaseFacade.addCourse(it, courseType)
                    }
                }
            } finally {
                RWLocks.ClearEnrollmentsLock.writeLock().unlock()
            }

            val allCourses = databaseFacade.getAllCourses(courseType).filterNotNull().toMutableList()

            val filteredCourseList: MutableList<Course> =
                    if (courseType == Table.featured) {
                        if (!applyFilter && !sharedPreferenceHelper.filterForFeatured.contains(StepikFilter.PERSISTENT)) {
                            filterApplicator.getFilteredFeaturedFromDefault(allCourses)
                        } else {
                            filterApplicator.getFilteredFeaturedFromSharedPrefs(allCourses)
                        }
                    } else {
                        allCourses
                    }
            if ((filteredCourseList.size < MIN_COURSES_ON_SCREEN) && hasNextPage.get()) {
                //try to load next in loop
            } else {
                val progressesMap = getProgressesFromDb(filteredCourseList)
                val coursesForShow = when (courseType) {
                    Table.enrolled -> {
                        //progresses should be shown only for enrolled lists
                        applyProgressesToCourses(progressesMap, filteredCourseList)
                        sortByLastAction(filteredCourseList, progressesMap)
                    }
                    else -> filteredCourseList
                }
                mainHandler.post {
                    if (coursesForShow.isEmpty()) {
                        isEmptyCourses.set(true)
                        view?.showEmptyCourses()
                    } else {
                        view?.showCourses(coursesForShow)
                    }
                }
                break
            }
        }
    }

    private fun handleMeta(response: CoursesStepicResponse) {
        hasNextPage.set(response.meta.has_next)
        if (hasNextPage.get()) {
            currentPage.set(response.meta.page + 1) // page for next loading
        }
    }

    private fun getFromDatabaseAndShow(applyFilter: Boolean, courseType: Table) {
        val coursesBeforeLoading = databaseFacade.getAllCourses(courseType).filterNotNull()
        if (coursesBeforeLoading.isNotEmpty()) {
            val progressMap = getProgressesFromDb(coursesBeforeLoading)
            val coursesForShow =
                    if (courseType == Table.enrolled) {
                        //apply only for enrolled list/carousel
                        applyProgressesToCourses(progressMap, coursesBeforeLoading)
                        sortByLastAction(coursesBeforeLoading, progressMap)
                    } else {
                        if (!applyFilter && !sharedPreferenceHelper.filterForFeatured.contains(StepikFilter.PERSISTENT)) {
                            filterApplicator.getFilteredFeaturedFromDefault(coursesBeforeLoading)
                        } else {
                            filterApplicator.getFilteredFeaturedFromSharedPrefs(coursesBeforeLoading)
                        }
                    }
            if (coursesForShow.isNotEmpty()) {
                mainHandler.post {
                    view?.showCourses(coursesForShow)
                }
            } else if (hasNextPage.get()) {
                mainHandler.post { view?.showLoading() }
            }
        } else {
            if (hasNextPage.get()) {
                //do not show loading, if we have not the next page
                //loading is useless in this case
                mainHandler.post { view?.showLoading() }
            }
        }
    }

    fun refreshData(courseType: Table, applyFilter: Boolean, isRefreshing: Boolean) {
        if (currentNumberOfTasks >= MAX_CURRENT_NUMBER_OF_TASKS) {
            return
        }
        currentPage.set(1);
        hasNextPage.set(true)
        downloadData(courseType, applyFilter, isRefreshing = isRefreshing)
    }

    @WorkerThread
    private fun sortByLastAction(courses: List<Course>, idProgressesMap: Map<String?, Progress>): MutableList<Course> {
        return courses.sortedWith(Comparator { course1, course2 ->
            val progress1: Progress? = idProgressesMap[course1.progress]
            val progress2: Progress? = idProgressesMap[course2.progress]

            val lastViewed1 = progress1?.lastViewed?.toLongOrNull()
            val lastViewed2 = progress2?.lastViewed?.toLongOrNull()

            if (lastViewed1 == null && lastViewed2 == null) {
                return@Comparator (course2.courseId - course1.courseId).toInt() // course2 - course1 (greater id is 1st)
            }

            if (lastViewed1 == null) {
                return@Comparator 1 // 1st after 2nd
            }

            if (lastViewed2 == null) {
                return@Comparator -1 //1st before 2nd. 2nd to end
            }

            return@Comparator (lastViewed2 - lastViewed1).toInt()
        }).toMutableList()
    }

    @WorkerThread
    private fun getProgressesFromDb(courses: List<Course>): Map<String?, Progress> {
        val progressIds = courses.mapNotNull {
            it.progress
        }
        return databaseFacade.getProgresses(progressIds).associateBy { it.id }
    }

    private fun applyProgressesToCourses(progresses: Map<String?, Progress>, courses: List<Course>) {
        courses.forEach { course ->
            progresses[course.progress]?.let {
                course.progressObject = it
            }
        }
    }

    fun loadMore(courseType: Table, needFilter: Boolean) {
        downloadData(courseType, needFilter, isRefreshing = false, isLoadMore = true)
    }
}
