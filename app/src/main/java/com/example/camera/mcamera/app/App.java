package com.example.camera.mcamera.app;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;


public class App extends Application {
    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        Fabric.with(this, new Crashlytics());
        // TODO: Move this to where you establish a user session
        logUser();

    }
    private void logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier("zealjiang");
        Crashlytics.setUserEmail("zealjiang@126.com");
        Crashlytics.setUserName("zealjiang");
    }


}
