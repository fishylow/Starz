package com.universe;

import org.joml.*;

public class Camera {

    // Camera attributes
    public Vector3d position;
    public Vector3f front;
    public Vector3f up;
    public Vector3f right;
    public Vector3f worldUp;

    // Euler Angles
    public float yaw;
    public float pitch;

    // Camera options
    public float movementSpeed; // Units per second (e.g., light-years per second)
    public float mouseSensitivity;
    public float zoom; // Field of view

    // Initial values
    private static final float YAW         = -90.0f; // Pointing down negative Z initially
    private static final float PITCH       =  0.0f;
    private static final float SPEED       =  1.0f; // Start with 1 ly/sec movement
    private static final float SENSITIVITY =  0.1f;
    private static final float ZOOM        =  45.0f; // Default FOV

    // Constructor with vector
    public Camera(Vector3d position) {
        this(position, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    // Constructor with scalar values
    public Camera(double posX, double posY, double posZ) {
         this(new Vector3d(posX, posY, posZ), new Vector3f(0.0f, 1.0f, 0.0f));
    }
    
    // Main constructor
    public Camera(Vector3d position, Vector3f up) {
        this.position = new Vector3d(position); // Use double for position due to large distances
        this.worldUp = new Vector3f(up);
        this.yaw = YAW;
        this.pitch = PITCH;
        this.front = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        
        this.movementSpeed = SPEED;
        this.mouseSensitivity = SENSITIVITY;
        this.zoom = ZOOM;
        updateCameraVectors();
    }

    // Calculates the front vector from the Camera's (updated) Euler Angles
    public void updateCameraVectors() {
        // Calculate the new Front vector
        Vector3f newFront = new Vector3f();
        newFront.x = (float)(java.lang.Math.cos(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        newFront.y = (float)java.lang.Math.sin(java.lang.Math.toRadians(pitch));
        newFront.z = (float)(java.lang.Math.sin(java.lang.Math.toRadians(yaw)) * java.lang.Math.cos(java.lang.Math.toRadians(pitch)));
        this.front = newFront.normalize();
        // Also re-calculate the Right and Up vector
        this.right = new Vector3f(this.front).cross(this.worldUp).normalize(); 
        this.up    = new Vector3f(this.right).cross(this.front).normalize();
    }

    // Returns the view matrix calculated using Euler Angles and the LookAt Matrix
    public Matrix4f getViewMatrix() {
        // Important: JOML's lookAt uses a target POINT, not a direction vector.
        // So we calculate the target point as position + front.
        // Need to convert position (double) to float for matrix math.
        Vector3f positionF = new Vector3f((float)position.x, (float)position.y, (float)position.z);
        Vector3f target = new Vector3f(positionF).add(front);
        
        // Using the float version of position for the view matrix
        return new Matrix4f().lookAt(positionF, target, up);
    }
    
    // Overload for cases where double precision view might be needed (requires careful handling in shaders)
    // public Matrix4d getViewMatrixDouble() {
    //     Vector3d target = new Vector3d(position).add(new Vector3d(front.x, front.y, front.z));
    //     return new Matrix4d().lookAt(position, target, new Vector3d(up.x, up.y, up.z));
    // }

    // Processes input received from any keyboard-like input system.
    // Accepts input parameter in the form of camera defined ENUM (to abstract it from windowing systems)
    public void processKeyboard(CameraMovement direction, double deltaTime) {
        double velocity = movementSpeed * deltaTime;
        
        // Note: Calculations done in double precision for position
        Vector3d frontD = new Vector3d(front.x, front.y, front.z); // Convert direction to double for movement calc
        Vector3d rightD = new Vector3d(right.x, right.y, right.z);
        Vector3d upD    = new Vector3d(up.x, up.y, up.z); // Use camera's up for vertical movement
        
        if (direction == CameraMovement.FORWARD) {
            position.add(new Vector3d(frontD).mul(velocity));
        }
        if (direction == CameraMovement.BACKWARD) {
            position.sub(new Vector3d(frontD).mul(velocity));
        }
        if (direction == CameraMovement.LEFT) {
            position.sub(new Vector3d(rightD).mul(velocity));
        }
        if (direction == CameraMovement.RIGHT) {
            position.add(new Vector3d(rightD).mul(velocity));
        }
         if (direction == CameraMovement.UP) { // Use camera's local UP
             position.add(new Vector3d(upD).mul(velocity));
         }
         if (direction == CameraMovement.DOWN) { // Use camera's local DOWN
             position.sub(new Vector3d(upD).mul(velocity));
         }
    }

    // Processes input received from a mouse input system.
    // Expects the offset value in both the x and y direction.
    public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw   += xoffset;
        pitch += yoffset;

        // Make sure that when pitch is out of bounds, screen doesn't get flipped
        if (constrainPitch) {
            if (pitch > 89.0f) {
                pitch = 89.0f;
            }
            if (pitch < -89.0f) {
                pitch = -89.0f;
            }
        }

        // Update Front, Right and Up Vectors using the updated Euler angles
        updateCameraVectors();
    }

    // Processes input received from a mouse scroll-wheel event. 
    // Only requires input on the vertical wheel-axis (used for zoom/FOV)
    public void processMouseScroll(float yoffset) {
        zoom -= yoffset;
        if (zoom < 1.0f) {
            zoom = 1.0f;
        }
        if (zoom > 120.0f) { // Wider FOV limit
            zoom = 120.0f;
        }
    }
    
    // Adjust camera movement speed - true to increase, false to decrease
    public void adjustSpeed(boolean increase) {
        if (increase) {
            // Double the movement speed
            movementSpeed *= 1.5f;
            // Cap the maximum speed to avoid going too fast
            if (movementSpeed > 100.0f) {
                movementSpeed = 100.0f;
            }
        } else {
            // Halve the movement speed
            movementSpeed /= 1.5f;
            // Set a minimum speed to avoid getting stuck
            if (movementSpeed < 0.01f) {
                movementSpeed = 0.01f;
            }
        }
    }

    // Define the directions for keyboard processing
    public enum CameraMovement {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }
} 