package androidx.viewpager.widget

import android.content.Context
import android.util.AttributeSet

class StepikSlowViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {

    companion object {
        private const val VELOCITY = 200
    }

    /**
     * NOTE: it is easiest way to handle velocity of ViewPager. Be care it is in android.support.v4.view
     * it is needed for changing velocity on setting current item programatically
     */

    override fun setCurrentItemInternal(item: Int, smoothScroll: Boolean, always: Boolean) {
        setCurrentItemInternal(item, smoothScroll, always, VELOCITY)
    }
}