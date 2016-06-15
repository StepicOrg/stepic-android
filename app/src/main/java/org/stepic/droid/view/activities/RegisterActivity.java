package org.stepic.droid.view.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.ResponseBody;
import com.yandex.metrica.YandexMetrica;

import org.jetbrains.annotations.Nullable;
import org.stepic.droid.R;
import org.stepic.droid.base.FragmentActivityBase;
import org.stepic.droid.core.ActivityFinisher;
import org.stepic.droid.core.ProgressHandler;
import org.stepic.droid.util.ProgressHelper;
import org.stepic.droid.util.StringUtil;
import org.stepic.droid.util.ValidatorUtil;
import org.stepic.droid.web.RegistrationResponse;

import java.lang.annotation.Annotation;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnFocusChange;
import retrofit.Callback;
import retrofit.Converter;
import retrofit.Response;
import retrofit.Retrofit;


public class RegisterActivity extends FragmentActivityBase {

    public static final String ERROR_DELIMITER = " ";

    @Bind(R.id.root_view)
    View mRootView;

    @Bind(R.id.sign_up_btn)
    Button mCreateAccountButton;

    @Bind(R.id.actionbar_close_btn_layout)
    View mCloseButton;

    @Bind(R.id.first_name_reg)
    TextView mFirstNameView;

    @Bind(R.id.second_name_reg)
    TextView mSecondNameView;

    @Bind(R.id.email_reg)
    TextView mEmailView;

    @Bind(R.id.password_reg)
    TextView mPassword;

    @Bind(R.id.first_name_reg_wrapper)
    TextInputLayout mFirstNameViewWrapper;

    @Bind(R.id.second_name_reg_wrapper)
    TextInputLayout mSecondNameViewWrapper;

    @Bind(R.id.email_reg_wrapper)
    TextInputLayout mEmailViewWrapper;

    @Bind(R.id.password_reg_wrapper)
    TextInputLayout mPasswordWrapper;

    @BindString(R.string.password_too_short)
    String mPasswordTooShortMessage;


    ProgressDialog mProgress;
    TextWatcher mPasswordWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(org.stepic.droid.R.layout.activity_register);
        ButterKnife.bind(this);
        overridePendingTransition(org.stepic.droid.R.anim.slide_in_from_bottom, org.stepic.droid.R.anim.no_transition);

        hideSoftKeypad();

        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
        mCloseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.loading));
        mProgress.setMessage(getString(R.string.loading_message));
        mProgress.setCancelable(false);

        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    createAccount();
                    return true;
                }
                return false;
            }
        });

        mPassword.addTextChangedListener(mPasswordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (ValidatorUtil.isPasswordLengthValid(s.length())) {
                    hideError(mPasswordWrapper);
                }
            }
        });

        mRootView.requestFocus();
    }


    private void createAccount() {
        String firstName = mFirstNameView.getText().toString().trim();
        String lastName = mSecondNameView.getText().toString().trim();
        final String email = mEmailView.getText().toString().trim();
        final String password = mPassword.getText().toString();

        if (!ValidatorUtil.isPasswordValid(password)) {
            showError(mPasswordWrapper, mPasswordTooShortMessage);
            return;
        }

        hideError(mFirstNameViewWrapper);
        hideError(mSecondNameViewWrapper);
        hideError(mEmailViewWrapper);
        hideError(mPasswordWrapper);

        mShell.getApi().signUp(firstName, lastName, email, password).enqueue(new Callback<RegistrationResponse>() {
            @Override
            public void onResponse(Response<RegistrationResponse> response, Retrofit retrofit) {
                ProgressHelper.dismiss(mProgress);
                if (response.isSuccess()) {
                    mLoginManager.login(email, password, new ProgressHandler() {
                        @Override
                        public void activate() {
                            hideSoftKeypad();
                            ProgressHelper.activate(mProgress);
                        }

                        @Override
                        public void dismiss() {
                            ProgressHelper.dismiss(mProgress);
                        }
                    }, new ActivityFinisher() {
                        @Override
                        public void onFinish() {
                            finish();
                        }
                    });
                } else {
                    Converter<ResponseBody, RegistrationResponse> errorConverter =
                            retrofit.responseConverter(RegistrationResponse.class, new Annotation[0]);
                    RegistrationResponse error = null;
                    try {
                        error = errorConverter.convert(response.errorBody());
                    } catch (Exception e) {
                        YandexMetrica.reportError("registration important error", e); //it is unknown response Expected BEGIN_OBJECT but was STRING at line 1 column 1 path
                    }
                    handleErrorRegistrationResponse(error);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                ProgressHelper.dismiss(mProgress);
                Toast.makeText(RegisterActivity.this, R.string.connectionProblems, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    @Override
    protected void onDestroy() {
        mPassword.removeTextChangedListener(mPasswordWatcher);
        mPassword.setOnEditorActionListener(null);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(org.stepic.droid.R.anim.no_transition, org.stepic.droid.R.anim.slide_out_to_bottom);
    }

    private void hideError(TextInputLayout textInputLayout) {
        if (textInputLayout != null) {
            textInputLayout.setError("");
            textInputLayout.setErrorEnabled(false);
        }
    }

    private void showError(TextInputLayout textInputLayout, String errorText) {
        if (textInputLayout != null) {
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(errorText);
        }
    }

    private void handleErrorRegistrationResponse(@Nullable RegistrationResponse registrationResponse) {
        if (registrationResponse == null) return;
        showError(mEmailViewWrapper, getErrorString(registrationResponse.getEmail()));
        showError(mFirstNameViewWrapper, getErrorString(registrationResponse.getFirst_name()));
        showError(mSecondNameViewWrapper, getErrorString(registrationResponse.getLast_name()));
        showError(mPasswordWrapper, getErrorString(registrationResponse.getPassword()));
    }

    @Nullable
    private String getErrorString(String[] values) {
        return StringUtil.join(values, ERROR_DELIMITER);
    }

    @OnFocusChange({R.id.email_reg, R.id.first_name_reg, R.id.second_name_reg})
    public void setClearErrorOnFocus(View view, boolean hasFocus) {
        if (hasFocus) {
            if (view.getId() == R.id.email_reg) {
                hideError(mEmailViewWrapper);
            }

            if (view.getId() == R.id.first_name_reg) {
                hideError(mFirstNameViewWrapper);
            }
            if (view.getId() == R.id.second_name_reg) {
                hideError(mSecondNameViewWrapper);
            }
        }
    }

}
