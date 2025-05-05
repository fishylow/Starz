package com.universe;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UiRenderer {
    private int vaoId;
    private int vboId;
    private int elementBufferId;

    private ShaderProgram uiShader;
    private int fontTextureId;
    private STBTTBakedChar.Buffer charData;
    
    private FloatBuffer vertices;
    private int[] indices;
    
    // Font size and texture dimensions
    private static final int BITMAP_WIDTH = 512;
    private static final int BITMAP_HEIGHT = 512;
    private static final float FONT_SIZE = 24.0f;  // Smaller font size for better quality
    
    public UiRenderer(ByteBuffer fontBuffer) {
        // Initialize UI shader
        try {
            uiShader = new ShaderProgram("shaders/ui.vert", "shaders/ui.frag");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load UI shaders: " + e.getMessage());
        }
        
        // Initialize VAO for UI rendering
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create VBO for quad vertices (position, texcoord, color)
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        // Each vertex has: position(2), texcoord(2), color(4)
        vertices = BufferUtils.createFloatBuffer(4 * 8); // 4 vertices, 8 floats per vertex
        // Create a placeholder buffer, we'll update it later
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Color attribute
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 8 * Float.BYTES, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Element buffer for indices
        elementBufferId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBufferId);
        
        // Quad indices (2 triangles)
        indices = new int[] { 0, 1, 2, 2, 3, 0 };
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length);
        indicesBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        
        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        // Initialize font texture
        initFontTexture(fontBuffer);
    }
    
    private void initFontTexture(ByteBuffer fontBuffer) {
        // Bake font bitmap using STB TrueType
        charData = STBTTBakedChar.malloc(96);  // ASCII 32..126 is 95 chars
        
        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_WIDTH * BITMAP_HEIGHT);
        
        // Bake the font to a bitmap for a set of codepoints (ASCII 32..126)
        int result = STBTruetype.stbtt_BakeFontBitmap(
            fontBuffer,
            FONT_SIZE,
            bitmap,
            BITMAP_WIDTH,
            BITMAP_HEIGHT,
            32, // First char
            charData
        );
        
        if (result <= 0) {
            System.err.println("Warning: Font baking resulted in " + result + " characters");
        }
        
        // Create texture from bitmap
        fontTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_WIDTH, BITMAP_HEIGHT, 0, 
                    GL_RED, GL_UNSIGNED_BYTE, bitmap);
                    
        // Use better filtering for the font texture
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
        // Prevent edge artifacts
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    public void renderSidebar(int windowWidth, int windowHeight, float sidebarWidth, boolean sidebarOpen) {
        if (!sidebarOpen) return;
        
        // Set up orthographic projection
        Matrix4f projection = new Matrix4f().ortho(
            0, windowWidth, 
            windowHeight, 0, // Flip Y axis for screen coordinates (0,0 at top-left)
            -1, 1
        );
        
        // Background color (more opaque dark background)
        float[] bgColor = {0.08f, 0.08f, 0.12f, 0.92f};  // Darker and more opaque
        
        // Sidebar position on the right
        float x = windowWidth - sidebarWidth;
        float y = 0;
        float width = sidebarWidth;
        float height = windowHeight;
        
        // Start UI rendering
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Use UI shader
        uiShader.use();
        uiShader.setMat4("projection", projection);
        uiShader.setInt("textTexture", 0);
        uiShader.setBoolean("useTexture", false);  // Just a colored quad, no texture
        
        // Bind VAO
        glBindVertexArray(vaoId);
        
        // Update vertices for the sidebar background
        updateQuadVertices(x, y, width, height, 0, 0, 0, 0, bgColor);
        
        // Draw sidebar background
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        
        // Restore state
        glBindVertexArray(0);
        uiShader.unuse();
        
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
    
    public void renderText(String text, float x, float y, float scale, float[] color) {
        if (text == null || text.isEmpty()) return;
        
        // Setup for text rendering
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.use();
        // Get viewport size and create orthographic projection
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int viewportWidth = viewport.get(2);
        int viewportHeight = viewport.get(3);
        
        Matrix4f projection = new Matrix4f().ortho(
            0, viewportWidth, 
            viewportHeight, 0,
            -1, 1
        );
        uiShader.setMat4("projection", projection);
        uiShader.setBoolean("useTexture", true);
        
        // Activate texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        
        // Bind VAO
        glBindVertexArray(vaoId);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pCodePoint = stack.mallocInt(1);
            
            // These need to be reset for each string
            FloatBuffer x0 = stack.floats(0.0f);
            FloatBuffer y0 = stack.floats(0.0f);
            
            STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
            
            float currentX = x;
            float currentY = y;
            
            // For each character in the string
            for (int i = 0; i < text.length(); ) {
                i += getCodePoint(text, i, pCodePoint);
                
                int cp = pCodePoint.get(0);
                if (cp < 32 || cp > 126) cp = 32;  // Invalid char, replace with space
                
                float xShift = x0.get(0);
                float yShift = y0.get(0);
                
                // Get bitmap q for the char
                STBTruetype.stbtt_GetBakedQuad(
                    charData, 
                    BITMAP_WIDTH, BITMAP_HEIGHT, 
                    cp - 32, 
                    x0, y0, 
                    q, 
                    true
                );
                
                // Calculate correct position for this character
                float x1 = currentX + (q.x0() - xShift) * scale;
                float y1 = currentY + (q.y0() - yShift) * scale;
                float x2 = currentX + (q.x1() - xShift) * scale;
                float y2 = currentY + (q.y1() - yShift) * scale;
                
                // Update vertices for the character quad
                updateQuadVertices(
                    x1, y1, 
                    x2 - x1, y2 - y1, 
                    q.s0(), q.t0(), q.s1(), q.t1(), 
                    color
                );
                
                // Draw the character
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
                
                // Advance cursor position (apply kerning)
                currentX += (x0.get(0) - xShift) * scale;
            }
        }
        
        // Cleanup
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        uiShader.unuse();
        glDisable(GL_BLEND);
    }
    
    private void updateQuadVertices(float x, float y, float width, float height, 
                                   float u0, float v0, float u1, float v1, 
                                   float[] color) {
        vertices.clear();
        
        // Bottom-left
        vertices.put(x).put(y + height);           // Position
        vertices.put(u0).put(v1);                  // TexCoord
        vertices.put(color[0]).put(color[1]).put(color[2]).put(color[3]); // Color
        
        // Bottom-right
        vertices.put(x + width).put(y + height);   // Position
        vertices.put(u1).put(v1);                  // TexCoord
        vertices.put(color[0]).put(color[1]).put(color[2]).put(color[3]); // Color
        
        // Top-right
        vertices.put(x + width).put(y);            // Position
        vertices.put(u1).put(v0);                  // TexCoord
        vertices.put(color[0]).put(color[1]).put(color[2]).put(color[3]); // Color
        
        // Top-left
        vertices.put(x).put(y);                    // Position
        vertices.put(u0).put(v0);                  // TexCoord
        vertices.put(color[0]).put(color[1]).put(color[2]).put(color[3]); // Color
        
        vertices.flip();
        
        // Update buffer
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private static int getCodePoint(String text, int i, IntBuffer cpOut) {
        char c1 = text.charAt(i);
        if (Character.isHighSurrogate(c1) && i + 1 < text.length()) {
            char c2 = text.charAt(i + 1);
            if (Character.isLowSurrogate(c2)) {
                cpOut.put(0, Character.toCodePoint(c1, c2));
                return 2;
            }
        }
        cpOut.put(0, c1);
        return 1;
    }
    
    public void cleanup() {
        if (uiShader != null) uiShader.cleanup();
        if (vboId != 0) glDeleteBuffers(vboId);
        if (elementBufferId != 0) glDeleteBuffers(elementBufferId);
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        if (fontTextureId != 0) glDeleteTextures(fontTextureId);
        if (charData != null) charData.free();
    }

    /**
     * Renders a simple crosshair (a small dot) in the center of the screen.
     * @param windowWidth The current width of the window.
     * @param windowHeight The current height of the window.
     */
    public void renderCrosshair(float windowWidth, float windowHeight) {
        float crosshairSize = 4.0f; // Size of the crosshair dot
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        
        // White color for the crosshair
        float[] crosshairColor = {1.0f, 1.0f, 1.0f, 0.8f}; // Slightly transparent white
        
        // Use the UI shader
        uiShader.use();
        
        // Set projection matrix for 2D rendering
        Matrix4f ortho = new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0);
        uiShader.setMat4("projection", ortho);
        
        // Tell shader we are rendering a colored quad (not textured text)
        uiShader.setBoolean("useTexture", false);
        
        // Bind the VAO
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        // Update vertices for the crosshair dot (a small quad)
        updateQuadVertices(
            centerX - crosshairSize / 2.0f, 
            centerY - crosshairSize / 2.0f, 
            crosshairSize, 
            crosshairSize, 
            0, 0, 1, 1, // Dummy UVs, not used
            crosshairColor
        );
        
        // Draw the crosshair dot
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBufferId);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        
        // Clean up state
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        // Set back to texture mode for subsequent text rendering
        uiShader.setBoolean("useTexture", true); 
        uiShader.unuse();
    }
} 