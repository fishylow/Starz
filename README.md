# Universe Simulation

A 3D universe simulation application built with Java, LWJGL 3, and JOML. It visualizes stars based on their real life data.

## Features

*   **3D Navigation:** Explore the universe using WASD, Space/LCtrl, and mouse controls.
*   **Star Visualization:** Renders stars based on spectral type, position, and magnitude from `stars.csv`.
*   **Data Loading:** Loads star and basic planet data from `stars.csv` and `planets.csv`.
*   **Star Information:** Displays information about the star currently in focus (closest to the center of the view) in a sidebar.
*   **Search Functionality:** Search for stars by name or Hipparcos ID and teleport to them.
*   **Visual Effects:** Includes basic bloom and glow effects for stars.
*   **Crosshair:** A simple dot crosshair to aid aiming.
*   **Performance Optimizations:** Includes view frustum culling and spatial partitioning for overlap checks.

## Requirements

*   **Java:** JDK 11 or later.
*   **Maven:** Apache Maven 3.6 or later (for building).
*   **Graphics Card:** OpenGL 3.3 compatible graphics card.

## Building

This project uses Maven. To build the project and create an executable JAR file:

1.  Ensure you have Java JDK 11+ and Maven installed and configured in your system's PATH.
2.  Open a terminal or command prompt in the project's root directory (where `pom.xml` is located).
3.  Run the following Maven command:
    ```bash
    mvn clean package
    ```
4.  This will compile the code and create an executable JAR file in the `target/` directory (e.g., `target/universe-sim-1.0-SNAPSHOT.jar`).

## Running

After building the project:

1.  Navigate to the `target/` directory in your terminal.
2.  Run the JAR file using Java:
    ```bash
    java -jar universe-sim-1.0-SNAPSHOT.jar
    ```
    *(Replace `universe-sim-1.0-SNAPSHOT.jar` with the actual JAR file name if it differs)*

**Note:** Ensure the `stars.csv` and `planets.csv` files are present in the same directory where you run the JAR file, or update the file paths in `src/main/java/com/universe/UniverseSim.java` if needed. The application currently loads them relative to the execution directory.

## Controls

*   **Mouse:** Look around.
*   **W, A, S, D:** Move camera forward, left, backward, right.
*   **Space:** Move camera up.
*   **Left Ctrl:** Move camera down.
*   **Scroll Wheel:** Zoom in/out (adjust Field of View).
*   **+ / -:** Increase/Decrease camera movement speed.
*   **Tab:** Toggle the information sidebar.
*   **/**: Enter/Exit star search mode.
    *   Type star name or HIP ID (e.g., `hip11767`)
    *   **Enter:** Search and teleport to the star.
    *   **Backspace:** Delete last character.
*   **Esc:** Exit the application. 