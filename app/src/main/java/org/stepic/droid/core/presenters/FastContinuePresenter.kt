package org.stepic.droid.core.presenters

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import org.stepic.droid.core.presenters.contracts.FastContinueView
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepic.droid.util.emptyOnErrorStub
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.domain.course_list.model.UserCoursesLoaded
import org.stepik.android.model.Course
import org.stepik.android.view.injection.course_list.UserCoursesLoadedBus
import timber.log.Timber
import javax.inject.Inject

class FastContinuePresenter
@Inject
constructor(
    private val sharedPreferenceHelper: SharedPreferenceHelper,
    @MainScheduler
    private val mainScheduler: Scheduler,
    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @UserCoursesLoadedBus
    private val userCoursesLoadedObservable: Observable<UserCoursesLoaded>
) : PresenterBase<FastContinueView>() {

    private var disposable: Disposable? = null
    private var courseListItem: CourseListItem.Data? = null

    fun onCreated() {
        if (sharedPreferenceHelper.authResponseFromStore != null) {
            if (courseListItem == null) {
                view?.onLoading()
                subscribeToFirstCourse()
            } else {
                view?.onShowCourse(courseListItem as CourseListItem.Data)
            }
        } else {
            view?.onAnonymous()
        }
    }

    // TODO Handle enrollment updates
    private fun subscribeToFirstCourse() {
        disposable = userCoursesLoadedObservable
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext = {
                    if (it is UserCoursesLoaded.FirstCourse) {
                        courseListItem = it.courseListItem
                        view?.onShowCourse(it.courseListItem)
                    } else {
                        view?.onEmptyCourse()
                    }
                },
                onError = emptyOnErrorStub
            )
    }

    override fun detachView(view: FastContinueView) {
        disposable?.dispose()
        super.detachView(view)
    }


}
