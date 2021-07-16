package org.stepik.android.view.injection.course_list.factory

import dagger.assisted.AssistedFactory
import org.stepik.android.domain.catalog.model.CatalogBlockContent
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.model.Course
import org.stepik.android.presentation.course_continue.model.CourseContinueInteractionSource
import org.stepik.android.view.catalog.ui.adapter.delegate.CourseListAdapterDelegate

@AssistedFactory
interface CourseListAdapterDelegateFactory {
    fun create(
        isHandleInAppPurchase: Boolean,
        onTitleClick: (Long) -> Unit,
        onBlockSeen: (String, CatalogBlockContent.FullCourseList) -> Unit,
        onCourseContinueClicked: (Course, CourseViewSource, CourseContinueInteractionSource) -> Unit,
        onCourseClicked: (CourseListItem.Data) -> Unit
    ): CourseListAdapterDelegate
}