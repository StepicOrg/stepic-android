package org.stepik.android.presentation.lesson

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepic.droid.util.emptyOnErrorStub
import org.stepic.droid.util.getStepType
import org.stepik.android.domain.last_step.model.LastStep
import org.stepik.android.domain.lesson.interactor.LessonContentInteractor
import org.stepik.android.domain.lesson.interactor.LessonInteractor
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.model.Lesson
import org.stepik.android.model.Section
import org.stepik.android.model.Unit
import org.stepik.android.presentation.base.PresenterBase
import org.stepik.android.domain.lesson.model.LessonDeepLinkData
import org.stepik.android.domain.step.interactor.StepIndexingInteractor
import org.stepik.android.domain.view_assignment.interactor.ViewAssignmentReportInteractor
import org.stepik.android.model.Progress
import org.stepik.android.presentation.lesson.mapper.LessonStateMapper
import javax.inject.Inject

class LessonPresenter
@Inject
constructor(
    private val analytic: Analytic,

    private val lessonInteractor: LessonInteractor,
    private val lessonContentInteractor: LessonContentInteractor,

    private val stateMapper: LessonStateMapper,

    private val progressObservable: Observable<Progress>,

    private val stepViewReportInteractor: ViewAssignmentReportInteractor,
    private val stepIndexingInteractor: StepIndexingInteractor,

    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler
) : PresenterBase<LessonView>() {
    private var state: LessonView.State = LessonView.State.Idle
        set(value) {
            field = value
            view?.setState(value)
        }

    private var currentStepPosition = -1
        set(value) {
            field = value
            endIndexing()
            startIndexing(value)
        }

    init {
        subscribeForProgressesUpdates()
    }

    override fun attachView(view: LessonView) {
        super.attachView(view)
        view.setState(state)

        startIndexing(currentStepPosition)
    }

    override fun detachView(view: LessonView) {
        endIndexing()
        super.detachView(view)
    }

    /**
     * Data initialization variants
     */
    fun onLesson(lesson: Lesson, unit: Unit, section: Section, isFromNextLesson: Boolean, forceUpdate: Boolean = false) {
        obtainLessonData(lessonInteractor.getLessonData(lesson, unit, section, isFromNextLesson), forceUpdate)
    }

    fun onLastStep(lastStep: LastStep, forceUpdate: Boolean = false) {
        obtainLessonData(lessonInteractor.getLessonData(lastStep), forceUpdate)
    }

    fun onDeepLink(deepLinkData: LessonDeepLinkData, forceUpdate: Boolean = false) {
        obtainLessonData(lessonInteractor.getLessonData(deepLinkData), forceUpdate)
    }

    fun onEmptyData() {
        if (state == LessonView.State.Idle) {
            state = LessonView.State.LessonNotFound
        }
    }

    private fun obtainLessonData(lessonDataSource: Maybe<LessonData>, forceUpdate: Boolean = false) {
        if (state != LessonView.State.Idle &&
            !(state == LessonView.State.NetworkError && forceUpdate)
        ) {
            return
        }

        state = LessonView.State.Loading
        compositeDisposable += lessonDataSource
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onComplete = { state = LessonView.State.LessonNotFound },
                onSuccess  = { state = LessonView.State.LessonLoaded(it, LessonView.StepsState.Idle); resolveStepsState() },
                onError    = { state = LessonView.State.NetworkError }
            )
    }

    /**
     * Steps loading
     */
    private fun resolveStepsState() {
        val oldState = (state as? LessonView.State.LessonLoaded)
            ?: return

        val stepIds = oldState.lessonData.lesson.steps
        val unit = oldState.lessonData.unit

        if (stepIds.isEmpty()) {
            state = oldState.copy(stepsState = LessonView.StepsState.EmptySteps)
        } else {
            state = oldState.copy(stepsState = LessonView.StepsState.Loading)

            compositeDisposable += lessonContentInteractor
                .getStepItems(unit, *stepIds)
                .observeOn(mainScheduler)
                .subscribeOn(backgroundScheduler)
                .subscribeBy(
                    onSuccess  = { state = oldState.copy(stepsState = LessonView.StepsState.Loaded(it)); view?.showStepAtPosition(oldState.lessonData.stepPosition) },
                    onError    = { state = oldState.copy(stepsState = LessonView.StepsState.NetworkError) }
                )
        }
    }

    /**
     * Lesson info tooltip
     */
    fun onShowLessonInfoClicked(position: Int) {
        val state = (state as? LessonView.State.LessonLoaded)
            ?: return

        val stepWorth = (state.stepsState as? LessonView.StepsState.Loaded)
            ?.stepItems
            ?.getOrNull(position)
            ?.stepWrapper
            ?.step
            ?.worth
            ?: return

        view?.showLessonInfoTooltip(stepWorth, state.lessonData.lesson.timeToComplete, -1)
    }

    /**
     * Progresses
     */
    private fun subscribeForProgressesUpdates() {
        compositeDisposable += progressObservable
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext = { progress ->
                    val newState = stateMapper.mergeStateWithProgress(state, progress)
                    if (state !== newState) { // compare by reference
                        state = newState
                    }
                },
                onError = emptyOnErrorStub
            )
    }

    /**
     * Step view
     */
    fun onStepOpened(position: Int) {
        val state = (state as? LessonView.State.LessonLoaded)
            ?: return

        val stepsState = (state.stepsState as? LessonView.StepsState.Loaded)
            ?: return

        val stepItem = stepsState
            .stepItems
            .getOrNull(position)
            ?: return

        currentStepPosition = position

        compositeDisposable += stepViewReportInteractor
            .reportViewAssignment(stepItem.stepWrapper.step, stepItem.assignment, state.lessonData.unit, state.lessonData.course)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(onError = emptyOnErrorStub)

        /**
         * Analytic
         */
        val step = stepItem.stepWrapper.step
        analytic.reportEventWithName(Analytic.Steps.STEP_OPENED, step.getStepType())
        analytic.reportAmplitudeEvent(
            AmplitudeAnalytic.Steps.STEP_OPENED, mapOf(
                AmplitudeAnalytic.Steps.Params.TYPE to step.getStepType(),
                AmplitudeAnalytic.Steps.Params.NUMBER to step.position,
                AmplitudeAnalytic.Steps.Params.STEP to step.id
            ))
    }

    /**
     * Indexing
     */
    private fun startIndexing(position: Int) {
        val state = (state as? LessonView.State.LessonLoaded)
            ?: return

        val step = (state.stepsState as? LessonView.StepsState.Loaded)
            ?.stepItems
            ?.getOrNull(position)
            ?.stepWrapper
            ?.step
            ?: return

        stepIndexingInteractor.startIndexing(state.lessonData.unit, state.lessonData.lesson, step)
    }

    private fun endIndexing() {
        stepIndexingInteractor.endIndexing()
    }
}