package org.stepik.android.remote.stories

import io.reactivex.Observable
import io.reactivex.Single
import org.stepik.android.data.stories.source.StoryTemplatesRemoteDataSource
import org.stepik.android.model.StoryTemplate
import org.stepik.android.remote.stories.service.StoryService
import javax.inject.Inject

class StoryTemplatesRemoteDataSourceImpl
@Inject
constructor(
    private val storyService: StoryService
) : StoryTemplatesRemoteDataSource {
    override fun getStoryTemplates(lang: String): Single<List<StoryTemplate>> =
        getStoryTemplatesByPage(lang)

    private fun getStoryTemplatesByPage(lang: String): Single<List<StoryTemplate>> =
        Observable.range(0, 100)
            .concatMapSingle { storyService.getStoryTemplate(it, true, lang) }
            .takeUntil { it.meta.hasNext }
            .map { it.storyTemplates }
            .reduce(emptyList()) { a, b -> a + b }
}