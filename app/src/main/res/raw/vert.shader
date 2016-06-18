// Shaders from these files should be manualy copied to the corresponding variables (vertexShader and fragmentShader respectively)

uniform mat4 u_MVPMatrix;           // A constant representing the combined model/view/projection matrix.
uniform mat4 u_MVMatrix;            // A constant representing the combined model/view matrix.

uniform float u_HighestY;         // The highest Z coordinate of a model in the eye view space.
uniform float u_LowestY;          // The lowest Z coordinate of a model in the eye view space.
uniform float u_FillLevel;        // fragments with Z < ( (v_Position - u_LowestZ) / (u_HighestZ - u_LowestZ) ) should be colored.

attribute vec4 a_Position;     		// Per-vertex position information we will pass in.
attribute vec4 a_Color;             // Per-vertex color information we will pass in.
attribute vec3 a_Normal;       		// Per-vertex normal information we will pass in.

varying vec3 v_Position;            // This will be passed into the fragment shader.
varying vec3 v_Normal;              // This will be passed into the fragment shader.
varying vec4 v_Color;               // This will be passed into the fragment shader.

void main() {        	            // The entry point for our vertex shader.
    // Z coordinate normalized with respect to u_LowestZ and u_HighestZ.
    float normalizedY = (a_Position.y - u_LowestY) / (u_HighestY - u_LowestY);
    // Decide on the color according to the normalized Z component of a vertex.
    if (normalizedY < u_FillLevel) {
        // If less than the fill level -> keep the color.
        v_Color = a_Color;
    } else {
        // Else -> paint dark gray.
        v_Color = vec4(0.7, 0.7, 0.7, 1.0);
    }

    // Transform the vertex into eye space.
    v_Position = vec3(u_MVMatrix * a_Position);
    // Transform the normal's orientation into eye space.
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));
    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix * a_Position;
}