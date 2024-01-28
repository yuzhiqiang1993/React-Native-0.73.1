/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

/**
 * Base Activity for React Native applications.
 */
public abstract class ReactActivity extends AppCompatActivity
        implements DefaultHardwareBackBtnHandler, PermissionAwareActivity {

    private final ReactActivityDelegate mDelegate;

    /**
     * 在页面构造方法中调用了createReactActivityDelegate()方法，用于创建ReactActivityDelegate对象
     */
    protected ReactActivity() {
        mDelegate = createReactActivityDelegate();
    }

    /**
     * 在构造方法中调用了getMainComponentName()方法，用于获取JS端注册的主组件名称，子类可以重写该方法，返回自定义的主组件名称
     *
     * @return
     */
    protected @Nullable String getMainComponentName() {
        return null;
    }

    /**
     * 在构造方法中调用了createReactActivityDelegate()方法，用于创建ReactActivityDelegate对象，子类可以重写该方法，返回自定义的ReactActivityDelegate对象
     *
     * @return
     */
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new ReactActivityDelegate(this, getMainComponentName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 调用了ReactActivityDelegate的onCreate()方法
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 调用了ReactActivityDelegate的onPause()方法
        mDelegate.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 调用了ReactActivityDelegate的onResume()方法
        mDelegate.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 调用了ReactActivityDelegate的onDestroy()方法
        mDelegate.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        mDelegate.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 调用了ReactActivityDelegate的onKeyDown()方法
        return mDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 调用了ReactActivityDelegate的onKeyUp()方法
        return mDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (!mDelegate.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (!mDelegate.onNewIntent(intent)) {
            super.onNewIntent(intent);
        }
    }

    @Override
    public void requestPermissions(
            String[] permissions, int requestCode, PermissionListener listener) {
        // 调用了ReactActivityDelegate的requestPermissions()方法
        mDelegate.requestPermissions(permissions, requestCode, listener);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        mDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mDelegate.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDelegate.onConfigurationChanged(newConfig);
    }

    /**
     * 获取ReactActivityDelegate对象
     *
     * @return
     */
    protected final ReactNativeHost getReactNativeHost() {
        return mDelegate.getReactNativeHost();
    }

    /**
     * 获取ReactInstanceManager对象
     *
     * @return
     */
    protected final ReactInstanceManager getReactInstanceManager() {
        return mDelegate.getReactInstanceManager();
    }

    /**
     * 加载JS Bundle
     *
     * @param appKey
     */
    protected final void loadApp(String appKey) {
        mDelegate.loadApp(appKey);
    }
}
