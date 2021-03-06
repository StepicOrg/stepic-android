package org.stepik.android.presentation.learning_actions.reducer

import org.stepik.android.presentation.learning_actions.LearningActionsFeature.State
import org.stepik.android.presentation.learning_actions.LearningActionsFeature.Message
import org.stepik.android.presentation.learning_actions.LearningActionsFeature.Action
import org.stepik.android.presentation.wishlist.reducer.WishlistReducer
import ru.nobird.android.presentation.redux.reducer.StateReducer
import javax.inject.Inject

class LearningActionsReducer
@Inject
constructor(
    private val wishlistReducer: WishlistReducer
) : StateReducer<State, Message, Action> {
    override fun reduce(state: State, message: Message): Pair<State, Set<Action>> =
        when (message) {
            is Message.WishlistMessage -> {
                val (wishlistState, wishlistActions) = wishlistReducer.reduce(state.wishlistState, message.message)
                state.copy(wishlistState = wishlistState) to wishlistActions.map(Action::WishlistAction).toSet()
            }
        } ?: state to emptySet()
}