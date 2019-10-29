package org.stepic.droid.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_feedback.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.base.FragmentBase
import org.stepic.droid.util.DeviceInfoUtil
import org.stepik.android.domain.feedback.model.SupportEmailData
import org.stepik.android.presentation.feedback.FeedbackPresenter
import org.stepik.android.presentation.feedback.FeedbackView
import javax.inject.Inject

class FeedbackFragment : FragmentBase(), FeedbackView {
    companion object {
        fun newInstance(): FeedbackFragment {
            val args = Bundle()

            val fragment = FeedbackFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var feedbackPresenter: FeedbackPresenter

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun injectComponent() {
        App.component()
            .feedbackComponentBuilder()
            .build()
            .inject(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedbackPresenter = ViewModelProviders
            .of(this, viewModelFactory)
            .get(FeedbackPresenter::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_feedback, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    override fun onStart() {
        super.onStart()
        feedbackPresenter.attachView(this)
    }

    override fun onStop() {
        feedbackPresenter.detachView(this)
        super.onStop()
    }

    override fun onDestroyView() {
        destroyButtons()
        super.onDestroyView()
    }

    override fun sendTextFeedback(supportEmailData: SupportEmailData) {
        screenManager.openTextFeedBack(requireContext(), supportEmailData)
    }

    private fun initButtons() {
        feedbackGoodButton.setOnClickListener {
            if (config.isAppInStore) {
                screenManager.showStoreWithApp(activity)
            } else {
                setupTextFeedback()
            }
        }
        feedbackBadButton.setOnClickListener { setupTextFeedback() }
    }

    private fun setupTextFeedback() {
        feedbackPresenter.sendTextFeedback(
            getString(R.string.feedback_subject),
            DeviceInfoUtil.getInfosAboutDevice(context, "\n")
        )
    }

    private fun destroyButtons() {
        feedbackGoodButton.setOnClickListener(null)
        feedbackBadButton.setOnClickListener(null)
    }

}