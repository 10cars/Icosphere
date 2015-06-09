/*
 * Copyright (c) 2015 10cars Software
 */

package com.tencarssoftware.icosphere;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private final float[] lightPosInEyeSpace = new float[4];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private final OnSurfaceCreatedCallback callback;
    private Icosphere sphere;
    private float angleX;
    private float angleY;
    private float color[];

    public MyGLRenderer(OnSurfaceCreatedCallback callback) {
        this.callback = callback;
    }

    public static int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        int[] params = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, params, 0);
        if (params[0] == 0) {
            Log.d("MyGLRenderer", "Shader info log: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Unable to compile shader.");
        }
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -0.5f, 0f, 0f, -10f, 0f, 1f, 0f);
        float[] mLightModelMatrix = new float[16];
        float[] lightPosInModelSpace = new float[]{0f, 0f, 0f, 1f};
        float[] lightPosInWorldSpace = new float[4];
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.multiplyMV(lightPosInWorldSpace, 0, mLightModelMatrix, 0, lightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, viewMatrix, 0, lightPosInWorldSpace, 0);
        callback.onSurfaceCreated();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (sphere != null) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 0f, 0f, -2.5f); // push away a bit
            Matrix.rotateM(modelMatrix, 0, angleX, 0f, 1f, 0f); // apply rotation x
            Matrix.rotateM(modelMatrix, 0, angleY, 1f, 0f, 0f); // apply rotation y
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
            sphere.draw(mvpMatrix, mvMatrix, lightPosInEyeSpace, color);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 15f);
    }

    public void setAngleX(float angleX) {
        this.angleX = angleX % 360f;
    }

    public void setAngleY(float angleY) {
        this.angleY = angleY % 360f;
    }

    public void setColor(float[] color) {
        this.color = color;
    }

    public Icosphere getSphere() {
        return sphere;
    }

    public void setSphere(Icosphere sphere) {
        this.sphere = sphere;
    }

}
