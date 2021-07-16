package org.stepik.android.view.catalog.ui.adapter.delegate

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.header_catalog_block.view.*
import kotlinx.android.synthetic.main.item_course_list_new.*
import kotlinx.android.synthetic.main.item_course_list_new.view.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.ui.util.CoursesSnapHelper
import org.stepik.android.domain.catalog.model.CatalogBlock
import org.stepik.android.domain.catalog.model.CatalogBlockContent
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.domain.course_payments.mapper.DefaultPromoCodeMapper
import org.stepik.android.model.Course
import org.stepik.android.presentation.course_continue.model.CourseContinueInteractionSource
import org.stepik.android.presentation.course_list_redux.CourseListFeature
import org.stepik.android.presentation.course_list_redux.model.CatalogBlockStateWrapper
import org.stepik.android.view.base.ui.adapter.layoutmanager.TableLayoutManager
import org.stepik.android.view.catalog.mapper.CourseCountMapper
import org.stepik.android.view.catalog.model.CatalogItem
import org.stepik.android.view.catalog.ui.delegate.CatalogBlockHeaderDelegate
import org.stepik.android.view.course.mapper.DisplayPriceMapper
import org.stepik.android.view.course_list.resolver.TableLayoutHorizontalSpanCountResolver
import org.stepik.android.view.course_list.ui.adapter.delegate.CourseListItemAdapterDelegate
import org.stepik.android.view.course_list.ui.adapter.delegate.CourseListPlaceHolderAdapterDelegate
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.ui.adapterdelegates.AdapterDelegate
import ru.nobird.android.ui.adapterdelegates.DelegateViewHolder
import ru.nobird.android.ui.adapters.DefaultDelegateAdapter

class RecommendedCourseListAdapterDelegate
@AssistedInject
constructor(
    private val analytic: Analytic,
    private val courseCountMapper: CourseCountMapper,
    private val defaultPromoCodeMapper: DefaultPromoCodeMapper,
    private val displayPriceMapper: DisplayPriceMapper,
    @Assisted private val isHandleInAppPurchase: Boolean,
    @Assisted private val onBlockSeen: (String) -> Unit,
    @Assisted private val onCourseContinueClicked: (Course, CourseViewSource, CourseContinueInteractionSource) -> Unit,
    @Assisted private val onCourseClicked: (CourseListItem.Data) -> Unit,
    private val tableLayoutHorizontalSpanCountResolver: TableLayoutHorizontalSpanCountResolver
) : AdapterDelegate<CatalogItem, DelegateViewHolder<CatalogItem>>() {
    private val sharedViewPool = RecyclerView.RecycledViewPool()

    override fun isForViewType(position: Int, data: CatalogItem): Boolean =
        data is CatalogItem.Block && data.catalogBlockStateWrapper is CatalogBlockStateWrapper.RecommendedCourseList

    override fun onCreateViewHolder(parent: ViewGroup): DelegateViewHolder<CatalogItem> =
        CourseRecommendationsViewHolder(createView(parent, R.layout.item_course_list_new))

    private inner class CourseRecommendationsViewHolder(
        override val containerView: View
    ) : DelegateViewHolder<CatalogItem>(containerView), LayoutContainer {
        private var catalogBlock: CatalogBlock? = null

        private val courseListTitleContainer = containerView.catalogBlockContainer
        private val catalogBlockTitleDelegate = CatalogBlockHeaderDelegate(courseListTitleContainer)

        private val skeletonCount = context.resources.getInteger(R.integer.course_list_rows) * context.resources.getInteger(R.integer.course_list_columns)
        private val courseItemsSkeleton: List<CourseListItem> = List(skeletonCount) { CourseListItem.PlaceHolder() }
        private val courseItemAdapter: DefaultDelegateAdapter<CourseListItem> = DefaultDelegateAdapter()
        private val viewStateDelegate = ViewStateDelegate<CourseListFeature.State>()

        private var tableLayoutManager: TableLayoutManager

        init {
            viewStateDelegate.addState<CourseListFeature.State.Idle>(courseListCoursesRecycler)
            viewStateDelegate.addState<CourseListFeature.State.Loading>(courseListTitleContainer, courseListCoursesRecycler)
            viewStateDelegate.addState<CourseListFeature.State.Content>(courseListTitleContainer, courseListCoursesRecycler)
            viewStateDelegate.addState<CourseListFeature.State.Empty>()
            viewStateDelegate.addState<CourseListFeature.State.NetworkError>()

            courseItemAdapter += CourseListPlaceHolderAdapterDelegate()
            courseItemAdapter += CourseListItemAdapterDelegate(
                analytic = analytic,
                onItemClicked = { courseListItem ->  onCourseClicked(courseListItem) },
                onContinueCourseClicked = {
                    onCourseContinueClicked(it.course, CourseViewSource.Recommendation, CourseContinueInteractionSource.COURSE_WIDGET)
                },
                isHandleInAppPurchase = isHandleInAppPurchase,
                defaultPromoCodeMapper = defaultPromoCodeMapper,
                displayPriceMapper = displayPriceMapper
            )

            val rowCount = context.resources.getInteger(R.integer.course_list_rows)
            val columnsCount = context.resources.getInteger(R.integer.course_list_columns)
            tableLayoutManager = TableLayoutManager(context, columnsCount, rowCount, RecyclerView.HORIZONTAL, false)

            with(courseListCoursesRecycler) {
                adapter = courseItemAdapter
                layoutManager = tableLayoutManager
                itemAnimator?.changeDuration = 0
                val snapHelper = CoursesSnapHelper(rowCount)
                snapHelper.attachToRecyclerView(this)
                setRecycledViewPool(sharedViewPool)
            }
        }

        override fun onBind(data: CatalogItem) {
            data as CatalogItem.Block
            val catalogBlockCourseListItem = data.catalogBlockStateWrapper as CatalogBlockStateWrapper.RecommendedCourseList
            initLoading(catalogBlockCourseListItem)
            catalogBlock = catalogBlockCourseListItem.catalogBlockItem
            catalogBlockTitleDelegate.setInformation(catalogBlockCourseListItem.catalogBlockItem)
            render(catalogBlockCourseListItem.state)
        }

        private fun render(state: CourseListFeature.State) {
            viewStateDelegate.switchState(state)
            when (state) {
                is CourseListFeature.State.Idle -> {
                    courseItemAdapter.items = courseItemsSkeleton
                }

                is CourseListFeature.State.Loading -> {
                    courseItemAdapter.items = courseItemsSkeleton
                }

                is CourseListFeature.State.Content -> {
                    courseItemAdapter.items = state.courseListItems
                    catalogBlockTitleDelegate.setCount(courseCountMapper.mapCourseCountToString(context, state.courseListItems.size))
                    tableLayoutHorizontalSpanCountResolver.resolveSpanCount(courseItemAdapter.itemCount).let { resolvedSpanCount ->
                        if (tableLayoutManager.spanCount != resolvedSpanCount) {
                            tableLayoutManager.spanCount = resolvedSpanCount
                        }
                    }
                }

                else ->
                    courseItemAdapter.items = emptyList()
            }
        }
    }

    private fun initLoading(catalogBlockFullCourseList: CatalogBlockStateWrapper.RecommendedCourseList) {
        if (catalogBlockFullCourseList.catalogBlockItem.content !is CatalogBlockContent.RecommendedCourses) {
            return
        }
        onBlockSeen(catalogBlockFullCourseList.id)
    }
}