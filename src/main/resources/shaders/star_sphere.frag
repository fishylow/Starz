#version 330 core
out vec4 FragColor;

in vec3 fragColor;
in vec3 fragNormal;
in vec3 fragPosWorld;
in vec3 fragPosView;
in float glowFactor; // Based on AbsMag and Distance

uniform vec3 cameraPos;

// Function to increase saturation
vec3 saturateColor(vec3 color, float saturation) {
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    vec3 grey = vec3(luminance);
    return mix(grey, color, saturation);
}

void main()
{
    vec3 normalizedNormal = normalize(fragNormal);
    vec3 viewDir = normalize(-fragPosView);
    float NdotV = max(dot(normalizedNormal, viewDir), 0.0);
    float rim = pow(1.0 - NdotV, 2.5); // Sharper rim falloff

    // --- Base Color Enhancement ---
    vec3 baseColor = fragColor;
    // Increase saturation more significantly
    vec3 enhancedColor = saturateColor(baseColor, 1.8);

    // --- Glow Calculation ---
    // Make glow stronger, more sensitive to glowFactor
    float glowStrength = pow(glowFactor, 1.5) * 1.5; // Exponential scaling, increased multiplier

    // --- Halo/Rim Effect ---
    // Halo color tinted by the star's enhanced color, less white
    vec3 haloColor = mix(enhancedColor, vec3(1.0), 0.15); // Less white mix
    vec3 rimGlow = haloColor * rim * glowStrength * 0.8; // Stronger rim contribution

    // --- Core Brightness ---
    // Brighter core, less falloff towards the center
    float centerGlow = pow(NdotV, 0.5); // Less steep falloff from center
    vec3 centerColor = mix(enhancedColor, vec3(1.0), 0.6); // Keep center bright
    vec3 coreBrightness = centerColor * centerGlow;

    // --- Combine Effects ---
    vec3 finalColor = enhancedColor * 0.4 + coreBrightness * 0.6 + rimGlow;

    // --- Bloom/Atmospheric Effect ---
    // Increase the intensity and spread of the bloom
    float bloomIntensity = clamp(glowStrength * 0.4, 0.0, 1.2); // Increased multiplier (0.2 to 0.4), allow slightly over 1.0
    // Use a slightly lower power for the rim falloff to widen the bloom
    vec3 bloomColor = enhancedColor * pow(rim, 1.2) * bloomIntensity;
    finalColor += bloomColor;

    // --- Tonemapping --- (Slightly adjusted to preserve brights)
    // Using a slightly modified Reinhard curve
    finalColor = finalColor / (finalColor + vec3(0.8)); // Lower denominator boosts brightness a bit
    // Clamp final color to avoid potential issues (optional but safe)
    finalColor = clamp(finalColor, 0.0, 1.0);

    // --- Alpha Calculation --- (Copied from previous state, seems okay)
    float alpha = 1.0;
    float closenessFactor = 1.0;
    if (length(fragPosWorld - cameraPos) < 5.0) {
        closenessFactor = 2.5;
    }
    if (rim > 0.4) {
        alpha = mix(1.0, 0.6 * closenessFactor, (rim - 0.4) * 1.0);
        if (NdotV > 0.5) { // Using NdotV directly for center opacity check
            alpha = 1.0;
        }
        if (glowFactor < 0.5) {
            alpha = max(alpha, 0.7);
        }
    } else {
        alpha = 1.0;
    }
    if (length(fragPosWorld - cameraPos) < 3.0 && NdotV > 0.2) {
        alpha = 1.0;
    }

    FragColor = vec4(finalColor, alpha);
} 