package org.stepic.droid.persistence.storage.dao

import android.content.ContentValues
import android.database.Cursor
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.stepic.droid.di.storage.StorageSingleton
import org.stepic.droid.persistence.model.DownloadTask
import org.stepic.droid.persistence.model.PersistentItem
import org.stepic.droid.persistence.model.Structure
import org.stepic.droid.persistence.model.isCorrect
import org.stepic.droid.persistence.storage.structure.DBStructurePersistentItem
import org.stepic.droid.storage.dao.DaoBase
import org.stepic.droid.storage.dao.IDao
import org.stepic.droid.storage.operations.DatabaseOperations
import org.stepic.droid.util.getBoolean
import javax.inject.Inject

@StorageSingleton
class PersistentItemDaoImpl
@Inject
constructor(
        databaseOperations: DatabaseOperations
): DaoBase<PersistentItem>(databaseOperations), IDao<PersistentItem>, PersistentItemDao {
    override fun getDbName() = DBStructurePersistentItem.PERSISTENT_ITEMS

    override fun getDefaultPrimaryColumn() = DBStructurePersistentItem.Columns.ORIGINAL_PATH // actually ORIGINAL_PATH + STEP
    override fun getDefaultPrimaryValue(persistentObject: PersistentItem): String = persistentObject.task.originalPath

    override fun getContentValues(persistentObject: PersistentItem) = ContentValues().apply {
        put(DBStructurePersistentItem.Columns.ORIGINAL_PATH, persistentObject.task.originalPath)
        put(DBStructurePersistentItem.Columns.LOCAL_FILE_NAME, persistentObject.localFileName)
        put(DBStructurePersistentItem.Columns.LOCAL_FILE_DIR, persistentObject.localFileDir)
        put(DBStructurePersistentItem.Columns.IS_IN_APP_INTERNAL_DIR, if (persistentObject.isInAppInternalDir) 1 else 0)

        put(DBStructurePersistentItem.Columns.DOWNLOAD_ID, persistentObject.downloadId)
        put(DBStructurePersistentItem.Columns.STATUS, persistentObject.status.name)

        put(DBStructurePersistentItem.Columns.COURSE, persistentObject.task.structure.course)
        put(DBStructurePersistentItem.Columns.SECTION, persistentObject.task.structure.section)
        put(DBStructurePersistentItem.Columns.UNIT, persistentObject.task.structure.unit)
        put(DBStructurePersistentItem.Columns.LESSON, persistentObject.task.structure.lesson)
        put(DBStructurePersistentItem.Columns.STEP, persistentObject.task.structure.step)
    }

    override fun parsePersistentObject(cursor: Cursor) = PersistentItem(
            localFileName = cursor.getString(cursor.getColumnIndex(DBStructurePersistentItem.Columns.LOCAL_FILE_NAME)),
            localFileDir  = cursor.getString(cursor.getColumnIndex(DBStructurePersistentItem.Columns.LOCAL_FILE_DIR)),
            isInAppInternalDir = cursor.getBoolean(DBStructurePersistentItem.Columns.IS_IN_APP_INTERNAL_DIR),
            downloadId    = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.DOWNLOAD_ID)),

            status        = PersistentItem.Status.valueOf(cursor.getString(cursor.getColumnIndex(DBStructurePersistentItem.Columns.STATUS))),

            task = DownloadTask(
                    originalPath = cursor.getString(cursor.getColumnIndex(DBStructurePersistentItem.Columns.ORIGINAL_PATH)),
                    structure    = Structure(
                            course  = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.COURSE)),
                            section = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.SECTION)),
                            unit    = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.UNIT)),
                            lesson  = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.LESSON)),
                            step    = cursor.getLong(cursor.getColumnIndex(DBStructurePersistentItem.Columns.STEP))
                    )
            )
    )

    override fun getItems(selector: Map<String, String>): Single<List<PersistentItem>> =
        Single.fromCallable { if (selector.isEmpty()) getAll() else getAll(selector) }

    override fun getItem(selector: Map<String, String>): Maybe<PersistentItem> = Maybe.create { emitter ->
        get(selector)?.let(emitter::onSuccess) ?: emitter.onComplete()
    }

    override fun getAllCorrectItems(): Observable<List<PersistentItem>> = Observable.fromCallable {
        val statuses = PersistentItem.Status.values().filter(PersistentItem.Status::isCorrect).joinToString { "'${it.name}'" }
        getAllInRange(DBStructurePersistentItem.Columns.STATUS, statuses)
    }
}