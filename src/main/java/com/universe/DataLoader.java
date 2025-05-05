package com.universe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLoader {

    // Load stars from a file within the classpath/jar
    public static Map<String, Star> loadStarsFromResources(String resourcePath) throws IOException {
        Map<String, Star> stars = new HashMap<>();
        // Try to load as a resource stream first
        InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            // Fallback to file system if not found in resources (e.g., during development)
            System.err.println("Warning: Could not find star data as resource: " + resourcePath + ". Trying filesystem.");
            return loadStarsFromFile(resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
             parseStars(reader, stars);
        }
        System.out.println("Loaded " + stars.size() + " stars from resource: " + resourcePath);
        return stars;
    }

    // Load stars from an external file path
    public static Map<String, Star> loadStarsFromFile(String filePath) throws IOException {
        Map<String, Star> stars = new HashMap<>();
         try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            parseStars(reader, stars);
        }
        System.out.println("Loaded " + stars.size() + " stars from file: " + filePath);
        return stars;
    }

    private static void parseStars(BufferedReader reader, Map<String, Star> stars) throws IOException {
         String line;
         boolean headerSkipped = false;
         int lineNum = 0;
         while ((line = reader.readLine()) != null) {
             lineNum++;
             if (!headerSkipped) {
                 headerSkipped = true;
                 // Check for new format header
                 if (line.toLowerCase().contains("hip") && line.toLowerCase().contains("hab")) {
                     System.out.println("Detected new star data format with Hipparcos IDs and galactic coordinates");
                 } else if (!line.toLowerCase().contains("system") && !line.toLowerCase().contains("name")) {
                     System.err.println("Warning: Unexpected header in stars file: " + line);
                 }
                 continue; // Skip header row
             }
             line = line.trim();
             if (line.isEmpty() || line.startsWith("#") || line.matches("^,*")) continue; // Skip empty lines, comments, or lines with only commas

             String[] parts = line.split(",", -1); // Simple split

             try {
                 // Try to detect format based on number of columns
                 if (parts.length >= 8 && isNumeric(parts[0])) {
                     // New format: Hip,Hab?,Display Name,Spectral Class,Distance,Xg,Yg,Zg,AbsMag
                     parseNewFormatStar(parts, stars, lineNum);
                 } else if (parts.length >= 7) {
                     // Old format: SystemName,Name,StellarClass,DistanceLy,RA,Dec,Mass,AbsMag
                     parseOldFormatStar(parts, stars, lineNum);
                 } else {
                     System.err.println("Skipping malformed star line #" + lineNum + ": " + line + " (Not enough fields)");
                 }
             } catch (NumberFormatException e) {
                 System.err.println("Skipping star line #" + lineNum + " due to number format error: " + line + " - " + e.getMessage());
             } catch (Exception e) { // Catch other potential errors during Star creation
                 System.err.println("Skipping star line #" + lineNum + " due to error creating Star object: " + line + " - " + e.getMessage());
             }
         }
    }

    // Helper method to check if string can be parsed as numeric value
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Parse star data in the new format with Hipparcos IDs and galactic coordinates
    private static void parseNewFormatStar(String[] parts, Map<String, Star> stars, int lineNum) throws NumberFormatException {
        // Parse Hipparcos ID (0 if not available)
        int hipId = parts[0].trim().isEmpty() ? 0 : Integer.parseInt(parts[0].trim());
        
        // Parse habitability flag (1 or non-empty = habitable)
        boolean habitable = !parts[1].trim().isEmpty() && !parts[1].trim().equals("0");
        
        // Get display name
        String displayName = parts[2].trim();
        if (displayName.isEmpty()) {
            System.err.println("Skipping star line #" + lineNum + ": Name is empty.");
            return;
        }
        
        // Parse spectral class
        String spectralClass = parts[3].trim();
        
        // Parse distance in parsecs (0 for Sun)
        double distance = parts[4].trim().isEmpty() ? 0.0 : Double.parseDouble(parts[4].trim());
        
        // Parse galactic coordinates (0 for Sun or if unavailable)
        double xGalactic = parts[5].trim().isEmpty() ? 0.0 : Double.parseDouble(parts[5].trim());
        double yGalactic = parts[6].trim().isEmpty() ? 0.0 : Double.parseDouble(parts[6].trim());
        double zGalactic = parts[7].trim().isEmpty() ? 0.0 : Double.parseDouble(parts[7].trim());
        
        // Parse absolute magnitude
        double absMag = parts.length > 8 && !parts[8].trim().isEmpty() ? 
            Double.parseDouble(parts[8].trim()) : 
            (displayName.equalsIgnoreCase("Sun") ? 4.85 : 0.0); // Default to 4.85 for Sun
        
        // Create star object with new format data
        Star star = new Star(hipId, habitable, displayName, spectralClass, 
                              distance, xGalactic, yGalactic, zGalactic, absMag);
        
        // Add star to map - use lowercase name as key for consistency
        String nameKey = displayName.toLowerCase();
        stars.put(nameKey, star);
        
        // Also store by Hip ID if available and non-zero
        if (hipId > 0) {
            stars.put("hip" + hipId, star);
        }
    }

    // Parse star data in the old format (for backward compatibility)
    private static void parseOldFormatStar(String[] parts, Map<String, Star> stars, int lineNum) throws NumberFormatException {
        String systemName = parts[0].trim();
        String name = parts[1].trim();
        String stellarClass = parts[2].trim();
        
        // Handle Sun's distance explicitly (CSV shows 0, which is correct)
        double distance = Double.parseDouble(parts[3].trim());
        String ra = parts[4].trim();
        String dec = parts[5].trim();
        
        // Handle Sun's mass explicitly
        double mass = name.equalsIgnoreCase("Sun") ? 1.0 : Double.parseDouble(parts[6].trim());
        double absMag = name.equalsIgnoreCase("Sun") ? 4.85 : Double.parseDouble(parts[7].trim()); // Use Sun's standard value

        if (name.isEmpty()) {
             System.err.println("Skipping star line #" + lineNum + ": Name is empty.");
             return;
        }

        Star star = new Star(name, stellarClass, distance, ra, dec, mass, absMag);
        
        // Store by lowercase compound key (system+name) for unique identification
        String key = (systemName + "_" + name).toLowerCase().replace(" ", "_");
        stars.put(key, star);
        
        // Also store by name alone for backward compatibility, but only if there's no collision
        String nameKey = name.toLowerCase();
        if (!stars.containsKey(nameKey)) {
            stars.put(nameKey, star);
        }
    }

    // Load planets from a resource path
    public static List<Planet> loadPlanetsFromResources(String resourcePath, Map<String, Star> stars) throws IOException {
        List<Planet> planets = new ArrayList<>();
        InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            System.err.println("Warning: Could not find planet data as resource: " + resourcePath + ". Trying filesystem.");
            return loadPlanetsFromFile(resourcePath, stars);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            parsePlanets(reader, planets, stars);
        }
         System.out.println("Loaded " + planets.size() + " planets from resource: " + resourcePath);
        return planets;
    }
    
    // Load planets from an external file path
     public static List<Planet> loadPlanetsFromFile(String filePath, Map<String, Star> stars) throws IOException {
        List<Planet> planets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            parsePlanets(reader, planets, stars);
        }
        System.out.println("Loaded " + planets.size() + " planets from file: " + filePath);
        return planets;
    }
    

    private static void parsePlanets(BufferedReader reader, List<Planet> planets, Map<String, Star> stars) throws IOException {
         String line;
         boolean headerSkipped = false;
         int lineNum = 0;
         while ((line = reader.readLine()) != null) {
            lineNum++;
             if (!headerSkipped) {
                 headerSkipped = true;
                  if (!line.toLowerCase().startsWith("name,star,dfs")) {
                      System.err.println("Warning: Unexpected header in planets file: " + line);
                 }
                 continue; // Skip header row
             }
              line = line.trim();
              if (line.isEmpty() || line.startsWith("#") || line.matches("^,*")) continue; // Skip empty lines, comments, or lines with only commas

             String[] parts = line.split(",", -1);

              if (parts.length < 6) {
                  System.err.println("Skipping malformed planet line #" + lineNum + ": " + line + " (Expected 6+ fields, got " + parts.length + ")");
                  continue;
              }

             try {
                 String name = parts[0].trim();
                 String starName = parts[1].trim();
                 double dfs = Double.parseDouble(parts[2].trim());
                 double mass = Double.parseDouble(parts[3].trim());
                 double radius = Double.parseDouble(parts[4].trim());
                 // Simple check for rings (1 or "yes", case-insensitive)
                 String ringsStr = parts[5].trim().toLowerCase();
                 boolean hasRings = ringsStr.equals("1") || ringsStr.equals("yes");
                 
                 if (name.isEmpty() || starName.isEmpty()) {
                      System.err.println("Skipping planet line #" + lineNum + ": Name or Star Name is empty.");
                      continue;
                 }

                 Star hostStar = stars.get(starName.toLowerCase());
                 if (hostStar != null) {
                     Planet planet = new Planet(name, hostStar, dfs, mass, radius, hasRings);
                     planets.add(planet);
                 } else {
                     System.err.println("Skipping planet " + name + " on line #" + lineNum + ": Could not find host star '" + starName + "'. Ensure stars are loaded first and names match (case-insensitive).");
                 }
             } catch (NumberFormatException e) {
                 System.err.println("Skipping planet line #" + lineNum + " due to number format error: " + line + " - " + e.getMessage());
             } catch (ArrayIndexOutOfBoundsException e) {
                 System.err.println("Skipping planet line #" + lineNum + " due to missing fields: " + line);
             } catch (Exception e) {
                 System.err.println("Skipping planet line #" + lineNum + " due to error creating Planet object: " + line + " - " + e.getMessage());
             }
         }
    }
} 