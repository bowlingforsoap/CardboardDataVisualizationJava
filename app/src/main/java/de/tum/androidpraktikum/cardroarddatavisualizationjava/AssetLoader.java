package de.tum.androidpraktikum.cardroarddatavisualizationjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import static android.opengl.GLES20.*;
import static android.opengl.GLUtils.*;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class AssetLoader {
    public static final String TAG = "AssetLoader";

    public static final int VERTEX_SHADER = 0;
    public static final int FRAGMENT_SHADER = 1;
    public static final int LIGHT_VERTEX_SHADER = 2;
    public static final int LIGHT_FRAGMENT_SHADER = 3;
    public static final int SKYBOX_VERTEX_SHADER = 4;
    public static final int SKYBOX_FRAGMENT_SHADER = 5;

    /**
     * Retrieves a shader by a {@code shader} code.
     *
     * @param shader - int code of the shader to retrieve.
     * @return shader code as a String.
     */
    @NonNull
    public static String loadShader(final Context appContext, int shader) {
        InputStream shaderInputStream = null;
        // Initialize the input stream
        switch (shader) {
            case VERTEX_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.vert);
                break;
            case FRAGMENT_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.frag);
                break;
            case LIGHT_VERTEX_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.light_vert);
                break;
            case LIGHT_FRAGMENT_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.light_frag);
                break;
            case SKYBOX_VERTEX_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.skybox_vert);
                break;
            case SKYBOX_FRAGMENT_SHADER:
                shaderInputStream = appContext.getResources().openRawResource(R.raw.skybox_frag);
                break;
        }

        StringBuilder shaderCode = null;
        try {
            int bytesAvailable = shaderInputStream.available();
            shaderCode = new StringBuilder(bytesAvailable);
            int content;

            while ((content = shaderInputStream.read()) != -1) {
                shaderCode.append((char) content);
            }

            shaderInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading the shader file!");
        }

        return shaderCode.toString();
    }

    /**
     * Loads a texture specified with a given {@code resourceId}.
     *
     * @param appContext
     * @param resourceIds
     * @return
     */
    public static int[] loadTextures(final Context appContext, final int... resourceIds) {
        // Generate texture/handle;
        final int[] textureHandle = new int[resourceIds.length];
        glGenTextures(1, textureHandle, 0);

        // Load the texture from resources.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // By default, Android applies pre-scaling to bitmaps depending on the resolution of your device and which resource folder you placed the image in. We don’t want Android to scale our bitmap at all, so to be sure, we set inScaled to false.

        Bitmap bitmap;

        for (int i = 0; i < resourceIds.length; i++) {
            bitmap = BitmapFactory.decodeResource(appContext.getResources(), resourceIds[i], options);

            // Bind to texture in OpenGL. Binding to a texture tells OpenGL that subsequent OpenGL calls should affect this texture.
            glBindTexture(GL_TEXTURE_2D, textureHandle[i]);
            // Set filtering
            // Texture value used for minification.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            // Texture value used for magnification.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // Load the bitmap into the bound texture.
            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            glBindTexture(GL_TEXTURE_2D, 0);
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        return textureHandle;
    }

    /**
     * Loads a cubemap texture from the provided resources and returns the
     * texture ID. Returns 0 if the load failed.
     *
     * @param context
     * @param cubeResources An array of resources corresponding to the cube map. Should be
     *                      provided in this order: left, right, bottom, top, front, back.
     * @return
     */
    public static int loadCubeMap(Context context, int ... cubeResources) {
        final int[] textureObjectIds = new int[1];
        glGenTextures(1, textureObjectIds, 0);

        if (textureObjectIds[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.");
            return 0;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap[] cubeBitmaps = new Bitmap[6];
        for (int i = 0; i < 6; i++) {
            cubeBitmaps[i] =
                    BitmapFactory.decodeResource(context.getResources(),
                            cubeResources[i], options);

            if (cubeBitmaps[i] == null) {
                Log.w(TAG, "Resource ID " + cubeResources[i]
                        + " could not be decoded.");
                glDeleteTextures(1, textureObjectIds, 0);
                return 0;
            }
        }
        // Linear filtering for minification and magnification
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureObjectIds[0]);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, cubeBitmaps[0], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, cubeBitmaps[1], 0);

        texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, cubeBitmaps[2], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, cubeBitmaps[3], 0);

        texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, cubeBitmaps[4], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, cubeBitmaps[5], 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        for (Bitmap bitmap : cubeBitmaps) {
            bitmap.recycle();
        }

        return textureObjectIds[0];
    }
}