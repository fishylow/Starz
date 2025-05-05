package com.universe;

public class Units {
    public static final double AU_TO_KM = 149597870.7;
    public static final double LY_TO_KM = 9.461e12;
    public static final double AU_TO_LY = AU_TO_KM / LY_TO_KM; // Approx 1.58125e-5

    public static final double EARTH_MASS_TO_KG = 5.972e24;
    public static final double SOLAR_MASS_TO_KG = 1.989e30;
    public static final double EARTH_MASS_TO_SOLAR_MASS = EARTH_MASS_TO_KG / SOLAR_MASS_TO_KG; // Approx 3.003e-6

    public static final double EARTH_RADIUS_TO_KM = 6371.0;
    public static final double SOLAR_RADIUS_KM = 696340.0;

    // Potentially add solar radius, etc. if needed later
} 