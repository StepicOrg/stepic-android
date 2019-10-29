package org.stepic.droid.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import biz.kasual.materialnumberpicker.MaterialNumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepic.droid.ui.util.TimeIntervalUtil
import org.stepic.droid.util.SuppressFBWarnings
import timber.log.Timber
import javax.inject.Inject

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
class TimeIntervalPickerDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "time_interval_picker_dialog"
        private const val CHOSEN_POSITION_KEY = "CHOSEN_POSITION_KEY"
        fun newInstance(): TimeIntervalPickerDialogFragment =
                TimeIntervalPickerDialogFragment()

        interface Callback {
            fun onTimeIntervalPicked(chosenInterval: Int)
        }
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferenceHelper

    lateinit var picker: MaterialNumberPicker

    private lateinit var callback: Callback

    init {
        App.component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CHOSEN_POSITION_KEY, picker.value)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        picker = MaterialNumberPicker(context)
        picker.minValue = 0
        picker.maxValue = TimeIntervalUtil.values.size - 1
        picker.displayedValues = TimeIntervalUtil.values
        picker.value = savedInstanceState?.getInt(CHOSEN_POSITION_KEY) ?: sharedPreferences.timeNotificationCode
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        picker.wrapSelectorWheel = false
        try {
            picker.setTextSize(50f) //Warning: reflection!
        } catch (exception: Exception) {
            Timber.e("reflection failed -> ignore")
        }

        callback = if (targetFragment != null) {
            targetFragment as Callback
        } else {
            activity as Callback
        }

        return MaterialDialog.Builder(requireContext())
            .theme(Theme.LIGHT)
            .title(R.string.choose_notification_time_interval)
            .customView(picker, false)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                //todo set result to Ok with position
                callback.onTimeIntervalPicked(picker.value)
            }
            .build()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // explicitly click Negative or cancel by back button || touch outside
        callback.onTimeIntervalPicked(sharedPreferences.timeNotificationCode)
    }

}
