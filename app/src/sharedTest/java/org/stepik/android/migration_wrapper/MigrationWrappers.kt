package org.stepik.android.migration_wrapper

import org.stepic.droid.storage.migration.MigrationFrom62To63
import org.stepic.droid.storage.migration.MigrationFrom63To64
import org.stepic.droid.storage.migration.MigrationFrom64To65
import org.stepic.droid.storage.migration.Migrations

object MigrationWrappers {
    private const val LAST_TESTED_DATABASE_VERSION = 63
    private val oldMigrations =
        Migrations
            .migrations
            .slice(0 until LAST_TESTED_DATABASE_VERSION - 1)
            .map { object : MigrationWrapper(it) {} } as List<MigrationWrapper>


    val allMigration = oldMigrations + MigrationWrapperFrom62To63(MigrationFrom62To63) +
            listOf(
                object : MigrationWrapper(MigrationFrom63To64) {},
                object : MigrationWrapper(MigrationFrom64To65) {}
            )
}