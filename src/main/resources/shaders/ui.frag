#version 330 core

in vec2 TexCoord;
in vec4 Color;

out vec4 FragColor;

uniform sampler2D textTexture;
uniform bool useTexture;

void main() {
    if (useTexture) {
        // For text rendering with SDF fonts
        float alpha = texture(textTexture, TexCoord).r;
        FragColor = vec4(Color.rgb, Color.a * alpha);
    } else {
        // For colored quads (sidebar background)
        FragColor = Color;
    }
} 