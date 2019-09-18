package org.stepik.android.domain.step_content_text.interactor

import io.reactivex.Single
import org.stepic.droid.preferences.SharedPreferenceHelper
import javax.inject.Inject

class TextContentFontInteractor
@Inject
constructor(
    private val sharedPreferenceHelper: SharedPreferenceHelper
) {
    fun fetchTextContentFontSize(): Single<Float> =
        Single.fromCallable {
            sharedPreferenceHelper.stepContentFontSize
        }
}