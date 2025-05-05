package com.universe;

public abstract class CelestialBody {
    protected String name;
    // Using double for coordinates is essential for astronomical scales
    protected double x, y, z; // Position in Cartesian coordinates (light-years from Sol)
    protected double massKg; // Store mass consistently in kg
    protected double radiusKm; // Store radius consistently in km

    public CelestialBody(String name) {
        this.name = name;
    }

    // Getters
    public String getName() { return name; }
    public double getMassKg() { return massKg; }
    public double getRadiusKm() { return radiusKm; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
 
    // Position is calculated and set by subclasses
    protected void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Abstract methods to be implemented by subclasses
    protected abstract void calculatePosition();
    // Method to estimate radius based on available data (like spectral class)
    protected abstract void calculateRadius();
} 