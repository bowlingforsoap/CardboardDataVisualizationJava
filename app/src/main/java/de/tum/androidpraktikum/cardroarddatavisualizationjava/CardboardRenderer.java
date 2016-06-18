package de.tum.androidpraktikum.cardroarddatavisualizationjava;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;

import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.AgingVessel;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Brewkettle;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.BrightBeerVessel;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Unit;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.shaders.ShaderRetriever;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class CardboardRenderer implements GvrView.StereoRenderer {
    private final Context appContext;

    private Unit[] modelData = new Unit[6];

    {
        for (int i = 0; i < 6; i++) {
            modelData[i] = new Unit();
        }
    }

    private static final String TAG = "CardboardRenderer";

    private final float NEAR = 1.0f;
    private float FAR = 10.0f;

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private float[] mProjectionMatrix = new float[16];

    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private float[] mMVPMatrix = new float[16];

    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private float[] mLightModelMatrix = new float[16];

    /**
     * Store our model data in a float buffer.
     */
    private final FloatBuffer mAgingVesselPositions;
    private final FloatBuffer mAgingVesselNormals;

    private final FloatBuffer mBrightBeerVesselPositions;
    private final FloatBuffer mBrightBeerVesselNormals;

    private final FloatBuffer mBrewkettlePositions;
    private final FloatBuffer mBrewkettleNormals;

    /**
     * This will be used to pass in the transformation matrix.
     */
    private int mMVPMatrixHandle;

    /**
     * This will be used to pass in the modelview matrix.
     */
    private int mMVMatrixHandle;

    /**
     * This will be used to pass in the light position.
     */
    private int mLightPosHandle;

    /**
     * This will be used to pass in the (i.e. {@link AgingVessel}.)HIGHEST vertex.
     */
    private int mHighestYHandle;

    /**
     * This will be used to pass in the (i.e. {@link AgingVessel}.)LOWEST vertex.
     */
    private int mLowestYHandle;

    /**
     * This will be used to pass in the fill level of the {@link Unit}.
     */
    private int mFillLevel;

    /**
     * This will be used to pass in model position information.
     */
    private int mPositionHandle;

    /**
     * This will be used to pass in model color information.
     */
    private int mColorHandle;

    /**
     * This will be used to pass in model normal information.
     */
    private int mNormalHandle;

    /**
     * How many bytes per float.
     */
    private final int mBytesPerFloat = 4;

    /**
     * Size of the position data in elements.
     */
    private final int mPositionDataSize = 3;

    /**
     * Size of the color data in elements.
     */
    private final int mColorDataSize = 4;

    /**
     * Size of the normal data in elements.
     */
    private final int mNormalDataSize = 3;

    /**
     * Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     * we multiply this by our transformation matrices.
     */
    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    /**
     * Used to hold the current position of the light in world space (after transformation via model matrix).
     */
    private final float[] mLightPosInWorldSpace = new float[4];

    /**
     * Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)
     */
    private final float[] mLightPosInEyeSpace = new float[4];

    /**
     * This is a handle to our per-vertex cube shading program.
     */
    private int mPerVertexProgramHandle;

    /**
     * This is a handle to our light point program.
     */
    private int mPointProgramHandle;

    /**
     * Initialize the model data.
     */
    public CardboardRenderer(Context appContext) {
        // Store the application context for shader retrieval.
        this.appContext = appContext;

        // Initialize the aging vessel buffers.
        mAgingVesselPositions = ByteBuffer.allocateDirect(AgingVessel.POSITIONS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mAgingVesselPositions.put(AgingVessel.POSITIONS).position(0);

        mAgingVesselNormals = ByteBuffer.allocateDirect(AgingVessel.NORMALS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mAgingVesselNormals.put(AgingVessel.NORMALS).position(0);

        // Initialize the brewkettle buffers.
        mBrewkettlePositions = ByteBuffer.allocateDirect(Brewkettle.POSITIONS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBrewkettlePositions.put(Brewkettle.POSITIONS).position(0);

        mBrewkettleNormals = ByteBuffer.allocateDirect(Brewkettle.NORMALS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBrewkettleNormals.put(Brewkettle.NORMALS).position(0);

        // Initialize the bright beer vessel buffers.
        mBrightBeerVesselPositions = ByteBuffer.allocateDirect(BrightBeerVessel.POSITIONS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBrightBeerVesselPositions.put(BrightBeerVessel.POSITIONS).position(0);

        mBrightBeerVesselNormals = ByteBuffer.allocateDirect(BrightBeerVessel.NORMALS.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBrightBeerVesselNormals.put(BrightBeerVessel.NORMALS).position(0);
    }

    /*@Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to black.
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.VERTEX_SHADER);
        final String fragmentShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.FRAGMENT_SHADER);

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_Normal"});

        // Define a simple shader program for our point.
        final String pointVertexShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.LIGHT_VERTEX_SHADER);
        final String pointFragmentShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.LIGHT_FRAGMENT_SHADER);

        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[] {"a_Position"});
    }*/

    /*@Override
    // is done in onDrawEye?
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float NEAR = 1.0f;
        final float FAR = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, NEAR, FAR);
    }*/

    /*@Override
    public void onDrawFrame(GL10 glUnused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");

        // Lights
        //

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 1.2f, -5.0f);
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        // in onDrawEye
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // Draw some cubes.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        // in onDrawEye
        drawModel(eye, mAgingVesselPositions, mAgingVesselNormals, AgingVessel.VERTICES, new float[] {1.0f, 0.0f, 0.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 30, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        // in onDrawEye
        drawModel(eye, mBrightBeerVesselPositions, mBrightBeerVesselNormals, BrightBeerVessel.VERTICES, new float[] {0.0f, 1.0f, 0.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, -30, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        // in onDrawEye
        drawModel(eye, mBrewkettlePositions, mBrewkettleNormals, Brewkettle.VERTICES, new float[] {0.0f, 0.0f, 1.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 60, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        // in onDrawEye
        drawModel(eye, mAgingVesselPositions, mAgingVesselNormals, AgingVessel.VERTICES, new float[] {1.0f, 0.0f, 0.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, -60, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        // in onDrawEye
        drawModel(eye, mBrightBeerVesselPositions, mBrightBeerVesselNormals, BrightBeerVessel.VERTICES, new float[] {0.0f, 1.0f, 0.0f, 1.0f});

        // in onDrawEye
//      Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }*/

    /**
     * Draws a model given by it's {@code positions}, {@code normals} FloatBuffer-s and a float array containing the color.
     * Preserves the {@code lowestY} and {@code highestY} values.
     * @param mEyeViewMatrix
     * @param mEyeProjectionMatrix
     * @param positions
     * @param normals
     * @param highestY
     * @param lowestY
     * @param fillLevel [0; 1]
     * @param numVertices
     * @param color
     */
    private void drawModel(float[] mEyeViewMatrix, float[] mEyeProjectionMatrix, FloatBuffer positions, FloatBuffer normals, float highestY, float lowestY, float fillLevel, int numVertices, float[] color) {
        // Check the given color array
        if (color == null || color.length != 4) {
            throw new RuntimeException("Bad color array format! Expecting 4 values..");
        }

        // Pass attributes and stuff to the shader program
        //
        // Pass in the position information
        positions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, positions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        GLES20.glVertexAttrib4f(mColorHandle, color[0], color[1], color[2], color[3]);
        GLES20.glDisableVertexAttribArray(mColorHandle);

        // Pass in the normal information
        normals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, normals);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // Projection matrix construction
        //
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mEyeViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mEyeProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Pass in the highest and the lowest vertices

        GLES20.glUniform1f(mHighestYHandle, highestY);
        GLES20.glUniform1f(mLowestYHandle, lowestY);
        GLES20.glUniform1f(mFillLevel, fillLevel);

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);
    }

    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight(float[] mEyeViewMatrix, float[] mEyeProjectionMatrix) {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);
        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mEyeViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mEyeProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType   The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    private int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle   An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes           Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    /**
     * Update current {@code modelData} with the new {@code modelData}.
     *
     * @param newModelData
     */
    public void updateModelData(Unit[] newModelData) {
        if (newModelData != null && newModelData.length != 6) {
            throw new RuntimeException("Bad model data format! Expecting 6 values..");
        }

        modelData = newModelData;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mHighestYHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_HighestY");
        mLowestYHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LowestY");
        mFillLevel = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_FillLevel");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 1.2f, -5.0f);
        //Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);

        // Draw some cubes.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);

        /*Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 30, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        // in onDrawEye
        drawModel(mBrightBeerVesselPositions, mBrightBeerVesselNormals, BrightBeerVessel.VERTICES, new float[] {0.0f, 1.0f, 0.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, -30, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        // in onDrawEye
        drawModel(mBrewkettlePositions, mBrewkettleNormals, Brewkettle.VERTICES, new float[] {0.0f, 0.0f, 1.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 60, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        // in onDrawEye
        drawModel(mAgingVesselPositions, mAgingVesselNormals, AgingVessel.VERTICES, new float[] {1.0f, 0.0f, 0.0f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, -60, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
        // in onDrawEye
        drawModel(mBrightBeerVesselPositions, mBrightBeerVesselNormals, BrightBeerVessel.VERTICES, new float[] {0.0f, 1.0f, 0.0f, 1.0f});
*/

    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Get eye matrices from the Eye object
        float[] mEyeViewMatrix = eye.getEyeView();
        float[] mEyeProjectionMatrix = eye.getPerspective(NEAR, FAR);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mEyeViewMatrix, 0, mEyeViewMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mEyeViewMatrix, 0, mLightPosInWorldSpace, 0);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);
        // TODO: change fillLevel to a value from a real Unit
        drawModel(mEyeViewMatrix, mEyeProjectionMatrix, mAgingVesselPositions, mAgingVesselNormals, AgingVessel.HIGHEST[1], AgingVessel.LOWEST[1], 0.5f, AgingVessel.VERTICES, new float[]{1.0f, 0.0f, 0.0f, 1.0f});

        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight(mEyeViewMatrix, mEyeProjectionMatrix);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {
        Log.i(TAG, "onSurfaceChanged");
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
        // Set the background clear color to black.
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);


        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.VERTEX_SHADER);
        final String fragmentShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.FRAGMENT_SHADER);

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_Normal"});

        // Define a simple shader program for our point.
        final String pointVertexShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.LIGHT_VERTEX_SHADER);
        final String pointFragmentShader = ShaderRetriever.getShaderCode(appContext, ShaderRetriever.LIGHT_FRAGMENT_SHADER);

        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[]{"a_Position"});
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }
}
