#version 330 core
out vec4 FragColor; // Output fragment color

in vec3 ourColor; // Input color from vertex shader (interpolated)

void main()
{
    // Set the fragment color to the color received from the vertex shader
    FragColor = vec4(ourColor, 1.0);
} 