package org.stepic.droid.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.stepic.droid.R;
import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.base.FragmentBase;
import org.stepic.droid.base.MainApplication;
import org.stepic.droid.core.ProfilePresenter;
import org.stepic.droid.core.modules.ProfileModule;
import org.stepic.droid.core.presenters.NotificationTimePresenter;
import org.stepic.droid.core.presenters.contracts.NotificationTimeView;
import org.stepic.droid.core.presenters.contracts.ProfileView;
import org.stepic.droid.model.UserViewModel;
import org.stepic.droid.ui.custom.BetterSwitch;
import org.stepic.droid.ui.dialogs.TimeIntervalPickerDialogFragment;
import org.stepic.droid.ui.util.TimeIntervalUtil;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;

public class ProfileFragment extends FragmentBase implements ProfileView, NotificationTimeView {

    private static final String USER_ID_KEY = "user_id_key";
    private static final int NOTIFICATION_INTERVAL_REQUEST_CODE = 11;

    public static ProfileFragment newInstance(long userId) {
        Bundle args = new Bundle();
        args.putLong(USER_ID_KEY, userId);
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.mainInfoRoot)
    View mainInfoRoot;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.profileName)
    TextView profileName;

    @BindView(R.id.profileImage)
    ImageView profileImage;

    @BindDrawable(R.drawable.placeholder_icon)
    Drawable userPlaceholder;

    @BindView(R.id.shortBioValue)
    TextView shortBioValue;

    @BindView(R.id.aboutMeRoot)
    View aboutMeRoot;

    @BindView(R.id.currentStreakValue)
    TextView currentStreakValue;

    @BindView(R.id.currentStreakSuffix)
    TextView currentStreakSuffix;

    @BindView(R.id.currentStreakPrefix)
    TextView currentStreakPrefix;

    @BindView(R.id.maxStreakPrefix)
    TextView maxStreakPrefix;

    @BindView(R.id.maxStreakValue)
    TextView maxStreakValue;

    @BindView(R.id.maxStreakSuffix)
    TextView maxStreakSuffix;

    @BindView(R.id.shortBioTitle)
    TextView shortBioTitle;

    @BindView(R.id.infoTitle)
    TextView infoTitle;

    @BindString(R.string.about_me)
    String aboutMeTitle;

    @BindString(R.string.short_bio)
    String shortBioTitleString;

    @BindView(R.id.infoValue)
    TextView infoValue;

    @Inject
    ProfilePresenter profilePresenter;

    @Inject
    NotificationTimePresenter notificationTimePresenter;

    @BindView(R.id.empty_users)
    View emptyUsers;

    @BindView(R.id.load_progressbar)
    View loadingView;

    @BindView(R.id.report_problem)
    View reportProblemRoot;

    @BindView(R.id.content_root)
    View contentRoot;

    @BindView(R.id.notificationStreakSwitch)
    BetterSwitch notificationStreakSwitch;

    @BindView(R.id.notificationIntervalTitle)
    View notificationIntervalTitle;

    @BindView(R.id.notificationIntervalValue)
    TextView notificationIntervalValue;

    @BindView(R.id.notificationTimeZoneInfo)
    TextView notificationTimeZoneInfo;

    long userId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        userId = getArguments().getLong(USER_ID_KEY);
        analytic.reportEvent(Analytic.Profile.OPEN_SCREEN_OVERALL);
    }

    @Override
    protected void injectComponent() {
        MainApplication
                .component()
                .plus(new ProfileModule()).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initToolbar();
        initTimezone();

        profilePresenter.attachView(this);
        notificationTimePresenter.attachView(this);
        profilePresenter.initProfile(userId);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analytic.reportEvent(Analytic.Profile.CLICK_IMAGE);
            }
        });
        View.OnClickListener clickStreakValue = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analytic.reportEvent(Analytic.Profile.CLICK_STREAK_VALUE);
            }
        };
        currentStreakValue.setOnClickListener(clickStreakValue);
        maxStreakValue.setOnClickListener(clickStreakValue);
        profileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analytic.reportEvent(Analytic.Profile.CLICK_FULL_NAME);
            }
        });

        notificationIntervalValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analytic.reportEvent(Analytic.Interaction.CLICK_CHOOSE_NOTIFICATION_INTERVAL);
                DialogFragment dialogFragment = TimeIntervalPickerDialogFragment.Companion.newInstance();
                if (!dialogFragment.isAdded()) {
                    dialogFragment.setTargetFragment(ProfileFragment.this, NOTIFICATION_INTERVAL_REQUEST_CODE);
                    dialogFragment.show(getFragmentManager(), null);
                }
            }
        });
    }

    private void initTimezone() {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendHourOfDay(2)
                .appendLiteral(":00 (")
                .appendTimeZoneName()
                .appendLiteral(')')
                .toFormatter();
        DateTime utc = DateTime.now(DateTimeZone.UTC).withMillisOfDay(0);
        String print = dateTimeFormatter.print(utc.withZone(DateTimeZone.getDefault()));
        notificationTimeZoneInfo.setText(getString(R.string.streak_updated_timezone, print));
    }

    @Override
    public void onDestroyView() {
        notificationStreakSwitch.setOnCheckedChangeListener(null);
        profileName.setOnClickListener(null);
        currentStreakValue.setOnClickListener(null);
        maxStreakValue.setOnClickListener(null);
        profileImage.setOnClickListener(null);
        notificationIntervalValue.setOnClickListener(null);
        notificationTimePresenter.detachView(this);
        profilePresenter.detachView(this);
        super.onDestroyView();
    }

    private void initToolbar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void streaksIsLoaded(int currentStreak, int maxStreak) {
        String suffixCurrent = getResources().getQuantityString(R.plurals.day_number, currentStreak);
        String suffixMax = getResources().getQuantityString(R.plurals.day_number, maxStreak);

        currentStreakSuffix.setText(suffixCurrent);
        maxStreakSuffix.setText(suffixMax);

        currentStreakValue.setText(currentStreak + "");
        maxStreakValue.setText(maxStreak + "");

        showStreakRoot(true);
    }

    private void showStreakRoot(boolean needShow) {
        if (needShow) {
            currentStreakSuffix.setVisibility(View.VISIBLE);
            currentStreakValue.setVisibility(View.VISIBLE);
            currentStreakPrefix.setVisibility(View.VISIBLE);

            maxStreakSuffix.setVisibility(View.VISIBLE);
            maxStreakValue.setVisibility(View.VISIBLE);
            maxStreakPrefix.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showLoadingAll() {
        contentRoot.setVisibility(View.GONE);
        emptyUsers.setVisibility(View.GONE);
        reportProblemRoot.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showNameImageShortBio(@NotNull UserViewModel userViewModel) {
        emptyUsers.setVisibility(View.GONE);
        reportProblemRoot.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        contentRoot.setVisibility(View.VISIBLE);

        if (userViewModel.isMyProfile()) {
            shortBioTitle.setText(aboutMeTitle);
            notificationTimePresenter.tryShowNotificationSetting();
        } else {
            shortBioTitle.setText(shortBioTitleString);
        }

        mainInfoRoot.setVisibility(View.VISIBLE);
        profileName.setText(userViewModel.getFullName());
        Glide
                .with(getContext())
                .load(userViewModel.getImageLink())
                .asBitmap()
                .placeholder(userPlaceholder)
                .into(profileImage);

        if (userViewModel.getShortBio().isEmpty() && userViewModel.getInformation().isEmpty()) {
            aboutMeRoot.setVisibility(View.GONE);
        } else {
            if (!userViewModel.getShortBio().isEmpty()) {
                shortBioValue.setText(userViewModel.getShortBio());
                aboutMeRoot.setVisibility(View.VISIBLE);
            } else {
                shortBioTitle.setVisibility(View.GONE);
                shortBioValue.setVisibility(View.GONE);
            }

            if (!userViewModel.getInformation().isEmpty()) {
                infoValue.setText(userViewModel.getInformation());
                aboutMeRoot.setVisibility(View.VISIBLE);
            } else {
                infoValue.setVisibility(View.GONE);
                infoTitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onInternetFailed() {
        loadingView.setVisibility(View.GONE);
        contentRoot.setVisibility(View.GONE);
        emptyUsers.setVisibility(View.GONE);
        reportProblemRoot.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProfileNotFound() {
        loadingView.setVisibility(View.GONE);
        contentRoot.setVisibility(View.GONE);
        reportProblemRoot.setVisibility(View.GONE);
        emptyUsers.setVisibility(View.VISIBLE);
    }

    @Override
    public void showNotificationEnabledState(boolean notificationEnabled, @NotNull String notificationTimeValueString) {
        notificationStreakSwitch.setChecked(notificationEnabled);
        if (notificationStreakSwitch.getVisibility() != View.VISIBLE) {
            notificationStreakSwitch.setVisibility(View.VISIBLE);
        }
        if (notificationEnabled) {
            hideNotificationTime(false);
        } else {
            hideNotificationTime(true);
        }

        notificationStreakSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                notificationTimePresenter.switchNotificationStreak(isChecked);
                hideNotificationTime(!isChecked);
            }
        });
        notificationIntervalValue.setText(notificationTimeValueString); //need to set for show default value, when user enable it
    }

    @Override
    public void hideNotificationTime(boolean needHide) {
        int visibility = needHide ? View.GONE : View.VISIBLE;
        notificationTimeZoneInfo.setVisibility(visibility);
        notificationIntervalTitle.setVisibility(visibility);
        notificationIntervalValue.setVisibility(visibility);
    }

    @Override
    public void setNewTimeInterval(@NotNull String timePresentationString) {
        notificationIntervalValue.setText(timePresentationString);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTIFICATION_INTERVAL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                int intervalCode = data.getIntExtra(TimeIntervalPickerDialogFragment.Companion.getResultIntervalCodeKey(), TimeIntervalUtil.INSTANCE.getDefaultTimeCode());
                notificationTimePresenter.setStreakTime(intervalCode);
                analytic.reportEvent(Analytic.Streak.CHOOSE_INTERVAL_PROFILE, intervalCode + "");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                analytic.reportEvent(Analytic.Streak.CHOOSE_INTERVAL_CANCELED_PROFILE);
            }
        }
    }
}
