package com.universe;

import java.util.ArrayList;
import java.util.List;

public class SphereMesh {
    public final float[] positions;
    public final float[] normals;
    public final int[] indices;
    public final int vertexCount;
    public final int indexCount;

    public SphereMesh(int latitudeBands, int longitudeBands) {
        List<Float> posList = new ArrayList<>();
        List<Float> normList = new ArrayList<>();
        List<Integer> idxList = new ArrayList<>();

        for (int lat = 0; lat <= latitudeBands; lat++) {
            double theta = lat * Math.PI / latitudeBands;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int lon = 0; lon <= longitudeBands; lon++) {
                double phi = lon * 2 * Math.PI / longitudeBands;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                float x = (float) (cosPhi * sinTheta);
                float y = (float) cosTheta;
                float z = (float) (sinPhi * sinTheta);

                posList.add(x);
                posList.add(y);
                posList.add(z);

                normList.add(x);
                normList.add(y);
                normList.add(z);
            }
        }

        for (int lat = 0; lat < latitudeBands; lat++) {
            for (int lon = 0; lon < longitudeBands; lon++) {
                int first = (lat * (longitudeBands + 1)) + lon;
                int second = first + longitudeBands + 1;

                idxList.add(first);
                idxList.add(second);
                idxList.add(first + 1);

                idxList.add(second);
                idxList.add(second + 1);
                idxList.add(first + 1);
            }
        }

        positions = new float[posList.size()];
        normals = new float[normList.size()];
        indices = new int[idxList.size()];
        for (int i = 0; i < posList.size(); i++) positions[i] = posList.get(i);
        for (int i = 0; i < normList.size(); i++) normals[i] = normList.get(i);
        for (int i = 0; i < idxList.size(); i++) indices[i] = idxList.get(i);
        vertexCount = positions.length / 3;
        indexCount = indices.length;
    }
} 