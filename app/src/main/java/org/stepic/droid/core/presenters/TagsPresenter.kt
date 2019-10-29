package org.stepic.droid.core.presenters

import androidx.annotation.MainThread
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import org.stepic.droid.core.presenters.contracts.TagsView
import org.stepic.droid.di.catalog.CatalogScope
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepic.droid.web.Api
import javax.inject.Inject

@CatalogScope
class TagsPresenter
@Inject
constructor(
    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler,
    private val api: Api
) : PresenterBase<TagsView>() {

    private var disposable: Disposable? = null

    @MainThread
    fun onNeedShowTags() {
        disposable?.dispose()

        disposable = api
            .featuredTags
            .map {
                it.tags
            }
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribe(
                { view?.onTagsFetched(it) },
                { view?.onTagsNotLoaded() }
            )
    }

    override fun detachView(view: TagsView) {
        super.detachView(view)
        disposable?.dispose()
    }
}
