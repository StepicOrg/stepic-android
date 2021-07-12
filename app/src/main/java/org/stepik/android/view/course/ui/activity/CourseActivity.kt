package org.stepik.android.view.course.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import kotlinx.android.synthetic.main.activity_course.*
import kotlinx.android.synthetic.main.error_course_not_found.*
import kotlinx.android.synthetic.main.error_no_connection_with_button.*
import kotlinx.android.synthetic.main.header_course.*
import kotlinx.android.synthetic.main.header_course_placeholder.*
import org.solovyev.android.checkout.Billing
import org.solovyev.android.checkout.Checkout
import org.solovyev.android.checkout.UiCheckout
import org.stepic.droid.R
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.analytic.experiments.CoursePurchaseWebviewSplitTest
import org.stepic.droid.analytic.experiments.InAppPurchaseSplitTest
import org.stepic.droid.base.App
import org.stepic.droid.base.FragmentActivityBase
import org.stepic.droid.configuration.RemoteConfig
import org.stepic.droid.ui.dialogs.LoadingProgressDialogFragment
import org.stepic.droid.ui.dialogs.UnauthorizedDialogFragment
import org.stepic.droid.ui.util.snackbar
import org.stepic.droid.util.ProgressHelper
import org.stepic.droid.util.resolveColorAttribute
import org.stepik.android.domain.course.analytic.CourseJoinedEvent
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.last_step.model.LastStep
import org.stepik.android.domain.purchase_notification.analytic.PurchaseNotificationClicked
import org.stepik.android.model.Course
import org.stepik.android.presentation.course.CoursePresenter
import org.stepik.android.presentation.course.CourseView
import org.stepik.android.presentation.course.model.EnrollmentError
import org.stepik.android.presentation.user_courses.model.UserCourseAction
import org.stepik.android.presentation.wishlist.model.WishlistAction
import org.stepik.android.view.base.web.CustomTabsHelper
import org.stepik.android.view.course.routing.CourseDeepLinkBuilder
import org.stepik.android.view.course.routing.CourseScreenTab
import org.stepik.android.view.course.routing.getCourseIdFromDeepLink
import org.stepik.android.view.course.routing.getCourseTabFromDeepLink
import org.stepik.android.view.course.routing.getPromoCodeFromDeepLink
import org.stepik.android.view.course.ui.adapter.CoursePagerAdapter
import org.stepik.android.view.course.ui.delegates.CourseHeaderDelegate
import org.stepik.android.view.course_content.ui.fragment.CourseContentFragment
import org.stepik.android.view.course_reviews.ui.fragment.CourseReviewsFragment
import org.stepik.android.view.fragment_pager.FragmentDelegateScrollStateChangeListener
import org.stepik.android.view.in_app_web_view.ui.dialog.InAppWebViewDialogFragment
import org.stepik.android.view.injection.course.CourseHeaderDelegateFactory
import org.stepik.android.view.magic_links.ui.dialog.MagicLinkDialogFragment
import org.stepik.android.view.purchase_notification.notification.PurchaseNotificationDelegate
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.view.base.ui.extension.showIfNotExists
import javax.inject.Inject

class CourseActivity : FragmentActivityBase(), CourseView, InAppWebViewDialogFragment.Callback, MagicLinkDialogFragment.Callback {
    companion object {
        private const val EXTRA_COURSE = "course"
        private const val EXTRA_COURSE_ID = "course_id"
        private const val EXTRA_AUTO_ENROLL = "auto_enroll"
        private const val EXTRA_TAB = "tab"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_OPEN_COURSE_PURCHASE = "open_course_purchase"

        private const val NO_ID = -1L

        private const val UNAUTHORIZED_DIALOG_TAG = "unauthorized_dialog"

        fun createIntent(context: Context, course: Course, source: CourseViewSource, autoEnroll: Boolean = false, tab: CourseScreenTab = CourseScreenTab.INFO): Intent =
            Intent(context, CourseActivity::class.java)
                .putExtra(EXTRA_COURSE, course)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_AUTO_ENROLL, autoEnroll)
                .putExtra(EXTRA_TAB, tab.ordinal)

        fun createIntent(context: Context, courseId: Long, source: CourseViewSource, tab: CourseScreenTab = CourseScreenTab.INFO, openCoursePurchase: Boolean = false): Intent =
            Intent(context, CourseActivity::class.java)
                .putExtra(EXTRA_COURSE_ID, courseId)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_TAB, tab.ordinal)
                .putExtra(EXTRA_OPEN_COURSE_PURCHASE, openCoursePurchase)

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    private var courseId: Long = NO_ID
    private val analyticsOnPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(page: Int) {
            when (coursePagerAdapter.getItem(page)) {
                is CourseReviewsFragment ->
                    analytic
                        .reportAmplitudeEvent(
                            AmplitudeAnalytic.CourseReview.SCREEN_OPENED,
                            mapOf(AmplitudeAnalytic.CourseReview.Params.COURSE to courseId.toString())
                        )

                is CourseContentFragment ->
                    analytic
                        .reportAmplitudeEvent(
                            AmplitudeAnalytic.Sections.SCREEN_OPENED,
                            mapOf(AmplitudeAnalytic.Sections.Params.COURSE to courseId.toString())
                        )
            }
        }
    }
    private lateinit var coursePagerAdapter: CoursePagerAdapter
    private val coursePresenter: CoursePresenter by viewModels { viewModelFactory }
    private lateinit var courseHeaderDelegate: CourseHeaderDelegate

    private var unauthorizedDialogFragment: DialogFragment? = null
    private val progressDialogFragment: DialogFragment =
        LoadingProgressDialogFragment.newInstance()

    private val viewStateDelegate =
        ViewStateDelegate<CourseView.State>()

    private var viewPagerScrollState: Int =
        ViewPager.SCROLL_STATE_IDLE

    private var isInSwipeableViewState = false

    private var hasSavedInstanceState: Boolean = false

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    @Inject
    internal lateinit var coursePurchaseWebviewSplitTest: CoursePurchaseWebviewSplitTest

    @Inject
    internal lateinit var inAppPurchaseSplitTest: InAppPurchaseSplitTest

    @Inject
    internal lateinit var courseDeeplinkBuilder: CourseDeepLinkBuilder

    @Inject
    internal lateinit var billing: Billing

    @Inject
    internal lateinit var courseHeaderDelegateFactory: CourseHeaderDelegateFactory

    private lateinit var uiCheckout: UiCheckout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)

        setSupportActionBar(courseToolbar)
        val actionBar = this.supportActionBar
            ?: throw IllegalStateException("support action bar should be set")

        with(actionBar) {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        val course: Course? = intent.getParcelableExtra(EXTRA_COURSE)
        val deepLinkCourseId = intent.getCourseIdFromDeepLink()

        if (savedInstanceState == null && deepLinkCourseId != null) {
            analytic.reportEvent(Analytic.DeepLink.USER_OPEN_COURSE_DETAIL_LINK, deepLinkCourseId.toString())
            analytic.reportEvent(Analytic.DeepLink.USER_OPEN_LINK_GENERAL)

            // handle wrong deeplink interceptions
            intent.data?.let { uri -> screenManager.redirectToWebBrowserIfNeeded(this, uri) }
        }

        if (course != null) {
            courseToolbarTitle.text = course.title
        }

        courseId = intent.getLongExtra(EXTRA_COURSE_ID, NO_ID)
            .takeIf { it != NO_ID }
            ?: course?.id
            ?: deepLinkCourseId
            ?: NO_ID

        injectComponent(courseId)

        if (intent.action == PurchaseNotificationDelegate.NOTIFICATION_CLICKED) {
            analytic.report(PurchaseNotificationClicked(courseId))
        }

        val courseViewSource = (intent.getSerializableExtra(EXTRA_SOURCE) as? CourseViewSource)
            ?: intent.getCourseIdFromDeepLink()?.let { CourseViewSource.DeepLink(intent?.dataString ?: "") }
            ?: CourseViewSource.Unknown

        courseHeaderDelegate =
            courseHeaderDelegateFactory
                .create(
                    courseActivity = this,
                    coursePresenter = coursePresenter,
                    courseViewSource = courseViewSource,
                    isAuthorized = sharedPreferenceHelper.authResponseFromStore != null,
//                    mustShowCourseBenefits = true,
                    // TODO APPS-3336 Uncomment code
                    mustShowCourseBenefits = firebaseRemoteConfig.getBoolean(RemoteConfig.IS_COURSE_REVENUE_AVAILABLE_ANDROID) && course?.actions?.viewRevenue?.enabled == true,
                    showCourseBenefitsAction = { screenManager.showCourseBenefits(this, courseId, course?.title) },
                    onSubmissionCountClicked = {
                        screenManager.showCachedAttempts(this, courseId)
                    },
                    isLocalSubmissionsEnabled = firebaseRemoteConfig[RemoteConfig.IS_LOCAL_SUBMISSIONS_ENABLED].asBoolean()
                )

        uiCheckout = Checkout.forActivity(this, billing)
        initViewPager(courseId)
        initViewStateDelegate()

        hasSavedInstanceState = savedInstanceState != null

        setDataToPresenter(courseViewSource)

        courseSwipeRefresh.setOnRefreshListener {
            setDataToPresenter(courseViewSource, forceUpdate = true)
        }
        tryAgain.setOnClickListener { setDataToPresenter(courseViewSource, forceUpdate = true) }
        goToCatalog.setOnClickListener {
            screenManager.showCatalog(this)
            finish()
        }
    }

    private fun setDataToPresenter(courseViewSource: CourseViewSource, forceUpdate: Boolean = false) {
        val course: Course? = intent.getParcelableExtra(EXTRA_COURSE)

        if (course != null) {
            coursePresenter.onCourse(course, courseViewSource, forceUpdate)
        } else {
            coursePresenter.onCourseId(courseId, courseViewSource, intent.getPromoCodeFromDeepLink(), forceUpdate)
        }
    }

    private fun injectComponent(courseId: Long) {
        App.componentManager()
            .courseComponent(courseId)
            .coursePresentationComponentBuilder()
            .build()
            .inject(this)
    }

    private fun releaseComponent(courseId: Long) {
        App.componentManager()
            .releaseCourseComponent(courseId)
    }

    override fun onStart() {
        super.onStart()
        coursePresenter.attachView(this)
    }

    override fun onResume() {
        super.onResume()
        if (coursePurchaseWebviewSplitTest.currentGroup != CoursePurchaseWebviewSplitTest.Group.InAppWebview) {
            coursePresenter.handleCoursePurchasePressed()
        }
        coursePager.addOnPageChangeListener(analyticsOnPageChangeListener)
        if (!hasSavedInstanceState) {
            setCurrentTab()
        }
    }

    override fun onPause() {
        coursePager.removeOnPageChangeListener(analyticsOnPageChangeListener)
        super.onPause()
    }

    override fun onStop() {
        coursePresenter.detachView(this)
        super.onStop()
    }

    private fun setCurrentTab() {
        val tab = CourseScreenTab
            .values()
            .getOrNull(intent.getIntExtra(EXTRA_TAB, -1))
            ?: intent.getCourseTabFromDeepLink()

        coursePager.currentItem =
            when (tab) {
                CourseScreenTab.REVIEWS -> 1
                CourseScreenTab.SYLLABUS -> 2
                else -> 0
            }
        if (coursePager.currentItem == 0) {
            analyticsOnPageChangeListener.onPageSelected(0)
        }
    }

    private fun initViewPager(courseId: Long) {
        coursePagerAdapter = CoursePagerAdapter(courseId, this, supportFragmentManager)
        coursePager.adapter = coursePagerAdapter
        val onPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageScrollStateChanged(scrollState: Int) {
                viewPagerScrollState = scrollState
                resolveSwipeRefreshState()
            }
        }
        coursePager.addOnPageChangeListener(FragmentDelegateScrollStateChangeListener(coursePager, coursePagerAdapter))
        coursePager.addOnPageChangeListener(onPageChangeListener)

        courseTabs.setupWithViewPager(coursePager)
    }

    private fun initViewStateDelegate() {
        viewStateDelegate.addState<CourseView.State.EmptyCourse>(courseEmpty)
        viewStateDelegate.addState<CourseView.State.NetworkError>(errorNoConnection)
        viewStateDelegate.addState<CourseView.State.CourseLoaded>(courseHeader, courseTabs, coursePager)
        viewStateDelegate.addState<CourseView.State.BlockingLoading>(courseHeader, courseTabs, coursePager)
        viewStateDelegate.addState<CourseView.State.Loading>(courseHeaderPlaceholder, courseTabs, coursePager)
        viewStateDelegate.addState<CourseView.State.Idle>(courseHeaderPlaceholder, courseTabs, coursePager)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.course_activity_menu, menu)
        courseHeaderDelegate.onOptionsMenuCreated(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            courseHeaderDelegate.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
        }

    override fun applyTransitionPrev() {
        // no-op
    }

    private fun resolveSwipeRefreshState() {
        courseSwipeRefresh.isEnabled =
                viewPagerScrollState == ViewPager.SCROLL_STATE_IDLE &&
                    isInSwipeableViewState
    }

    override fun setState(state: CourseView.State) {
        courseSwipeRefresh.isRefreshing = false
        isInSwipeableViewState = (state is CourseView.State.CourseLoaded || state is CourseView.State.NetworkError)
        resolveSwipeRefreshState()

        when (state) {
            is CourseView.State.CourseLoaded -> {
                courseHeaderDelegate.courseHeaderData = state.courseHeaderData

                if (intent.getBooleanExtra(EXTRA_AUTO_ENROLL, false)) {
                    intent.removeExtra(EXTRA_AUTO_ENROLL)
                    coursePresenter.autoEnroll()

                    analytic.report(
                        CourseJoinedEvent(
                            CourseJoinedEvent.SOURCE_PREVIEW,
                            state.courseHeaderData.course,
                            state.courseHeaderData.stats.isWishlisted
                        )
                    )
                }

                if (intent.getBooleanExtra(EXTRA_OPEN_COURSE_PURCHASE, false)) {
                    coursePresenter.openCoursePurchaseInWeb()
                }

                ProgressHelper.dismiss(supportFragmentManager, LoadingProgressDialogFragment.TAG)
            }

            is CourseView.State.BlockingLoading -> {
                courseHeaderDelegate.courseHeaderData = state.courseHeaderData
                ProgressHelper.activate(progressDialogFragment, supportFragmentManager, LoadingProgressDialogFragment.TAG)
            }
        }
        viewStateDelegate.switchState(state)
        invalidateOptionsMenu()
    }

    override fun showEmptyAuthDialog(course: Course) {
        if (unauthorizedDialogFragment?.isAdded != true) {
            unauthorizedDialogFragment = UnauthorizedDialogFragment.newInstance(course)
            unauthorizedDialogFragment?.show(supportFragmentManager, UNAUTHORIZED_DIALOG_TAG)
        }
    }

    override fun showEnrollmentError(errorType: EnrollmentError) {
        @StringRes
        val errorMessage =
            when (errorType) {
                EnrollmentError.NO_CONNECTION ->
                    R.string.course_error_enroll

                EnrollmentError.FORBIDDEN ->
                    R.string.join_course_web_exception

                EnrollmentError.UNAUTHORIZED ->
                    R.string.unauthorization_detail

                EnrollmentError.SERVER_ERROR ->
                    R.string.course_purchase_server_error

                EnrollmentError.BILLING_ERROR ->
                    R.string.course_purchase_billing_error

                EnrollmentError.BILLING_CANCELLED ->
                    R.string.course_purchase_billing_cancelled

                EnrollmentError.BILLING_NOT_AVAILABLE ->
                    R.string.course_purchase_billing_not_available

                EnrollmentError.COURSE_ALREADY_OWNED ->
                    R.string.course_purchase_already_owned

                EnrollmentError.BILLING_NO_PURCHASES_TO_RESTORE ->
                    R.string.course_purchase_billing_no_purchases_to_restore
            }

        coursePager.snackbar(messageRes = errorMessage)
    }

    override fun shareCourse(course: Course) {
        startActivity(shareHelper.getIntentForCourseSharing(course))
    }

    override fun showCourseShareTooltip() {
        courseHeaderDelegate.showCourseShareTooltip()
    }

    override fun showSaveUserCourseSuccess(userCourseAction: UserCourseAction) {
        @StringRes
        val successMessage =
            when (userCourseAction) {
                UserCourseAction.ADD_FAVORITE ->
                    R.string.course_action_favorites_add_success

                UserCourseAction.REMOVE_FAVORITE ->
                    R.string.course_action_favorites_remove_success

                UserCourseAction.ADD_ARCHIVE ->
                    R.string.course_action_archive_add_success

                UserCourseAction.REMOVE_ARCHIVE ->
                    R.string.course_action_archive_remove_success
            }
        coursePager.snackbar(messageRes = successMessage)
    }

    override fun showSaveUserCourseError(userCourseAction: UserCourseAction) {
        @StringRes
        val errorMessage =
            when (userCourseAction) {
                UserCourseAction.ADD_FAVORITE ->
                    R.string.course_action_favorites_add_failure

                UserCourseAction.REMOVE_FAVORITE ->
                    R.string.course_action_favorites_remove_failure

                UserCourseAction.ADD_ARCHIVE ->
                    R.string.course_action_archive_add_failure

                UserCourseAction.REMOVE_ARCHIVE ->
                    R.string.course_action_archive_remove_failure
            }
        coursePager.snackbar(messageRes = errorMessage)
    }

    override fun showWishlistActionSuccess(wishlistAction: WishlistAction) {
        @StringRes
        val successMessage =
            if (wishlistAction == WishlistAction.ADD) {
                R.string.wishlist_action_add_success
            } else {
                R.string.wishlist_action_remove_success
            }
        coursePager.snackbar(messageRes = successMessage)
    }

    override fun showWishlistActionFailure(wishlistAction: WishlistAction) {
        @StringRes
        val errorMessage =
            if (wishlistAction == WishlistAction.ADD) {
                R.string.wishlist_action_add_failure
            } else {
                R.string.wishlist_action_remove_failure
            }
        coursePager.snackbar(messageRes = errorMessage)
    }

    /**
     * BillingView
     */
    override fun createUiCheckout(): UiCheckout =
        uiCheckout

    override fun openCoursePurchaseInWeb(courseId: Long, queryParams: Map<String, List<String>>?) {
        val url = courseDeeplinkBuilder.createCourseLink(courseId, CourseScreenTab.PAY, queryParams)
        when (coursePurchaseWebviewSplitTest.currentGroup) {
            CoursePurchaseWebviewSplitTest.Group.Control -> {
                MagicLinkDialogFragment
                    .newInstance(url)
                    .showIfNotExists(supportFragmentManager, MagicLinkDialogFragment.TAG)
            }
            CoursePurchaseWebviewSplitTest.Group.InAppWebview -> {
                InAppWebViewDialogFragment
                    .newInstance(getString(R.string.course_purchase), url, isProvideAuth = true)
                    .showIfNotExists(supportFragmentManager, InAppWebViewDialogFragment.TAG)
            }
            CoursePurchaseWebviewSplitTest.Group.ChromeTab -> {
                MagicLinkDialogFragment
                    .newInstance(url, handleUrlInParent = true)
                    .showIfNotExists(supportFragmentManager, MagicLinkDialogFragment.TAG)
            }
        }
    }

    override fun showTrialLesson(lessonId: Long) {
        screenManager.showTrialLesson(this, lessonId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!uiCheckout.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        releaseComponent(courseId)
        super.onDestroy()
    }

    override fun showCourse(course: Course, source: CourseViewSource, isAdaptive: Boolean) {
        if (isAdaptive) {
            screenManager.continueAdaptiveCourse(this, course)
        } else {
            coursePager.snackbar(messageRes = R.string.course_error_continue_learning)
        }
    }

    override fun showSteps(course: Course, source: CourseViewSource, lastStep: LastStep) {
        screenManager.continueCourse(this, lastStep)
    }

    override fun setBlockingLoading(isLoading: Boolean) {
        if (isLoading) {
            ProgressHelper.activate(progressDialogFragment, supportFragmentManager, LoadingProgressDialogFragment.TAG)
        } else {
            ProgressHelper.dismiss(supportFragmentManager, LoadingProgressDialogFragment.TAG)
        }
    }

    override fun onDismissed() {
        coursePresenter.handleCoursePurchasePressed()
    }

    override fun handleUrl(url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        builder.setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(resolveColorAttribute(R.attr.colorSurface))
                .setSecondaryToolbarColor(resolveColorAttribute(R.attr.colorSurface))
                .build()
        )
        val customTabsIntent = builder.build()
        val packageName = CustomTabsHelper.getPackageNameToUse(this)
        analytic.reportAmplitudeEvent(
            AmplitudeAnalytic.ChromeTab.CHROME_TAB_OPENED,
            mapOf(AmplitudeAnalytic.ChromeTab.Params.FALLBACK to (packageName == null))
        )
        if (packageName == null) {
            InAppWebViewDialogFragment
                .newInstance(getString(R.string.course_purchase), url, isProvideAuth = false)
                .showIfNotExists(supportFragmentManager, InAppWebViewDialogFragment.TAG)
        } else {
            customTabsIntent.intent.`package` = packageName
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }
    }
}