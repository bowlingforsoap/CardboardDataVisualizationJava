package de.tum.androidpraktikum.cardroarddatavisualizationjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class AssetLoader {
    public static final int VERTEX_SHADER = 0;
    public static final int FRAGMENT_SHADER = 1;
    public static final int LIGHT_VERTEX_SHADER = 2;
    public static final int LIGHT_FRAGMENT_SHADER = 3;

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
     * @param appContext
     * @param resourceIds
     * @return
     */
    public static int[] loadTextures(final Context appContext, final int ... resourceIds) {
        // Generate texture/handle;
        final int[] textureHandle = new int[resourceIds.length];
        GLES20.glGenTextures(1, textureHandle, 0);

        // Load the texture from resources.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // By default, Android applies pre-scaling to bitmaps depending on the resolution of your device and which resource folder you placed the image in. We don’t want Android to scale our bitmap at all, so to be sure, we set inScaled to false.

        Bitmap bitmap;

        for (int i = 0; i < resourceIds.length; i++) {
            bitmap = BitmapFactory.decodeResource(appContext.getResources(), resourceIds[i], options);

            // Bind to texture in OpenGL. Binding to a texture tells OpenGL that subsequent OpenGL calls should affect this texture.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[i]);
            // Set filtering
            // Texture value used for minification.
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            // Texture value used for magnification.
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        return textureHandle;
    }
}
