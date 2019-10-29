package org.stepic.droid.util.resolvers;

import androidx.annotation.DrawableRes;

import org.jetbrains.annotations.NotNull;
import org.stepic.droid.R;
import org.stepic.droid.di.AppSingleton;
import org.stepic.droid.ui.quiz.ChoiceQuizDelegate;
import org.stepic.droid.ui.quiz.NotSupportedQuizDelegate;
import org.stepic.droid.ui.quiz.NumberQuizDelegate;
import org.stepic.droid.ui.quiz.QuizDelegate;
import org.stepic.droid.ui.quiz.StringQuizDelegate;
import org.stepic.droid.util.AppConstants;
import org.stepik.android.model.Step;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

@AppSingleton
public class StepTypeResolverImpl implements StepTypeResolver {

    private final Map<String, Integer> mapFromTypeToDrawableRes;
    private final int peerReviewDrawableRes;

    @Inject
    StepTypeResolverImpl() {
        Timber.d("create step type resolver: %s", toString());
        mapFromTypeToDrawableRes = new HashMap<>();

        peerReviewDrawableRes = R.drawable.ic_peer_review;
        int simpleQuestionDrawable = R.drawable.ic_easy_quiz;
        int videoDrawable = R.drawable.ic_video_pin;
        int animationDrawable = R.drawable.ic_animation;
        int hardQuizDrawable = R.drawable.ic_hard_quiz;
        int theoryQuizDrawable = R.drawable.ic_theory;

        mapFromTypeToDrawableRes.put(AppConstants.TYPE_TEXT, theoryQuizDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_VIDEO, videoDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_MATCHING, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_SORTING, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_MATH, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_FREE_ANSWER, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_STRING, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_CHOICE, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_NUMBER, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_DATASET, hardQuizDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_ANIMATION, animationDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_CHEMICAL, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_PUZZLE, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_PYCHARM, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_CODE, hardQuizDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_ADMIN, hardQuizDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_SQL, simpleQuestionDrawable);
        mapFromTypeToDrawableRes.put(AppConstants.TYPE_LINUX_CODE, simpleQuestionDrawable);
    }

    @Override
    @DrawableRes
    public int getDrawableForType(String type, boolean isPeerReview) {
        if (isPeerReview) {
            return peerReviewDrawableRes;
        }

        Integer drawable = mapFromTypeToDrawableRes.get(type);
        if (drawable == null) {
            drawable = mapFromTypeToDrawableRes.get(AppConstants.TYPE_TEXT);
        }

        return drawable;
    }

    @Override
    public int getDrawableTintForStep(boolean isViewed) {
        if (isViewed) {
            return R.color.viewed_step;
        } else {
            return R.color.unviewed_step;
        }
    }

    @NotNull
    @Override
    public QuizDelegate getQuizDelegate(Step step) {
        QuizDelegate errorDelegate = new NotSupportedQuizDelegate();
        if (step == null
                || step.getBlock() == null
                || step.getBlock().getName() == null
                || step.getBlock().getName().equals(""))
            return errorDelegate;

        String type = step.getBlock().getName();
        switch (type) {
            case AppConstants.TYPE_CHOICE:
                return new ChoiceQuizDelegate();
            case AppConstants.TYPE_STRING:
                return new StringQuizDelegate();
            case AppConstants.TYPE_NUMBER:
                return new NumberQuizDelegate();
            default:
                return errorDelegate;
        }
    }
}
