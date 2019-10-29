package org.stepic.droid.persistence.content

import io.reactivex.Single
import org.stepic.droid.persistence.model.DownloadConfiguration
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepik.android.model.Step

interface StepContentResolver {
    fun getDownloadableContentFromStep(step: Step, configuration: DownloadConfiguration): Set<String>
    fun resolvePersistentContent(step: Step): Single<StepPersistentWrapper>
}