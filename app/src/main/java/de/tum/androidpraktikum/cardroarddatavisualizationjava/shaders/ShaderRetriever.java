package de.tum.androidpraktikum.cardroarddatavisualizationjava.shaders;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.tum.androidpraktikum.cardroarddatavisualizationjava.R;

public class ShaderRetriever {
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
    public static String getShaderCode(Context appContext, int shader) {
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
}
