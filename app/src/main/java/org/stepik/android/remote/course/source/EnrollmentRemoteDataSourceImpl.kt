package org.stepik.android.remote.course.source

import io.reactivex.Completable
import org.stepik.android.data.course.source.EnrollmentRemoteDataSource
import org.stepik.android.model.Enrollment
import org.stepik.android.remote.course.model.EnrollmentRequest
import org.stepik.android.remote.course.service.EnrollmentService
import javax.inject.Inject

class EnrollmentRemoteDataSourceImpl
@Inject
constructor(
    private val enrollmentService: EnrollmentService
) : EnrollmentRemoteDataSource {
    override fun addEnrollment(courseId: Long): Completable =
        enrollmentService.joinCourse(EnrollmentRequest(Enrollment(courseId)))

    override fun removeEnrollment(courseId: Long): Completable =
        enrollmentService.dropCourse(courseId)
}