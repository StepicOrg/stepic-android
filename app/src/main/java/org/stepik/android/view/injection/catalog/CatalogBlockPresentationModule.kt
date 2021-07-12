package org.stepik.android.view.injection.catalog

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.stepik.android.presentation.base.injection.ViewModelKey
import org.stepik.android.presentation.catalog.CatalogFeature
import org.stepik.android.presentation.catalog.CatalogViewModel
import org.stepik.android.presentation.catalog.dispatcher.CatalogActionDispatcher
import org.stepik.android.presentation.catalog.reducer.CatalogReducer
import org.stepik.android.presentation.course_continue_redux.CourseContinueFeature
import org.stepik.android.presentation.course_continue_redux.dispatcher.CourseContinueActionDispatcher
import org.stepik.android.presentation.course_list_redux.dispatcher.CourseListActionDispatcher
import org.stepik.android.presentation.enrollment.dispatcher.EnrollmentActionDispatcher
import org.stepik.android.presentation.filter.FiltersFeature
import org.stepik.android.presentation.filter.dispatcher.FiltersActionDispatcher
import org.stepik.android.presentation.progress.dispatcher.ProgressActionDispatcher
import org.stepik.android.presentation.stories.StoriesFeature
import org.stepik.android.presentation.stories.dispatcher.StoriesActionDispatcher
import org.stepik.android.presentation.user_courses.dispatcher.UserCoursesActionDispatcher
import org.stepik.android.presentation.wishlist.dispatcher.WishlistActionDispatcher
import ru.nobird.android.core.model.safeCast
import ru.nobird.android.presentation.redux.container.wrapWithViewContainer
import ru.nobird.android.presentation.redux.dispatcher.transform
import ru.nobird.android.presentation.redux.dispatcher.wrapWithActionDispatcher
import ru.nobird.android.presentation.redux.feature.ReduxFeature

@Module
object CatalogBlockPresentationModule {
    @Provides
    @IntoMap
    @ViewModelKey(CatalogViewModel::class)
    internal fun provideCatalogBlockPresenter(
        catalogReducer: CatalogReducer,
        catalogActionDispatcher: CatalogActionDispatcher,
        storiesActionDispatcher: StoriesActionDispatcher,
        filtersActionDispatcher: FiltersActionDispatcher,
        courseListActionDispatcher: CourseListActionDispatcher,
        courseContinueActionDispatcher: CourseContinueActionDispatcher,
        userCoursesActionDispatcher: UserCoursesActionDispatcher,
        progressActionDispatcher: ProgressActionDispatcher,
        enrollmentActionDispatcher: EnrollmentActionDispatcher,
        wishlistActionDispatcher: WishlistActionDispatcher
    ): ViewModel =
        CatalogViewModel(
            ReduxFeature(
                CatalogFeature.State(
                    storiesState = StoriesFeature.State.Idle,
                    filtersState = FiltersFeature.State.Idle,
                    blocksState = CatalogFeature.BlocksState.Idle,
                    courseContinueState = CourseContinueFeature.State.Idle
                ), catalogReducer
            )
                .wrapWithActionDispatcher(catalogActionDispatcher)
                .wrapWithActionDispatcher(
                    storiesActionDispatcher.transform(
                        transformAction = { it.safeCast<CatalogFeature.Action.StoriesAction>()?.action },
                        transformMessage = CatalogFeature.Message::StoriesMessage
                    )
                )
                .wrapWithActionDispatcher(
                    filtersActionDispatcher.transform(
                        transformAction = { it.safeCast<CatalogFeature.Action.FiltersAction>()?.action },
                        transformMessage = CatalogFeature.Message::FiltersMessage
                    )
                )
                .wrapWithActionDispatcher(
                    courseListActionDispatcher.transform(
                        transformAction = { it.safeCast<CatalogFeature.Action.CourseListAction>()?.action },
                        transformMessage = { CatalogFeature.Message.CourseListMessage(it.id, it) }
                    )
                )
                .wrapWithActionDispatcher(
                    courseContinueActionDispatcher.transform(
                        transformAction = { it.safeCast<CatalogFeature.Action.CourseContinueAction>()?.action },
                        transformMessage = CatalogFeature.Message::CourseContinueMessage
                    )
                )
                .wrapWithActionDispatcher(
                    userCoursesActionDispatcher.transform(
                        transformAction = { null },
                        transformMessage = CatalogFeature.Message::UserCourseMessage
                    )
                )
                .wrapWithActionDispatcher(
                    progressActionDispatcher.transform(
                        transformAction = { null },
                        transformMessage = CatalogFeature.Message::ProgressMessage
                    )
                )
                .wrapWithActionDispatcher(
                    enrollmentActionDispatcher.transform(
                        transformAction = { null },
                        transformMessage = CatalogFeature.Message::EnrollmentMessage
                    )
                )
                .wrapWithActionDispatcher(
                    wishlistActionDispatcher.transform(
                        transformAction = { null },
                        transformMessage = CatalogFeature.Message::WishlistMessage
                    )
                )
                .wrapWithViewContainer()
        )
}