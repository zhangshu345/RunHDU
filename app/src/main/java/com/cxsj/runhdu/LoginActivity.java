package com.cxsj.runhdu;

import android.support.design.widget.TextInputLayout;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.cxsj.runhdu.utils.InputCheckHelper;
import com.cxsj.runhdu.utils.MD5Util;
import com.cxsj.runhdu.utils.StatusJsonCheckHelper;
import com.dd.CircularProgressButton;
import com.cxsj.runhdu.constant.URLs;
import com.cxsj.runhdu.utils.HttpUtil;
import com.cxsj.runhdu.utils.ActivityManager;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private CircularProgressButton loginButton;
    private EditText usernameText;
    private TextInputLayout usernameInputLayout;
    private TextInputLayout pwInputLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginButton = (CircularProgressButton) findViewById(R.id.login_button);
        usernameText = (EditText) findViewById(R.id.username_text);
        usernameInputLayout = (TextInputLayout) findViewById(R.id.username_input_layout);
        pwInputLayout = (TextInputLayout) findViewById(R.id.pw_input_layout);
        loginButton.setIndeterminateProgressMode(true);
        setToolbar(R.id.login_toolbar, true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        loginButton.setOnClickListener(this);
        usernameText.setOnClickListener(this);

        if (!TextUtils.isEmpty(username)) {
            usernameText.setText(username);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                usernameInputLayout.setErrorEnabled(false);
                pwInputLayout.setErrorEnabled(false);

                String username = usernameInputLayout.getEditText().getText().toString();
                String password = pwInputLayout.getEditText().getText().toString();
                //检查输入
                InputCheckHelper.check(username, password, null, new InputCheckHelper.CheckCallback() {
                    @Override
                    public void onPass() {
                        loginButton.setProgress(1);
                        loginButton.setClickable(false);
                        HttpUtil.load(URLs.LOGIN)
                                .addParam("name", username)
                                .addParam("password", MD5Util.encode(password))
                                .post(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        runOnUiThread(() -> {
                                            pwInputLayout.setError("网络连接失败。");
                                            loginButton.setProgress(0);
                                            loginButton.setClickable(true);
                                        });
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        final String result = response.body().string();
                                        //检查返回的json
                                        runOnUiThread(() -> checkReturn(result));
                                    }
                                });
                    }

                    @Override
                    public void onFailure(int which, String msg) {
                        if (which == InputCheckHelper.ERR_USERNAME) {
                            usernameInputLayout.setError(msg);
                        } else if (which == InputCheckHelper.ERR_PASSWORD) {
                            pwInputLayout.setError(msg);
                        }
                    }
                });
                break;
            default:
        }
    }

    //检查返回的json
    private void checkReturn(String res) {
        Log.i("Login", res);
        StatusJsonCheckHelper.check(res, new StatusJsonCheckHelper.CheckCallback() {
            @Override
            public void onPass() {
                usernameInputLayout.setEnabled(false);
                defaultPrefs.put("username", usernameInputLayout.getEditText().getText().toString());
                defaultPrefs.put("MD5Pw", MD5Util.encode(pwInputLayout.getEditText().getText().toString()));
                loginButton.setProgress(100);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        toActivity(LoginActivity.this, MainActivity.class);
                        ActivityManager.finishAll();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onFailure(String msg, int which) {
                if (which == 0) {
                    usernameInputLayout.setError(msg);
                } else {
                    pwInputLayout.setError(msg);
                }
                loginButton.setProgress(0);
                loginButton.setIdleText("重试");
                loginButton.setClickable(true);
            }
        });
    }
}
