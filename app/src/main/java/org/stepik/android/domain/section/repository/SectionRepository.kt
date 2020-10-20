package org.stepik.android.domain.section.repository

import io.reactivex.Maybe
import io.reactivex.Single
import ru.nobird.android.domain.rx.maybeFirst
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.model.Section

interface SectionRepository {
    fun getSection(sectionId: Long, primarySourceType: DataSourceType = DataSourceType.CACHE): Maybe<Section> =
        getSections(listOf(sectionId), primarySourceType = primarySourceType).maybeFirst()

    fun getSections(sectionIds: List<Long>, primarySourceType: DataSourceType = DataSourceType.CACHE): Single<List<Section>>
}