package org.stepic.droid.analytic;

import android.os.Bundle;

import org.jetbrains.annotations.NotNull;

public interface Analytic {
    String METRICA_FAIL_LOGIN = "fail login";
    String METRICA_SUCCESS_LOGIN = "success login";
    String METRICA_DROP_COURSE = "drop course";
    String METRICA_LOAD_SERVICE = "Load Service";
    String METRICA_REFRESH_COURSE = "Pull from top to refresh course";
    String METRICA_REFRESH_SECTION = "Pull from top to refresh section";
    String METRICA_REFRESH_UNIT = "Pull from top to refresh section unit";
    String METRICA_LONG_TAP_COURSE = "Long tap on course";
    String METRICA_LESSON_IN_STORE_STATE_NULL = "lesson was null in store state manager";
    String METRICA_UNIT_IN_STORE_STATE_NULL = "unit was null in store state manager";

    interface Interaction{
        String CLICK_SIGN_IN = "click sign in on launch screen";
        String CLICK_SIGN_UP = "click sign up";
        String CLICK_SIGN_IN_ON_SIGN_IN_SCREEN = "click sign in on sign in on sign-in screen";
        String METRICA_CLICK_DELETE_SECTION = "Click delete section from cache";
        String METRICA_CLICK_CACHE_SECTION = "Click cache section";
        String METRICA_CLICK_CACHE_UNIT = "Click cache unit";
        String METRICA_CLICK_DELETE_UNIT = "Click delete unit from cache";
        String METRICA_CLICK_LOGOUT = "Click logout";
        String METRICA_CLICK_CLEAR_CACHE = "Click clear cache button";
        String METRICA_CLICK_YES_CLEAR_CACHE = "Click Accept clear cache";
        String METRICA_CLICK_YES_LOGOUT = "Click accept logout";
        String METRICA_CANCEL_VIDEO_QUALITY = "Cancel video quality dialog";
        String METRICA_YES_CLEAR_VIDEOS = "clear videos from downloads";
    }

    interface AppIndexing {
        String courseDetail = "appindexing_course_detail";
    }

    void reportEvent(String eventName, Bundle bundle);

    void reportEventWithId(String eventName, String id);

    void reportEventWithIdName(String eventName, String id, String name);

    void reportEvent(String eventName);

    void reportError(String message, @NotNull Throwable throwable);
}
