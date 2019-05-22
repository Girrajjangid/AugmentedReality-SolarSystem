package com.arsolarsystem.arsolarsystem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

public class PermissionHelper {
    private static final String TAG = "PermissionHelper";
    private static final double MINI_OPENGL_VERSION = 3.0;
/**
 * Creates and shows a Toast containing an error message. If there was an exception passed in it
 * will be appended to the toast. The error will also be written to the Log
 */
public static void dispalyError(Context context,String errorMsg , Throwable exception) {
    final String Tag = context.getClass().getSimpleName();
    final String toastText;
    if (exception != null && exception.getMessage() != null) {
        Log.e(Tag, "dispalyError: " + errorMsg, exception);
        toastText = errorMsg + ": " + exception.getMessage();
    } else if (exception != null) {
        Log.e(Tag, errorMsg, exception);
        toastText = errorMsg;
    } else {
        Log.e(Tag, errorMsg);
        toastText = errorMsg;
    }
    new Handler(Looper.getMainLooper())
            .post(() -> {
                Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            });
}

    /**
     * Creates an ARCore session. This checks for the CAMERA permission, and if granted, checks the
     * state of the ARCore installation. If there is a problem an exception is thrown. Care must be
     * taken to update the installRequested flag as needed to avoid an infinite checking loop. It
     * should be set to true if null is returned from this method, and called again when the
     * application is resumed.
     *
     * @param activity - the activity currently active.
     * @param installRequested - the indicator for ARCore that when checking the state of ARCore, if
     *     an installation was already requested. This is true if this method previously returned
     *     null. and the camera permission has been granted.
     */
    public static Session createARSession(Activity activity , boolean installRequested) throws UnavailableException {
        Session session = null;
        // if we have the camera permission, create the session
        if (hasCameraPermission(activity)) {
            switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                case INSTALL_REQUESTED:
                    return null;
                case INSTALLED:
                    break;
            }
            session = new Session(activity);
            // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
        }
        return session;
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't. */
    public static void requestCameraPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, requestCode);
    }
    /** Check to see we have the necessary permissions for this app. */
    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    /** Check to see if we need to show the rationale for this permission. */
    public static boolean shouldShowRequestPermissionRationale(Activity activity){
        return ActivityCompat.shouldShowRequestPermissionRationale(activity , Manifest.permission.CAMERA);
    }
    /** Launch Application Setting to grant permission. */
    public static void launchPermssionSettings(Activity activity){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package",activity.getPackageName(),null));
        activity.startActivity(intent);
    }
    public static void handleSessionException(Activity activity, UnavailableException sessionException){
        String message;

        if (sessionException instanceof UnavailableApkTooOldException){
            message = "Please Update ARCore";
        }else if(sessionException instanceof UnavailableArcoreNotInstalledException){
            message = "Please install ARCore";
        }else if(sessionException instanceof UnavailableSdkTooOldException){
            message = "Please update this App";
        }else if ( sessionException instanceof UnavailableDeviceNotCompatibleException){
            message = "This device doesn't support this App";
        }else {
            message = "Failed to create AR Session";
            Log.e(TAG, "handleSessionException: ",sessionException );
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    @SuppressLint("ObsoleteSdkInt")
    public static boolean checkIsSupportedDeviceOrFinish(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "checkIsSupportedDeviceOrFinish: Sceneform required Android N or later");
            Toast.makeText(activity, "Sceneform required Android N or later", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
        String openGLVersionString = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo().getGlEsVersion();
        if (Double.parseDouble(openGLVersionString) < MINI_OPENGL_VERSION) {
            Log.e(TAG, "checkIsSupportedDeviceOrFinish: Sceneform Required OpenGLES 3.0 later");
            Toast.makeText(activity, "Sceneform Required OpenGLES 3.0 later", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
        return true;
    }
}
