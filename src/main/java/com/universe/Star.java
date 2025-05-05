package com.universe;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class Star extends CelestialBody {
    // New fields for updated data format
    private int hipId;           // Hipparcos catalog number
    private boolean habitable;   // Is the star potentially habitable?
    private String spectralClass; // e.g., G2V, M5.5Ve (renamed from stellarClass for clarity)
    private double distanceParsecs;
    private double distanceLy;   // Derived from parsecs
    private double xGalactic;    // Galactic X coordinate (parsecs)
    private double yGalactic;    // Galactic Y coordinate (parsecs)
    private double zGalactic;    // Galactic Z coordinate (parsecs)
    private double absoluteMagnitude;

    // Enhanced color map with more vibrant, saturated colors for stars
    private static final Map<Character, Color> SPECTRAL_COLOR_MAP = new HashMap<>();
    static {
        // Main sequence stars (O,B,A,F,G,K,M) - More vibrant and saturated
        SPECTRAL_COLOR_MAP.put('O', new Color(155, 180, 255));  // Intense blue
        SPECTRAL_COLOR_MAP.put('B', new Color(170, 195, 255));  // Bright blue-white
        SPECTRAL_COLOR_MAP.put('A', new Color(210, 225, 255));  // White with stronger blue tint
        SPECTRAL_COLOR_MAP.put('F', new Color(255, 245, 230));  // Creamy white
        SPECTRAL_COLOR_MAP.put('G', new Color(255, 230, 130));  // Vibrant yellow
        SPECTRAL_COLOR_MAP.put('K', new Color(255, 190, 100));  // Strong orange
        SPECTRAL_COLOR_MAP.put('M', new Color(255, 140, 90));   // Rich red-orange
        
        // Brown dwarfs - Distinct, deeper colors
        SPECTRAL_COLOR_MAP.put('L', new Color(230, 100, 50));   // Deep red-brown
        SPECTRAL_COLOR_MAP.put('T', new Color(200, 80, 40));    // Rich magenta-brown
        SPECTRAL_COLOR_MAP.put('Y', new Color(170, 70, 40));    // Dark, cool brown
        
        // Other types - Enhanced colors
        SPECTRAL_COLOR_MAP.put('W', new Color(120, 170, 255));  // Wolf-Rayet (strong blue)
        SPECTRAL_COLOR_MAP.put('C', new Color(255, 100, 100));  // Carbon stars (vivid red)
        SPECTRAL_COLOR_MAP.put('S', new Color(255, 120, 90));   // S-type stars (vibrant orange-red)
        SPECTRAL_COLOR_MAP.put('D', new Color(200, 210, 255));  // White dwarfs (pale blue-white)
        SPECTRAL_COLOR_MAP.put('Q', new Color(170, 180, 200));  // Neutron stars (cool blue-gray)
        SPECTRAL_COLOR_MAP.put('X', new Color(30, 30, 40));     // Black holes (very dark near-black)
        SPECTRAL_COLOR_MAP.put('P', new Color(180, 190, 255));  // Planetary nebulae (maintained)
        SPECTRAL_COLOR_MAP.put('N', new Color(240, 170, 130));  // Carbon stars (N - maintained)
        SPECTRAL_COLOR_MAP.put('R', new Color(255, 110, 90));   // Carbon stars (R - maintained)
    }

    // Rough estimates for main sequence (V) radius in Solar Radii
    private static final Map<Character, Double> SPECTRAL_RADIUS_MAP_V = new HashMap<>();
    static {
        SPECTRAL_RADIUS_MAP_V.put('O', 15.0);  // 7-20+ solar radii (highly variable)
        SPECTRAL_RADIUS_MAP_V.put('B', 7.0);   // 3.5-10 solar radii
        SPECTRAL_RADIUS_MAP_V.put('A', 1.8);   // 1.5-2.5 solar radii
        SPECTRAL_RADIUS_MAP_V.put('F', 1.3);   // 1.1-1.5 solar radii
        SPECTRAL_RADIUS_MAP_V.put('G', 1.0);   // Like Sun (0.9-1.1 solar radii)
        SPECTRAL_RADIUS_MAP_V.put('K', 0.8);   // 0.7-0.9 solar radii
        SPECTRAL_RADIUS_MAP_V.put('M', 0.5);   // 0.1-0.7 solar radii
        
        // Brown dwarfs and smaller objects
        SPECTRAL_RADIUS_MAP_V.put('L', 0.1);   // ~Jupiter sized
        SPECTRAL_RADIUS_MAP_V.put('T', 0.08);  // Smaller than Jupiter
        SPECTRAL_RADIUS_MAP_V.put('Y', 0.07);  // Smallest brown dwarfs
        
        // Other types
        SPECTRAL_RADIUS_MAP_V.put('W', 12.0);  // Wolf-Rayet stars (variable)
        SPECTRAL_RADIUS_MAP_V.put('C', 100.0); // Carbon stars (often giants)
        SPECTRAL_RADIUS_MAP_V.put('S', 80.0);  // S-type stars (often giants)
        SPECTRAL_RADIUS_MAP_V.put('D', 0.01);  // White dwarfs (Earth-sized)
        SPECTRAL_RADIUS_MAP_V.put('Q', 0.0001); // Neutron stars (city-sized)
        SPECTRAL_RADIUS_MAP_V.put('X', 0.0001); // Black holes (event horizon)
        SPECTRAL_RADIUS_MAP_V.put('N', 90.0);  // Carbon stars (old notation)
        SPECTRAL_RADIUS_MAP_V.put('R', 70.0);  // Carbon stars (old notation)
    }

    // Rough estimates for main sequence (V) mass in Solar Masses
    private static final Map<Character, Double> SPECTRAL_MASS_MAP_V = new HashMap<>();
    static {
        SPECTRAL_MASS_MAP_V.put('O', 40.0);  // 20-100+ solar masses
        SPECTRAL_MASS_MAP_V.put('B', 10.0);  // 3-20 solar masses
        SPECTRAL_MASS_MAP_V.put('A', 2.5);   // 1.5-3 solar masses
        SPECTRAL_MASS_MAP_V.put('F', 1.5);   // 1.1-1.8 solar masses
        SPECTRAL_MASS_MAP_V.put('G', 1.0);   // 0.8-1.2 solar masses
        SPECTRAL_MASS_MAP_V.put('K', 0.7);   // 0.5-0.8 solar masses
        SPECTRAL_MASS_MAP_V.put('M', 0.3);   // 0.1-0.5 solar masses
        
        // Brown dwarfs and smaller
        SPECTRAL_MASS_MAP_V.put('L', 0.08);  // 0.06-0.09 solar masses
        SPECTRAL_MASS_MAP_V.put('T', 0.05);  // 0.03-0.06 solar masses
        SPECTRAL_MASS_MAP_V.put('Y', 0.02);  // <0.03 solar masses
        
        // Other types
        SPECTRAL_MASS_MAP_V.put('W', 25.0);  // Wolf-Rayet stars (variable)
        SPECTRAL_MASS_MAP_V.put('C', 3.0);   // Carbon stars (variable)
        SPECTRAL_MASS_MAP_V.put('S', 2.5);   // S-type stars (variable)
        SPECTRAL_MASS_MAP_V.put('D', 0.7);   // White dwarfs
        SPECTRAL_MASS_MAP_V.put('Q', 1.4);   // Neutron stars
        SPECTRAL_MASS_MAP_V.put('X', 10.0);  // Black holes (variable)
        SPECTRAL_MASS_MAP_V.put('N', 2.8);   // Carbon stars (old notation)
        SPECTRAL_MASS_MAP_V.put('R', 2.5);   // Carbon stars (old notation)
    }

    // Constructor for the new data format
    public Star(int hipId, boolean habitable, String name, String spectralClass, 
                double distanceParsecs, double xGalactic, double yGalactic, double zGalactic, 
                double absoluteMagnitude) {
        super(name);
        this.hipId = hipId;
        this.habitable = habitable;
        this.spectralClass = spectralClass != null ? spectralClass.trim() : "";
        this.distanceParsecs = distanceParsecs;
        this.distanceLy = distanceParsecs * 3.26156; // Convert parsecs to light years
        this.xGalactic = xGalactic;
        this.yGalactic = yGalactic;
        this.zGalactic = zGalactic;
        this.absoluteMagnitude = absoluteMagnitude;

        // Infer mass from spectral class if not explicitly provided
        if (this.massKg <= 0) {
            estimateMassFromSpectralClass();
        }
        
        calculatePosition();
        calculateRadius();
    }

    // For backward compatibility
    public Star(String name, String spectralClass, double distanceLy, String raStr, String decStr, double massSolar, double absoluteMagnitude) {
        super(name);
        this.hipId = 0;
        this.habitable = false;
        this.spectralClass = spectralClass != null ? spectralClass.trim() : "";
        this.distanceLy = distanceLy;
        this.distanceParsecs = distanceLy / 3.26156; // Convert light years to parsecs
        this.xGalactic = 0;
        this.yGalactic = 0;
        this.zGalactic = 0;
        this.absoluteMagnitude = absoluteMagnitude;
        this.massKg = massSolar * Units.SOLAR_MASS_TO_KG;

        // Use old position calculation method for backward compatibility
        calculatePositionFromEquatorial(raStr, decStr);
        calculateRadius();
    }

    // Estimate mass based on spectral class
    private void estimateMassFromSpectralClass() {
        // Default to G-type (Sun-like) mass if class is unknown
        if (spectralClass == null || spectralClass.isEmpty()) {
            this.massKg = Units.SOLAR_MASS_TO_KG; // 1 solar mass
            return;
        }

        // Extract the spectral type (first character)
        char spectralType = Character.toUpperCase(spectralClass.charAt(0));
        
        // Get base mass from map
        double massSolar = SPECTRAL_MASS_MAP_V.getOrDefault(spectralType, 1.0);
        
        // Adjust based on subtype if present
        if (spectralClass.length() > 1 && Character.isDigit(spectralClass.charAt(1))) {
            int subtype = Character.getNumericValue(spectralClass.charAt(1));
            
            switch (spectralType) {
                case 'O':
                    massSolar = 40.0 - subtype * 3.0; // O0=40, O9=13
                    break;
                case 'B':
                    massSolar = 18.0 - subtype * 1.5; // B0=18, B9=4.5
                    break;
                case 'A':
                    massSolar = 3.2 - subtype * 0.18; // A0=3.2, A9=1.5
                    break;
                case 'F':
                    massSolar = 1.7 - subtype * 0.07; // F0=1.7, F9=1.0
                    break;
                case 'G':
                    massSolar = 1.1 - subtype * 0.04; // G0=1.1, G9=0.75
                    break;
                case 'K':
                    massSolar = 0.8 - subtype * 0.04; // K0=0.8, K9=0.45
                    break;
                case 'M':
                    massSolar = 0.5 - subtype * 0.04; // M0=0.5, M9=0.08
                    break;
                // Others use the default mass from the map
            }
        }
        
        // Adjust based on luminosity class if present
        String luminosityClass = getLuminosityClass();
        if (!luminosityClass.isEmpty()) {
            switch (luminosityClass) {
                case "I":   // Supergiants
                    massSolar *= 15.0;
                    break;
                case "II":  // Bright giants
                    massSolar *= 9.0;
                    break;
                case "III": // Giants
                    massSolar *= 5.0;
                    break;
                case "IV":  // Subgiants
                    massSolar *= 2.0;
                    break;
                // V (Main Sequence) uses default
            }
        }
        
        // Convert to kg
        this.massKg = massSolar * Units.SOLAR_MASS_TO_KG;
    }

    @Override
    protected void calculatePosition() {
        // For the Sun (special case)
        if (this.name.equalsIgnoreCase("Sun")) {
            setPosition(0, 0, 0);
            return;
        }
        
        // Use galactic coordinates directly
        // Convert parsecs to light years for consistency
        double xLy = xGalactic * 3.26156;
        double yLy = yGalactic * 3.26156;
        double zLy = zGalactic * 3.26156;
        
        // Center the coordinate system on the Sun
        setPosition(xLy, yLy, zLy);
    }
    
    // Legacy method for backward compatibility
    private void calculatePositionFromEquatorial(String raStr, String decStr) {
        // Handle the Sun explicitly at the origin
        if (this.name.equalsIgnoreCase("Sun")) {
            setPosition(0, 0, 0);
            return;
        }
        
        // Handle cases with missing coordinates
        if (raStr == null || decStr == null || raStr.equals("0") || decStr.equals("0") || this.distanceLy == 0) {
            System.err.println("Warning: Using default position (0,0,0) for star " + name + " due to missing coordinate data.");
            setPosition(0, 0, 0);
            return;
        }

        try {
            double raRad = parseRa(raStr);
            double decRad = parseDec(decStr);

            // Standard Equatorial to Cartesian conversion
            double x = distanceLy * Math.cos(decRad) * Math.cos(raRad);
            double y = distanceLy * Math.cos(decRad) * Math.sin(raRad);
            double z = distanceLy * Math.sin(decRad);
            setPosition(x, y, z);
        } catch (Exception e) {
            System.err.println("Error parsing coordinates for star " + name + ": " + raStr + " / " + decStr + " - " + e.getMessage());
            setPosition(0, 0, 0); // Default to origin on error
        }
    }

    @Override
    protected void calculateRadius() {
        // Special case for the Sun
        if (this.name.equalsIgnoreCase("Sun")) {
            this.radiusKm = Units.SOLAR_RADIUS_KM;
            return;
        }
        
        // Basic radius estimation from spectral class
        char spectralType = spectralClass.isEmpty() ? 'G' : Character.toUpperCase(spectralClass.charAt(0));
        double radiusSolar = SPECTRAL_RADIUS_MAP_V.getOrDefault(spectralType, 1.0);

        // Refine based on subtype if present
        if (spectralClass.length() > 1 && Character.isDigit(spectralClass.charAt(1))) {
            int subtype = Character.getNumericValue(spectralClass.charAt(1));
            
            switch (spectralType) {
                case 'O': 
                    radiusSolar = 20.0 - subtype * 1.0; // O0=20, O9=11
                    break;
                case 'B':
                    radiusSolar = 10.0 - subtype * 0.6; // B0=10, B9=4.6
                    break;
                case 'A':
                    radiusSolar = 2.5 - subtype * 0.08; // A0=2.5, A9=1.8
                    break;
                case 'F':
                    radiusSolar = 1.6 - subtype * 0.05; // F0=1.6, F9=1.15
                    break;
                case 'G':
                    radiusSolar = 1.1 - subtype * 0.03; // G0=1.1, G9=0.85
                    break;
                case 'K':
                    radiusSolar = 0.85 - subtype * 0.04; // K0=0.85, K9=0.5
                    break;
                case 'M':
                    radiusSolar = 0.5 - subtype * 0.03; // M0=0.5, M9=0.1
                    break;
                // Others use the default radius from the map
            }
        }

        // Adjust for luminosity class
        String luminosityClass = getLuminosityClass();
        if (!luminosityClass.isEmpty()) {
            switch (luminosityClass) {
                case "I":   // Supergiants - reduce scale to avoid ridiculously large stars
                    radiusSolar *= 20.0; // Was 100.0, reduced to be visually manageable
                    break;
                case "II":  // Bright giants
                    radiusSolar *= 10.0; // Was 50.0
                    break;
                case "III": // Giants
                    radiusSolar *= 6.0;  // Was 15.0
                    break;
                case "IV":  // Subgiants
                    radiusSolar *= 2.0;  // Was 3.0
                    break;
                // V (Main Sequence) uses default
            }
        }

        // Cap the maximum radius to avoid absurdly large stars
        // 25 solar radii is still visually impressive but not overwhelming
        double maxRadiusSolar = 25.0;
        radiusSolar = Math.min(radiusSolar, maxRadiusSolar);
        
        // Refine based on mass for main sequence stars
        if (luminosityClass.equals("V") && massKg > 0) {
            double massInSolar = massKg / Units.SOLAR_MASS_TO_KG;
            if (massInSolar > 0.1 && massInSolar < 2.0) {
                // Radius ~ M^0.8 for M < 1 M_sun, ~M^0.57 for M > 1 M_sun
                radiusSolar = (massInSolar < 1.0) ? Math.pow(massInSolar, 0.8) : Math.pow(massInSolar, 0.57);
            } else if (massInSolar >= 2.0) {
                // More massive stars - radius increases more slowly with mass
                radiusSolar = Math.pow(massInSolar, 0.5);
            }
        }

        this.radiusKm = radiusSolar * Units.SOLAR_RADIUS_KM;
    }

    // Extract luminosity class from spectral class string
    private String getLuminosityClass() {
        if (spectralClass == null || spectralClass.isEmpty()) {
            return "";
        }
        
        // Look for Roman numerals at the end and handle special cases
        // Case insensitive check for better matching
        String upperClass = spectralClass.toUpperCase();
        
        // Check for specific luminosity class indicators
        if (upperClass.contains(" I")) {
            if (upperClass.contains(" III")) return "III";
            else if (upperClass.contains(" II")) return "II";
            else return "I";
        } else if (upperClass.contains(" IV")) {
            return "IV";
        } else if (upperClass.contains(" V")) {
            return "V";
        } else if (upperClass.contains(" VI")) {
            return "VI"; // Subdwarfs
        } else if (upperClass.contains(" VII")) {
            return "VII"; // White dwarfs in some classification systems
        }
        
        // Try to match at the end of the string if not found with space prefix
        if (upperClass.endsWith("III")) return "III";
        else if (upperClass.endsWith("II")) return "II";
        else if (upperClass.endsWith("IV")) return "IV";
        else if (upperClass.endsWith("VI")) return "VI";
        else if (upperClass.endsWith("VII")) return "VII";
        else if (upperClass.endsWith("I")) return "I";
        else if (upperClass.endsWith("V")) return "V";
        
        // If no roman numeral found, we need smarter detection
        // Check for dwarf/giant indicators in text
        if (upperClass.contains("DWARF") || upperClass.contains("D")) {
            return "V"; // Main sequence
        } else if (upperClass.contains("GIANT") || upperClass.contains("G")) {
            return "III"; // Giant
        } else if (upperClass.contains("SUPERGIANT") || upperClass.contains("SG")) {
            return "I"; // Supergiant
        }
        
        // If no clear indicator, assume main sequence for most stars
        return "V";
    }

    // Parses HH:MM:SS.ss into radians (for backward compatibility)
    private double parseRa(String raStr) {
        String[] parts = raStr.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid RA format: " + raStr);
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        double totalHours = hours + minutes / 60.0 + seconds / 3600.0;
        if (totalHours < 0 || totalHours >= 24) throw new IllegalArgumentException("RA hours out of range [0, 24): " + totalHours);
        return Math.toRadians(totalHours * 15.0); // 1 hour = 15 degrees
    }

    // Parses (+/-)DD:MM:SS.ss into radians (for backward compatibility)
    private double parseDec(String decStr) {
        String signStr = "+";
        String valStr = decStr;
        if (decStr.startsWith("-") || decStr.startsWith("+")) {
            signStr = decStr.substring(0, 1);
            valStr = decStr.substring(1);
        }

        String[] parts = valStr.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid Dec format: " + decStr);
        double degrees = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        
        double totalDegrees = degrees + minutes / 60.0 + seconds / 3600.0;

        if (signStr.equals("-")) {
            totalDegrees = -totalDegrees;
        }
        if (totalDegrees < -90 || totalDegrees > 90) throw new IllegalArgumentException("Dec degrees out of range [-90, 90]: " + totalDegrees);
        return Math.toRadians(totalDegrees);
    }

    public Color getColor() {
        if (spectralClass == null || spectralClass.isEmpty()) {
            return SPECTRAL_COLOR_MAP.getOrDefault('G', Color.YELLOW); // Default G type
        }
        char spectralType = Character.toUpperCase(spectralClass.charAt(0));
        return SPECTRAL_COLOR_MAP.getOrDefault(spectralType, Color.WHITE);
    }

    // Getters
    public int getHipId() { return hipId; }
    public boolean isHabitable() { return habitable; }
    public String getSpectralClass() { return spectralClass; }
    public double getDistanceParsecs() { return distanceParsecs; }
    public double getDistanceLy() { return distanceLy; }
    public double getXGalactic() { return xGalactic; }
    public double getYGalactic() { return yGalactic; }
    public double getZGalactic() { return zGalactic; }
    public double getAbsoluteMagnitude() { return absoluteMagnitude; }
    
    // For backwards compatibility
    public String getStellarClass() { return spectralClass; }
    public String getSystemName() { return ""; } // No system name in new format
} 