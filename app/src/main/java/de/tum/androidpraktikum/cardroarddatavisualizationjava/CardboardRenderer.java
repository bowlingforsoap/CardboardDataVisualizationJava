package de.tum.androidpraktikum.cardroarddatavisualizationjava;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Model;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Skybox;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Unit;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.shaders.SkyboxShaderProgram;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class CardboardRenderer implements GvrView.StereoRenderer {
    private static final String TAG = "CardboardRenderer";
    /**
     * Determines the number of interpolated colors (for the fill level) to be shown between the two consequently fetched colors.
     */
    public static final int COLOR_STEPS_PER_INTERVAL = 10;
    /**
     * The current step in interpolating the color [1, COLOR_STEPS_PER_INTERVAL].
     */
    private int[] stepNum = new int[NUM_OF_UNITS];
    /**
     * The new color retrieved per model.
     */
    private float[][] newColor = new float[NUM_OF_UNITS][4];

    /**
     * The color before the latest retrieval per model.
     */
    private float[][] currColor = new float[NUM_OF_UNITS][4];
    private final MainActivity mainActivity;

    /**
     * Shows the model data.
     */
    private Toast toast;

    /**
     * Number of brewery units to render. Does not necessarily corresponds to the number of the 3D breweryModels.
     */
    public static final int NUM_OF_UNITS = 6;

    /**
     * Storage for brewery model data.
     */
    private Unit[] breweryModelsData = new Unit[NUM_OF_UNITS];

    {
        for (int i = 0; i < NUM_OF_UNITS; i++) {
            breweryModelsData[i] = new Unit();
            stepNum[i] = 1;
        }
    }

    /**
     * Storage for brewery breweryModels.
     */
    private Model[] breweryModels = new Model[3];
    /**
     * Storage for the floor model.
     */
    private Model floorModel;
    private Skybox skybox;


    // Center of screen to be re-projected in model space.
    private float[] center = new float[4];

    /**
     * Near clipping plane.
     */
    private final float NEAR = 1.0f;
    /**
     * Far clipping plane.
     */
    private float FAR = 40.0f;

    /**
     * Store the model matrix. This matrix is used to move breweryModels from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[][] modelMatrix = new float[NUM_OF_UNITS][16];
    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];
    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private float[] mvpMatrix = new float[16];
    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private float[] mLightModelMatrix = new float[16];

    //Store our model data in a float buffer.
    //TODO: add description
    private final FloatBuffer[] breweryModelsBuffers = new FloatBuffer[9];
    private final FloatBuffer[] floorModelBuffers = new FloatBuffer[3];

    /**
     * This will be used to pass in the transformation matrix.
     */
    private int mvpMatrixHandle;
    /**
     * This will be used to pass in the modelview matrix.
     */
    private int mvMatrixHandle;

    /**
     * Handle to the texture data.
     */
    private int floorTilesTextureDataHandle;
    /**
     * Handle to the heightmap texture data.
     */
    private int floorTilesHeightmapDataHandle;
    /**
     * Handle to the texture data of brewery breweryModels.
     */
    private int breweryModelsTextureDataHandle;
    /**
     * This will be used to pass in the texture itself.
     */
    private int textureUniformHandle;

    private SkyboxShaderProgram skyboxShaderProgram;
    private int cubemapTextureDataHandle;
    // Uniforms.
    private int uMatrixLocation;
    private int uTextureUnitLocation;
    // Atributes.
    private int aPostionLocation;

    /**
     * This will be used to pass in the light position.
     */
    private int lightUniformPosHandle;
    /**
     * This will be used to pass in the highest Y coordinate of the model.
     */
    private int highestYUniformHandle;
    /**
     * This will be used to pass in the the lowest Y coordinate of the model.
     */
    private int lowestYUniformHandle;

    /**
     * This will be used to pass in the fill level of the {@link Unit}.
     */
    private int fillLevel;

    /**
     * This will be used to pass in texture per-vertex coordinates.
     */
    private int texCoordinateHandle;
    /**
     * This will be used to pass in model position information.
     */
    private int positionHandle;
    /**
     * This will be used to pass in model color information.
     */
    private int colorHandle;
    /**
     * This will be used to pass in model normal information.
     */
    private int normalHandle;

    /**
     * How many bytes per float.
     */
    public static final int BYTES_PER_FLOAT = 4;

    /**
     * Size of the position data in elements.
     */
    private final int positionDataSize = 3;
    /**
     * Size of the color data in elements.
     */
    private final int mColorDataSize = 4;
    /**
     * Size of the normal data in elements.
     */
    private final int normalDataSize = 3;
    /**
     * Size of the texel data in elements.
     */
    private final int textureDataSize = 2;

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
    private int perVertexProgramHandle;
    /**
     * This is a handle to our light point program.
     */
    private int pointProgramHandle;
    /**
     * This is a handle to our skybox program.
     */
    private int skyboxProgramHandle;

    /**
     * Initialize the model data.
     */
    public CardboardRenderer(final MainActivity mainActivity) {
        // Store the application context for shader retrieval.
        this.mainActivity = mainActivity;
        // The toast to show model data.
        toast = Toast.makeText(this.mainActivity, "", Toast.LENGTH_SHORT);

        // Initialize 3D breweryModels.
        // Brewery breweryModels.
        breweryModels[0] = new Model(mainActivity, R.raw.aging_vessel);
        breweryModels[1] = new Model(mainActivity, R.raw.brewkettle);
        breweryModels[2] = new Model(mainActivity, R.raw.bright_beer_vessel);
        // Floor.
        floorModel = new Model(mainActivity, R.raw.floor);
        // Skybox.
        skybox = new Skybox();

        // Fill brewery model's buffers.
        fillBuffers(breweryModelsBuffers, breweryModels);

        // Fill the floor model's buffers.
        fillBuffers(floorModelBuffers, floorModel);
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

        // We are looking toward the distance.
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

        final String vertexShader = AssetLoader.loadShader(mainActivity, AssetLoader.VERTEX_SHADER);
        final String fragmentShader = AssetLoader.loadShader(mainActivity, AssetLoader.FRAGMENT_SHADER);
        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        perVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_Normal", "a_TexCoordinate"});

        // Define a simple shader program for our point.
        final String pointVertexShader = AssetLoader.loadShader(mainActivity, AssetLoader.LIGHT_VERTEX_SHADER);
        final String pointFragmentShader = AssetLoader.loadShader(mainActivity, AssetLoader.LIGHT_FRAGMENT_SHADER);
        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);

        pointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[]{"a_Position"});

        // Skybox shader.
        /*final String skyboxVertexShader = AssetLoader.loadShader(mainActivity, AssetLoader.SKYBOX_VERTEX_SHADER);
        final String skyboxFragmentShader = AssetLoader.loadShader(mainActivity, AssetLoader.SKYBOX_FRAGMENT_SHADER);
        final int skyboxVertexShaderHandle =compileShader(GLES20.GL_VERTEX_SHADER, skyboxVertexShader);
        final int skyboxFragmentShaderHandle =compileShader(GLES20.GL_FRAGMENT_SHADER, skyboxFragmentShader);

        skyboxProgramHandle = createAndLinkProgram(skyboxVertexShaderHandle, skyboxFragmentShaderHandle, new String[] { "a_Position" });*/
        skyboxShaderProgram = new SkyboxShaderProgram(mainActivity);
        skybox = new Skybox();

        // Load handles for the texture bitmaps.
        int[] textureDataHandles = AssetLoader.loadTextures(mainActivity, R.drawable.floor_tiles_texture, R.drawable.floor_tiles_heightmap, R.drawable.brewery_models_texture);
        floorTilesTextureDataHandle = textureDataHandles[0];
        floorTilesHeightmapDataHandle = textureDataHandles[1];
        breweryModelsTextureDataHandle = textureDataHandles[2];

        // Load a handle for the skybox cubemap.
        cubemapTextureDataHandle = AssetLoader.loadCubeMap(mainActivity, R.drawable.sky_afternoon_left, R.drawable.sky_afternoon_right, R.drawable.sky_afternoon_bottom, R.drawable.sky_afternoon_top, R.drawable.sky_afternoon_front
                , R.drawable.sky_afternoon_back);
    }

    @Override
    /**
     * i - height
     * i1 - width
     */
    public void onSurfaceChanged(int i, int i1) {
        Log.i(TAG, "onSurfaceChanged: i = " + i + "; i1 = " + i1);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        /*
        // Determine the model user is looking at.
        //

        // TODO: maybe, move in a separate method.
        // Gonna be used to determine the angle.
        float[] vecStraight = {0.f, 0.f, 1.f, 1.f};
        // Used to determined where the user is looking.
        float[] center = new float[]{0.f, 0.f, 0.f, 1.f};
        float[] centerInHeadViewSpace = new float[4];
        // TODO: play with mViewMatrix, maybe add a global camera matrix and re-init here
        float[] headViewMatrix = new float[16];
        Matrix.multiplyMM(headViewMatrix, 0, headTransform.getHeadView(), 0, mViewMatrix, 0);
        Matrix.multiplyMV(centerInHeadViewSpace, 0, headViewMatrix, 0, center, 0);

        // Normalize the Z component of the center of the view.
        // Take Y component as 0 to project on ZX plane.
        float lengthCenterInHeadViewSpace = Matrix.length(centerInHeadViewSpace[0], centerInHeadViewSpace[1] * 0, centerInHeadViewSpace[2]);
        centerInHeadViewSpace[2] = centerInHeadViewSpace[2] / lengthCenterInHeadViewSpace;
        // Calculate the dot product. Only Z component is non-zero in vecStraight.
        float dot = centerInHeadViewSpace[2] * vecStraight[2];
        float angle = (float) ((Math.acos(dot) / Math.PI) * 180);
        // If in the bottom half of the unit circle -> result is (360 - angle).
        if (centerInHeadViewSpace[0] > 0) {
            angle = 360 - angle;
        }

        final short modelNum = getModelNum(angle);

        // Show the info Toast with the model data.
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast.setText(breweryModelsData[modelNum].toString());
                toast.setGravity(Gravity.LEFT, 0, 0);
                toast.show();
            }
        });*/

        // Update sound.
        mainActivity.getGvrAudioEngine().update();

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set program handles for drawing.
        mvpMatrixHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_MVPMatrix");
        mvMatrixHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_MVMatrix");
        textureUniformHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_Texture");
        lightUniformPosHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_LightPos");
        highestYUniformHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_HighestY");
        lowestYUniformHandle = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_LowestY");
        fillLevel = GLES20.glGetUniformLocation(perVertexProgramHandle, "u_FillLevel");

        texCoordinateHandle = GLES20.glGetAttribLocation(perVertexProgramHandle, "a_TexCoordinate");
        positionHandle = GLES20.glGetAttribLocation(perVertexProgramHandle, "a_Position");
        colorHandle = GLES20.glGetAttribLocation(perVertexProgramHandle, "a_Color");
        normalHandle = GLES20.glGetAttribLocation(perVertexProgramHandle, "a_Normal");

        // Set program handles for skybox
        uMatrixLocation = GLES20.glGetUniformLocation(skyboxProgramHandle, "u_Matrix");
        uTextureUnitLocation = GLES20.glGetUniformLocation(skyboxProgramHandle, "u_TextureUnit");
        aPostionLocation = GLES20.glGetAttribLocation(skyboxProgramHandle, "a_Position");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, -5.0f, 10.0f, 0.0f);
        //Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        //Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);

        // Draw the models.
        float displacementAngle = 360.f / NUM_OF_UNITS;
        for (int i = 0; i < NUM_OF_UNITS; i++) {
            Matrix.setIdentityM(modelMatrix[i], 0);
            Matrix.rotateM(modelMatrix[i], 0, displacementAngle * i, 0.0f, 1.0f, 0.0f);
            Matrix.translateM(modelMatrix[i], 0, 0.0f, -5.f, -15.0f);
            // Rotate the breweryModels.
            Matrix.rotateM(modelMatrix[i], 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        }
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

        // Get screen center coordinates.
        Viewport viewport = eye.getViewport();
        viewport.getClass();

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(perVertexProgramHandle);

        for (int i = 0; i < NUM_OF_UNITS; i++) {
            // Draw all the units.
            // TODO: may need some tweaking, if the breweryModelsData remains the same, but has to be used for a lot 3D breweryModels
            drawModel(modelMatrix[i], mEyeViewMatrix, mEyeProjectionMatrix, // matrices
                    breweryModelsBuffers[(i * 3) % 9], breweryModelsBuffers[(i * 3 + 1) % 9], breweryModelsBuffers[(i * 3 + 2) % 9], // model buffers
                    breweryModels[i % 3].getHighest()[1], breweryModels[i % 3].getLowest()[1], // lowest/highest
                    breweryModelsData[i].level, breweryModels[i % 3].getModelInfo().getVertices(), interpolateColors(currColor[i], newColor[i], 0),
                    breweryModelsTextureDataHandle); // textures
        }

        // Floor.
        // TODO: move to global scope.
        // TODO: scale dat bitch
        float[] floorModelMatrix = new float[16];
        Matrix.setIdentityM(floorModelMatrix, 0);
        Matrix.translateM(floorModelMatrix, 0, 0.f, -5.f, 0.f);
        Matrix.scaleM(floorModelMatrix, 0, 2.f, 2.f, 2.f);
        drawModel(floorModelMatrix, mEyeViewMatrix, mEyeProjectionMatrix, floorModelBuffers[0], floorModelBuffers[1], floorModelBuffers[2], floorModel.getHighest()[1], floorModel.getLowest()[1], 0, floorModel.getModelInfo().getVertices(), new float[]{.1f, .1f, .7f, 1.f}, floorTilesTextureDataHandle);

        // Walls.
        for (int i = 0; i < 4; i++) {
            Matrix.setIdentityM(floorModelMatrix, 0);
            //Matrix.scaleM(floorModelMatrix, 0, 1.f, 1f, 0.5f);
            Matrix.translateM(floorModelMatrix, 0, 20.f * (-1 + (i % 2) * 2) * (1 - i / 2), -25.f, 20.f * (-1 + (i % 2) * 2) * (i / 2));
            Matrix.rotateM(floorModelMatrix, 0, 90.f, 1.f * (i / 2), 0.f, 1.f * (1 - i /2));
            drawModel(floorModelMatrix, mEyeViewMatrix, mEyeProjectionMatrix, floorModelBuffers[0], floorModelBuffers[1], floorModelBuffers[2], floorModel.getHighest()[1], floorModel.getLowest()[1], 0, floorModel.getModelInfo().getVertices(), new float[]{.1f, .1f, .7f, 1.f}, floorTilesTextureDataHandle);
        }

        // Draw a point to indicate the light.
        //GLES20.glUseProgram(pointProgramHandle);
        //drawLight(mEyeViewMatrix, mEyeProjectionMatrix);


        drawSkybox(mEyeViewMatrix, mEyeProjectionMatrix);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }


    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    /**
     * Fills the given {@code buffers} with the data from {@code models}.
     *
     * @param buffers
     * @param models
     */
    private void fillBuffers(FloatBuffer[] buffers, Model... models) {
        // Check the lengths.
        if (buffers.length != models.length * 3) {
            throw new RuntimeException("Buffers have insufficient capacity!");
        }

        // Fill the buffers.
        for (int i = 0; i < models.length; i++) {
            // Positions.
            buffers[i * 3] = ByteBuffer.allocateDirect(models[i].getPositions().length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            buffers[i * 3].put(models[i].getPositions()).position(0);
            // Normals.
            buffers[i * 3 + 1] = ByteBuffer.allocateDirect(models[i].getNormals().length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            buffers[i * 3 + 1].put(models[i].getNormals()).position(0);
            // Texels.
            buffers[i * 3 + 2] = ByteBuffer.allocateDirect(models[i].getTexels().length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            buffers[i * 3 + 2].put(models[i].getTexels()).position(0);
        }
    }

    /**
     * Draws a model given by it's {@code positions}, {@code normals} FloatBuffer-s and a float array containing the color.
     * Preserves the {@code lowestY} and {@code highestY} values.
     *
     * @param modelMatrix         the number of the model matrix to apply
     * @param eyeViewMatrix
     * @param eyeProjectionMatrix
     * @param positions
     * @param normals
     * @param highestY
     * @param lowestY
     * @param fillLevel           [0; 1]
     * @param numVertices
     * @param color
     */
    private void drawModel(float[] modelMatrix, float[] eyeViewMatrix, float[] eyeProjectionMatrix, FloatBuffer positions, FloatBuffer normals, FloatBuffer texels, float highestY, float lowestY, float fillLevel, int numVertices, float[] color, int textureDataHandle) {
        // Check the given color array
        if (color == null || color.length != 4) {
            throw new RuntimeException("Bad color array format! Expecting 4 values..");
        }

        // Pass attributes and stuff to the shader program
        //

        // TODO: try getting rid of '.position(0)' and see what happens
        // Pass in the position information
        positions.position(0);
        GLES20.glVertexAttribPointer(positionHandle, positionDataSize, GLES20.GL_FLOAT, false,
                0, positions);
        GLES20.glEnableVertexAttribArray(positionHandle);
        // Pass in the normal information
        normals.position(0);
        GLES20.glVertexAttribPointer(normalHandle, normalDataSize, GLES20.GL_FLOAT, false,
                0, normals);
        GLES20.glEnableVertexAttribArray(normalHandle);
        // Pass in the texture information.
        texels.position(0);
        GLES20.glVertexAttribPointer(texCoordinateHandle, textureDataSize, GLES20.GL_FLOAT, false, 0, texels);
        GLES20.glEnableVertexAttribArray(texCoordinateHandle);

        // Pass in the texture itself (sampler).
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(textureUniformHandle, 0);

        // Attributes.
        //
        // Pass in the color information
        GLES20.glVertexAttrib4f(colorHandle, color[0], color[1], color[2], color[3]);
        GLES20.glDisableVertexAttribArray(colorHandle);

        // Projection matrix construction
        //
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mvpMatrix, 0, eyeViewMatrix, 0, modelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvpMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mvpMatrix, 0, eyeProjectionMatrix, 0, mvpMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(lightUniformPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Pass in the highest and the lowest vertices
        GLES20.glUniform1f(highestYUniformHandle, highestY);
        GLES20.glUniform1f(lowestYUniformHandle, lowestY);
        GLES20.glUniform1f(this.fillLevel, fillLevel);

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);
    }

    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight(float[] mEyeViewMatrix, float[] mEyeProjectionMatrix) {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(pointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(pointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);
        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mvpMatrix, 0, mEyeViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mEyeProjectionMatrix, 0, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    private void drawSkybox(float[] viewMatrix, float[] projectionMatrix) {
        float[] viewProjectionMatrix = new float[16];
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        skyboxShaderProgram.useProgram();

        skyboxShaderProgram.setUniforms(viewProjectionMatrix, cubemapTextureDataHandle);

        skybox.bindData(skyboxShaderProgram);

        skybox.draw();
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType   The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    public static int compileShader(final int shaderType, final String shaderSource) {
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
    public static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
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
     * Update current {@code breweryModelsData} with the new {@code breweryModelsData}. Sets stepNum to 1.
     *
     * @param newModelData
     */
    public void updateModelData(Unit[] newModelData) {
        if (newModelData != null && newModelData.length != NUM_OF_UNITS) {
            throw new RuntimeException("Bad model data format! Expecting " + NUM_OF_UNITS + " values..");
        }

        // Update the model data.
        breweryModelsData = newModelData;

        for (int i = 0; i < NUM_OF_UNITS; i++) {
            // Remap breweryModelsData to [0, 1] range
            breweryModelsData[i].level /= 100;
            // Update the colors for rendering.
            newColor[i] = getColorFromTemperature(breweryModelsData[i].temperature);
            // Update the current interpolation step number.
            stepNum[i] = 1;
        }
    }

    /**
     * Interpolates between the given colors {@code COLOR_STEPS_PER_INTERVAL} number of times
     *
     * @param currColor color before data retrieval
     * @param newColor  color after data retrieval
     * @return float[4] interpolatedColor
     */
    private float[] interpolateColors(float[] currColor, float[] newColor, int modelNum) {
        int colorLength = currColor.length;
        if (colorLength != newColor.length) {
            Log.e(TAG, "Bad color format! Expecting arrays of the same length(==4). Aborting..");
            return new float[]{0.f, 0.f, 0.f, 0.f};
        }

        // Calculate the interpolated color.
        float[] interpolatedColor = new float[colorLength];
        float leftFactor = (COLOR_STEPS_PER_INTERVAL - stepNum[modelNum]) / COLOR_STEPS_PER_INTERVAL;
        float rightFactor = 1 - leftFactor;
        for (int i = 0; i < colorLength; i++) {
            interpolatedColor[i] = currColor[i] * leftFactor + newColor[i] * rightFactor;
        }

        // Increment stepNum.
        incrementStepNum(modelNum);

        return interpolatedColor;
    }

    /**
     * Defines the logic behind setting the {@code newColor}.
     *
     * @param temperature
     */
    private float[] getColorFromTemperature(int temperature) {
        // A color for the temperature == 50.
        float[] fiftyColor = new float[]{1.f, 1.f, .0f, 1.f}; // temperature == 50 -> color == YELLOW
        float[] result = new float[4];
        float leftFactor, rightFactor;

        // Interpolate between the given zeroColor, fiftyColor, hundredColor.
        if (temperature <= 50 && temperature >= 0) {
            leftFactor = (50.f - temperature) / 100;
            rightFactor = 1 - leftFactor;
            // A color for the temperature == 0.
            float[] zeroColor = new float[]{0.f, 0.f, 1.f, 1.f}; // temperature == 0 -> color == BLUE
            result[0] = zeroColor[0] * leftFactor + fiftyColor[0] * rightFactor;
            result[1] = zeroColor[1] * leftFactor + fiftyColor[1] * rightFactor;
            result[2] = zeroColor[2] * leftFactor + fiftyColor[2] * rightFactor;
        } else if (temperature <= 100 && temperature >= 0) {
            leftFactor = (100.f - temperature) / 100;
            rightFactor = 1 - leftFactor;
            // A color for the temperature == 100.
            float[] hundredColor = new float[]{1.f, .0f, .0f, 1.f}; // temperature == 100 -> color == RED
            result[0] = fiftyColor[0] * leftFactor + hundredColor[0] * rightFactor;
            result[1] = fiftyColor[1] * leftFactor + hundredColor[1] * rightFactor;
            result[2] = fiftyColor[2] * leftFactor + hundredColor[2] * rightFactor;
        }
        result[3] = 1.f;

        return result;
    }

    private void incrementStepNum(int modelNum) {
        if (stepNum[modelNum] < COLOR_STEPS_PER_INTERVAL)
            stepNum[modelNum] += 1;
    }

    /**
     * Updates the current and the new colors representing temperature in the renderer
     * @param newColor
     */
    /*public void updateColors(float[] newColor) {
        currColor = this.newColor;
        this.newColor = newColor;
    }*/

    /**
     * Get the model number from the current observation angle on ZX plane.
     *
     * @param lookingAngle current observation angle on ZX plane.
     * @return model number if all ok, -1 otherwise.
     */
    private short getModelNum(float lookingAngle) {
        final float[] modelAngles = new float[]{0.f, 60.f, 120.f, 180.f, 240.f, 300.f, 360.f};
        // Differences between the modelAngles and the lookingAngle on the previous and current steps respectively.
        float prevDiff = modelAngles[0] - lookingAngle;
        float currDiff;
        // The to-be-devised model number.
        short modelNum = -1;

        for (int i = 1; i < modelAngles.length; i++) {
            currDiff = modelAngles[i] - lookingAngle;

            if (prevDiff < 0 && currDiff > 0) { // If the sign changes on this interval (aka the angle is inside the interval).
                if (currDiff > Math.abs(prevDiff)) {
                    // If difference with the current angle is bigger than with the previous, devise the modelNum from the previous angle.
                    modelNum = (short) (modelAngles[i - 1] / 60.f);
                    break;
                } else {
                    // If difference with the current angle is smaller than with the previous, devise the modelNum from the current angle.
                    modelNum = (short) (modelAngles[i] / 60.f);
                    break;
                }

            } else {
                if (currDiff == 0) {
                    // If currDiff is 0, devise the angle from the current angle.
                    modelNum = (short) (modelAngles[i] / 60.f);
                    break;
                } else if (prevDiff == 0) {
                    // If prevDiff is 0, devise the angle from the previous angle.
                    modelNum = (short) (modelAngles[i - 1] / 60.f);
                    break;
                }
            }

            prevDiff = currDiff;
        }

        // If model num is 6 make it 0.
        modelNum = (short) (modelNum % 6);

        return modelNum;
    }
}