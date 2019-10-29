package org.stepic.droid.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.stepic.droid.R

class LogoutAreYouSureDialog : DialogFragment() {
    companion object {
        fun newInstance(): LogoutAreYouSureDialog =
                LogoutAreYouSureDialog()

        interface OnLogoutSuccessListener {
            fun onLogout()
        }
    }

    var listener: OnLogoutSuccessListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnLogoutSuccessListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_confirmation)
            .setMessage(R.string.are_you_sure_logout)
            .setPositiveButton(R.string.yes) { _, _ ->
                listener?.onLogout()
            }
            .setNegativeButton(R.string.no, null)
            .create()
}
