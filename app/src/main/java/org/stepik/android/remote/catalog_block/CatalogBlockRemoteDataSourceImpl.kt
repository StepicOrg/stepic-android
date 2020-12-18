package org.stepik.android.remote.catalog_block

import io.reactivex.Single
import org.stepik.android.data.catalog_block.source.CatalogBlockRemoteDataSource
import org.stepik.android.domain.catalog_block.model.CatalogBlockItem
import org.stepik.android.remote.catalog_block.model.CatalogBlockResponse
import org.stepik.android.remote.catalog_block.service.CatalogBlockService
import javax.inject.Inject

class CatalogBlockRemoteDataSourceImpl
@Inject
constructor(
    private val catalogBlockService: CatalogBlockService
) : CatalogBlockRemoteDataSource {
    override fun getCatalogBlocks(language: String): Single<List<CatalogBlockItem>> =
        catalogBlockService
            .getCatalogBlocks(mapOf("language" to language)) // TODO Switch to a "query" object
            .map(CatalogBlockResponse::catalogBlocks)
}