/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecreativelab.drawar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.googlecreativelab.drawar.rendering.BackgroundRenderer;
import com.googlecreativelab.drawar.rendering.DataHolder;
import com.googlecreativelab.drawar.rendering.LineShaderRenderer;
import com.googlecreativelab.drawar.rendering.LineUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;


/**
 * This is a complex example that shows how to create an augmented reality (AR) application using
 * the ARCore API.
 */

public class DrawAR extends AppCompatActivity implements GLSurfaceView.Renderer, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{
    private static final String TAG = DrawAR.class.getSimpleName();

    private GLSurfaceView mSurfaceView;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private LineShaderRenderer mLineShaderRenderer = new LineShaderRenderer();
    private Frame mFrame;

    private float[] projmtx = new float[16];
    private float[] viewmtx = new float[16];
    private float[] mZeroMatrix = new float[16];

    private boolean mPaused = false;

    private float mScreenWidth = 0;
    private float mScreenHeight = 0;

    private BiquadFilter biquadFilter;
    private Vector3f mLastPoint;
    private AtomicReference<Vector2f> lastTouch = new AtomicReference<>();

    private GestureDetectorCompat mDetector;

    private LinearLayout mSettingsUI;
    private LinearLayout pattern;
    private LinearLayout mButtonBar;




    private float mLineWidthMax = 0.6f;
    private float mDistanceScale = 0.0f;
    private float mLineSmoothing = 0.1f;

    private float[] mLastFramePosition;

    private AtomicBoolean bIsTracking = new AtomicBoolean(true);
    private AtomicBoolean bReCenterView = new AtomicBoolean(false);
    private AtomicBoolean bTouchDown = new AtomicBoolean(false);
    private AtomicBoolean bClearDrawing = new AtomicBoolean(false);
    private AtomicBoolean bLineParameters = new AtomicBoolean(false);
    private AtomicBoolean bUndo = new AtomicBoolean(false);
    private AtomicBoolean bNewStroke = new AtomicBoolean(false);

    private ArrayList<ArrayList<Vector3f>> mStrokes;
    private ArrayList<ArrayList<ArrayList<Vector3f>>> allStrockes;
    private Integer id =-1;

    private DisplayRotationHelper mDisplayRotationHelper;
    private Snackbar mMessageSnackbar;

    private boolean bInstallRequested;

    private TrackingState mState;
    private String code="0000";
    /**
     * Setup the app when main activity is created
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent!=null){
            if (intent.hasExtra("code")){
                code = intent.getStringExtra("code");
                TextView codeTextview =  findViewById(R.id.codeRoom);
                codeTextview.setText(code);
            }
        }

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("roomList").child(code);





        mSurfaceView = findViewById(R.id.surfaceview);
        mSettingsUI = findViewById(R.id.strokeUI);
        pattern = findViewById(R.id.pattern);
        ImageView imageView = (ImageView) findViewById(R.id.patterneImage);
        imageView.setImageResource(R.drawable.modele2);
        mButtonBar = findViewById(R.id.button_bar);



        // Hide the settings ui
        mSettingsUI.setVisibility(View.GONE);
        pattern.setVisibility(View.GONE);

        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
        // Reset the zero matrix
        Matrix.setIdentityM(mZeroMatrix, 0);

        mLastPoint = new Vector3f(0, 0, 0);

        bInstallRequested = false;

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Setup touch detector
        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);
        mStrokes = new ArrayList<>();
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<ArrayList<ArrayList<ArrayList<Vector3f>>>> t = new GenericTypeIndicator<ArrayList<ArrayList<ArrayList<Vector3f>>>>() {};
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                ArrayList<ArrayList<ArrayList<Vector3f>>> value = dataSnapshot.getValue(t);
                allStrockes = value;
                if (id == -1){
                    mStrokes = new ArrayList<>();
                    id = allStrockes.size();
                    allStrockes.add(mStrokes);
                    save();
                }
                if (allStrockes.size()>=id) {
                    allStrockes.set(id, mStrokes);
                }else{
                    allStrockes.add(id,mStrokes);
                }
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }


    /**
     * addStroke adds a new stroke to the scene
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private void addStroke(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addStroke(newPoint);
    }


    /**
     * addPoint adds a point to the current stroke
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private void addPoint(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addPoint(newPoint);
    }


    /**
     * addStroke creates a new stroke
     *
     * @param newPoint a 3D point in world space
     */
    private void addStroke(Vector3f newPoint) {
        biquadFilter = new BiquadFilter(mLineSmoothing);
        for (int i = 0; i < 1500; i++) {
            biquadFilter.update(newPoint);
        }
        Vector3f p = biquadFilter.update(newPoint);
        mLastPoint = new Vector3f(p);
        mStrokes.add(new ArrayList<Vector3f>());
        mStrokes.get(mStrokes.size() - 1).add(mLastPoint);

    }

    /**
     * addPoint adds a point to the current stroke
     *
     * @param newPoint a 3D point in world space
     */
    private void addPoint(Vector3f newPoint) {
        if (LineUtils.distanceCheck(newPoint, mLastPoint)) {
            Vector3f p = biquadFilter.update(newPoint);
            mLastPoint = new Vector3f(p);
            mStrokes.get(mStrokes.size() - 1).add(mLastPoint);

        }
    }
    private void save(){
        myRef.setValue(allStrockes);
    }

    /**
     * onResume part of the Android Activity Lifecycle
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !bInstallRequested)) {
                    case INSTALL_REQUESTED:
                        bInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!PermissionHelper.hasCameraPermission(this)) {
                    PermissionHelper.requestCameraPermission(this);
                    return;
                }

                mSession = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(mSession);
            if (!mSession.isSupported(config)) {
                Log.e(TAG, "Exception creating session Device Does Not Support ARCore", exception);
            }
            mSession.configure(config);
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.
        mSession.resume();
        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();
        mPaused = false;
    }

    /**
     * onPause part of the Android Activity Lifecycle
     */
    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.

        if (mSession != null) {
            mDisplayRotationHelper.onPause();
            mSurfaceView.onPause();
            mSession.pause();
        }

        mPaused = false;


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenHeight = displayMetrics.heightPixels;
        mScreenWidth = displayMetrics.widthPixels;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
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


    /**
     * Create renderers after the Surface is Created and on the GL Thread
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        if (mSession == null) {
            return;
        }

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
            mLineShaderRenderer.createOnGlThread(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        mScreenWidth = width;
        mScreenHeight = height;
    }


    /**
     * update() is executed on the GL Thread.
     * The method handles all operations that need to take place before drawing to the screen.
     * The method :
     * extracts the current projection matrix and view matrix from the AR Pose
     * handles adding stroke and points to the data collections
     * updates the ZeroMatrix and performs the matrix multiplication needed to re-center the drawing
     * updates the Line Renderer with the current strokes, color, distance scale, line width etc
     */
    private void update() {

        if (mSession == null) {
            return;
        }

        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

            mFrame = mSession.update();
            Camera camera = mFrame.getCamera();

            mState = camera.getTrackingState();

            // Update tracking states
            if (mState == TrackingState.TRACKING && !bIsTracking.get()) {
                bIsTracking.set(true);
            } else if (mState== TrackingState.STOPPED && bIsTracking.get()) {
                bIsTracking.set(false);
                bTouchDown.set(false);
            }

            // Get projection matrix.
            camera.getProjectionMatrix(projmtx, 0, AppSettings.getNearClip(), AppSettings.getFarClip());
            camera.getViewMatrix(viewmtx, 0);

            float[] position = new float[3];
            camera.getPose().getTranslation(position, 0);

            // Check if camera has moved much, if thats the case, stop touchDown events
            // (stop drawing lines abruptly through the air)
            if (mLastFramePosition != null) {
                Vector3f distance = new Vector3f(position[0], position[1], position[2]);
                distance.sub(new Vector3f(mLastFramePosition[0], mLastFramePosition[1], mLastFramePosition[2]));

                if (distance.length() > 0.15) {
                    bTouchDown.set(false);
                }
            }
            mLastFramePosition = position;

            // Multiply the zero matrix
            Matrix.multiplyMM(viewmtx, 0, viewmtx, 0, mZeroMatrix, 0);


            if (bNewStroke.get()) {
                bNewStroke.set(false);
                addStroke(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            } else if (bTouchDown.get()) {
                addPoint(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bReCenterView.get()) {
                bReCenterView.set(false);
                mZeroMatrix = getCalibrationMatrix();
            }

            if (bClearDrawing.get()) {
                bClearDrawing.set(false);
                clearDrawing();
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bUndo.get()) {
                bUndo.set(false);
                if (mStrokes.size() > 0) {
                    mStrokes.remove(mStrokes.size() - 1);
                    mLineShaderRenderer.bNeedsUpdate.set(true);
                }
            }
            mLineShaderRenderer.setDrawDebug(bLineParameters.get());
            if (mLineShaderRenderer.bNeedsUpdate.get()) {
                draw();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void draw(){
        ArrayList<ArrayList<Vector3f>> tmp=new ArrayList<>();
        for (ArrayList<ArrayList<Vector3f>> user:allStrockes){
            tmp.addAll(user);
        }
        mLineShaderRenderer.setColor(AppSettings.getColor());
        mLineShaderRenderer.mDrawDistance = AppSettings.getStrokeDrawDistance();
        mLineShaderRenderer.setDistanceScale(mDistanceScale);
        mLineShaderRenderer.setLineWidth(mLineWidthMax);
        mLineShaderRenderer.clear();

        mLineShaderRenderer.updateStrokes(tmp);
        mLineShaderRenderer.upload();
    }



    /**
     * GL Thread Loop
     * clears the Color Buffer and Depth Buffer, draws the current texture from the camera
     * and draws the Line Renderer if ARCore is tracking the world around it
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        if (mPaused) return;

        update();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mFrame == null) {
            return;
        }

        // Draw background.
        mBackgroundRenderer.draw(mFrame);

        // Draw Lines
        if (mFrame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            mLineShaderRenderer.draw(viewmtx, projmtx, mScreenWidth, mScreenHeight, AppSettings.getNearClip(), AppSettings.getFarClip());
        }
    }


    /**
     * Get a matrix usable for zero calibration (only position and compass direction)
     */
    public float[] getCalibrationMatrix() {
        float[] t = new float[3];
        float[] m = new float[16];

        mFrame.getCamera().getPose().getTranslation(t, 0);
        float[] z = mFrame.getCamera().getPose().getZAxis();
        Vector3f zAxis = new Vector3f(z[0], z[1], z[2]);
        zAxis.y = 0;
        zAxis.normalize();

        double rotate = Math.atan2(zAxis.x, zAxis.z);

        Matrix.setIdentityM(m, 0);
        Matrix.translateM(m, 0, t[0], t[1], t[2]);
        Matrix.rotateM(m, 0, (float) Math.toDegrees(rotate), 0, 1, 0);
        return m;
    }


    /**
     * Clears the Datacollection of Strokes and sets the Line Renderer to clear and update itself
     * Designed to be executed on the GL Thread
     */
    public void clearDrawing() {
        mStrokes.clear();
        mLineShaderRenderer.clear();
    }


    /**
     * onClickUndo handles the touch input on the GUI and sets the AtomicBoolean bUndo to be true
     * the actual undo functionality is executed in the GL Thread
     */
    public void onClickUndo(View button) {
        bUndo.set(true);
    }

    /**
     * onClickLineDebug toggles the Line Renderer's Debug View on and off. The line renderer will
     * highlight the lines on the same depth plane to allow users to draw things more coherently
     */
    public void onClickLineDebug(View button) {
        bLineParameters.set(!bLineParameters.get());
    }


    /**
     * onClickSettings toggles showing and hiding the Line Width, Smoothing, and Debug View toggle
     */
    public void onClickSettings(View button) {
        ImageButton settingsButton = findViewById(R.id.settingsButton);

        if (mSettingsUI.getVisibility() == View.GONE) {
            mSettingsUI.setVisibility(View.VISIBLE);


            settingsButton.setColorFilter(getResources().getColor(R.color.active));
        } else {
            mSettingsUI.setVisibility(View.GONE);
            settingsButton.setColorFilter(getResources().getColor(R.color.gray));
        }
    }
    public void onClickPatterne(View button) {


        if (pattern.getVisibility() == View.GONE) {
            pattern.setVisibility(View.VISIBLE);

        } else {
            pattern.setVisibility(View.GONE);

        }
    }

    /**
     * onClickClear handle showing an AlertDialog to clear the drawing
     */
    public void onClickClear(View button) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Sure you want to clear?");

        // Set up the buttons
        builder.setPositiveButton("Clear ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bClearDrawing.set(true);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    /**
     * onClickRecenter handles the touch input on the GUI and sets the AtomicBoolean bReCEnterView to be true
     * the actual recenter functionality is executed on the GL Thread
     */
    public void onClickRecenter(View button) {
        bReCenterView.set(true);
    }

    // ------- Touch events

    /**
     * onTouchEvent handles saving the lastTouch screen position and setting bTouchDown and bNewStroke
     * AtomicBooleans to trigger addPoint and addStroke on the GL Thread to be called
     */
    @Override
    public boolean onTouchEvent(MotionEvent tap) {
        this.mDetector.onTouchEvent(tap);

        if (tap.getAction() == MotionEvent.ACTION_DOWN ) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            bNewStroke.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_MOVE || tap.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_UP || tap.getAction() == MotionEvent.ACTION_CANCEL) {
            bTouchDown.set(false);
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            save();
            return true;
        }

        return super.onTouchEvent(tap);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    /**
     * onDoubleTap shows and hides the Button Bar at the Top of the View
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mButtonBar.getVisibility() == View.GONE) {
            mButtonBar.setVisibility(View.VISIBLE);
        } else {
            mButtonBar.setVisibility(View.GONE);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent tap) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void capture(View v){
        DataHolder.getInstance().setData(mStrokes);
        DataHolder.getInstance().setData(projmtx);
        Intent intent = new Intent(this,Capture.class);
        startActivity(intent);

    }

    public void changeColor(View v){
        int id = v.getId();

        switch (id){
            case (R.id.blue):
                AppSettings.setColor(70,180,235);
                break;
            case (R.id.red):
                AppSettings.setColor(214,70,70);
                break;
            case (R.id.orange):
                AppSettings.setColor(235,125,70);
                break;
            case (R.id.green):
                AppSettings.setColor(70,235,125);
                break;
            case (R.id.white):
                AppSettings.setColor(255,255,255);
                break;
            case (R.id.black):
                AppSettings.setColor(33,33,33);
                break;

        }
        mLineShaderRenderer.bNeedsUpdate.set(true);

    }

}
