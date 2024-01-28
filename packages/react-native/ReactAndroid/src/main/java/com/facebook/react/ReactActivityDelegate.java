/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Callback;
import com.facebook.react.config.ReactFeatureFlags;
import com.facebook.react.modules.core.PermissionListener;

/**
 * Delegate class for {@link ReactActivity}. You can subclass this to provide custom implementations
 * for e.g. {@link #getReactNativeHost()}, if your Application class doesn't implement {@link
 * ReactApplication}.
 */
public class ReactActivityDelegate {

    private final @Nullable Activity mActivity;
    private final @Nullable String mMainComponentName;

    private @Nullable PermissionListener mPermissionListener;
    private @Nullable Callback mPermissionsCallback;
    private ReactDelegate mReactDelegate;

    @Deprecated
    public ReactActivityDelegate(Activity activity, @Nullable String mainComponentName) {
        mActivity = activity;
        mMainComponentName = mainComponentName;
    }

    public ReactActivityDelegate(ReactActivity activity, @Nullable String mainComponentName) {
        mActivity = activity;
        mMainComponentName = mainComponentName;
    }

    /**
     * Public API to populate the launch options that will be passed to React. Here you can customize
     * the values that will be passed as `initialProperties` to the Renderer.
     *
     * @return Either null or a key-value map as a Bundle
     */
    protected @Nullable Bundle getLaunchOptions() {
        return null;
    }

    protected @Nullable Bundle composeLaunchOptions() {
        Bundle composedLaunchOptions = getLaunchOptions();
        if (isFabricEnabled()) {
            if (composedLaunchOptions == null) {
                composedLaunchOptions = new Bundle();
            }
            composedLaunchOptions.putBoolean("concurrentRoot", true);
        }
        return composedLaunchOptions;
    }

    protected ReactRootView createRootView() {
        return new ReactRootView(getContext());
    }

    protected ReactRootView createRootView(Bundle initialProps) {
        return new ReactRootView(getContext());
    }

    /**
     * Get the {@link ReactNativeHost} used by this app. By default, assumes {@link
     * Activity#getApplication()} is an instance of {@link ReactApplication} and calls {@link
     * ReactApplication#getReactNativeHost()}. Override this method if your application class does not
     * implement {@code ReactApplication} or you simply have a different mechanism for storing a
     * {@code ReactNativeHost}, e.g. as a static field somewhere.
     */
    protected ReactNativeHost getReactNativeHost() {
        return ((ReactApplication) getPlainActivity().getApplication()).getReactNativeHost();
    }

    public ReactHost getReactHost() {
        return ((ReactApplication) getPlainActivity().getApplication()).getReactHost();
    }

    public ReactInstanceManager getReactInstanceManager() {
        return mReactDelegate.getReactInstanceManager();
    }

    public String getMainComponentName() {
        return mMainComponentName;
    }

    protected void onCreate(Bundle savedInstanceState) {
        // 获取要加载的JS端注册的主组件名称
        String mainComponentName = getMainComponentName();
        // 获取要传递给JS端的启动参数
        final Bundle launchOptions = composeLaunchOptions();
        // 创建ReactDelegate对象
        if (ReactFeatureFlags.enableBridgelessArchitecture) {
            /**
             * 开启了 Bridgeless 架构时
             * 传入的参数分别为：
             * 1. Activity对象
             * 2. ReactHost接口实现类对象，用于承载React实例的容器，一般为Activity对象或Fragment
             * 3. JS端注册的主组件名称
             * 4. 启动参数
             *
             * 由此可见，启动了Bridgeless架构后，ReactDelegate的构造方法中不再需要App应用级别的ReactNativeHost对象，更改为粒度更小的页面级别的ReactHost对象
             * 原因是：在传统的 Bridge 架构中，通信是通过 JavaScript 和原生之间的桥梁来进行的，而这个桥梁是由应用级别的 ReactNativeHost 管理的。
             * 这种方式需要进行消息的序列化、传递、反序列化，可能引入一些性能开销。
             * 而在 Bridgeless 架构中，由于避开了桥梁的使用，JavaScript 和原生代码可以更直接地进行通信，因此不再需要应用级别的 ReactNativeHost 来管理桥梁。
             * 相反，可以在页面级别使用更轻量级的 ReactHost 来管理 React Native 实例，降低了通信的开销，提高了性能。
             * 这种调整是为了在 Bridgeless 架构中更灵活地管理 React Native 实例，使其更适应于页面级别的使用。
             */
            mReactDelegate =
                    new ReactDelegate(getPlainActivity(), getReactHost(), mainComponentName, launchOptions);
        } else {
            /**
             * 未开启Bridgeless架构时
             * 传入的参数分别为：
             * 1. Activity对象
             * 2. ReactNativeHost接口实现类对象，是应用级别的React容器，负责管理整个应用程序React实例的创建，销毁，和配置。
             * 3. JS端注册的主组件名称
             * 4. 启动参数
             */
            mReactDelegate =
                    new ReactDelegate(
                            getPlainActivity(), getReactNativeHost(), mainComponentName, launchOptions) {
                        @Override
                        protected ReactRootView createRootView() {
                            return ReactActivityDelegate.this.createRootView(launchOptions);
                        }
                    };
        }
        if (mainComponentName != null) {
            // 加载JS端注册的主组件
            loadApp(mainComponentName);
        }
    }

    protected void loadApp(String appKey) {
        //调用ReactDelegate的loadApp方法
        mReactDelegate.loadApp(appKey);
        /**
         * 将ReactRootView添加到Activity的ContentView中
         * 如果开启了Bridgeless架构，getReactRootView()方法返回的是ReactSurfaceView对象，强转为 ReactRootView 类型
         * 如果未开启Bridgeless架构，获取的就是ReactRootView
         *
         */
        getPlainActivity().setContentView(mReactDelegate.getReactRootView());
    }

    protected void onPause() {
        mReactDelegate.onHostPause();
    }

    protected void onResume() {
        mReactDelegate.onHostResume();

        if (mPermissionsCallback != null) {
            mPermissionsCallback.invoke();
            mPermissionsCallback = null;
        }
    }

    protected void onDestroy() {
        mReactDelegate.onHostDestroy();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mReactDelegate.onActivityResult(requestCode, resultCode, data, true);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!ReactFeatureFlags.enableBridgelessArchitecture) {
            if (getReactNativeHost().hasInstance()
                    && getReactNativeHost().getUseDeveloperSupport()
                    && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                event.startTracking();
                return true;
            }
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mReactDelegate.shouldShowDevMenuOrReload(keyCode, event);
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (!ReactFeatureFlags.enableBridgelessArchitecture) {
            if (getReactNativeHost().hasInstance()
                    && getReactNativeHost().getUseDeveloperSupport()
                    && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                getReactNativeHost().getReactInstanceManager().showDevOptionsDialog();
                return true;
            }
        }
        return false;
    }

    public boolean onBackPressed() {
        return mReactDelegate.onBackPressed();
    }

    public boolean onNewIntent(Intent intent) {
        if (!ReactFeatureFlags.enableBridgelessArchitecture) {
            if (getReactNativeHost().hasInstance()) {
                getReactNativeHost().getReactInstanceManager().onNewIntent(intent);
                return true;
            }
        }
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (!ReactFeatureFlags.enableBridgelessArchitecture) {
            if (getReactNativeHost().hasInstance()) {
                getReactNativeHost().getReactInstanceManager().onWindowFocusChange(hasFocus);
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (!ReactFeatureFlags.enableBridgelessArchitecture) {
            if (getReactNativeHost().hasInstance()) {
                getReactInstanceManager().onConfigurationChanged(getContext(), newConfig);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(
            String[] permissions, int requestCode, PermissionListener listener) {
        mPermissionListener = listener;
        getPlainActivity().requestPermissions(permissions, requestCode);
    }

    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        mPermissionsCallback =
                new Callback() {
                    @Override
                    public void invoke(Object... args) {
                        if (mPermissionListener != null
                                && mPermissionListener.onRequestPermissionsResult(
                                requestCode, permissions, grantResults)) {
                            mPermissionListener = null;
                        }
                    }
                };
    }

    protected Context getContext() {
        return Assertions.assertNotNull(mActivity);
    }

    protected Activity getPlainActivity() {
        return ((Activity) getContext());
    }

    /**
     * Override this method if you wish to selectively toggle Fabric for a specific surface. This will
     * also control if Concurrent Root (React 18) should be enabled or not.
     *
     * @return true if Fabric is enabled for this Activity, false otherwise.
     */
    protected boolean isFabricEnabled() {
        return false;
    }
}
