package de.tum.androidpraktikum.cardroarddatavisualizationjava.shaders;

/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/

import static android.opengl.GLES20.glUseProgram;
import android.content.Context;
import android.opengl.GLES20;

import de.tum.androidpraktikum.cardroarddatavisualizationjava.AssetLoader;
import de.tum.androidpraktikum.cardroarddatavisualizationjava.CardboardRenderer;

abstract class ShaderProgram {
    // Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_COLOR = "u_Color";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String U_TIME = "u_Time";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    protected static final String A_DIRECTION_VECTOR = "a_DirectionVector";
    protected static final String A_PARTICLE_START_TIME = "a_ParticleStartTime";

    // Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId,
                            int fragmentShaderResourceId) {
        final String skyboxVertexShader = AssetLoader.loadShader(context, AssetLoader.SKYBOX_VERTEX_SHADER);
        final String skyboxFragmentShader = AssetLoader.loadShader(context, AssetLoader.SKYBOX_FRAGMENT_SHADER);
        final int skyboxVertexShaderHandle = CardboardRenderer.compileShader(GLES20.GL_VERTEX_SHADER, skyboxVertexShader);
        final int skyboxFragmentShaderHandle = CardboardRenderer.compileShader(GLES20.GL_FRAGMENT_SHADER, skyboxFragmentShader);

        program = CardboardRenderer.createAndLinkProgram(skyboxVertexShaderHandle, skyboxFragmentShaderHandle, new String[] { "a_Position" });
    }

    public void useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program);
    }
}
