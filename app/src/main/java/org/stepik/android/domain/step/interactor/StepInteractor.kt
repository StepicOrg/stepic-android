package org.stepik.android.domain.step.interactor

import io.reactivex.Observable
import io.reactivex.Single
import org.stepic.droid.persistence.content.StepContentResolver
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepic.droid.util.concat
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.discussion_thread.repository.DiscussionThreadRepository
import org.stepik.android.domain.step.repository.StepRepository
import org.stepik.android.model.Step
import org.stepik.android.model.comments.DiscussionThread
import org.stepik.android.view.injection.step.StepDiscussionBus
import javax.inject.Inject

class StepInteractor
@Inject
constructor(
    private val discussionThreadRepository: DiscussionThreadRepository,

    @StepDiscussionBus
    private val stepDiscussionObservable: Observable<Long>,
    private val stepRepository: StepRepository,
    private val stepContentResolver: StepContentResolver
) {
    fun getStepUpdates(stepId: Long, shouldSkipFirstValue: Boolean = false): Observable<StepPersistentWrapper> =
        Observable
            .just(stepId)
            .concat(stepDiscussionObservable)
            .skip(if (shouldSkipFirstValue) 1 else 0)
            .filter { it == stepId }
            .flatMapMaybe { stepRepository.getStep(stepId, DataSourceType.REMOTE) }
            .flatMapSingle(stepContentResolver::resolvePersistentContent)

    fun getDiscussionThreads(step: Step): Single<List<DiscussionThread>> =
        discussionThreadRepository
            .getDiscussionThreads(*step.discussionThreads?.toTypedArray() ?: arrayOf())
}