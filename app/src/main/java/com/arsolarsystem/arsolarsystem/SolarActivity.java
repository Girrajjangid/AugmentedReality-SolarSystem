package com.arsolarsystem.arsolarsystem;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SolarActivity extends AppCompatActivity {
    private static final int RC_PERMISSION = 0x123;
    private boolean installRequired;
    private static final String TAG = "SolarActivity";
    private GestureDetector gestureDetector;
    private Snackbar loadindMessageSnackbar = null;

    private ArSceneView arSceneView;

    private ModelRenderable sunRenderable;
    private ModelRenderable mercuryRenderable;
    private ModelRenderable venusRenderable;
    private ModelRenderable earthRenderable;
    private ModelRenderable lunaRenderable;
    private ModelRenderable marsRenderable;
    private ModelRenderable jupiterRenderable;
    private ModelRenderable saturnRenderable;
    private ModelRenderable uranusRenderable;
    private ModelRenderable neptuneRenderable;

    private ViewRenderable solarControlsRenderable;

    //True once scene is loaded
    private boolean hasFinishedLoading = false;

    //True once the scene has been placed
    private boolean hasPlacedsolarSystem = false;

    // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
    private static final float AU_TO_METERS = 0.5f;

    private SolarSettings solarSettings = new SolarSettings();

    // CompletableFuture requires api level 24
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar);
        if (!PermissionHelper.checkIsSupportedDeviceOrFinish(this)) {
            // Not a supported device;
            return;
        }
        arSceneView = findViewById(R.id.ar_scene_view);

        //It initilize models
        initializeModels();

        //set on tap gesture detector.
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        //Set a touch listener on the Scene to listen for taps.
        arSceneView.getScene().setOnTouchListener(new Scene.OnTouchListener() {
            @Override
            public boolean onSceneTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
                // If the solar system hasn't been placed yet, detect a tap and then check to see if
                // the tap occurred on an ARCore plane to place the solar system.
                if (!hasPlacedsolarSystem) {
                    return gestureDetector.onTouchEvent(motionEvent);
                }
                // Otherwise return false so that the touch event can propagate to the scene.
                return false;
            }
        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView.getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if (loadindMessageSnackbar == null) {
                    return;
                }
                Frame frame = arSceneView.getArFrame();
                if (frame == null) {
                    return;
                }
                if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                    return;
                }
                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                    }
                }
            }
        });

        // Lastly request CAMERA permission which is required by ARCore.
        PermissionHelper.requestCameraPermission(this, RC_PERMISSION);
    }

    private void initializeModels() {
        //Build all the Planets Model
        CompletableFuture<ModelRenderable> sunStage = ModelRenderable.builder().setSource(this, Uri.parse("Sol.sfb")).build();
        CompletableFuture<ModelRenderable> mercuryStage = ModelRenderable.builder().setSource(this, Uri.parse("Mercury.sfb")).build();
        CompletableFuture<ModelRenderable> venusStage = ModelRenderable.builder().setSource(this, Uri.parse("Venus.sfb")).build();
        CompletableFuture<ModelRenderable> earthStage = ModelRenderable.builder().setSource(this, Uri.parse("Earth.sfb")).build();
        CompletableFuture<ModelRenderable> lunaStage = ModelRenderable.builder().setSource(this, Uri.parse("Luna.sfb")).build();
        CompletableFuture<ModelRenderable> marsStage = ModelRenderable.builder().setSource(this, Uri.parse("Mars.sfb")).build();
        CompletableFuture<ModelRenderable> jupiterStage = ModelRenderable.builder().setSource(this, Uri.parse("Jupiter.sfb")).build();
        CompletableFuture<ModelRenderable> saturnStage = ModelRenderable.builder().setSource(this, Uri.parse("Saturn.sfb")).build();
        CompletableFuture<ModelRenderable> uranusStage = ModelRenderable.builder().setSource(this, Uri.parse("Uranus.sfb")).build();
        CompletableFuture<ModelRenderable> neptuneStage = ModelRenderable.builder().setSource(this, Uri.parse("Neptune.sfb")).build();

        CompletableFuture<ViewRenderable> solarControlsStage = ViewRenderable.builder().setView(this, R.layout.solar_controls).build();

        CompletableFuture.allOf(sunStage,
                mercuryStage,
                venusStage,
                earthStage,
                lunaStage,
                marsStage,
                jupiterStage,
                saturnStage,
                uranusStage,
                neptuneStage,
                solarControlsStage)
                .handle((aVoid, throwable) -> {
                    // When you build a Renderable, Sceneform loads its resources in the background while
                    // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                    // before calling get().
                    if (throwable != null) {
                        PermissionHelper.dispalyError(this, "Unable to load Renderables ", throwable);
                        return null;
                    }
                    try {
                        sunRenderable = sunStage.get();
                        mercuryRenderable = mercuryStage.get();
                        venusRenderable = venusStage.get();
                        earthRenderable = earthStage.get();
                        lunaRenderable = lunaStage.get();
                        marsRenderable = marsStage.get();
                        jupiterRenderable = jupiterStage.get();
                        saturnRenderable = saturnStage.get();
                        uranusRenderable = uranusStage.get();
                        neptuneRenderable = neptuneStage.get();
                        solarControlsRenderable = solarControlsStage.get();

                        //Everything finished loading successfully.
                        hasFinishedLoading = true;

                        Toast.makeText(this, "Renderables Initialized Successfully", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "onCreate: Models loading Successfully");

                    } catch (ExecutionException | InterruptedException e) {
                        PermissionHelper.dispalyError(this, "Unable to load to Renderable", e);
                    }
                    return null;
                });

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (arSceneView == null) {
            return;
        }
        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = PermissionHelper.createARSession(this, installRequired);
                if (session == null) {
                    installRequired = PermissionHelper.hasCameraPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                PermissionHelper.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            PermissionHelper.dispalyError(this, "Unable to get Camera", e);
            finish();
            return;
        }
        if (arSceneView != null) {
            showLoadingMessage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionHelper.hasCameraPermission(this)) {
            if (!PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                PermissionHelper.launchPermssionSettings(this);
            } else {
                Toast.makeText(this, "Camera permission need to run this Application", Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    private void onSingleTap(MotionEvent tap) {
        if (!hasFinishedLoading) {
            Toast.makeText(this, "Move your Phone", Toast.LENGTH_SHORT).show();
            //We cant do anything yet
        }
        Frame frame = arSceneView.getArFrame();
        if (frame != null) {
            if (!hasPlacedsolarSystem && tryPlaceSolarSystem(tap, frame)) {
                hasPlacedsolarSystem = true;
            }
        }
    }

    private boolean tryPlaceSolarSystem(MotionEvent tap, Frame frame) {
        if (tap != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hitResult : frame.hitTest(tap)) {
                Trackable trackable = hitResult.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hitResult.getHitPose())) {
                    //Create an Anchor
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arSceneView.getScene());
                    Node solarSystem = createSolarSystem();
                    anchorNode.addChild(solarSystem);
                    return true;
                }
            }
        }
        return false;
    }

    private Node createSolarSystem() {
        Node base = new Node();

        Node sun = new Node();
        sun.setParent(base);
        sun.setLocalPosition(new Vector3(0.0f, 0.5f, 0.0f));

        Node sunVisual = new Node();
        sunVisual.setParent(sun);
        sunVisual.setRenderable(sunRenderable);
        sunVisual.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));

        Node solarControl = new Node();
        solarControl.setParent(sun);
        solarControl.setRenderable(solarControlsRenderable);
        solarControl.setLocalPosition(new Vector3(0.0f, 0.25f, 0.0f));

        View solarControlView = solarControlsRenderable.getView();
        SeekBar orbitSpeedbar = solarControlView.findViewById(R.id.orbitSpeedBar);
        orbitSpeedbar.setProgress((int) (solarSettings.getOrbitSpeedMultiplier() * 10.0f));
        orbitSpeedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / (float) orbitSpeedbar.getMax();
                solarSettings.setOrbitSpeedMultiplier(ratio * 10.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar rotationSpeedbar = solarControlView.findViewById(R.id.rotationSpeedBar);
        orbitSpeedbar.setProgress((int) (solarSettings.getRotationSpeedMultiplier() * 10.0f));
        rotationSpeedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / (float) rotationSpeedbar.getMax();
                solarSettings.setRotationSpeedMultiplier(ratio * 10.0f);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Toggle the Solar Controls on and off by tapping the Sun.
        sun.setOnTapListener((hitTestResult, motionEvent) -> solarControl.setEnabled(!solarControl.isEnabled()));

        createPlanet("Mercury", sun, 0.4f, 47f, mercuryRenderable, 0.019f);
        createPlanet("Venus", sun, 0.7f, 35f, venusRenderable, 0.0475f);

        Node earth = createPlanet("Earth", sun, 1.0f, 29f, earthRenderable, 0.05f);

        createPlanet("Moon", earth, 0.15f, 100f, lunaRenderable, 0.018f);

        createPlanet("Mars", sun, 1.5f, 24f, marsRenderable, 0.0265f);

        createPlanet("Jupiter", sun, 2.2f, 13f, jupiterRenderable, 0.16f);

        createPlanet("Saturn", sun, 3.5f, 9f, saturnRenderable, 0.1325f);

        createPlanet("Uranus", sun, 5.2f, 7f, uranusRenderable, 0.1f);

        createPlanet("Neptune", sun, 6.1f, 5f, neptuneRenderable, 0.074f);
        return base;
    }

    private Node createPlanet(String name,
                             Node parent,
                             float auFromParent,
                             float orbitDegreePerSecond,
                             ModelRenderable renderable,
                             float planetScale) {
        // Orbit is a rotating node with no renderable positioned at the sun.
        // The planet is positioned relative to the orbit so that it appears to rotate around the sun.
        // This is done instead of making the sun rotate so each planet can orbit at its own speed.
        RotatingNode orbit = new RotatingNode(solarSettings,true);
        orbit.setDegreePerSecond(orbitDegreePerSecond);
        orbit.setParent(parent);

        // Create the planet and position it relative to the sun.
        Planet planet = new Planet(this, name, planetScale, renderable, solarSettings);
        planet.setParent(orbit);
        planet.setLocalPosition(new Vector3(auFromParent * AU_TO_METERS, 0.0f, 0.0f));
        return planet;
    }


    private void showLoadingMessage() {
        if (loadindMessageSnackbar != null && loadindMessageSnackbar.isShownOrQueued()) {
            return;
        }
        loadindMessageSnackbar =
                Snackbar.make(SolarActivity.this.findViewById(android.R.id.content), R.string.plane_finding, Snackbar.LENGTH_INDEFINITE);
        loadindMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadindMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if(loadindMessageSnackbar==null){
            return;
        }
        loadindMessageSnackbar.dismiss();
        loadindMessageSnackbar = null;
    }
}
