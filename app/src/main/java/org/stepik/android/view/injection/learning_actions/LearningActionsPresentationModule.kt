package org.stepik.android.view.injection.learning_actions

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.stepik.android.presentation.base.injection.ViewModelKey
import org.stepik.android.presentation.learning_actions.LearningActionsFeature
import org.stepik.android.presentation.learning_actions.LearningActionsViewModel
import org.stepik.android.presentation.learning_actions.reducer.LearningActionsReducer
import org.stepik.android.presentation.wishlist.WishlistFeature
import org.stepik.android.presentation.wishlist.dispatcher.WishlistActionDispatcher
import ru.nobird.android.core.model.safeCast
import ru.nobird.android.presentation.redux.container.wrapWithViewContainer
import ru.nobird.android.presentation.redux.dispatcher.transform
import ru.nobird.android.presentation.redux.dispatcher.wrapWithActionDispatcher
import ru.nobird.android.presentation.redux.feature.ReduxFeature

@Module
object LearningActionsPresentationModule {
    /**
     * Presentation
     */
    @Provides
    @IntoMap
    @ViewModelKey(LearningActionsViewModel::class)
    internal fun provideLearningActionsPresenter(
        learningActionsReducer: LearningActionsReducer,
        wishlistActionDispatcher: WishlistActionDispatcher
    ): ViewModel =
        LearningActionsViewModel(
            ReduxFeature(
                LearningActionsFeature.State(
                    wishlistState = WishlistFeature.State.Idle
                ), learningActionsReducer
            )
                .wrapWithActionDispatcher(
                    wishlistActionDispatcher.transform(
                        transformAction = { it.safeCast<LearningActionsFeature.Action.WishlistAction>()?.action },
                        transformMessage = LearningActionsFeature.Message::WishlistMessage
                    ))
                .wrapWithViewContainer()
        )
}