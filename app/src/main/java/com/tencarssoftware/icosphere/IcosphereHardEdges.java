/*
 * Copyright (c) 2015 10cars Software
 */

package com.tencarssoftware.icosphere;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class IcosphereHardEdges extends Icosphere {
    private static final String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uMVMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec3 aNormal;" +
                    "varying vec3 vPosition;" +
                    "varying vec3 vNormal;" +
                    "void main() {" +
                    "    vPosition = vec3(uMVMatrix * aPosition);" +
                    "    vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));" +
                    "    gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private static final String fragmentShaderCode =
                    "precision mediump float;" +
                    "uniform vec3 uLightPosition;" +
                    "uniform vec4 uColor;" +
                    "varying vec3 vPosition;" +
                    "varying vec3 vNormal;" +
                    "void main() {" +
                    "    float distance = length(uLightPosition - vPosition);" +
                    "    vec3 lightVector = normalize(uLightPosition - vPosition);" +
                    "    float diffuse = max(dot(vNormal, lightVector), 0.1);" +
                    "    diffuse = diffuse * (1.0 / (1.0 + (0.25 * (distance - 1.0) * (distance - 1.0))));" +
                    "    gl_FragColor = uColor * diffuse;" +
                    "}";


    private int positionHandle;
    private int normalHandle;
    private int colorHandle;
    private int mvMatrixHandle;
    private int mvpMatrixHandle;
    private int lightPositionHandle;

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private int sphereVertexBufferHandle;
    private int sphereNormalsBufferHandle;
    private int vertexBufferCapacity;

    public IcosphereHardEdges(int refinementCount) {
        super(refinementCount);
    }

    @Override
    public void draw(float[] mvpMatrix, float[] mvMatrix, float[] lightPosInEyeSpace, float[] color) {
        GLES20.glUseProgram(getProgram());
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(normalHandle);
        if (useVBOs()) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVertexBufferHandle);
            glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereNormalsBufferHandle);
            glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        } else {
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);
        }
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glUniform3f(lightPositionHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexBufferCapacity / 3);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
    }

    @Override
    protected int getVertexShader() {
        return MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    }

    @Override
    protected int getFragmentShader() {
        return MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
    }

    @Override
    protected void setupHandles(int program) {
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        lightPositionHandle = GLES20.glGetUniformLocation(program, "uLightPosition");
    }

    @Override
    protected void setupBuffers() {
        final int buffers[] = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        sphereVertexBufferHandle = buffers[0];
        sphereNormalsBufferHandle = buffers[1];
    }

    private float[] getVertex(short index) {
        float[] v = new float[3];
        v[0] = vertices[index * 3];
        v[1] = vertices[index * 3 + 1];
        v[2] = vertices[index * 3 + 2];
        return v;
    }

    private float[] createNormal(float[] v1, float[] v2, float[] v3) {
        float[] vU = {v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};
        float[] vV = {v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2]};

        float[] normal = {
                (vU[1] * vV[2]) - (vU[2] * vV[1]),
                (vU[2] * vV[0]) - (vU[0] * vV[2]),
                (vU[0] * vV[1]) - (vU[1] * vV[0])};

        // normalise the normal
        final float length = Matrix.length(normal[0], normal[1], normal[2]);

        normal[0] /= length;
        normal[1] /= length;
        normal[2] /= length;
        return normal;
    }

    @Override
    protected void fillBuffers() {
        vertexBuffer = ByteBuffer.allocateDirect(drawList.length * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalBuffer = ByteBuffer.allocateDirect(drawList.length * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        int i = 0;
        while (i < drawList.length) {
            float[] v1 = getVertex(drawList[i++]);
            vertexBuffer.put(v1);
            float[] v2 = getVertex(drawList[i++]);
            vertexBuffer.put(v2);
            float[] v3 = getVertex(drawList[i++]);
            vertexBuffer.put(v3);
            float[] normal = createNormal(v1, v2, v3);
            normalBuffer.put(normal);
            normalBuffer.put(normal);
            normalBuffer.put(normal);
        }
        vertexBuffer.position(0);
        vertices = null;
        normalBuffer.position(0);
        drawList = null;
        // save the capacity for drawing, if using VBOs we'll get rid of the actual buffer later
        vertexBufferCapacity = vertexBuffer.capacity();
    }

    @Override
    protected void bindBuffers() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVertexBufferHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereNormalsBufferHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES20.GL_STATIC_DRAW);
        vertexBuffer.limit(0);
        vertexBuffer = null;
        normalBuffer.limit(0);
        normalBuffer = null;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

}
