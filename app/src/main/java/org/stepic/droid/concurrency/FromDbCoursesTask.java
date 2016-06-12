package org.stepic.droid.concurrency;

import com.squareup.otto.Bus;

import org.jetbrains.annotations.NotNull;
import org.stepic.droid.base.MainApplication;
import org.stepic.droid.core.IShell;
import org.stepic.droid.events.courses.FinishingGetCoursesFromDbEvent;
import org.stepic.droid.events.courses.StartingGetCoursesFromDbEvent;
import org.stepic.droid.model.Course;
import org.stepic.droid.store.operations.DatabaseManager;

import java.util.List;

import javax.inject.Inject;

public class FromDbCoursesTask extends StepicTask<Void, Void, List<Course>> {

    @Inject
    IShell mShell;

    @Inject
    Bus mBus;

    @Inject
    DatabaseManager dbOperationsCourses;

    private DatabaseManager.Table mCourseType;

    public FromDbCoursesTask(@NotNull DatabaseManager.Table courseType) {
        super(MainApplication.getAppContext());

        MainApplication.component(mContext).inject(this);

        mCourseType = courseType;
    }

    @Override
    protected List<Course> doInBackgroundBody(Void... params) throws Exception {
        return dbOperationsCourses.getAllCourses(mCourseType);
    }

    @Override
    protected void onPreExecute() {
        mBus.post(new StartingGetCoursesFromDbEvent(mCourseType));
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(AsyncResultWrapper<List<Course>> listAsyncResultWrapper) {
        super.onPostExecute(listAsyncResultWrapper);
        mBus.post(new FinishingGetCoursesFromDbEvent(mCourseType, listAsyncResultWrapper.getResult()));
    }
}
