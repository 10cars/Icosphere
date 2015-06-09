/*
 * Copyright (c) 2015 10cars Software
 */

package com.tencarssoftware.icosphere;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.support.v4.util.LongSparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Icosphere {
    public static final int[] sizeList = new int[]{12, 42, 162, 642, 2562, 10242};
    private static final String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uMVMatrix;" +
                    "attribute vec4 aPosition;" +
                    "varying vec3 vPosition;" +
                    "varying vec3 vNormal;" +
                    "void main() {" +
                    "    vPosition = vec3(uMVMatrix * aPosition);" +
                    "    vNormal = vec3(uMVMatrix * vec4(aPosition.xyz, 0.0));" +
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
    private static final float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
    private static final short initialDrawList[] = {
            0, 11, 5,
            0, 5, 1,
            0, 1, 7,
            0, 7, 10,
            0, 10, 11,

            1, 5, 9,
            5, 11, 4,
            11, 10, 2,
            10, 7, 6,
            7, 1, 8,

            3, 9, 4,
            3, 4, 2,
            3, 2, 6,
            3, 6, 8,
            3, 8, 9,

            4, 9, 5,
            2, 4, 11,
            6, 2, 10,
            8, 6, 7,
            9, 8, 1
    };
    private final int program;
    private final boolean useVBOs;
    protected float[] vertices;
    protected short[] drawList;

    private int positionHandle;
    private int colorHandle;
    private int mvMatrixHandle;
    private int mvpMatrixHandle;
    private int lightPositionHandle;

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private int sphereVertexBufferHandle;
    private int sphereDrawListBufferHandle;
    private int sphereDrawListLength;

    public Icosphere(int refinementCount) {
        useVBOs = canUseVBOs();
        int vertexShader = getVertexShader();
        int fragmentShader = getFragmentShader();
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        setupHandles(program);
        if (useVBOs) {
            setupBuffers();
        }
        recreate(refinementCount);
    }

    public void recreate(int refinementCount) {
        createModel(refinementCount);
        fillBuffers();
        if (useVBOs) {
            bindBuffers();
        }
    }

    public void draw(float[] mvpMatrix, float[] mvMatrix, float[] lightPosInEyeSpace, float[] color) {
        GLES20.glUseProgram(getProgram());
        GLES20.glEnableVertexAttribArray(positionHandle);

        if (useVBOs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVertexBufferHandle);
            glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        } else {
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        }

        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glUniform3f(lightPositionHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        if (useVBOs) {
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sphereDrawListBufferHandle);
            glDrawElements(GLES20.GL_TRIANGLES, sphereDrawListLength, GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereDrawListLength, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        }
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void addVertex(float x, float y, float z, int i) {
        float length = Matrix.length(x, y, z);
        vertices[i * 3] = x / length;
        vertices[i * 3 + 1] = y / length;
        vertices[i * 3 + 2] = z / length;
    }

    private short initialize() {
        short vCount = 0;
        addVertex(0f, 1f, 1f / t, vCount++);
        addVertex(0f, 1f, -1f / t, vCount++);
        addVertex(0f, -1f, 1f / t, vCount++);
        addVertex(0f, -1f, -1f / t, vCount++);

        addVertex(1f, -1f / t, 0f, vCount++);
        addVertex(1f, 1f / t, 0f, vCount++);
        addVertex(-1f, -1f / t, 0f, vCount++);
        addVertex(-1f, 1f / t, 0f, vCount++);

        addVertex(-1f / t, 0f, -1f, vCount++);
        addVertex(1f / t, 0f, -1f, vCount++);
        addVertex(-1f / t, 0f, 1f, vCount++);
        addVertex(1f / t, 0f, 1f, vCount++);
        return vCount;
    }

    private short findMidPoint(short v1, short v2, short vCount, LongSparseArray<Short> vertexCache) {
        float x = (vertices[v1 * 3] + vertices[v2 * 3]) / 2f;
        float y = (vertices[v1 * 3 + 1] + vertices[v2 * 3 + 1]) / 2f;
        float z = (vertices[v1 * 3 + 2] + vertices[v2 * 3 + 2]) / 2f;
        short tmp;
        if (v1 > v2) {
            tmp = v1;
            v1 = v2;
            v2 = tmp;
        }
        Short index = vertexCache.get((long) v1 << 32 | (long) v2);
        if (index == null) {
            addVertex(x, y, z, vCount);
            vertexCache.put((long) v1 << 32 | (long) v2, vCount);
            index = vCount;
        }
        return index;
    }

    private void refine(short vCount, int refinementCount) {
        int length = drawList.length;
        int index;

        LongSparseArray<Short> vertexCache = new LongSparseArray<>();
        for (int k = 0; k < refinementCount; k++) {
            short newDrawList[] = new short[(20 * (int) Math.pow(4, k + 1)) * 3];
            index = 0;
            for (int faceCount = 0; faceCount < length / 3; faceCount++) {
                short v1 = drawList[faceCount * 3];
                short v2 = drawList[faceCount * 3 + 1];
                short v3 = drawList[faceCount * 3 + 2];

                short a = findMidPoint(v1, v2, vCount, vertexCache);
                if (a == vCount)
                    vCount++;
                short b = findMidPoint(v2, v3, vCount, vertexCache);
                if (b == vCount)
                    vCount++;
                short c = findMidPoint(v3, v1, vCount, vertexCache);
                if (c == vCount)
                    vCount++;

                newDrawList[index++] = v1;
                newDrawList[index++] = a;
                newDrawList[index++] = c;

                newDrawList[index++] = c;
                newDrawList[index++] = b;
                newDrawList[index++] = v3;

                newDrawList[index++] = a;
                newDrawList[index++] = v2;
                newDrawList[index++] = b;

                newDrawList[index++] = a;
                newDrawList[index++] = b;
                newDrawList[index++] = c;
            }

            drawList = newDrawList;
            length = index;
        }
    }

    protected int getVertexShader() {
        return MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    }

    protected int getFragmentShader() {
        return MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
    }

    protected void setupHandles(int program) {
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        lightPositionHandle = GLES20.glGetUniformLocation(program, "uLightPosition");
    }

    protected void setupBuffers() {
        final int buffers[] = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        sphereVertexBufferHandle = buffers[0];
        sphereDrawListBufferHandle = buffers[1];
    }

    private void createModel(int refinementCount) {
        drawList = initialDrawList.clone();
        vertices = new float[sizeList[refinementCount] * 3];
        short count = initialize();
        refine(count, refinementCount);
    }

    protected void fillBuffers() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        vertices = null;

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawList.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawList);
        drawListBuffer.position(0);
        drawList = null;

        sphereDrawListLength = drawListBuffer.capacity();
    }

    protected void bindBuffers() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVertexBufferHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sphereDrawListBufferHandle);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, drawListBuffer.capacity() * 2, drawListBuffer, GLES20.GL_STATIC_DRAW);
        vertexBuffer.limit(0);
        vertexBuffer = null;
        drawListBuffer.limit(0);
        drawListBuffer = null;
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    protected int getProgram() {
        return program;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void glDrawElements(int mode, int count, int type, int offset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            GLES20.glDrawElements(mode, count, type, offset);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    protected void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int offset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, offset);
        }
    }

    protected boolean useVBOs() {
        return useVBOs;
    }

    private boolean canUseVBOs() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

}
