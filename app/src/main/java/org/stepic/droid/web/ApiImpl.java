package org.stepic.droid.web;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.facebook.login.LoginManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vk.sdk.VKSdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stepic.droid.R;
import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.configuration.Config;
import org.stepic.droid.configuration.RemoteConfig;
import org.stepic.droid.core.ScreenManager;
import org.stepic.droid.core.StepikLogoutManager;
import org.stepic.droid.di.AppSingleton;
import org.stepic.droid.di.network.StethoInterceptor;
import org.stepic.droid.jsonHelpers.adapters.CodeOptionsAdapterFactory;
import org.stepic.droid.jsonHelpers.adapters.UTCDateAdapter;
import org.stepic.droid.jsonHelpers.deserializers.DatasetDeserializer;
import org.stepic.droid.jsonHelpers.deserializers.FeedbackDeserializer;
import org.stepic.droid.jsonHelpers.deserializers.ReplyDeserializer;
import org.stepic.droid.jsonHelpers.serializers.ReplySerializer;
import org.stepic.droid.model.NotificationCategory;
import org.stepic.droid.model.StepikFilter;
import org.stepic.droid.notifications.model.Notification;
import org.stepic.droid.preferences.SharedPreferenceHelper;
import org.stepic.droid.social.ISocialType;
import org.stepic.droid.social.SocialManager;
import org.stepic.droid.util.AppConstants;
import org.stepic.droid.util.CompatibilityExtensionsKt;
import org.stepic.droid.util.DateTimeHelper;
import org.stepic.droid.util.DeviceInfoUtil;
import org.stepic.droid.util.RWLocks;
import org.stepic.droid.web.achievements.AchievementsService;
import org.stepic.droid.web.model.adaptive.RatingRequest;
import org.stepic.droid.web.model.adaptive.RatingResponse;
import org.stepic.droid.web.model.adaptive.RatingRestoreResponse;
import org.stepic.droid.web.model.adaptive.RecommendationReactionsRequest;
import org.stepic.droid.web.model.adaptive.RecommendationsResponse;
import org.stepic.droid.web.model.story_templates.StoryTemplatesResponse;
import org.stepic.droid.web.storage.RemoteStorageService;
import org.stepik.android.model.Reply;
import org.stepik.android.model.ReplyWrapper;
import org.stepik.android.model.Submission;
import org.stepik.android.model.Tag;
import org.stepik.android.model.adaptive.RatingItem;
import org.stepik.android.model.adaptive.RecommendationReaction;
import org.stepik.android.model.attempts.DatasetWrapper;
import org.stepik.android.model.feedback.Feedback;
import org.stepik.android.model.user.Profile;
import org.stepik.android.model.user.RegistrationCredentials;
import org.stepik.android.remote.assignment.model.AssignmentResponse;
import org.stepik.android.remote.attempt.model.AttemptRequest;
import org.stepik.android.remote.attempt.model.AttemptResponse;
import org.stepik.android.remote.certificate.model.CertificateResponse;
import org.stepik.android.remote.course.model.CourseResponse;
import org.stepik.android.remote.course.model.CourseReviewSummaryResponse;
import org.stepik.android.remote.course.model.EnrollmentRequest;
import org.stepik.android.remote.email_address.model.EmailAddressResponse;
import org.stepik.android.remote.last_step.model.LastStepResponse;
import org.stepik.android.remote.lesson.model.LessonResponse;
import org.stepik.android.remote.progress.model.ProgressResponse;
import org.stepik.android.remote.section.model.SectionResponse;
import org.stepik.android.remote.step.model.StepResponse;
import org.stepik.android.remote.submission.model.SubmissionRequest;
import org.stepik.android.remote.submission.model.SubmissionResponse;
import org.stepik.android.remote.unit.model.UnitResponse;
import org.stepik.android.remote.user.model.UserResponse;
import org.stepik.android.remote.user_activity.model.UserActivityResponse;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

@AppSingleton
public class ApiImpl implements Api {
    private final int TIMEOUT_IN_SECONDS = 60;
    private final Interceptor stethoInterceptor;
    private static final String USER_AGENT_NAME = "User-Agent";

    private final Context context;
    private final SharedPreferenceHelper sharedPreference;
    private final Config config;
    private final Analytic analytic;
    private final StepikLogoutManager stepikLogoutManager;
    private final ScreenManager screenManager;
    private final UserAgentProvider userAgentProvider;

    private final StepicRestLoggedService loggedService;
    private StepicRestOAuthService oAuthService;
    private final StepicEmptyAuthService stepikEmptyAuthService;
    private final RemoteStorageService remoteStorageService;
    private final RatingService ratingService;
    private final AchievementsService achievementsService;

    private final Retrofit authorizedRetrofit;

    @Inject
    public ApiImpl(
            Context context, SharedPreferenceHelper sharedPreference,
            Config config,
            Analytic analytic, StepikLogoutManager stepikLogoutManager,
            ScreenManager screenManager,
            UserAgentProvider userAgentProvider,
            FirebaseRemoteConfig firebaseRemoteConfig,
            @StethoInterceptor Interceptor stethoInterceptor
    ) {
        this.context = context;
        this.sharedPreference = sharedPreference;
        this.config = config;
        this.analytic = analytic;
        this.stepikLogoutManager = stepikLogoutManager;
        this.screenManager = screenManager;
        this.userAgentProvider = userAgentProvider;
        this.stethoInterceptor = stethoInterceptor;

        makeOauthServiceWithNewAuthHeader(this.sharedPreference.isLastTokenSocial() ? TokenType.social : TokenType.loginPassword);

        authorizedRetrofit = createAuthorizedRetrofit(config.getBaseUrl());

        achievementsService = authorizedRetrofit.create(AchievementsService.class);
        loggedService = authorizedRetrofit.create(StepicRestLoggedService.class);
        remoteStorageService = authorizedRetrofit.create(RemoteStorageService.class);
        ratingService = createAuthorizedRetrofit(firebaseRemoteConfig.getString(RemoteConfig.ADAPTIVE_BACKEND_URL)).create(RatingService.class);

        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        setTimeout(okHttpClient, TIMEOUT_IN_SECONDS);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(this.config.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(generateGsonFactory())
                .client(okHttpClient.build())
                .build();
        stepikEmptyAuthService = retrofit.create(StepicEmptyAuthService.class);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                CookieSyncManager.createInstance(this.context);
            }
        } catch (Exception ex) {
            this.analytic.reportError(Analytic.Error.COOKIE_MANAGER_ERROR, ex);
        }
    }

    public StepicRestLoggedService getLoggedService() {
        return loggedService;
    }

    public RemoteStorageService getRemoteStorageService() {
        return remoteStorageService;
    }

    public AchievementsService getAchievementsService() {
        return achievementsService;
    }

    public Retrofit getAuthorizedRetrofit() {
        return authorizedRetrofit;
    }

    private Retrofit createAuthorizedRetrofit(final String host) {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request newRequest = addUserAgentTo(chain);
                try {
                    RWLocks.AuthLock.readLock().lock();
                    AuthenticationStepikResponse response = sharedPreference.getAuthResponseFromStore();
                    String urlForCookies = newRequest.url().toString();
                    if (response == null) {
                        //it is Anonymous, we can log it.

                        CookieManager cookieManager = android.webkit.CookieManager.getInstance();
                        String cookies = cookieManager.getCookie(config.getBaseUrl()); //if token is expired or doesn't exist -> manager return null
                        Timber.d("set cookie for url %s is %s", urlForCookies, cookies);
                        if (cookies == null) {
                            updateCookieForBaseUrl();
                            cookies = android.webkit.CookieManager.getInstance().getCookie(urlForCookies);
                        }
                        if (cookies != null) {
                            String csrfTokenFromCookies = getCsrfTokenFromCookies(cookies);
                            if (sharedPreference.getProfile() == null) {
                                StepicProfileResponse stepicProfileResponse = stepikEmptyAuthService.getUserProfileWithCookie(config.getBaseUrl(), cookies, csrfTokenFromCookies).execute().body();
                                if (stepicProfileResponse != null) {
                                    Profile profile = stepicProfileResponse.getProfile();
                                    sharedPreference.storeProfile(profile);
                                }
                            }
                            newRequest = newRequest
                                    .newBuilder()
                                    .addHeader(AppConstants.cookieHeaderName, cookies)
                                    .addHeader(AppConstants.refererHeaderName, config.getBaseUrl())
                                    .addHeader(AppConstants.csrfTokenHeaderName, csrfTokenFromCookies)
                                    .build();
                        }
                    } else if (isNeededUpdate(response)) {
                        try {
                            RWLocks.AuthLock.readLock().unlock();
                            RWLocks.AuthLock.writeLock().lock();
                            Timber.d("writer 1");
                            response = sharedPreference.getAuthResponseFromStore();
                            if (isNeededUpdate(response)) {
                                retrofit2.Response<AuthenticationStepikResponse> authenticationStepicResponse;
                                try {
                                    authenticationStepicResponse = oAuthService.updateToken(config.getRefreshGrantType(), response.getRefreshToken()).execute();
                                    response = authenticationStepicResponse.body();
                                } catch (IOException e) {
                                    return chain.proceed(newRequest);
                                } catch (Exception e) {
                                    analytic.reportError(Analytic.Error.CANT_UPDATE_TOKEN, e);
                                    return chain.proceed(newRequest);
                                }
                                if (response == null || !response.isSuccess()) {
                                    //it is worst case:
                                    String message;
                                    if (response == null) {
                                        message = "response was null";
                                    } else {
                                        message = response.toString();
                                    }

                                    String extendedMessage = "";
                                    if (authenticationStepicResponse.isSuccessful()) {
                                        extendedMessage = "was success " + authenticationStepicResponse.code();
                                    } else {
                                        try {
                                            extendedMessage = "failed " + authenticationStepicResponse.code() + " " + authenticationStepicResponse.errorBody().string();
                                            if (authenticationStepicResponse.code() == 401) {
                                                // logout user
                                                stepikLogoutManager.logout(
                                                        new Function0<Unit>() {
                                                            @Override
                                                            public Unit invoke() {
                                                                try {
                                                                    LoginManager.getInstance().logOut();
                                                                    VKSdk.logout();
                                                                } catch (Exception e) {
                                                                    analytic.reportError(Analytic.Error.FAIL_LOGOUT_WHEN_REFRESH, e);
                                                                }
                                                                screenManager.showLaunchScreenAfterLogout(context);
                                                                Toast.makeText(context, R.string.logout_user_error, Toast.LENGTH_SHORT).show();
                                                                return Unit.INSTANCE;
                                                            }
                                                        }
                                                );
                                            }

                                        } catch (Exception ex) {
                                            analytic.reportError(Analytic.Error.FAIL_REFRESH_TOKEN_INLINE_GETTING, ex);
                                        }
                                    }
                                    analytic.reportError(Analytic.Error.FAIL_REFRESH_TOKEN_ONLINE_EXTENDED, new FailRefreshException(extendedMessage));
                                    analytic.reportError(Analytic.Error.FAIL_REFRESH_TOKEN_ONLINE, new FailRefreshException(message));
                                    analytic.reportEvent(Analytic.Web.UPDATE_TOKEN_FAILED);
                                    return chain.proceed(newRequest);
                                }

                                //Update is success:
                                sharedPreference.storeAuthInfo(response);
                            }
                        } finally {
                            RWLocks.AuthLock.readLock().lock();
                            Timber.d("writer 2");
                            RWLocks.AuthLock.writeLock().unlock();
                        }
                    }
                    if (response != null) {
                        //it is good way
                        newRequest = newRequest.newBuilder().addHeader(AppConstants.authorizationHeaderName, getAuthHeaderValueForLogged()).build();
                    }
                    Response originalResponse = chain.proceed(newRequest);
                    List<String> setCookieHeaders = originalResponse.headers(AppConstants.setCookieHeaderName);
                    if (!setCookieHeaders.isEmpty()) {
                        for (String value : setCookieHeaders) {
                            Timber.d("save for url %s,  cookie %s", urlForCookies, value);
                            if (value != null) {
                                CookieManager.getInstance().setCookie(urlForCookies, value); //set-cookie is not empty
                            }
                        }
                    }
                    return originalResponse;
                } finally {
                    RWLocks.AuthLock.readLock().unlock();
                }
            }
        };
        okHttpBuilder.addNetworkInterceptor(interceptor);
        okHttpBuilder.addNetworkInterceptor(this.stethoInterceptor);
        setTimeout(okHttpBuilder, TIMEOUT_IN_SECONDS);
        OkHttpClient okHttpClient = okHttpBuilder.build();
        return new Retrofit.Builder()
                .baseUrl(host)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(generateGsonFactory())
                .client(okHttpClient)
                .build();
    }

    private void makeOauthServiceWithNewAuthHeader(final TokenType type) {
        sharedPreference.storeLastTokenType(type == TokenType.social);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request newRequest = addUserAgentTo(chain);
                String credential = Credentials.basic(config.getOAuthClientId(type), config.getOAuthClientSecret(type));
                newRequest = newRequest.newBuilder().addHeader(AppConstants.authorizationHeaderName, credential).build();
                return chain.proceed(newRequest);
            }
        };
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        setTimeout(okHttpBuilder, TIMEOUT_IN_SECONDS);
        okHttpBuilder.addNetworkInterceptor(interceptor);
        okHttpBuilder.addNetworkInterceptor(this.stethoInterceptor);
        okHttpBuilder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
        Retrofit notLogged = new Retrofit.Builder()
                .baseUrl(config.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(generateGsonFactory())
                .client(okHttpBuilder.build())
                .build();
        oAuthService = notLogged.create(StepicRestOAuthService.class);
    }

    private Converter.Factory generateGsonFactory() {
        Gson gson = new GsonBuilder()
//                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .enableComplexMapKeySerialization()
                .registerTypeAdapterFactory(new CodeOptionsAdapterFactory())
                .registerTypeAdapter(DatasetWrapper.class, new DatasetDeserializer())
                .registerTypeAdapter(ReplyWrapper.class, new ReplyDeserializer())
                .registerTypeAdapter(ReplyWrapper.class, new ReplySerializer())
                .registerTypeAdapter(Date.class, new UTCDateAdapter())
                .registerTypeAdapter(Feedback.class, new FeedbackDeserializer())
                .create();
        return GsonConverterFactory.create(gson);
    }

    private void setTimeout(OkHttpClient.Builder builder, int seconds) {
        builder.connectTimeout(seconds, TimeUnit.SECONDS);
        builder.readTimeout(seconds, TimeUnit.SECONDS);
    }

    @Override
    public Call<AuthenticationStepikResponse> authWithNativeCode(String code, SocialManager.SocialType type, @Nullable String email) {
        makeOauthServiceWithNewAuthHeader(TokenType.social);
        String codeType = null;
        if (type.needUseAccessTokenInsteadOfCode()) {
            codeType = "access_token";
        }
        return oAuthService.getTokenByNativeCode(type.getIdentifier(), code, config.getGrantType(TokenType.social), config.getRedirectUri(), codeType, email);
    }

    @Override
    public Call<AuthenticationStepikResponse> authWithLoginPassword(String login, String password) {
        makeOauthServiceWithNewAuthHeader(TokenType.loginPassword);
        String encodedPassword = URLEncoder.encode(password);
        String encodedLogin = URLEncoder.encode(login);
        return oAuthService.authWithLoginPassword(config.getGrantType(TokenType.loginPassword), encodedLogin, encodedPassword);
    }

    @Override
    public Call<AuthenticationStepikResponse> authWithCode(String code) {
        makeOauthServiceWithNewAuthHeader(TokenType.social);
        return oAuthService.getTokenByCode(config.getGrantType(TokenType.social), code, config.getRedirectUri());
    }

    @Override
    public Call<RegistrationResponse> signUp(String firstName, String lastName, String email, String password) {
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request newRequest = addUserAgentTo(chain);

                String cookies = CookieManager.getInstance().getCookie(config.getBaseUrl()); //if token is expired or doesn't exist -> manager return null
                if (cookies == null) {
                    updateCookieForBaseUrl();
                    cookies = android.webkit.CookieManager.getInstance().getCookie(config.getBaseUrl());
                }
                if (cookies == null)
                    return chain.proceed(newRequest);


                String csrftoken = getCsrfTokenFromCookies(cookies);
                Request.Builder requestBuilder = newRequest
                        .newBuilder()
                        .addHeader(AppConstants.refererHeaderName, config.getBaseUrl())
                        .addHeader(AppConstants.csrfTokenHeaderName, csrftoken)
                        .addHeader(AppConstants.cookieHeaderName, cookies);
                newRequest = requestBuilder.build();
                return chain.proceed(newRequest);
            }
        };
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.addNetworkInterceptor(interceptor);
        okHttpBuilder.addNetworkInterceptor(this.stethoInterceptor);
        setTimeout(okHttpBuilder, TIMEOUT_IN_SECONDS);
        Retrofit notLogged = new Retrofit.Builder()
                .baseUrl(config.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(generateGsonFactory())
                .client(okHttpBuilder.build())
                .build();
        StepicRestOAuthService tempService = notLogged.create(StepicRestOAuthService.class);
        return tempService.createAccount(new UserRegistrationRequest(new RegistrationCredentials(firstName, lastName, email, password)));
    }

    @Nullable
    private String tryGetCsrfFromOnePair(String keyValueCookie) {
        List<HttpCookie> cookieList = HttpCookie.parse(keyValueCookie);
        for (HttpCookie item : cookieList) {
            if (item.getName() != null && item.getName().equals(config.getCsrfTokenCookieName())) {
                return item.getValue();
            }
        }
        return null;
    }

    @NonNull
    private String getCsrfTokenFromCookies(String cookies) {
        String csrftoken = null;
        String[] cookiePairs = cookies.split(";");
        for (String cookieItem : cookiePairs) {
            csrftoken = tryGetCsrfFromOnePair(cookieItem);
            if (csrftoken != null) {
                break;
            }
        }
        if (csrftoken == null) {
            csrftoken = "";
            analytic.reportEvent(Analytic.Error.COOKIE_WAS_EMPTY);
        }
        return csrftoken;
    }

    public Single<UserCoursesResponse> getUserCourses(int page) {
        return loggedService.getUserCourses(page);
    }

    public Single<CourseResponse> getPopularCourses(int page) {
        EnumSet<StepikFilter> enumSet = sharedPreference.getFilterForFeatured();
        String lang = enumSet.iterator().next().getLanguage();
        return loggedService.getPopularCourses(page, lang);
    }

    @Override
    public Call<StepicProfileResponse> getUserProfile() {
        return loggedService.getUserProfile();
    }

    @Override
    public Call<UserResponse> getUsers(long[] userIds) {
        return loggedService.getUsers(userIds);
    }

    @Override
    public Single<UserResponse> getUsersRx(long[] userIds) {
        return loggedService.getUsersRx(userIds);
    }

    @Override
    public Completable joinCourse(EnrollmentRequest enrollmentRequest) {
        return loggedService.joinCourse(enrollmentRequest);
    }

    @Override
    public Call<SectionResponse> getSections(long[] sectionsIds) {
        return loggedService.getSections(sectionsIds);
    }

    @Override
    public Single<SectionResponse> getSectionsRx(long[] sectionsIds) {
        return loggedService.getSectionsRx(sectionsIds);
    }

    @Override
    public Call<UnitResponse> getUnits(List<Long> units) {
        return loggedService.getUnits(units);
    }

    @Override
    public Single<UnitResponse> getUnitsRx(long[] units) {
        return loggedService.getUnitsRx(units);
    }

    @Override
    public Single<UnitResponse> getUnits(long courseId, long lessonId) {
        return loggedService.getUnits(courseId, lessonId);
    }

    @Override
    public Call<LessonResponse> getLessons(long[] lessons) {
        return loggedService.getLessons(lessons);
    }

    @Override
    public Single<LessonResponse> getLessonsRx(long[] lessons) {
        return loggedService.getLessonsRx(lessons);
    }

    @Override
    public Single<LessonResponse> getLessons(long lessonId) {
        return getLessonsRx(new long[]{lessonId});
    }

    @Override
    public Single<StepResponse> getSteps(long[] steps) {
        return loggedService.getSteps(steps);
    }

    @Override
    public Single<StepResponse> getStepsByLessonId(long lessonId) {
        return loggedService.getStepsByLessonId(lessonId);
    }

    @Override
    public Completable dropCourse(long courseId) {
        if (!config.isUserCanDropCourse()) return null;
        return loggedService.dropCourse(courseId);
    }

    @Override
    public Call<ProgressResponse> getProgresses(String[] progresses) {
        return loggedService.getProgresses(progresses);
    }

    @Override
    public Single<ProgressResponse> getProgressesReactive(String[] progresses) {
        return loggedService.getProgressesReactive(progresses);
    }

    @Override
    public Single<AssignmentResponse> getAssignments(long[] assignmentsIds) {
        return loggedService.getAssignments(assignmentsIds);
    }

    @Override
    public void loginWithSocial(final FragmentActivity activity, ISocialType type) {
        String socialIdentifier = type.getIdentifier();
        String url = config.getBaseUrl() + "/accounts/" + socialIdentifier + "/login?next=/oauth2/authorize/?" + Uri.encode("client_id=" + config.getOAuthClientId(TokenType.social) + "&response_type=code");
        Uri uri = Uri.parse(url);
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
        activity.startActivity(intent);
    }

    @Override
    public Call<SearchResultResponse> getSearchResultsCourses(int page, String rawQuery) {
        EnumSet<StepikFilter> enumSet = sharedPreference.getFilterForFeatured();
        String lang = enumSet.iterator().next().getLanguage();
        String encodedQuery = URLEncoder.encode(rawQuery);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, rawQuery);
        analytic.reportEvent(FirebaseAnalytics.Event.SEARCH, bundle);

        return loggedService.getSearchResults(page, encodedQuery, lang);
    }

    @Override
    public Single<QueriesResponse> getSearchQueries(String query) {
        return loggedService.getSearchQueries(query);
    }

    @Override
    public Call<CourseResponse> getCourses(int page, @Nullable long[] ids) {
        if (ids == null || ids.length == 0) {
            ids = new long[]{0};
        }
        return loggedService.getCourses(page, ids);
    }

    @Override
    public Single<CourseResponse> getCoursesReactive(int page, @NotNull long[] ids) {
        if (ids.length == 0) {
            ids = new long[]{0};
        }
        return loggedService.getCoursesReactive(page, ids);
    }

    @Override
    public Single<CourseResponse> getCoursesReactive(@NotNull long[] ids) {
        return loggedService.getCoursesReactive(ids);
    }

    @Override
    public Call<AttemptResponse> createNewAttempt(long stepId) {
        AttemptRequest attemptRequest = new AttemptRequest(stepId);
        return loggedService.createNewAttempt(attemptRequest);
    }

    @Override
    public Single<AttemptResponse> createNewAttemptReactive(long stepId) {
        return loggedService.createNewAttemptReactive(new AttemptRequest(stepId));
    }

    @Override
    public Call<SubmissionResponse> createNewSubmission(Reply reply, long attemptId) {
        SubmissionRequest submissionRequest = new SubmissionRequest(reply, attemptId);
        return loggedService.createNewSubmission(submissionRequest);
    }

    @Override
    public Single<SubmissionResponse> createNewSubmissionReactive(Submission submission) {
        return loggedService.createNewSubmissionReactive(new SubmissionRequest(submission));
    }

    @Override
    public Call<AttemptResponse> getExistingAttempts(long stepId) {
        return loggedService.getExistingAttempts(stepId, getCurrentUserId());
    }

    @Override
    public Single<AttemptResponse> getExistingAttemptsReactive(long stepId) {
        return loggedService.getExistingAttemptsReactive(stepId, getCurrentUserId());
    }

    @Override
    public Call<SubmissionResponse> getSubmissions(long attemptId) {
        return loggedService.getExistingSubmissions(attemptId, "desc");
    }

    @Override
    public Single<SubmissionResponse> getSubmissionsReactive(long attemptId) {
        return loggedService.getExistingSubmissionsReactive(attemptId, "desc");
    }

    @Override
    public Call<SubmissionResponse> getSubmissionForStep(long stepId) {
        return loggedService.getExistingSubmissionsForStep(stepId);
    }

    @Override
    public Single<SubmissionResponse> getSubmissionForStepReactive(long stepId) {
        return loggedService.getExistingSubmissionsForStepReactive(stepId);
    }

    @Override
    public Call<Void> remindPassword(String email) {
        String encodedEmail = URLEncoder.encode(email);

        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request newRequest = addUserAgentTo(chain);

                List<HttpCookie> cookies = getCookiesForBaseUrl();
                if (cookies == null)
                    return chain.proceed(newRequest);
                String csrftoken = null;
                String sessionId = null;
                for (HttpCookie item : cookies) {
                    if (item.getName() != null && item.getName().equals(config.getCsrfTokenCookieName())) {
                        csrftoken = item.getValue();
                        continue;
                    }
                    if (item.getName() != null && item.getName().equals(config.getSessionCookieName())) {
                        sessionId = item.getValue();
                    }
                }

                String cookieResult = config.getCsrfTokenCookieName() + "=" + csrftoken + "; " + config.getSessionCookieName() + "=" + sessionId;
                if (csrftoken == null) return chain.proceed(newRequest);
                HttpUrl url = newRequest
                        .url()
                        .newBuilder()
                        .addQueryParameter("csrfmiddlewaretoken", csrftoken)
                        .addQueryParameter("csrfmiddlewaretoken", csrftoken)
                        .build();
                newRequest = newRequest.newBuilder()
                        .addHeader("referer", config.getBaseUrl())
                        .addHeader("X-CSRFToken", csrftoken)
                        .addHeader("Cookie", cookieResult)
                        .url(url)
                        .build();
                return chain.proceed(newRequest);
            }
        };
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.addNetworkInterceptor(interceptor);
        okHttpBuilder.addNetworkInterceptor(this.stethoInterceptor);
        setTimeout(okHttpBuilder, TIMEOUT_IN_SECONDS);
        Retrofit notLogged = new Retrofit.Builder()
                .baseUrl(config.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(generateGsonFactory())
                .client(okHttpBuilder.build())
                .build();
        StepicEmptyAuthService tempService = notLogged.create(StepicEmptyAuthService.class);
        return tempService.remindPassword(encodedEmail);

    }

    @Override
    public Call<EmailAddressResponse> getEmailAddresses(@NotNull long[] ids) {
        return loggedService.getEmailAddresses(ids);
    }

    @Override
    public Call<DeviceResponse> getDevices() {
        return loggedService.getDevices(getCurrentUserId());
    }

    @Override
    public Call<DeviceResponse> getDevicesByRegistrationId(String token) {
        return loggedService.getDeviceByRegistrationId(token);
    }

    @Override
    public Call<DeviceResponse> renewDeviceRegistration(long deviceId, String token) {
        String description = DeviceInfoUtil.getShortInfo(context);
        DeviceRequest deviceRequest = new DeviceRequest(deviceId, token, description);
        return loggedService.renewDeviceRegistration(deviceId, deviceRequest);
    }

    @Override
    public Call<DeviceResponse> registerDevice(String token) {
        String description = DeviceInfoUtil.getShortInfo(context);
        DeviceRequest deviceRequest = new DeviceRequest(token, description);
        return loggedService.registerDevice(deviceRequest);
    }

    @Override
    public Call<CourseResponse> getCourse(long id) {
        long[] ids = new long[]{id};
        return loggedService.getCourses(ids);
    }

    @Override
    public Call<Void> setReadStatusForNotification(long notificationId, boolean isRead) {
        Notification notification = new Notification();
        notification.setUnread(!isRead);
        return loggedService.putNotification(notificationId, new NotificationRequest(notification));
    }

    @Override
    public Completable setReadStatusForNotificationReactive(long notificationId, boolean isRead) {
        Notification notification = new Notification();
        notification.setUnread(!isRead);
        return loggedService.putNotificationReactive(notificationId, new NotificationRequest(notification));
    }

    @Override
    public Call<Void> removeDevice(long deviceId) {
        return loggedService.removeDevice(deviceId);
    }

    @Override
    public Single<CertificateResponse> getCertificates(long userId, int page) {
        return loggedService.getCertificates(userId, page);
    }

    @Override
    public Single<UnitResponse> getUnitsByLessonId(long lessonId) {
        return loggedService.getUnitsByLessonId(lessonId);
    }

    @Override
    public Call<NotificationResponse> getNotifications(NotificationCategory notificationCategory, int page) {
        String categoryType = getNotificationCategoryString(notificationCategory);
        return loggedService.getNotifications(page, categoryType);
    }

    @Override
    public Call<Void> markAsReadAllType(@NotNull NotificationCategory notificationCategory) {
        String categoryType = getNotificationCategoryString(notificationCategory);
        return loggedService.markAsRead(categoryType);
    }

    @Override
    public Single<NotificationStatusesResponse> getNotificationStatuses() {
        return loggedService.getNotificationStatuses();
    }

    @Override
    public Call<UserActivityResponse> getUserActivities(long userId) {
        return loggedService.getUserActivities(userId);
    }

    @Override
    public Single<UserActivityResponse> getUserActivitiesReactive(long userId) {
        return loggedService.getUserActivitiesReactive(userId);
    }

    @Override
    public Single<LastStepResponse> getLastStepResponse(@NonNull String lastStepId) {
        return loggedService.getLastStepResponse(lastStepId);
    }

    @Override
    public Single<CourseCollectionsResponse> getCourseCollections(String language) {
        return loggedService.getCourseLists(language);
    }

    @Override
    public Single<CourseReviewSummaryResponse> getCourseReviewSummaries(long[] courseIds) {
        return loggedService.getCourseReviews(courseIds);
    }

    @Override
    public Single<TagResponse> getFeaturedTags() {
        return loggedService.getFeaturedTags();
    }

    @Override
    public Single<RecommendationsResponse> getNextRecommendations(long courseId, int count) {
        return loggedService.getNextRecommendations(courseId, count);
    }

    @Override
    public Completable createReaction(RecommendationReaction reaction) {
        return loggedService.createRecommendationReaction(new RecommendationReactionsRequest(reaction));
    }

    @Override
    public Single<List<RatingItem>> getRating(long courseId, int count, int days) {
        return ratingService.getRating(courseId, count, days, getCurrentUserId()).map(new Function<RatingResponse, List<RatingItem>>() {
            @Override
            public List<RatingItem> apply(RatingResponse ratingResponse) {
                return ratingResponse.getUsers();
            }
        });
    }

    @Override
    public Completable putRating(long courseId, long exp) {
        return ratingService.putRating(new RatingRequest(exp, courseId, getAccessToken()));
    }

    @Override
    public Single<RatingRestoreResponse> restoreRating(long courseId) {
        return ratingService.restoreRating(courseId, getAccessToken());
    }


    @Override
    public Observable<StoryTemplatesResponse> getStoryTemplates(int page) {
        final Locale locale = CompatibilityExtensionsKt.getDefaultLocale(Resources.getSystem().getConfiguration());
        return loggedService.getStoryTemplate(page, true, locale.getLanguage());
    }

    @Override
    public Single<SearchResultResponse> getSearchResultsOfTag(int page, @NotNull Tag tag) {
        EnumSet<StepikFilter> enumSet = sharedPreference.getFilterForFeatured();
        String lang = enumSet.iterator().next().getLanguage();
        return loggedService.getSearchResultsOfTag(page, tag.getId(), lang);
    }


    @Nullable
    private String getNotificationCategoryString(NotificationCategory notificationCategory) {
        String categoryType;
        if (notificationCategory == NotificationCategory.all) {
            categoryType = null;
        } else {
            categoryType = notificationCategory.name();
        }
        return categoryType;
    }

    @Nullable
    private List<HttpCookie> getCookiesForBaseUrl() throws IOException {
        String lang = Locale.getDefault().getLanguage();
        retrofit2.Response response = stepikEmptyAuthService.getStepicForFun(lang).execute();
        Headers headers = response.headers();
        java.net.CookieManager cookieManager = new java.net.CookieManager();
        URI myUri;
        try {
            myUri = new URI(config.getBaseUrl());
        } catch (URISyntaxException e) {
            return null;
        }
        cookieManager.put(myUri, headers.toMultimap());
        return cookieManager.getCookieStore().get(myUri);
    }

    private void updateCookieForBaseUrl() throws IOException {
        String lang = Locale.getDefault().getLanguage();
        retrofit2.Response response = stepikEmptyAuthService.getStepicForFun(lang).execute();

        List<String> setCookieHeaders = response.headers().values(AppConstants.setCookieHeaderName);
        if (!setCookieHeaders.isEmpty()) {
            for (String value : setCookieHeaders) {
                if (value != null) {
                    CookieManager.getInstance().setCookie(config.getBaseUrl(), value); //set-cookie is not empty
                }
            }
        }
    }


    private String getAuthHeaderValueForLogged() {
        AuthenticationStepikResponse resp = sharedPreference.getAuthResponseFromStore();
        if (resp == null) {
            //not happen, look "resp null" in metrica before 07.2016
            return "";
        }
        String accessToken = resp.getAccessToken();
        String type = resp.getTokenType();
        return type + " " + accessToken;
    }

    private boolean isNeededUpdate(AuthenticationStepikResponse response) {
        if (response == null) {
            Timber.d("Token is null");
            return false;
        }

        long timestampStored = sharedPreference.getAccessTokenTimestamp();
        if (timestampStored == -1) return true;

        long nowTemp = DateTimeHelper.INSTANCE.nowUtc();
        long delta = nowTemp - timestampStored;
        long expiresMillis = (response.getExpiresIn() - 50) * 1000;
        return delta > expiresMillis;//token expired --> need update
    }

    private Request addUserAgentTo(Interceptor.Chain chain) {
        return chain
                .request()
                .newBuilder()
                .header(USER_AGENT_NAME, userAgentProvider.provideUserAgent())
                .build();
    }

    private long getCurrentUserId() {
        Profile profile = sharedPreference.getProfile();
        //noinspection StatementWithEmptyBody
        if (profile == null) {
            //practically it is not happens (yandex metrica)
            return 0;
        } else {
            return profile.getId();
        }
    }

    @Nullable
    private String getAccessToken() {
        final AuthenticationStepikResponse auth = sharedPreference.getAuthResponseFromStore();
        if (auth == null) {
            return null;
        } else {
            return auth.getAccessToken();
        }
    }
}
