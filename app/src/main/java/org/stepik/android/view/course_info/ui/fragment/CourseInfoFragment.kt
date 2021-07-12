package org.stepik.android.view.course_info.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_course_info.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepik.android.presentation.course_info.CourseInfoPresenter
import org.stepik.android.presentation.course_info.CourseInfoView
import org.stepik.android.view.course_info.mapper.toSortedItems
import org.stepik.android.view.course_info.ui.adapter.CourseInfoAdapter
import org.stepik.android.view.course_info.ui.adapter.decorators.CourseInfoBlockOffsetDecorator
import org.stepik.android.view.course_info.ui.adapter.delegates.CourseInfoInstructorsDelegate
import org.stepik.android.view.course_info.ui.adapter.delegates.CourseInfoTextBlockDelegate
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.view.base.ui.extension.argument
import javax.inject.Inject

class CourseInfoFragment : Fragment(), CourseInfoView {
    companion object {
        fun newInstance(courseId: Long): Fragment =
            CourseInfoFragment().apply {
                this.courseId = courseId
            }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var screenManager: ScreenManager

    private var courseId: Long by argument()

    private lateinit var courseInfoAdapter: CourseInfoAdapter
    private val courseInfoPresenter: CourseInfoPresenter by viewModels { viewModelFactory }

    private lateinit var viewStateDelegate: ViewStateDelegate<CourseInfoView.State>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent(courseId)

        savedInstanceState?.let(courseInfoPresenter::onRestoreInstanceState)

        courseInfoAdapter = CourseInfoAdapter(
            onVideoClicked = { mediaData ->
                screenManager.showVideo(this, mediaData, null)
            },
            onUserClicked = { user ->
                screenManager.openProfile(requireContext(), user.id)
            }
        )
    }

    private fun injectComponent(courseId: Long) {
        App.componentManager()
            .courseComponent(courseId)
            .inject(this)
    }

    private fun releaseComponent(courseId: Long) {
        App.componentManager()
            .releaseCourseComponent(courseId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_course_info, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        courseInfoRecycler.layoutManager = LinearLayoutManager(context)
        courseInfoRecycler.adapter = courseInfoAdapter

        courseInfoRecycler.addItemDecoration(
                CourseInfoBlockOffsetDecorator(resources.getDimension(R.dimen.course_info_block_margin).toInt(), intArrayOf(
                        courseInfoAdapter.delegates.indexOfFirst { it is CourseInfoTextBlockDelegate },
                        courseInfoAdapter.delegates.indexOfFirst { it is CourseInfoInstructorsDelegate }
                )))

        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<CourseInfoView.State.Loading>(courseInfoLoadingPlaceholder)
        viewStateDelegate.addState<CourseInfoView.State.CourseInfoLoaded>(courseInfoRecycler)
    }

    override fun onStart() {
        super.onStart()
        courseInfoPresenter.attachView(this)
    }

    override fun onStop() {
        courseInfoPresenter.detachView(this)
        super.onStop()
    }

    override fun setState(state: CourseInfoView.State) {
        viewStateDelegate.switchState(state)
        if (state is CourseInfoView.State.CourseInfoLoaded) {
            courseInfoAdapter.setSortedData(state.courseInfoData.toSortedItems(requireContext()))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        courseInfoPresenter.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        releaseComponent(courseId)
        super.onDestroy()
    }
}