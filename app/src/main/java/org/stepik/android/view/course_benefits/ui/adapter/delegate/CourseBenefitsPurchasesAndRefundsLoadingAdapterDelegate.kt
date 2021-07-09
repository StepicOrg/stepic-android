package org.stepik.android.view.course_benefits.ui.adapter.delegate

import android.view.View
import android.view.ViewGroup
import org.stepic.droid.R
import org.stepik.android.domain.course_benefits.model.CourseBenefitListItem
import ru.nobird.android.ui.adapterdelegates.AdapterDelegate
import ru.nobird.android.ui.adapterdelegates.DelegateViewHolder

class CourseBenefitsPurchasesAndRefundsLoadingAdapterDelegate : AdapterDelegate<CourseBenefitListItem, DelegateViewHolder<CourseBenefitListItem>>() {
    override fun isForViewType(position: Int, data: CourseBenefitListItem): Boolean =
        data is CourseBenefitListItem.Placeholder

    override fun onCreateViewHolder(parent: ViewGroup): DelegateViewHolder<CourseBenefitListItem> =
        ViewHolder(createView(parent, R.layout.item_skeleton_purchase_refund))

    private class ViewHolder(root: View) : DelegateViewHolder<CourseBenefitListItem>(root)
}