package org.stepic.droid.core

import androidx.annotation.StringRes
import org.stepic.droid.R

enum class DeadlineType(@StringRes val deadlineTitle: Int) {
    softDeadline(R.string.soft_deadline),
    hardDeadline(R.string.hard_deadline)
}