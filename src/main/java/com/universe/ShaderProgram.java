package com.universe;

import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class ShaderProgram {

    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    public ShaderProgram(String vertexResourcePath, String fragmentResourcePath) throws Exception {
        // Load and compile vertex shader
        String vertexShaderCode = loadResource(vertexResourcePath);
        vertexShaderId = createShader(vertexShaderCode, GL20.GL_VERTEX_SHADER);

        // Load and compile fragment shader
        String fragmentShaderCode = loadResource(fragmentResourcePath);
        fragmentShaderId = createShader(fragmentShaderCode, GL20.GL_FRAGMENT_SHADER);

        // Link shaders into a program
        programId = GL20.glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader program");
        }
        GL20.glAttachShader(programId, vertexShaderId);
        GL20.glAttachShader(programId, fragmentShaderId);
        GL20.glLinkProgram(programId);

        // Check for linking errors
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new Exception("Error linking Shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
        }

        // Detach shaders after successful link (optional but good practice)
        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId);
        }
        
        // Validate program (optional, for debugging)
         GL20.glValidateProgram(programId);
         if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
             System.err.println("Warning validating Shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
         }
    }

    private String loadResource(String resourcePath) throws IOException {
        StringBuilder result = new StringBuilder();
        InputStream in = ShaderProgram.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Could not find resource: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = GL20.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        GL20.glShaderSource(shaderId, shaderCode);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new Exception("Error compiling Shader code: " + GL20.glGetShaderInfoLog(shaderId, 1024));
        }

        return shaderId;
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public void cleanup() {
        unuse();
        if (programId != 0) {
             // Delete shaders if they were successfully created
            if (vertexShaderId != 0) {
                GL20.glDeleteShader(vertexShaderId);
            }
            if (fragmentShaderId != 0) {
                GL20.glDeleteShader(fragmentShaderId);
            }
            GL20.glDeleteProgram(programId);
        }
    }

    public void unuse() {
        GL20.glUseProgram(0);
    }

    // --- Uniform Setters --- 

    public void setInt(String name, int value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }

    public void setFloat(String name, float value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }

    public void setVec2(String name, Vector2f value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(2);
                value.get(buffer);
                GL20.glUniform2fv(location, buffer);
            }
        }
    }
    
    public void setVec3(String name, Vector3f value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
             try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(3);
                value.get(buffer);
                GL20.glUniform3fv(location, buffer);
            }
        }
    }

    public void setVec4(String name, Vector4f value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
             try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4);
                value.get(buffer);
                GL20.glUniform4fv(location, buffer);
            }
        }
    }
    
    public void setMat4(String name, Matrix4f value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                value.get(buffer);
                GL20.glUniformMatrix4fv(location, false, buffer);
            }
        }
    }
    
    // Add setters for other types (mat3, double matrices etc.) if needed
    
    public void setBoolean(String name, boolean value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1i(location, value ? 1 : 0);
        }
    }
} 