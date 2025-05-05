#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

out vec3 fragColor;
out vec3 fragNormal;
out vec3 fragPosWorld;
out vec3 fragPosView;
out float glowFactor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform vec3 starColor;
uniform float starDistance;
uniform float starAbsMag;
uniform vec3 cameraPos;
uniform float screenWidth;
uniform float screenHeight;
uniform float minVisibleSize;
uniform bool ensureVisible;

void main()
{
    // Get original model position
    vec4 worldPos = model * vec4(aPos, 1.0);
    
    // Ensure minimum star size by uniformly scaling the model matrix if needed
    if (ensureVisible) {
        // Transform to view space
        vec4 viewPos = view * worldPos;
        
        // Project to get clip space position
        vec4 clipPos = projection * viewPos;
        vec3 ndcPos = clipPos.xyz / clipPos.w;
        
        // Calculate screen coordinates
        vec2 screenPos = ndcPos.xy * 0.5 + 0.5;
        screenPos.x *= screenWidth;
        screenPos.y *= screenHeight;
        
        // Calculate distance-based minimum size (at least 2x2 pixels)
        float minPixelSize = 2.0; // Minimum 2x2 pixel size
        
        // Compute view-space size of the star
        float distanceFactor = clamp(1.0 - starDistance / 100.0, 0.0, 1.0);
        // Reduced by 25% for smaller close stars (from 2.0 to 1.5)
        float sizeFactor = mix(1.0, 1.5, distanceFactor); // Smaller scale for closer stars
        
        // Use uniform scaling to preserve roundness
        // Scale based on absolute magnitude also (brighter stars appear larger)
        float magnitudeFactor = max(0.1, (16.0 - min(starAbsMag, 16.0)) / 16.0);
        
        // We apply the scaling to the original position without distorting the sphere
        // This preserves the perfect round shape
        fragPosWorld = worldPos.xyz;
    } else {
        fragPosWorld = worldPos.xyz;
    }
    
    // Calculate view space position
    fragPosView = vec3(view * worldPos);
    
    // Normal transformation - preserve spherical shape
    fragNormal = mat3(transpose(inverse(model))) * aNormal;
    
    // Pass color to fragment shader
    fragColor = starColor;
    
    // Calculate glow factor based on distance and absolute magnitude
    // Lower magnitude = brighter star
    // We invert the relationship so higher value = brighter
    float brightnessFactor = 15.0 - min(starAbsMag, 15.0); // Clamp at 15
    
    // Distance falloff - should be stronger for brighter stars
    // Make glow stronger for closer stars
    glowFactor = brightnessFactor / max(0.1, starDistance);
    
    // Transform final position to clip space
    gl_Position = projection * view * worldPos;
} 