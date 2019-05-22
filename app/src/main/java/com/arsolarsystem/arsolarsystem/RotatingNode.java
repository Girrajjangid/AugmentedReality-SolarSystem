package com.arsolarsystem.arsolarsystem;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.QuaternionEvaluator;
import com.google.ar.sceneform.math.Vector3;

class RotatingNode extends Node {
    //We'll use animation property to make this node rotate.
    private ObjectAnimator orbitAnimation = null;
    private float degreePerSecond = 90.0f;

    private SolarSettings solarSettings;
    private final boolean isOrbit;
    private float lastSpeedMultiplier = 1.0f;

    public RotatingNode(SolarSettings solarSettings, boolean isOrbit) {
        this.solarSettings = solarSettings;
        this.isOrbit = isOrbit;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        //Animation hasn't be set up.
        if (orbitAnimation == null) {
            return;
        }
        //check if we need to change the speed of rotation
        float speedMultiplier = getSpeedMultiplier();

        // Nothing has changed continue to normal speed.
        if (lastSpeedMultiplier == speedMultiplier) {
            return;
        }
        if (speedMultiplier == 0.0f) {
            orbitAnimation.pause();
        } else {
            orbitAnimation.resume();

            float animatedFraction = orbitAnimation.getAnimatedFraction();
            orbitAnimation.setDuration(getAnimationDuration());
            orbitAnimation.setCurrentFraction(animatedFraction);
        }
        lastSpeedMultiplier = speedMultiplier;
    }

    private long getAnimationDuration() {
        return (long) (1000 * 360 / (degreePerSecond * getSpeedMultiplier()));
    }

    private float getSpeedMultiplier() {
        if (isOrbit) {
            return solarSettings.getOrbitSpeedMultiplier();
        } else {
            return solarSettings.getRotationSpeedMultiplier();
        }
    }

    public void setDegreePerSecond(float degreePerSecond) {
        this.degreePerSecond = degreePerSecond;
    }

    @Override
    public void onActivate() {
        startAnimation();
    }

    @Override
    public void onDeactivate() {
        stopAnimation();
    }

    private void startAnimation() {
        if (orbitAnimation != null) {
            return;
        }
        orbitAnimation = createAnimator();
        orbitAnimation.setTarget(this);
        orbitAnimation.setDuration(getAnimationDuration());
        orbitAnimation.start();
    }

    /** Returns an ObjectAnimator that makes this node rotate. */
    private ObjectAnimator createAnimator() {
        // Node's setLocalRotation method accepts Quaternions as parameters.
        // First, set up orientations that will animate a circle.
        Quaternion orientation1 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 0);
        Quaternion orientation2 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 120);
        Quaternion orientation3 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 240);
        Quaternion orientation4 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 360);
        ObjectAnimator orbitAnimation = new ObjectAnimator();
        orbitAnimation.setObjectValues(orientation1, orientation2, orientation3, orientation4);

        //NExt give it the localRotation Property.
        orbitAnimation.setPropertyName("localRotation");

        //Use Sceneform QuaterianEvaluator
        orbitAnimation.setEvaluator(new QuaternionEvaluator());

        //Allow orbitAnimation to repeat forever
        orbitAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        orbitAnimation.setRepeatMode(ObjectAnimator.RESTART);
        orbitAnimation.setInterpolator(new LinearInterpolator());
        orbitAnimation.setAutoCancel(true);

        return orbitAnimation;
    }


    private void stopAnimation() {
        if (orbitAnimation == null) {
            return;
        }
        orbitAnimation.cancel();
        orbitAnimation = null;
    }
}
