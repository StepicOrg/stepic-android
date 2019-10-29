package org.stepic.droid.features.achievements.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.error_no_connection_with_button.*
import kotlinx.android.synthetic.main.fragment_achievements_list.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.base.FragmentBase
import org.stepic.droid.features.achievements.presenters.AchievementsPresenter
import org.stepic.droid.features.achievements.presenters.AchievementsView
import org.stepic.droid.features.achievements.ui.adapters.AchievementsAdapter
import org.stepic.droid.features.achievements.ui.adapters.BaseAchievementsAdapter
import org.stepic.droid.features.achievements.ui.dialogs.AchievementDetailsDialog
import org.stepic.droid.model.AchievementFlatItem
import org.stepic.droid.ui.util.initCenteredToolbar
import org.stepic.droid.ui.util.setHeight
import ru.nobird.android.view.base.ui.extension.argument
import javax.inject.Inject

class AchievementsListFragment: FragmentBase(), AchievementsView {
    companion object {
        fun newInstance(userId: Long, isMyProfile: Boolean) =
            AchievementsListFragment().apply {
                this.userId = userId
                this.isMyProfile = isMyProfile
            }
    }

    @Inject
    lateinit var achievementsPresenter: AchievementsPresenter

    private var userId: Long by argument()
    private var isMyProfile: Boolean by argument()

    override fun injectComponent() {
        App
                .component()
                .profileComponentBuilder()
                .build()
                .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_achievements_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        initPlaceholders()

        initCenteredToolbar(R.string.achievements_title, showHomeButton = true)

        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = AchievementsAdapter().apply { onAchievementItemClick = {
            AchievementDetailsDialog.newInstance(it, isMyProfile).show(childFragmentManager, AchievementDetailsDialog.TAG)
        }}

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_divider_h)!!)
        recycler.addItemDecoration(divider)

        achievementsPresenter.attachView(this)
        fetchAchievements()

        tryAgain.setOnClickListener { fetchAchievements(true) }
    }

    private fun initPlaceholders() {
        val itemHeight = resources.getDimension(R.dimen.achievement_tile_height)
        val screenHeight = resources.displayMetrics.heightPixels

        for (i in 0..(screenHeight / itemHeight).toInt()) {
            progress.addView(layoutInflater.inflate(R.layout.view_achievement_item_placeholder, progress, false))
            val stroke = layoutInflater.inflate(R.layout.view_stroke, progress, false)
            stroke.setBackgroundResource(R.drawable.list_divider_h)
            stroke.setHeight(1)
            progress.addView(stroke)
        }
    }

    private fun fetchAchievements(forceUpdate: Boolean = false) {
        achievementsPresenter.showAchievementsForUser(userId, forceUpdate = forceUpdate)
    }

    override fun showAchievements(achievements: List<AchievementFlatItem>) {
        recycler.isVisible = true
        progress.isVisible = false
        error.isVisible = false
        (recycler.adapter as? BaseAchievementsAdapter)?.achievements = achievements
    }

    override fun onAchievementsLoadingError() {
        recycler.isVisible = false
        progress.isVisible = false
        error.isVisible = true
    }

    override fun onAchievementsLoading() {
        recycler.isVisible = false
        progress.isVisible = true
        error.isVisible = false
    }

    override fun onDestroyView() {
        achievementsPresenter.detachView(this)
        super.onDestroyView()
    }
}