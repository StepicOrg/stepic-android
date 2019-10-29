package org.stepic.droid.ui.activities

import android.view.MenuItem
import androidx.fragment.app.Fragment
import org.stepic.droid.R
import org.stepic.droid.base.SingleFragmentActivity
import org.stepic.droid.ui.fragments.PhotoViewFragment

class PhotoViewActivity : SingleFragmentActivity() {
    companion object {
        const val pathKey = "pathKey"
    }

    override fun createFragment(): Fragment {
        val path = intent.getStringExtra(pathKey)
        return PhotoViewFragment.newInstance(path)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_transition, R.anim.slide_out_to_bottom)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
}
