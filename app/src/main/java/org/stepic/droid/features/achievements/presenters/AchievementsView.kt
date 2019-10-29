package org.stepic.droid.features.achievements.presenters

import org.stepic.droid.model.AchievementFlatItem

interface AchievementsView {
    fun showAchievements(achievements: List<AchievementFlatItem>)
    fun onAchievementsLoadingError()
    fun onAchievementsLoading()

    sealed class State {
        object Idle : State()
        object Loading : State()
        class AchievementsLoaded(val achievements: List<AchievementFlatItem>) : State()
        object Error : State()
    }
}