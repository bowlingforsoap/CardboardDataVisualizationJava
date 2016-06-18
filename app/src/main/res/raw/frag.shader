// Shaders from these files should be manualy copied to the corresponding variables (vertexShader and fragmentShader respectively)

// Set the default precision to medium. We don't need as high of a
// precision in the fragment shader.
precision mediump float;

uniform vec3 u_LightPos;       	    // The position of the light in eye space.

varying vec3 v_Position;
varying vec3 v_Normal;
varying vec4 v_Color;             // This is the color from the vertex shader interpolated across the

void main()                    		// The entry point for our fragment shader.
{
    // Get a lighting direction vector from the light to the vertex.
    vec3 vecToLight = normalize(u_LightPos - v_Position);
    // Will be used for attenuation.
    float distance = length(u_LightPos - v_Position);
    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
    // pointing in the same direction then it will get max illumination.
    float diffuse = max(dot(v_Normal, vecToLight), 0.1);
    // Add attenuation.
    //diffuse = diffuse * (1.0 / (1.0 + 0.25 * distance * distance));
    diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance)));
    // Multiply the color by the diffuse illumination level to get final output color.
    gl_FragColor = diffuse * v_Color;     		// Pass the color directly through the pipeline.
}