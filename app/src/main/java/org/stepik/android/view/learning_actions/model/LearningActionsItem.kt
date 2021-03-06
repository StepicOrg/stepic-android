package org.stepik.android.view.learning_actions.model

import org.stepik.android.presentation.wishlist.WishlistFeature
import ru.nobird.android.core.model.Identifiable

sealed class LearningActionsItem {
    data class Wishlist(val state: WishlistFeature.State) : LearningActionsItem(), Identifiable<String> {
        override val id: String = "wishlist"
    }
}