/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react;

import android.app.Application;

import androidx.annotation.Nullable;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JSIModulePackage;
import com.facebook.react.bridge.JavaScriptExecutorFactory;
import com.facebook.react.bridge.ReactMarker;
import com.facebook.react.bridge.ReactMarkerConstants;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.common.SurfaceDelegate;
import com.facebook.react.common.SurfaceDelegateFactory;
import com.facebook.react.devsupport.DevSupportManagerFactory;
import com.facebook.react.devsupport.interfaces.DevLoadingViewManager;
import com.facebook.react.devsupport.interfaces.RedBoxHandler;

import java.util.List;

/**
 * Simple class that holds an instance of {@link ReactInstanceManager}. This can be used in your
 * {@link Application class} (see {@link ReactApplication}), or as a static field.
 */
public abstract class ReactNativeHost {

    private final Application mApplication;
    private @Nullable ReactInstanceManager mReactInstanceManager;

    protected ReactNativeHost(Application application) {
        mApplication = application;
    }

    /**
     * 获取当前的ReactInstanceManager实例，或者创建一个
     *
     * @return
     */
    public ReactInstanceManager getReactInstanceManager() {
        if (mReactInstanceManager == null) {
            ReactMarker.logMarker(ReactMarkerConstants.INIT_REACT_RUNTIME_START);
            ReactMarker.logMarker(ReactMarkerConstants.GET_REACT_INSTANCE_MANAGER_START);
            mReactInstanceManager = createReactInstanceManager();
            ReactMarker.logMarker(ReactMarkerConstants.GET_REACT_INSTANCE_MANAGER_END);
        }
        return mReactInstanceManager;
    }

    /**
     * Get whether this holder contains a {@link ReactInstanceManager} instance, or not. I.e. if
     * {@link #getReactInstanceManager()} has been called at least once since this object was created
     * or {@link #clear()} was called.
     */
    public boolean hasInstance() {
        return mReactInstanceManager != null;
    }

    /**
     * Destroy the current instance and release the internal reference to it, allowing it to be GCed.
     */
    public void clear() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.destroy();
            mReactInstanceManager = null;
        }
    }

    /**
     * 创建ReactInstanceManager实例
     * ReactInstanceManager用于管理和维护 React Native 的 JavaScript 执行环境。
     * 作用：
     * 创建和管理React Native实例
     * 处理JavaScript 资源的加载执行
     * 管理React Native模块
     * 处理生命周期事件，确保 JavaScript 执行环境与应用生命周期同步。
     *
     * @return
     */
    protected ReactInstanceManager createReactInstanceManager() {
        ReactMarker.logMarker(ReactMarkerConstants.BUILD_REACT_INSTANCE_MANAGER_START);
        ReactInstanceManagerBuilder builder = ReactInstanceManager.builder().setApplication(mApplication)// 设置Application，ReactInstanceManager需要用到Application上下文执行一些操作
                .setJSMainModulePath(getJSMainModuleName())// 设置 JS 入口文件的路径。指定 React Native 应用的主模块的文件路径
                .setUseDeveloperSupport(getUseDeveloperSupport())// 设置是否使用开发者支持。开发者支持模式允许在运行时进行热重载、调试等开发时特性。
                .setDevSupportManagerFactory(getDevSupportManagerFactory())// 设置开发者支持管理器的工厂。用于创建开发者支持管理器实例，管理开发者支持模式的一些功能。
                .setDevLoadingViewManager(getDevLoadingViewManager())// 设置开发者支持加载视图管理器。用于在开发者支持模式下显示加载视图。
                /*
                 * 设置是否需要跟Activity关联，默认值为true。
                 * 大多数情况下 React Native 在 Android 中通常是通过 Activity 来启动和管理的，React Native 页面嵌入到 Android 应用的 Activity 中。
                 * 但是有时候我们可能需要在 Android 应用的 Fragment 中嵌入 React Native 页面，或者作为自定义view等方式时，就需要设置 setRequireActivity(false)。
                 */
                .setRequireActivity(getShouldRequireActivity())
                .setSurfaceDelegateFactory(getSurfaceDelegateFactory())// 设置SurfaceDelegate工厂。用于创建SurfaceDelegate实例，用于管理Surface的创建和销毁。
                .setLazyViewManagersEnabled(getLazyViewManagersEnabled())// 设置是否启用懒加载视图管理器。懒加载视图管理器可以延迟视图管理器的创建，从而提高应用启动速度。
                .setRedBoxHandler(getRedBoxHandler())//设置 RedBox 处理程序。用于处理 JavaScript 运行时的错误和显示开发者支持的错误信息。
                .setJavaScriptExecutorFactory(getJavaScriptExecutorFactory())// 设置 JavaScript 执行器工厂。用于创建 JavaScript 执行器实例，用于执行 JavaScript 代码。
                .setJSIModulesPackage(getJSIModulePackage())// 设置 JSI 模块包。用于创建 JSI 模块实例，用于在 JavaScript 和 Native 之间进行通信。
                .setInitialLifecycleState(LifecycleState.BEFORE_CREATE)//设置初始的生命周期状态。在 BEFORE_CREATE 阶段初始化 React Native 实例。
                .setReactPackageTurboModuleManagerDelegateBuilder(getReactPackageTurboModuleManagerDelegateBuilder())//设置 React 包 TurboModule 管理器委托构建器。用于配置 React 包 TurboModule 管理器的实现。
                .setJSEngineResolutionAlgorithm(getJSEngineResolutionAlgorithm());//设置 JS 引擎分辨率算法。用于配置 JS 引擎的选择算法。

        /*
         * 将应用程序中的 ReactPackage 实例添加到 ReactInstanceManager 中,以便在 React Native 环境中使用。
         */
        for (ReactPackage reactPackage : getPackages()) {
            builder.addPackage(reactPackage);
        }

        /*获取 JS Bundle 文件路径。如果指定了 JS Bundle 文件路径，则使用该路径。*/
        String jsBundleFile = getJSBundleFile();
        if (jsBundleFile != null) {
            builder.setJSBundleFile(jsBundleFile);
        } else {
            builder.setBundleAssetName(Assertions.assertNotNull(getBundleAssetName()));
        }
        //使用上述配置项构建 ReactInstanceManager 实例。
        ReactInstanceManager reactInstanceManager = builder.build();
        ReactMarker.logMarker(ReactMarkerConstants.BUILD_REACT_INSTANCE_MANAGER_END);
        return reactInstanceManager;
    }

    /**
     * Get the {@link RedBoxHandler} to send RedBox-related callbacks to.
     */
    protected @Nullable RedBoxHandler getRedBoxHandler() {
        return null;
    }

    /**
     * Get the {@link JavaScriptExecutorFactory}. Override this to use a custom Executor.
     */
    protected @Nullable JavaScriptExecutorFactory getJavaScriptExecutorFactory() {
        return null;
    }

    protected @Nullable ReactPackageTurboModuleManagerDelegate.Builder getReactPackageTurboModuleManagerDelegateBuilder() {
        return null;
    }

    protected final Application getApplication() {
        return mApplication;
    }

    protected @Nullable JSIModulePackage getJSIModulePackage() {
        return null;
    }

    /**
     * Returns whether or not to treat it as normal if Activity is null.
     */
    public boolean getShouldRequireActivity() {
        return true;
    }

    /**
     * Returns whether view managers should be created lazily. See {@link
     * ViewManagerOnDemandReactPackage} for details.
     *
     * @experimental
     */
    public boolean getLazyViewManagersEnabled() {
        return false;
    }

    /**
     * Return the {@link SurfaceDelegateFactory} used by NativeModules to get access to a {@link
     * SurfaceDelegate} to interact with a surface. By default in the mobile platform the {@link
     * SurfaceDelegate} it returns is null, and the NativeModule needs to implement its own {@link
     * SurfaceDelegate} to decide how it would interact with its own container surface.
     */
    public SurfaceDelegateFactory getSurfaceDelegateFactory() {
        return new SurfaceDelegateFactory() {
            @Override
            public @Nullable SurfaceDelegate createSurfaceDelegate(String moduleName) {
                return null;
            }
        };
    }

    /**
     * Get the {@link DevLoadingViewManager}. Override this to use a custom dev loading view manager
     */
    protected @Nullable DevLoadingViewManager getDevLoadingViewManager() {
        return null;
    }

    /**
     * Returns the name of the main module. Determines the URL used to fetch the JS bundle from Metro.
     * It is only used when dev support is enabled. This is the first file to be executed once the
     * {@link ReactInstanceManager} is created. e.g. "index.android"
     */
    protected String getJSMainModuleName() {
        return "index.android";
    }

    /**
     * Returns a custom path of the bundle file. This is used in cases the bundle should be loaded
     * from a custom path. By default it is loaded from Android assets, from a path specified by
     * {@link getBundleAssetName}. e.g. "file://sdcard/myapp_cache/index.android.bundle"
     */
    protected @Nullable String getJSBundleFile() {
        return null;
    }

    /**
     * Returns the name of the bundle in assets. If this is null, and no file path is specified for
     * the bundle, the app will only work with {@code getUseDeveloperSupport} enabled and will always
     * try to load the JS bundle from Metro. e.g. "index.android.bundle"
     */
    protected @Nullable String getBundleAssetName() {
        return "index.android.bundle";
    }

    /**
     * Returns whether dev mode should be enabled. This enables e.g. the dev menu.
     */
    public abstract boolean getUseDeveloperSupport();

    /**
     * Get the {@link DevSupportManagerFactory}. Override this to use a custom dev support manager
     */
    protected @Nullable DevSupportManagerFactory getDevSupportManagerFactory() {
        return null;
    }

    /**
     * Returns a list of {@link ReactPackage} used by the app. You'll most likely want to return at
     * least the {@code MainReactPackage}. If your app uses additional views or modules besides the
     * default ones, you'll want to include more packages here.
     */
    protected abstract List<ReactPackage> getPackages();

    /**
     * Returns the {@link JSEngineResolutionAlgorithm} to be used when loading the JS engine. If null,
     * will try to load JSC first and fallback to Hermes if JSC is not available.
     */
    protected @Nullable JSEngineResolutionAlgorithm getJSEngineResolutionAlgorithm() {
        return null;
    }
}
