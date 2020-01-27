package org.stepik.android.domain.attempts.interactor

import io.reactivex.Single
import io.reactivex.rxkotlin.toObservable
import org.stepic.droid.storage.dao.IDao
import org.stepic.droid.util.mapToLongArray
import org.stepik.android.cache.submission.structure.DbStructureSubmission
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.lesson.repository.LessonRepository
import org.stepik.android.domain.section.repository.SectionRepository
import org.stepik.android.domain.step.repository.StepRepository
import org.stepik.android.domain.unit.repository.UnitRepository
import org.stepik.android.model.Lesson
import org.stepik.android.model.Step
import org.stepik.android.model.Submission
import org.stepik.android.model.Unit
import org.stepik.android.model.attempts.Attempt
import org.stepik.android.view.attempts.model.AttemptCacheItem
import javax.inject.Inject

class AttemptsInteractor
@Inject
constructor(
    private val attemptDao: IDao<Attempt>,
    private val submissionDao: IDao<Submission>,
    private val stepRepository: StepRepository,
    private val lessonRepository: LessonRepository,
    private val sectionRepository: SectionRepository,
    private val unitRepository: UnitRepository
) {

    // TODO Remove blocking gets
    /* fun fetchAttemptCacheItems(): Single<List<AttemptCacheItem>> =
        Single.fromCallable {
            val attempts = attemptDao.getAll()
            val submissions = submissionDao.getAllInRange(DbStructureSubmission.Columns.ATTEMPT_ID, attempts.map { it.id }.joinToString()).filter { it.status == Submission.Status.LOCAL }

            stepRepository
                .getSteps(*attempts.map { it.step }.toLongArray(), primarySourceType = DataSourceType.CACHE)

            val steps = stepRepository.getSteps(*attempts.mapToLongArray { it.step }, primarySourceType = DataSourceType.CACHE).blockingGet()
            val lessons = lessonRepository.getLessons(*steps.mapToLongArray { it.lesson }, primarySourceType = DataSourceType.CACHE).blockingGet()
            val units = lessons.flatMap { unitRepository.getUnitsByLessonId(it.id, primarySourceType = DataSourceType.CACHE).blockingGet() }
            val sections = sectionRepository.getSections(*units.mapToLongArray { it.section }, primarySourceType = DataSourceType.CACHE).blockingGet()

            val items = submissions.map { submission ->
                val attempt = attempts.find { it.id == submission.attempt }
                val lessonId = steps.find { it.id == attempt?.step }?.lesson
                val lesson = lessons.find { it.id == lessonId }

                val unit = units.find { it.lesson == lessonId }
                val section = sections.find { it.id == unit?.section }
                AttemptCacheItem.SubmissionItem(section = section!!, unit = unit!!, lesson = lesson!!, submission = submission, time = attempt?.time!!)
            }

            val lessonItems = items.map { AttemptCacheItem.LessonItem(it.section, it.unit, it.lesson) }.distinct().sortedBy { it.lesson.id }
            val sectionItems = lessonItems.map { AttemptCacheItem.SectionItem(it.section) }.distinct().sortedBy { it.section.id }

            val attemptCacheItems = mutableListOf<AttemptCacheItem>()

            sectionItems.forEach { sectionItem ->
                attemptCacheItems.add(sectionItem)
                val lessonsBySection = lessonItems.filter { it.section == sectionItem.section }
                lessonsBySection.forEach { lessonItem ->
                    attemptCacheItems.add(lessonItem)
                    val subs = items.filter { it.lesson == lessonItem.lesson }
                    attemptCacheItems += subs
                }
            }

            attemptCacheItems.forEach { item ->
                if (item is AttemptCacheItem.SectionItem) {
                    Timber.d("Section: ${item.section.title}")
                }
                if (item is AttemptCacheItem.LessonItem) {
                    Timber.d("Section: ${item.section.title} Lesson: ${item.lesson.title}")
                }
                if (item is AttemptCacheItem.SubmissionItem) {
                    Timber.d("Section: ${item.section.title} Lesson: ${item.lesson.title} Submission: ${item.submission.reply} Time: ${item.time}")
                }
            }
            attemptCacheItems.toList()
        }
     */

    fun fetchAttemptCacheItems(): Single<List<AttemptCacheItem>> =
        Single.fromCallable {
            val attempts = attemptDao.getAll()
            val submissions = submissionDao.getAllInRange(DbStructureSubmission.Columns.ATTEMPT_ID, attempts.map { it.id }.joinToString()).filter { it.status == Submission.Status.LOCAL }
            Pair(attempts, submissions)
        }.flatMap { (attempts, submissions) ->
            getSteps(attempts.mapToLongArray { it.step }, attempts, submissions)
        }

    private fun getSteps(ids: LongArray, attempts: List<Attempt>, submissions: List<Submission>): Single<List<AttemptCacheItem>> =
        stepRepository
            .getSteps(*ids, primarySourceType = DataSourceType.CACHE)
            .flatMap { steps -> getLessons(steps.mapToLongArray { it.lesson }, attempts, submissions, steps) }

    private fun getLessons(ids: LongArray, attempts: List<Attempt>, submissions: List<Submission>, steps: List<Step>): Single<List<AttemptCacheItem>> =
        lessonRepository
            .getLessons(*ids, primarySourceType = DataSourceType.CACHE)
            .flatMap { lessons -> lessons
                .toObservable()
                .flatMapSingle { lesson -> unitRepository.getUnitsByLessonId(lesson.id, primarySourceType = DataSourceType.CACHE) }
                .reduce(emptyList<Unit>()) { a, b ->  a + b }
                .flatMap { units -> getSections(units.mapToLongArray { it.section }, attempts, submissions, steps, lessons, units) }
            }

    private fun getSections(ids: LongArray, attempts: List<Attempt>, submissions: List<Submission>, steps: List<Step>, lessons: List<Lesson>, units: List<Unit>): Single<List<AttemptCacheItem>> =
        sectionRepository
            .getSections(*ids, primarySourceType = DataSourceType.CACHE)
            .map { sections ->
                val items = submissions.map { submission ->
                    val attempt = attempts.find { it.id == submission.attempt }
                    val lessonId = steps.find { it.id == attempt?.step }?.lesson
                    val lesson = lessons.find { it.id == lessonId }

                    val unit = units.find { it.lesson == lessonId }
                    val section = sections.find { it.id == unit?.section }
                    AttemptCacheItem.SubmissionItem(section = section!!, unit = unit!!, lesson = lesson!!, submission = submission, time = attempt?.time!!)
                }

                val lessonItems = items.map { AttemptCacheItem.LessonItem(it.section, it.unit, it.lesson) }.distinct().sortedBy { it.lesson.id }
                val sectionItems = lessonItems.map { AttemptCacheItem.SectionItem(it.section) }.distinct().sortedBy { it.section.id }

                val attemptCacheItems = mutableListOf<AttemptCacheItem>()

                sectionItems.forEach { sectionItem ->
                    attemptCacheItems.add(sectionItem)
                    val lessonsBySection = lessonItems.filter { it.section == sectionItem.section }
                    lessonsBySection.forEach { lessonItem ->
                        attemptCacheItems.add(lessonItem)
                        val subs = items.filter { it.lesson == lessonItem.lesson }
                        attemptCacheItems += subs
                    }
                }
                attemptCacheItems
            }
}