package org.stepic.droid.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_catalog.*
import org.stepic.droid.R
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.base.Client
import org.stepic.droid.base.FragmentBase
import org.stepic.droid.core.filters.contract.FiltersListener
import org.stepic.droid.core.presenters.CatalogPresenter
import org.stepic.droid.core.presenters.FiltersPresenter
import org.stepic.droid.core.presenters.TagsPresenter
import org.stepic.droid.core.presenters.contracts.CatalogView
import org.stepic.droid.core.presenters.contracts.FiltersView
import org.stepic.droid.core.presenters.contracts.TagsView
import org.stepic.droid.features.stories.presentation.StoriesPresenter
import org.stepic.droid.features.stories.presentation.StoriesView
import org.stepic.droid.features.stories.ui.activity.StoriesActivity
import org.stepic.droid.features.stories.ui.adapter.StoriesAdapter
import org.stepic.droid.model.CoursesCarouselInfo
import org.stepic.droid.model.StepikFilter
import org.stepic.droid.ui.adapters.CatalogAdapter
import org.stepic.droid.ui.custom.AutoCompleteSearchView
import org.stepic.droid.ui.util.initCenteredToolbar
import org.stepik.android.model.Tag
import ru.nobird.android.stories.transition.SharedTransitionIntentBuilder
import ru.nobird.android.stories.transition.SharedTransitionsManager
import ru.nobird.android.stories.ui.delegate.SharedTransitionContainerDelegate
import java.util.EnumSet
import javax.inject.Inject

class CatalogFragment : FragmentBase(),
        CatalogView, FiltersView, FiltersListener, TagsView, StoriesView {

    companion object {
        fun newInstance(): FragmentBase = CatalogFragment()

        private const val CATALOG_STORIES_KEY = "catalog_stories"
    }

    @Inject
    lateinit var catalogPresenter: CatalogPresenter

    @Inject
    lateinit var filtersPresenter: FiltersPresenter

    @Inject
    lateinit var filtersClient: Client<FiltersListener>

    @Inject
    lateinit var tagsPresenter: TagsPresenter

    @Inject
    lateinit var storiesPresenter: StoriesPresenter

    private val courseCarouselInfoList = mutableListOf<CoursesCarouselInfo>()

    private var searchMenuItem: MenuItem? = null

    private var needShowLangWidget = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        analytic.reportAmplitudeEvent(AmplitudeAnalytic.Catalog.CATALOG_SCREEN_OPENED)
        analytic.reportEvent(Analytic.Catalog.CATALOG_SCREEN_OPENED)
    }

    override fun injectComponent() {
        App
            .component()
            .catalogComponentBuilder()
            .build()
            .inject(this)

        needShowLangWidget = sharedPreferenceHelper.isNeedShowLangWidget
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_catalog, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCenteredToolbar(R.string.catalog_title, showHomeButton = false)
        initMainRecycler()

        tagsPresenter.attachView(this)
        filtersClient.subscribe(this)
        filtersPresenter.attachView(this)
        catalogPresenter.attachView(this)
        storiesPresenter.attachView(this)
        filtersPresenter.onNeedFilters()
        tagsPresenter.onNeedShowTags()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagsPresenter.detachView(this)
        filtersClient.unsubscribe(this)
        catalogPresenter.detachView(this)
        storiesPresenter.detachView(this)
        filtersPresenter.detachView(this)
    }

    private fun initMainRecycler() {
        catalogRecyclerView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchMenuItem?.collapseActionView()
            }
        }
        catalogRecyclerView.itemAnimator = null
        catalogRecyclerView.layoutManager = LinearLayoutManager(context)
        catalogRecyclerView.adapter = CatalogAdapter(
            config = config,
            courseListItems = courseCarouselInfoList,
            onFiltersChanged = { filtersPresenter.onFilterChanged(it) },
            onRetry = {
                filtersPresenter.onNeedFilters()
                tagsPresenter.onNeedShowTags()
            },
            onTagClicked = { tag -> onTagClicked(tag) },
            onStoryClicked = { _, position -> showStories(position) }
        )
    }

    private fun onTagClicked(tag: Tag) {
        screenManager.showListOfTag(activity, tag)
    }

    override fun showCollections(courseItems: List<CoursesCarouselInfo>) {
        this.courseCarouselInfoList.clear()
        this.courseCarouselInfoList.addAll(courseItems)
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.showCollections()
    }

    override fun offlineMode() {
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.enableOfflineMode()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as? AutoCompleteSearchView

        searchMenuItem?.setOnMenuItemClickListener {
            analytic.reportEvent(Analytic.Search.SEARCH_OPENED)
            false
        }

        searchView?.let {
            it.initSuggestions(catalogContainer)
            it.setCloseIconDrawableRes(getCloseIconDrawableRes())
            it.setSearchable(requireActivity())

            it.suggestionsOnTouchListener = View.OnTouchListener { _, _ ->
                hideSoftKeypad()
                false
            }

            it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    it.onSubmitted(query)
                    searchMenuItem?.collapseActionView()
                    return false
                }

                override fun onQueryTextChange(query: String): Boolean {
                    it.setConstraint(query)
                    return false
                }
            })
        }
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()

        (searchMenuItem?.actionView as? SearchView)?.setOnQueryTextListener(null)
        searchMenuItem = null
    }

    override fun onFiltersPrepared(filters: EnumSet<StepikFilter>) {
        updateFilters(filters)
    }

    override fun onFiltersChanged(filters: EnumSet<StepikFilter>) {
        updateFilters(filters)
    }

    private fun updateFilters(filters: EnumSet<StepikFilter>) {
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.setFilters(filters, needShowLangWidget)
        catalogAdapter.refreshPopular()

        catalogPresenter.onNeedLoadCatalog(filters)
    }

    override fun onTagsFetched(tags: List<Tag>) {
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.onTagLoaded(tags)
    }

    override fun onTagsNotLoaded() {
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.onTagNotLoaded()
    }

    override fun setState(state: StoriesView.State) {
        val catalogAdapter = catalogRecyclerView.adapter as CatalogAdapter
        catalogAdapter.storiesState = state
    }

    private fun showStories(position: Int) {
        val storiesViewHolder = catalogRecyclerView.findViewHolderForAdapterPosition(CatalogAdapter.STORIES_INDEX)
                as? CatalogAdapter.StoriesViewHolder
                ?: return

        val stories = storiesViewHolder.storiesAdapter.stories

        requireContext().startActivity(SharedTransitionIntentBuilder.createIntent(
                requireContext(), StoriesActivity::class.java, CATALOG_STORIES_KEY, position, stories
        ))
    }

    override fun onStart() {
        super.onStart()
        SharedTransitionsManager.registerTransitionDelegate(CATALOG_STORIES_KEY, object : SharedTransitionContainerDelegate {
            override fun getSharedView(position: Int): View? {
                val storiesViewHolder = catalogRecyclerView.findViewHolderForAdapterPosition(CatalogAdapter.STORIES_INDEX)
                        as? CatalogAdapter.StoriesViewHolder
                        ?: return null

                val storyViewHolder = storiesViewHolder.recycler.findViewHolderForAdapterPosition(position)
                        as? StoriesAdapter.StoryViewHolder
                        ?: return null

                return storyViewHolder.cover
            }

            override fun onPositionChanged(position: Int) {
                val storiesViewHolder = catalogRecyclerView.findViewHolderForAdapterPosition(CatalogAdapter.STORIES_INDEX)
                        as? CatalogAdapter.StoriesViewHolder
                        ?: return

                storiesViewHolder.recycler.layoutManager?.scrollToPosition(position)
                storiesViewHolder.storiesAdapter.selected = position

                if (position != -1) {
                    val story = storiesViewHolder.storiesAdapter.stories[position]
                    storiesPresenter.onStoryViewed(story.id)
                    analytic.reportAmplitudeEvent(AmplitudeAnalytic.Stories.STORY_OPENED, mapOf(
                            AmplitudeAnalytic.Stories.Values.STORY_ID to story.id
                    ))
                }
            }
        })
    }

    override fun onStop() {
        SharedTransitionsManager.unregisterTransitionDelegate(CATALOG_STORIES_KEY)
        super.onStop()
    }
}
