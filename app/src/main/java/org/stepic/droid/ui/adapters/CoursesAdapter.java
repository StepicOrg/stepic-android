package org.stepic.droid.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.stepic.droid.R;
import org.stepic.droid.base.App;
import org.stepic.droid.concurrency.MainHandler;
import org.stepic.droid.core.presenters.ContinueCoursePresenter;
import org.stepic.droid.model.CoursesCarouselColorType;
import org.stepic.droid.model.CoursesDescriptionContainer;
import org.stepic.droid.ui.adapters.viewhoders.CourseItemViewHolder;
import org.stepic.droid.ui.adapters.viewhoders.FooterItemViewHolder;
import org.stepic.droid.ui.adapters.viewhoders.HeaderItemViewHolder;
import org.stepik.android.model.Course;

import java.util.List;

import javax.inject.Inject;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class CoursesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Inject
    MainHandler mainHandler;

    private Drawable coursePlaceholder;

    private LayoutInflater inflater;

    private FragmentActivity contextActivity;
    private final List<Course> courses;
    private final ContinueCoursePresenter continueCoursePresenter;

    private CoursesDescriptionContainer descriptionContainer;

    private final static int HEADER_VIEW_TYPE = 3;
    private final static int ITEM_VIEW_TYPE = 2;
    private final static int FOOTER_VIEW_TYPE = 1;

    private int NUMBER_OF_PRE_ITEMS = 0;
    private final int NUMBER_OF_POST_ITEMS;
    private boolean isNeedShowFooter;
    private final CoursesCarouselColorType colorType;

    private final int courseListPadding;

    public CoursesAdapter(
            FragmentActivity activity,
            List<Course> courses,
            @NotNull ContinueCoursePresenter continueCoursePresenter,
            boolean withPagination,
            CoursesCarouselColorType colorType
    ) {
        this.colorType = colorType;
        if (withPagination) {
            NUMBER_OF_POST_ITEMS = 1;
        } else {
            NUMBER_OF_POST_ITEMS = 0;
        }
        contextActivity = activity;
        courseListPadding = activity.getResources().getDimensionPixelOffset(R.dimen.course_list_padding);

        this.courses = courses;
        this.continueCoursePresenter = continueCoursePresenter;
        inflater = (LayoutInflater) contextActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        App.Companion.component().inject(this);

        Bitmap coursePlaceholderBitmap = BitmapFactory.decodeResource(contextActivity.getResources(), R.drawable.general_placeholder);
        RoundedBitmapDrawable circularBitmapDrawable =
                RoundedBitmapDrawableFactory.create(contextActivity.getResources(), coursePlaceholderBitmap);
        circularBitmapDrawable.setCornerRadius(contextActivity.getResources().getDimension(R.dimen.course_image_radius));
        coursePlaceholder = circularBitmapDrawable;

        isNeedShowFooter = false;
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == HEADER_VIEW_TYPE) {
            View view = inflater.inflate(R.layout.course_collection_header_view, parent, false);
            ((RecyclerView.LayoutParams) view.getLayoutParams()).setMargins(
                    -courseListPadding, -courseListPadding, -courseListPadding, courseListPadding); // todo refactor layouts
            return new HeaderItemViewHolder(view);
        } else if (viewType == FOOTER_VIEW_TYPE) {
            View view = inflater.inflate(R.layout.loading_view, parent, false);
            return new FooterItemViewHolder(view);
        } else if (ITEM_VIEW_TYPE == viewType) {
            View view = inflater.inflate(R.layout.new_course_item, parent, false);
            return new CourseItemViewHolder(
                    view,
                    contextActivity,
                    coursePlaceholder,
                    continueCoursePresenter,
                    colorType
            );
        } else {
            throw new IllegalStateException("Not valid item type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case HEADER_VIEW_TYPE: {
                HeaderItemViewHolder headerItemViewHolder = (HeaderItemViewHolder) holder;
                headerItemViewHolder.bindData(descriptionContainer);
                break;
            }
            case ITEM_VIEW_TYPE: {
                CourseItemViewHolder courseItemViewHolder = (CourseItemViewHolder) holder;
                courseItemViewHolder.setDataOnView(courses.get(position - NUMBER_OF_PRE_ITEMS));
                break;
            }
            case FOOTER_VIEW_TYPE: {
                FooterItemViewHolder footerItemViewHolder = (FooterItemViewHolder) holder;
                footerItemViewHolder.setLoaderVisibiluty(isNeedShowFooter);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < NUMBER_OF_PRE_ITEMS) {
            return HEADER_VIEW_TYPE;
        } else if (position == getItemCount() - NUMBER_OF_POST_ITEMS) {
            return FOOTER_VIEW_TYPE;
        } else {
            return ITEM_VIEW_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return NUMBER_OF_PRE_ITEMS + courses.size() + NUMBER_OF_POST_ITEMS;
    }

    public void setDescriptionContainer(CoursesDescriptionContainer descriptionContainer) {
        if (this.descriptionContainer == null && descriptionContainer != null) {
            NUMBER_OF_PRE_ITEMS++;
            this.descriptionContainer = descriptionContainer;
            notifyItemInserted(0);
        } else if (this.descriptionContainer != null && descriptionContainer == null) {
            NUMBER_OF_PRE_ITEMS--;
            this.descriptionContainer = null;
            notifyItemRemoved(0);
        } else if (this.descriptionContainer != null) {
            this.descriptionContainer = descriptionContainer;
            notifyItemChanged(0);
        }
    }

    public void showLoadingFooter(boolean isNeedShow) {
        isNeedShowFooter = isNeedShow;
        postUpdateToNextFrame();
    }

    private void postUpdateToNextFrame() {
        mainHandler.post(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                notifyItemChanged(getItemCount() - 1);
                return Unit.INSTANCE;
            }
        });
    }
}
