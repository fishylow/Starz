#version 330 core
layout (location = 0) in vec3 aPos;   // Vertex position (input attribute 0)
layout (location = 1) in vec3 aColor; // Vertex color (input attribute 1)

out vec3 ourColor; // Pass color to fragment shader

uniform mat4 model;      // Model matrix (object's position/orientation/scale)
uniform mat4 view;       // View matrix (camera)
uniform mat4 projection; // Projection matrix (perspective)

void main()
{
    // Transform vertex position to clip space
    gl_Position = projection * view * model * vec4(aPos, 1.0);
    
    // Pass the color through to the fragment shader
    ourColor = aColor;
    
    // Set a default point size (can be made dynamic later)
    gl_PointSize = 2.0; 
} 