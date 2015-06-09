/*
 * Copyright (c) 2015 10cars Software
 */

package com.tencarssoftware.icosphere;


import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment implements OnSurfaceCreatedCallback {
    private static final float TOUCH_SCALE_FACTOR = 180f / 320f;
    private static final int MAX_REFINEMENT = 5;

    private GLSurfaceView mGLView;
    private MyGLRenderer renderer;
    private GestureDetectorCompat gestureDetector;
    private MenuItem menuDecrease;
    private MenuItem menuIncrease;
    private MenuItem menuSoftEdges;
    private MenuItem menuHardEdges;
    private MenuItem menuEnableX;
    private MenuItem menuDisableX;
    private MenuItem menuEnableY;
    private MenuItem menuDisableY;
    private TextView info;
    private Toolbar toolbar;
    private int refinementLevel;
    private boolean hardEdges;
    private boolean enableX;
    private boolean enableY;
    private float angleX;
    private float angleY;

    @Override
    public void onSurfaceCreated() {
        createSphere();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mGLView = (GLSurfaceView) rootView.findViewById(R.id.glview);
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        info = (TextView) rootView.findViewById(R.id.info);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            refinementLevel = savedInstanceState.getInt("refinementLevel");
            hardEdges = savedInstanceState.getBoolean("hardEdges");
            enableX = savedInstanceState.getBoolean("enableX");
            enableY = savedInstanceState.getBoolean("enableY");
            angleX = savedInstanceState.getFloat("angleX");
            angleY = savedInstanceState.getFloat("angleY");
        } else {
            refinementLevel = 0;
            hardEdges = true;
            enableX = enableY = true;
            angleX = angleY = 0f;
        }
        gestureDetector = new GestureDetectorCompat(getActivity(), new MyGestureListener());
        mGLView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        mGLView.setEGLContextClientVersion(2);
        renderer = new MyGLRenderer(this);
        renderer.setColor(convertColor(R.color.android_blue));
        renderer.setAngleX(angleX);
        renderer.setAngleY(angleY);

        mGLView.setRenderer(renderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        setupToolbar();
        updateInfo();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("refinementLevel", refinementLevel);
        outState.putBoolean("hardEdges", hardEdges);
        outState.putBoolean("enableX", enableX);
        outState.putBoolean("enableY", enableY);
        outState.putFloat("angleX", angleX);
        outState.putFloat("angleY", angleY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    private float[] convertColor(int id) {
        float[] color = new float[4];
        int c = getResources().getColor(id);
        color[0] = Color.red(c) / 255.0f;
        color[1] = Color.green(c) / 255.0f;
        color[2] = Color.blue(c) / 255.0f;
        color[3] = Color.alpha(c) / 255.0f;
        return color;
    }

    private void updateInfo() {
        StringBuilder sb = new StringBuilder();
        if (refinementLevel == 0)
            sb.append(getString(R.string.shape_icosahedron));
        else
            sb.append(getString(R.string.shape_icosphere));
        sb.append('\n');
        sb.append(getString(R.string.shape_vertex_count));
        sb.append(Icosphere.sizeList[refinementLevel]);
        sb.append('\n');
        if (hardEdges)
            sb.append(getString(R.string.shape_hard_edges));
        else
            sb.append(getString(R.string.shape_soft_edges));
        info.setText(sb);
    }

    private void setupMenuItems() {
        if (refinementLevel <= 0)
            disableMenuItem(menuDecrease);
        else
            enableMenuItem(menuDecrease);
        if (refinementLevel >= MAX_REFINEMENT)
            disableMenuItem(menuIncrease);
        else
            enableMenuItem(menuIncrease);
        if (hardEdges) {
            menuHardEdges.setVisible(false);
            menuSoftEdges.setVisible(true);
        } else {
            menuHardEdges.setVisible(true);
            menuSoftEdges.setVisible(false);
        }
        if (enableX) {
            menuEnableX.setVisible(false);
            menuDisableX.setVisible(true);
        } else {
            menuEnableX.setVisible(true);
            menuDisableX.setVisible(false);
        }
        if (enableY) {
            menuEnableY.setVisible(false);
            menuDisableY.setVisible(true);
        } else {
            menuEnableY.setVisible(true);
            menuDisableY.setVisible(false);
        }
    }

    private void setupToolbar() {
        toolbar.inflateMenu(R.menu.toolbar);
        Menu menu = toolbar.getMenu();
        menuDecrease = menu.findItem(R.id.action_decrease);
        menuIncrease = menu.findItem(R.id.action_increase);
        menuSoftEdges = menu.findItem(R.id.action_soft_edges);
        menuHardEdges = menu.findItem(R.id.action_hard_edges);
        menuEnableX = menu.findItem(R.id.action_enable_x);
        menuDisableX = menu.findItem(R.id.action_disable_x);
        menuEnableY = menu.findItem(R.id.action_enable_y);
        menuDisableY = menu.findItem(R.id.action_disable_y);
        setupMenuItems();
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_decrease:
                        decreaseRefinement();
                        break;
                    case R.id.action_increase:
                        increaseRefinement();
                        break;
                    case R.id.action_soft_edges:
                        setHardEdges(false);
                        break;
                    case R.id.action_hard_edges:
                        setHardEdges(true);
                        break;
                    case R.id.action_center:
                        center();
                        break;
                    case R.id.action_enable_x:
                        enableX = true;
                        break;
                    case R.id.action_disable_x:
                        enableX = false;
                        break;
                    case R.id.action_enable_y:
                        enableY = true;
                        break;
                    case R.id.action_disable_y:
                        enableY = false;
                        break;
                }
                updateInfo();
                setupMenuItems();
                return true;
            }
        });
    }

    private void createSphere() {
        if (hardEdges) {
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.setSphere(new IcosphereHardEdges(refinementLevel));
                    mGLView.requestRender();
                }
            });
        } else {
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.setSphere(new Icosphere(refinementLevel));
                    mGLView.requestRender();
                }
            });
        }
    }

    private void setHardEdges(boolean flag) {
        if (hardEdges != flag) {
            hardEdges = flag;
            createSphere();
        }
    }

    private void center() {
        angleX = 0f;
        angleY = 0f;
        renderer.setAngleX(angleX);
        renderer.setAngleY(angleY);
        mGLView.requestRender();
    }

    private void decreaseRefinement() {
        if (refinementLevel > 0) {
            refinementLevel--;
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.getSphere().recreate(refinementLevel);
                    mGLView.requestRender();
                }
            });
        }
    }

    private void increaseRefinement() {
        if (refinementLevel < MAX_REFINEMENT) {
            refinementLevel++;
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.getSphere().recreate(refinementLevel);
                    mGLView.requestRender();
                }
            });
        }
    }

    private void enableMenuItem(MenuItem item) {
        item.setEnabled(true);
        item.getIcon().setAlpha(255);
    }

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.getIcon().setAlpha(77);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (enableX) {
                angleX = angleX - (distanceX * TOUCH_SCALE_FACTOR);
                renderer.setAngleX(angleX);
                mGLView.requestRender();
            }
            if (enableY) {
                angleY = angleY - (distanceY * TOUCH_SCALE_FACTOR);
                renderer.setAngleY(angleY);
                mGLView.requestRender();
            }
            return true;
        }
    }

}
