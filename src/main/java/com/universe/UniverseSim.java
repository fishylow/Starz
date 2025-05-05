package com.universe;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.Version;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Main class for the Universe Simulation application.
 * Handles window creation, OpenGL setup, input processing, rendering loop, and data loading.
 */
public class UniverseSim {

    private long window; // Window handle
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private final String windowTitle = "Universe Simulation";

    private Map<String, Star> stars;
    private List<Planet> planets;

    // Camera
    private Camera camera;
    private Matrix4f projectionMatrix;
    // Define appropriate near and far clipping planes for the vast scale
    private static final float NEAR_PLANE = 0.01f;  // In light-years
    private static final float FAR_PLANE = 10000.0f; // In light-years (adjust as needed)

    // Input State
    private double lastX = windowWidth / 2.0;
    private double lastY = windowHeight / 2.0;
    private boolean firstMouse = true;
    private boolean speedBoostActive = false; // For Shift
    private boolean slowDownActive = false;  // For Ctrl

    // Simple timing
    private double lastFrameTime;
    private double deltaTime;

    // Sphere mesh and VAO/VBO/EBO for rendering stars as spheres
    private SphereMesh sphereMesh;
    private int sphereVaoId;
    private int sphereVboId;
    private int sphereNboId;
    private int sphereEboId;
    private ShaderProgram starSphereShader;

    // Sidebar
    private boolean sidebarOpen = true;
    private float sidebarWidth = 350f;

    private ByteBuffer fontBuffer;
    private int fontTextureId;
    private STBTTBakedChar.Buffer cdata;
    private boolean searchMode = false;
    private StringBuilder searchInput = new StringBuilder();
    private Star searchResult = null;
    private UiRenderer uiRenderer;  // Add UiRenderer

    /**
     * Starts the simulation.
     */
    public void run() {
        System.out.println("Starting Universe Simulation with LWJGL " + Version.getVersion() + "!");

        init();
        loop();
        cleanup();
    }

    /**
     * Initializes GLFW, OpenGL context, loads data, shaders, and sets up callbacks.
     */
    public void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); // Use OpenGL 3.3
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(windowWidth, windowHeight, windowTitle, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup key callback & other callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
            
            // Toggle sidebar on Tab press
            if (key == GLFW_KEY_TAB && action == GLFW_PRESS) {
                sidebarOpen = !sidebarOpen;
            }
            
            // Enter/exit search mode on slash key
            if (key == GLFW_KEY_SLASH && action == GLFW_PRESS) {
                searchMode = !searchMode;
                if (searchMode) {
                    searchInput.setLength(0); // Clear search input when entering search mode
                }
            }
            
            // Speed control with + and - keys
            if ((key == GLFW_KEY_EQUAL && (mods & GLFW_MOD_SHIFT) != 0 && action == GLFW_PRESS) || key == GLFW_KEY_EQUAL && action == GLFW_PRESS) {
                // Plus key (+) increases speed
                camera.adjustSpeed(true);
                System.out.println("Speed increased to: " + camera.movementSpeed + " ly/s");
            } else if (key == GLFW_KEY_MINUS && action == GLFW_PRESS) {
                // Minus key (-) decreases speed
                camera.adjustSpeed(false);
                System.out.println("Speed decreased to: " + camera.movementSpeed + " ly/s");
            }
            
            // Handle text input in search mode
            if (searchMode && action == GLFW_PRESS) {
                if (key == GLFW_KEY_BACKSPACE && searchInput.length() > 0) {
                    // Remove last character on backspace
                    searchInput.setLength(searchInput.length() - 1);
                } else if (key == GLFW_KEY_ENTER) {
                    // Try to find star by name on Enter
                    String query = searchInput.toString().toLowerCase().trim();
                    searchResult = null;
                    
                    // Try to find by Hipparcos ID first if query is numeric
                    if (query.startsWith("hip") && query.length() > 3) {
                        // Extract HIP number without prefix
                        String hipIdStr = query.substring(3);
                        try {
                            int hipId = Integer.parseInt(hipIdStr);
                            searchResult = stars.get("hip" + hipId);
                        } catch (NumberFormatException e) {
                            // Not a valid number, continue with name search
                        }
                    } else if (isNumeric(query)) {
                        // Try direct numeric HIP ID (without "hip" prefix)
                        try {
                            int hipId = Integer.parseInt(query);
                            searchResult = stars.get("hip" + hipId);
                        } catch (NumberFormatException e) {
                            // Not a valid number, continue with name search
                        }
                    }
                    
                    // If not found by HIP ID, search by name
                    if (searchResult == null) {
                        // Try exact name match first
                        searchResult = stars.get(query);
                        
                        // If not found, try partial match
                        if (searchResult == null) {
                            // Search by name using partial matching
                            for (Star star : stars.values()) {
                                if (star.getName().toLowerCase().contains(query)) {
                                    searchResult = star;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (searchResult != null) {
                        // Teleport to the found star
                        camera.position.set(searchResult.getX(), searchResult.getY(), searchResult.getZ() + 0.1); // Offset slightly
                        camera.updateCameraVectors();
                    }
                    
                    searchMode = false; // Exit search mode after search
                } else if (key >= GLFW_KEY_A && key <= GLFW_KEY_Z) {
                    // Add alphabetic characters
                    char c = (char) ('a' + (key - GLFW_KEY_A));
                    if ((mods & GLFW_MOD_SHIFT) != 0) {
                        c = Character.toUpperCase(c);
                    }
                    searchInput.append(c);
                } else if (key >= GLFW_KEY_0 && key <= GLFW_KEY_9) {
                    // Add numbers
                    char c = (char) ('0' + (key - GLFW_KEY_0));
                    searchInput.append(c);
                } else if (key == GLFW_KEY_SPACE) {
                    searchInput.append(' ');
                } else if (key == GLFW_KEY_APOSTROPHE) {
                    // Allow apostrophes for names like "Barnard's Star"
                    if ((mods & GLFW_MOD_SHIFT) != 0) {
                        searchInput.append('"');
                    } else {
                        searchInput.append('\'');
                    }
                } else if (key == GLFW_KEY_MINUS) {
                    // Allow hyphens
                    if ((mods & GLFW_MOD_SHIFT) != 0) {
                        searchInput.append('_');
                    } else {
                        searchInput.append('-');
                    }
                }
            }
        });

        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
             this.windowWidth = width;
             this.windowHeight = height;
             if (height > 0) { // Prevent division by zero
                 glViewport(0, 0, width, height);
                 updateProjectionMatrix(); // Update projection on resize
             }
        });
        
        // Mouse position callback
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }

            double xoffset = xpos - lastX;
            double yoffset = lastY - ypos; // reversed since y-coordinates go from bottom to top

            lastX = xpos;
            lastY = ypos;

            if (camera != null) { // Ensure camera is initialized
                camera.processMouseMovement((float) xoffset, (float) yoffset, true);
            }
        });

        // Mouse scroll callback
        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
             if (camera != null) {
                camera.processMouseScroll((float) yoffset); // Use yoffset for zoom
                 updateProjectionMatrix(); // FOV (zoom) changed, update projection
             }
        });
        
        // Capture the mouse cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color (background to dark gray for debug)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Pitch black background
        
        // Enable depth testing for 3D rendering
        glEnable(GL_DEPTH_TEST);
        // Accept fragment if it closer to the camera than the former one
        glDepthFunc(GL_LESS);
        
        // Enable backface culling to improve performance
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Enable blending for transparency and glow effects
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Set point size (in case we render stars as points in the future)
        glPointSize(4.0f);

        // Load data
        try {
            stars = DataLoader.loadStarsFromFile("stars.csv");
            
            // Remove overlapping stars (remove larger ones)
            removeOverlappingStars();
            
            planets = DataLoader.loadPlanetsFromFile("planets.csv", stars);
            System.out.println("Loaded " + stars.size() + " stars and " + planets.size() + " planets.");
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
            glfwSetWindowShouldClose(window, true);
            return;
        }
        
        // Initialize camera position (e.g., start near Earth/Sun)
        // TODO: Initialize Camera object
        
        // -- Initialize Camera --
        // Spawn just outside the Sun's surface along +Z
        Star sun = stars.get("sun");
        double sunRadiusLy = sun != null ? sun.getRadiusKm() * 1.057e-13 : 0.0; // 1 ly = 9.461e12 km
        double startDistLy = sunRadiusLy + 0.8; // 0.01 ly offset
        camera = new Camera(new Vector3d(0.0, 0.0, startDistLy)); 
        camera.pitch = 0.0f; // Look straight ahead initially
        camera.yaw = -90.0f; // Look towards negative Z
        camera.movementSpeed = 0.1f; // Start slower, maybe 0.1 ly/s
        camera.updateCameraVectors(); // Apply initial pitch/yaw
        System.out.println("Camera Initialized at: " + camera.position);
        System.out.println("Initial Camera Speed: " + camera.movementSpeed + " ly/s");

        // -- Initialize Projection Matrix --
        projectionMatrix = new Matrix4f();
        updateProjectionMatrix();

        // --- SPHERE MESH SETUP ---
        sphereMesh = new SphereMesh(16, 16); // 16x16 is a good balance of detail/performance
        // Create VAO
        sphereVaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(sphereVaoId);
        // Positions VBO
        sphereVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sphereVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, sphereMesh.positions, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        // Normals VBO
        sphereNboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sphereNboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, sphereMesh.normals, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(1);
        // EBO (indices)
        sphereEboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, sphereEboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, sphereMesh.indices, GL15.GL_STATIC_DRAW);
        // Unbind
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // Load star sphere shader
        try {
            starSphereShader = new ShaderProgram(
                "shaders/star_sphere.vert",
                "shaders/star_sphere.frag"
            );
        } catch (Exception e) {
            System.err.println("Failed to load/compile/link star sphere shaders: " + e.getMessage());
            glfwSetWindowShouldClose(window, true);
            return;
        }

        // stb font setup
        try {
            fontBuffer = MemoryUtil.memAlloc(512 * 1024);
            byte[] fontBytes = Files.readAllBytes(Paths.get("src/main/resources/Roboto-Medium.ttf"));
            fontBuffer.put(fontBytes);
            fontBuffer.flip();
            
            // Initialize UI renderer with the font buffer
            uiRenderer = new UiRenderer(fontBuffer);
            
            // We don't need these anymore as UiRenderer handles font rendering
            // cdata = STBTTBakedChar.malloc(96);
            // ByteBuffer bitmap = MemoryUtil.memAlloc(512 * 512);
            // STBTruetype.stbtt_BakeFontBitmap(fontBuffer, 32, bitmap, 512, 512, 32, cdata);
            // fontTextureId = GL11.glGenTextures();
            // GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
            // GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, 512, 512, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, bitmap);
            // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            // MemoryUtil.memFree(bitmap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + e.getMessage());
        }

        lastFrameTime = glfwGetTime();
    }

    private void loop() {
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // Calculate delta time
            double currentTime = glfwGetTime();
            deltaTime = currentTime - lastFrameTime;
            lastFrameTime = currentTime;
            
            // Input processing
            processInput(deltaTime);

            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Get current view matrix from camera
            Matrix4f viewMatrix = camera.getViewMatrix();

            // Render celestial objects
            render(viewMatrix, projectionMatrix);

            // Call a new method: renderSidebarOverlay();
            renderSidebarOverlay();

            // Render the crosshair
            if (uiRenderer != null) {
                uiRenderer.renderCrosshair(windowWidth, windowHeight);
            }

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void processInput(double dt) {
        if (camera == null) return;
        
        // Camera movement with WASD
        float currentSpeed = camera.movementSpeed;
        
        // WASD movement
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            camera.processKeyboard(Camera.CameraMovement.FORWARD, dt);
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            camera.processKeyboard(Camera.CameraMovement.BACKWARD, dt);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            camera.processKeyboard(Camera.CameraMovement.LEFT, dt);
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            camera.processKeyboard(Camera.CameraMovement.RIGHT, dt);
        }

        // Optional: Add Up/Down movement (e.g., Space/C or R/F)
        // if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
        //     camera.processKeyboard(Camera.CameraMovement.UP, currentSpeed * dt);
        // }
        // if (glfwGetKey(window, GLFW_KEY_LEFT_ALT) == GLFW_PRESS) { // Example: Alt for Down
        //     camera.processKeyboard(Camera.CameraMovement.DOWN, currentSpeed * dt);
        // }

        // The sidebar toggle and search mode controls have been moved to the key callback
        // to handle them as events rather than continuous input checks
    }

    private void render(Matrix4f view, Matrix4f projection) {
        starSphereShader.use();
        starSphereShader.setMat4("view", view);
        starSphereShader.setMat4("projection", projection);
        starSphereShader.setVec3("cameraPosView", new Vector3f(0,0,0));

        // Pass camera world position to shader for distance calculations
        starSphereShader.setVec3("cameraPos", new Vector3f((float)camera.position.x, (float)camera.position.y, (float)camera.position.z));

        // Pass window dimensions to shader for minimum star size calculations
        starSphereShader.setFloat("screenWidth", (float)windowWidth);
        starSphereShader.setFloat("screenHeight", (float)windowHeight);

        // --- Render Stars with Proper Blending ---
        glEnable(GL_DEPTH_TEST); // Ensure depth testing is on
        glDepthMask(true);      // Enable depth writing initially for opaque parts

        // Use standard alpha blending for the base rendering pass
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GL30.glBindVertexArray(sphereVaoId);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, sphereEboId);

        // Field of view angle threshold for culling
        double fovAngleCosine = Math.cos(Math.toRadians(75.0)); // Half of 150 degrees
        Vector3f camDir = camera.front;

        // Collect visible stars and sort them by distance (farthest first)
        Map<Double, Star> visibleStars = new TreeMap<>(Collections.reverseOrder()); // Sort farthest first
        for (Star star : stars.values()) {
            double dx = star.getX() - camera.position.x;
            double dy = star.getY() - camera.position.y;
            double dz = star.getZ() - camera.position.z;
            double distanceToStar = Math.sqrt(dx*dx + dy*dy + dz*dz);

            // Basic distance culling
            if (distanceToStar > 1000.0) continue;

            // FOV culling
            Vector3f dirToStar = new Vector3f((float)dx, (float)dy, (float)dz).normalize();
            double dotProduct = dirToStar.dot(camDir);

            if (dotProduct > fovAngleCosine) {
                // Ensure unique key for sorting
                while (visibleStars.containsKey(distanceToStar)) {
                    distanceToStar += 0.000001;
                }
                visibleStars.put(distanceToStar, star);
            }
        }

        // Render stars from farthest to nearest
        for (Map.Entry<Double, Star> entry : visibleStars.entrySet()) {
            double distanceToStar = entry.getKey();
            Star star = entry.getValue();

            // If the star's core should be opaque (based on shader logic proximity)
            // Keep depth writing enabled. Otherwise, disable it for the glow.
            boolean isCloseEnoughForOpaqueCore = distanceToStar < 5.0;
            glDepthMask(isCloseEnoughForOpaqueCore); // Only write depth for close stars

            // Calculate base scale factor
            float baseScale = (float)(star.getRadiusKm() * 1e-7); // Adjusted factor if needed

            // Scale adjustment for visibility
             if (distanceToStar < 100.0) {
                 float distanceFactor = (float)(1.0 - Math.min(distanceToStar / 100.0, 0.99));
                 float minScaleFactor = 1.0f + distanceFactor * 2.0f; // Adjust amplification maybe
                 baseScale = Math.max(baseScale, baseScale * minScaleFactor);
             } else {
                 baseScale = Math.max(baseScale, 0.0005f); // Smaller min size for far stars maybe
             }

            // Create model matrix
            Matrix4f model = new Matrix4f()
                .translate((float)star.getX(), (float)star.getY(), (float)star.getZ())
                .scale(baseScale);

            starSphereShader.setMat4("model", model);

            // Set star properties
            java.awt.Color c = star.getColor();
            Vector3f color = new Vector3f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f);
            starSphereShader.setVec3("starColor", color);
            starSphereShader.setFloat("starDistance", (float)distanceToStar);
            starSphereShader.setFloat("starAbsMag", (float)star.getAbsoluteMagnitude());
            starSphereShader.setFloat("minVisibleSize", 2.0f);
            starSphereShader.setBoolean("ensureVisible", distanceToStar < 100.0);

            // Draw the star
            GL11.glDrawElements(GL11.GL_TRIANGLES, sphereMesh.indexCount, GL11.GL_UNSIGNED_INT, 0);
        }

        // --- Cleanup ---
        glDepthMask(true); // Re-enable depth writing for subsequent rendering (like UI)
        GL30.glBindVertexArray(0);
        starSphereShader.unuse();
    }

    private void renderSidebarOverlay() {
        if (!sidebarOpen && !searchMode) return;
        
        // Render the sidebar background with a more opaque color
        uiRenderer.renderSidebar(windowWidth, windowHeight, sidebarWidth, sidebarOpen);
        
        // Define text colors with improved contrast
        float[] whiteColor = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] yellowColor = {1.0f, 0.9f, 0.0f, 1.0f};  // Slightly darker yellow
        float[] lightBlueColor = {0.7f, 0.85f, 1.0f, 1.0f}; // Adjusted blue
        float[] greenColor = {0.5f, 0.9f, 0.5f, 1.0f}; // Green for habitable stars
        
        // Sidebar content position
        float textX = windowWidth - sidebarWidth + 20;
        float textY = 30;  // Start a bit higher
        
        // Section Headings - use a slightly larger scale for better visibility
        uiRenderer.renderText("Star Info", textX, textY, 1.6f, yellowColor);
        textY += 36;  // More space after heading
        
        // Get focused star
        Star focusedStar = getStarClosestToCenter();
        if (focusedStar != null) {
            // Display name
            uiRenderer.renderText("Name: " + focusedStar.getName(), textX, textY, 1.0f, whiteColor); 
            textY += 24;
            
            // Display Hipparcos ID if available
            if (focusedStar.getHipId() > 0) {
                uiRenderer.renderText("HIP: " + focusedStar.getHipId(), textX, textY, 1.0f, whiteColor);
                textY += 24;
            }
            
            // Display habitability information with green color for habitable stars
            String habitableText = focusedStar.isHabitable() ? "Habitable: Yes" : "Habitable: No";
            float[] habitableColor = focusedStar.isHabitable() ? greenColor : whiteColor;
            uiRenderer.renderText(habitableText, textX, textY, 1.0f, habitableColor);
            textY += 24;
            
            // Display spectral class
            uiRenderer.renderText("Class: " + focusedStar.getSpectralClass(), textX, textY, 1.0f, whiteColor); 
            textY += 24;
            
            // Display distance in light years (converted from parsecs if needed)
            uiRenderer.renderText(String.format("Distance: %.2f ly", focusedStar.getDistanceLy()), 
                                  textX, textY, 1.0f, whiteColor); 
            textY += 24;
            
            // Format mass in scientific notation
            String formattedMass = formatScientificNotation(focusedStar.getMassKg());
            uiRenderer.renderText("Mass: " + formattedMass + " kg", textX, textY, 1.0f, whiteColor);
            textY += 24;
            
            // Display absolute magnitude
            uiRenderer.renderText(String.format("Abs Mag: %.2f", focusedStar.getAbsoluteMagnitude()), 
                                  textX, textY, 1.0f, whiteColor); 
            textY += 24;
            
            // Display galactic coordinates for the new format
            uiRenderer.renderText(String.format("Galactic: (%.2f, %.2f, %.2f) pc", 
                                  focusedStar.getXGalactic(), focusedStar.getYGalactic(), focusedStar.getZGalactic()), 
                                  textX, textY, 1.0f, whiteColor);
            textY += 24;
            
            // Display position in our simulation coordinates (light years)
            uiRenderer.renderText(String.format("Position: (%.2f, %.2f, %.2f) ly", 
                                  focusedStar.getX(), focusedStar.getY(), focusedStar.getZ()), 
                                  textX, textY, 1.0f, whiteColor); 
            textY += 40;
        } else {
            uiRenderer.renderText("No star in focus", textX, textY, 1.0f, whiteColor);
            textY += 40;
        }
        
        // Controls section
        uiRenderer.renderText("Controls", textX, textY, 1.3f, yellowColor);
        textY += 30;
        uiRenderer.renderText("[Tab] Toggle Sidebar", textX, textY, 0.9f, lightBlueColor); 
        textY += 20;
        uiRenderer.renderText("[/] Search Star", textX, textY, 0.9f, lightBlueColor); 
        textY += 20;
        uiRenderer.renderText("[+] Increase Speed", textX, textY, 0.9f, lightBlueColor);
        textY += 20;
        uiRenderer.renderText("[-] Decrease Speed", textX, textY, 0.9f, lightBlueColor);
        textY += 40;
        
        // Status section
        uiRenderer.renderText("Status", textX, textY, 1.3f, yellowColor);
        textY += 30;
        uiRenderer.renderText(String.format("Camera: (%.4f, %.4f, %.4f)", 
                              camera.position.x, camera.position.y, camera.position.z), 
                              textX, textY, 0.9f, whiteColor); 
        textY += 20;
        uiRenderer.renderText(String.format("Speed: %.6f ly/s", camera.movementSpeed), 
                              textX, textY, 0.9f, whiteColor); 
        textY += 40;
        
        // Search section
        if (searchMode) {
            uiRenderer.renderText("Search", textX, textY, 1.3f, yellowColor);
            textY += 30;
            uiRenderer.renderText("Enter name: " + searchInput.toString() + "_", 
                                  textX, textY, 1.0f, whiteColor); 
            textY += 24;
            
            if (searchResult != null) {
                uiRenderer.renderText("Found: " + searchResult.getName(), 
                                      textX, textY, 1.0f, whiteColor); 
                textY += 24;
                uiRenderer.renderText("[Enter] Teleport", textX, textY, 0.9f, lightBlueColor);
            }
        }
    }

    private void renderText(String text, float x, float y, float scale) {
        // This is a placeholder for proper text rendering
        // Will be implemented with shader-based rendering
        // For now we're just using console output
        System.out.println("UI Text: " + text);
    }

    private Star getStarClosestToCenter() {
        Vector3d camPos = camera.position;
        Vector3f camDir = camera.front;
        // Using a narrower angle for more precise selection
        double maxAngle = Math.toRadians(15.0); // Narrower detection cone (was 45 degrees)
        Star closest = null;
        double closestScore = Double.MAX_VALUE;
        
        // Collect all potentially visible stars
        Map<Double, Star> candidateStars = new TreeMap<>();
        
        for (Star star : stars.values()) {
            double dx = star.getX() - camPos.x;
            double dy = star.getY() - camPos.y;
            double dz = star.getZ() - camPos.z;
            double distanceToStar = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            // Skip very distant stars for cursor selection
            if (distanceToStar > 500.0) continue;
            
            Vector3f toStar = new Vector3f((float)dx, (float)dy, (float)dz).normalize();
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, camDir.dot(toStar)))); // Clamp to avoid precision errors
            
            // Store all stars within our selection cone
            if (angle < maxAngle) {
                // Ensure unique key by adding a small random offset for same-distance stars
                while (candidateStars.containsKey(distanceToStar)) {
                    distanceToStar += 0.000001;
                }
                
                candidateStars.put(distanceToStar, star);
            }
        }
        
        // If we have candidates, prioritize closer stars and those more centered in view
        if (!candidateStars.isEmpty()) {
            // First, check the closest stars (up to 5) with priority
            int count = 0;
            
            for (Map.Entry<Double, Star> entry : candidateStars.entrySet()) {
                count++;
                Star star = entry.getValue();
                double distanceToStar = entry.getKey();
                
                // Skip extremely far stars even within the cone
                if (distanceToStar > 100.0) continue;
                
                double dx = star.getX() - camPos.x;
                double dy = star.getY() - camPos.y;
                double dz = star.getZ() - camPos.z;
                Vector3f toStar = new Vector3f((float)dx, (float)dy, (float)dz).normalize();
                double angle = Math.acos(Math.max(-1.0, Math.min(1.0, camDir.dot(toStar))));
                
                // Calculate a combined score that prioritizes:
                // 1. Stars closer to center of view (smaller angle)
                // 2. Stars closer to camera (smaller distance)
                // Weight the angle more for stars that are close to each other
                double distanceWeight = Math.min(distanceToStar / 10.0, 1.0); // 0-1 range
                double angleWeight = angle / maxAngle; // 0-1 range
                
                // Combined score (lower is better)
                double score = angleWeight * 0.7 + distanceWeight * 0.3;
                
                // For very close stars (< 5ly), prioritize them even more
                if (distanceToStar < 5.0) {
                    score *= 0.5; // Half the score makes it more likely to be selected
                }
                
                if (score < closestScore) {
                    closest = star;
                    closestScore = score;
                }
                
                // Check only the closest few stars if there are many candidates
                if (count >= 10) break;
            }
            
            // If no good candidates from closest stars, just pick the nearest
            if (closest == null && !candidateStars.isEmpty()) {
                closest = candidateStars.values().iterator().next();
            }
        }
        
        return closest;
    }

    private void cleanup() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // Cleanup UI renderer
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        
        // Cleanup OpenGL resources
        if (starSphereShader != null) starSphereShader.cleanup();
        if (sphereVboId != 0) GL15.glDeleteBuffers(sphereVboId);
        if (sphereNboId != 0) GL15.glDeleteBuffers(sphereNboId);
        if (sphereEboId != 0) GL15.glDeleteBuffers(sphereEboId);
        if (sphereVaoId != 0) GL30.glDeleteVertexArrays(sphereVaoId);
        
        // Free memory
        if (fontBuffer != null) {
            MemoryUtil.memFree(fontBuffer);
        }
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    // Helper to update projection matrix (called on init and resize/zoom)
    private void updateProjectionMatrix() {
        if (windowHeight > 0 && camera != null) {
             float aspectRatio = (float) windowWidth / windowHeight;
             projectionMatrix.identity().perspective((float)Math.toRadians(camera.zoom),
                                                     aspectRatio, 
                                                     NEAR_PLANE, 
                                                     FAR_PLANE);
        }
    }

    // Helper method to format the mass in scientific notation
    private String formatScientificNotation(double value) {
        if (value == 0) return "0";
        
        int exp = (int) Math.floor(Math.log10(Math.abs(value)));
        double mantissa = value / Math.pow(10, exp);
        
        // Round to 2 decimal places
        mantissa = Math.round(mantissa * 100) / 100.0;
        
        return String.format("%.2f×10^%d", mantissa, exp);
    }

    // Helper method to check if string can be parsed as numeric value
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Detects and removes overlapping stars, keeping the smaller one
     * when stars are found to be overlapping each other.
     */
    private void removeOverlappingStars() {
        System.out.println("Starting overlap removal with " + stars.size() + " stars");
        
        // Only select stars which are duplicates at exactly the same position
        Set<String> starsToRemove = new HashSet<>();
        Map<String, List<Star>> positionMap = new HashMap<>();
        
        // First group stars by their integer position coordinates for quick filtering
        // This creates spatial buckets of stars that are in the same general area
        for (Star star : stars.values()) {
            // Create a bucket key based on integer position (rough grouping)
            String posKey = (int)star.getX() + "," + (int)star.getY() + "," + (int)star.getZ();
            
            if (!positionMap.containsKey(posKey)) {
                positionMap.put(posKey, new ArrayList<>());
            }
            positionMap.get(posKey).add(star);
        }
        
        // Now only check for overlaps within each position bucket
        for (List<Star> bucket : positionMap.values()) {
            // Skip tiny buckets (1 or 0 stars)
            if (bucket.size() <= 1) continue;
            
            // Check each pair in the bucket (much smaller number of comparisons)
            for (int i = 0; i < bucket.size(); i++) {
                Star star1 = bucket.get(i);
                // Skip if already marked for removal
                if (starsToRemove.contains(star1.getName())) continue;
                
                for (int j = i+1; j < bucket.size(); j++) {
                    Star star2 = bucket.get(j);
                    // Skip if already marked for removal
                    if (starsToRemove.contains(star2.getName())) continue;
                    
                    // Calculate 3D distance between the stars
                    double dx = star1.getX() - star2.getX();
                    double dy = star1.getY() - star2.getY();
                    double dz = star1.getZ() - star2.getZ();
                    double distanceBetween = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    
                    // Use a simpler check for overlap - if they're practically at the same position
                    // or if one is inside the other
                    double kmToLy = 1.057e-13; // Conversion factor: kilometers to light-years
                    double radius1_ly = star1.getRadiusKm() * kmToLy;
                    double radius2_ly = star2.getRadiusKm() * kmToLy;

                    // Check if the center of one star is inside the radius of the other
                    boolean overlap = (distanceBetween < radius1_ly) || (distanceBetween < radius2_ly);

                    // If stars overlap based on this condition
                    if (overlap) {
                        // Mark the larger star for removal
                        if (star1.getRadiusKm() > star2.getRadiusKm()) {
                            starsToRemove.add(star1.getName());
                            // Since star1 is removed, break this inner loop for star1
                            break;
                        } else {
                            starsToRemove.add(star2.getName());
                            // star2 is marked, continue checking star1 against others in the bucket
                        }
                    }
                }
            }
        }
        
        // Remove the identified stars
        int count = 0;
        for (String starName : starsToRemove) {
            stars.remove(starName);
            count++;
            
            // Garbage collect if we've removed a lot of stars
            if (count % 1000 == 0) {
                System.gc(); // Suggest garbage collection
            }
        }
        
        System.out.println("Removed " + starsToRemove.size() + " overlapping stars (larger ones).");
        
        // Force garbage collection to reclaim memory
        positionMap.clear();
        starsToRemove.clear();
        System.gc();
    }

    public static void main(String[] args) {
        // Ensure CSV files are in the right place or adjust paths in init()
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        System.out.println("Attempting to load data from default paths: stars.csv, planets.csv");
        
        new UniverseSim().run();
    }
} 