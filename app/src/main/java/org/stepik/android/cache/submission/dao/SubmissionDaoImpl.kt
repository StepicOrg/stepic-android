package org.stepik.android.cache.submission.dao

import android.content.ContentValues
import android.database.Cursor
import com.google.gson.GsonBuilder
import org.stepic.droid.jsonHelpers.adapters.UTCDateAdapter
import org.stepic.droid.jsonHelpers.deserializers.FeedbackDeserializer
import org.stepic.droid.jsonHelpers.deserializers.ReplyDeserializer
import org.stepic.droid.jsonHelpers.serializers.ReplySerializer
import org.stepic.droid.storage.dao.DaoBase
import org.stepic.droid.storage.operations.DatabaseOperations
import org.stepic.droid.util.getInt
import org.stepic.droid.util.getLong
import org.stepic.droid.util.getString
import org.stepic.droid.util.toObject
import org.stepik.android.cache.submission.structure.DbStructureSubmission
import org.stepik.android.model.ReplyWrapper
import org.stepik.android.model.Submission
import org.stepik.android.model.feedback.Feedback
import java.util.Date
import javax.inject.Inject

class SubmissionDaoImpl
@Inject
constructor(
    databaseOperations: DatabaseOperations
) : DaoBase<Submission>(databaseOperations) {
    private val gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .registerTypeAdapter(ReplyWrapper::class.java, ReplyDeserializer())
        .registerTypeAdapter(ReplyWrapper::class.java, ReplySerializer())
        .registerTypeAdapter(Date::class.java, UTCDateAdapter())
        .registerTypeAdapter(Feedback::class.java, FeedbackDeserializer())
        .create()

    override fun getDbName(): String =
        DbStructureSubmission.TABLE_NAME

    override fun getDefaultPrimaryColumn(): String =
        DbStructureSubmission.Columns.ATTEMPT_ID

    override fun getDefaultPrimaryValue(persistentObject: Submission): String =
        persistentObject.attempt.toString()

    override fun parsePersistentObject(cursor: Cursor): Submission =
        Submission(
            id = cursor.getLong(DbStructureSubmission.Columns.ID),
            status = Submission.Status.values()[cursor.getInt(DbStructureSubmission.Columns.STATUS)],
            score = cursor.getString(DbStructureSubmission.Columns.SCORE),
            hint = cursor.getString(DbStructureSubmission.Columns.HINT),
            time = cursor.getString(DbStructureSubmission.Columns.TIME),
            _reply = cursor.getString(DbStructureSubmission.Columns.REPLY)?.toObject(gson),
            attempt = cursor.getLong(DbStructureSubmission.Columns.ATTEMPT_ID),
            session = cursor.getString(DbStructureSubmission.Columns.SESSION),
            eta = cursor.getString(DbStructureSubmission.Columns.ETA),
            feedback = cursor.getString(DbStructureSubmission.Columns.FEEDBACK)?.toObject(gson)
        )

    override fun getContentValues(persistentObject: Submission): ContentValues =
        ContentValues().apply {
            put(DbStructureSubmission.Columns.ID, persistentObject.id)
            put(DbStructureSubmission.Columns.STATUS, persistentObject.status?.ordinal)
            put(DbStructureSubmission.Columns.SCORE, persistentObject.score)
            put(DbStructureSubmission.Columns.HINT, persistentObject.hint)
            put(DbStructureSubmission.Columns.TIME, persistentObject.time)
            put(DbStructureSubmission.Columns.REPLY, persistentObject.reply?.let(gson::toJson))
            put(DbStructureSubmission.Columns.ATTEMPT_ID, persistentObject.attempt)
            put(DbStructureSubmission.Columns.SESSION, persistentObject.session)
            put(DbStructureSubmission.Columns.ETA, persistentObject.eta)
            put(DbStructureSubmission.Columns.FEEDBACK, persistentObject.feedback?.let(gson::toJson))
        }
}