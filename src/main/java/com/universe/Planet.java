package com.universe;

public class Planet extends CelestialBody {
    private Star hostStar;
    private double distanceFromStarAU;
    private boolean hasRings;

    public Planet(String name, Star hostStar, double distanceFromStarAU, double massEarth, double radiusEarth, boolean hasRings) {
        super(name);
        this.hostStar = hostStar;
        this.distanceFromStarAU = distanceFromStarAU;
        // Convert mass/radius to consistent units (kg, km)
        this.massKg = massEarth * Units.EARTH_MASS_TO_KG;
        this.radiusKm = radiusEarth * Units.EARTH_RADIUS_TO_KM;
        this.hasRings = hasRings;

        calculatePosition();
        // Radius is directly given/calculated, so no separate call needed here
    }

    @Override
    protected void calculateRadius() {
        // Radius is directly calculated from input Earth radii in the constructor.
        // This method is required by the abstract class but doesn't need to do anything here.
    }

    @Override
    protected void calculatePosition() {
        if (hostStar == null) {
            System.err.println("Cannot calculate position for planet " + name + ": host star is null.");
            setPosition(0, 0, 0); // Default to origin
            return;
        }

        // Simple initial placement: Position planet along the host star's +X axis
        // relative to the star in the simulation's coordinate system (light-years).
        double distLy = distanceFromStarAU * Units.AU_TO_LY;

        double starX = hostStar.getX();
        double starY = hostStar.getY();
        double starZ = hostStar.getZ();

        // Place it offset from the star along the global X axis for simplicity.
        // A more realistic model would use orbital elements.
        setPosition(starX + distLy, starY, starZ);
    }

    // Getters
    public Star getHostStar() { return hostStar; }
    public double getDistanceFromStarAU() { return distanceFromStarAU; }
    public boolean hasRings() { return hasRings; }
} 