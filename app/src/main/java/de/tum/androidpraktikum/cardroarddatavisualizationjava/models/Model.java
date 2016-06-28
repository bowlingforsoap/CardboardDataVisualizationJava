package de.tum.androidpraktikum.cardroarddatavisualizationjava.models;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Created by strelchenkovadym on 6/26/16.
 */
public class Model {
    public static final String TAG = "Model";
    // TODO: 1. finish with this class 2. finish texture stuff 3. height map 4. sound 5. cubemap
    protected String name;

    protected float[] normals;
    protected float[] texels;
    protected float[] positions;

    private float[] highest;
    private float[] lowest;

    private ModelInfo modelInfo;

    public Model(Context appContext, int resId) {
        try {
            // Retrieve model info.
            modelInfo = getObjInfo(appContext, resId);

            // Initialize members.
            normals = new float[modelInfo.vertices * 3];
            positions = new float[modelInfo.vertices * 3];
            texels = new float[modelInfo.vertices * 2];

            // Initiate containers for model data.
            float[][] positions = new float[modelInfo.positions][3]; // XYZ
            float[][] texels = new float[modelInfo.texels][2]; // u, v
            float[][] normals = new float[modelInfo.normals][3]; // XYZ
            int[][] faces = new int[modelInfo.faces][9]; // an entry of pointers (indices). nine integers, to describe the three vertices of a triangular face, where each vertex gets three indexes, one for its position (P), one for its texel (T) and one for its normal (N)

            // Extract data from an OBJ specified by resId.
            extractOBJData(appContext, resId, positions, texels, normals, faces);

            // Write model data into this.normals, this.texels and this.positions, as well as in this.highest and this.lowest.
            writeModelData(faces, positions, texels, normals);
        } catch (IOException e) {
            // TODO: handle the exception.
            Log.e(TAG, "Error reading model from resources!");
        }
    }

    /**
     * Extracts info about the model (num of vertices, faces, texels, etc.).
     * @param appContext
     * @param resId
     * @return
     * @throws IOException
     */
    private ModelInfo getObjInfo(Context appContext, int resId) throws IOException {
        modelInfo = new ModelInfo();

        //open OBJ file
        BufferedReader objReader = new BufferedReader(new InputStreamReader(appContext.getResources().openRawResource(resId)));

        //read OBJ file
        String line;
        String type;
        while ((line = objReader.readLine()) != null) {
            type = line.substring(0, 2); // first 2 char-s

            if (type.equals("v ")) {
                modelInfo.positions++;
            } else if (type.equals("vt")) {
                modelInfo.texels++;
            } else if (type.equals("vn")) {
                modelInfo.normals++;
            } else if (type.equals("f ")) {
                modelInfo.faces++;
            }
        }
        //TODO: delete comment
        //Even though a 3D model has shared vertex data, in this tutorial OpenGL ES will process all vertices individually instead of as indexed arrays. You already know that OpenGL ES draws triangles, so the total number of vertices will be the total number of faces times their three defining points
        modelInfo.vertices = modelInfo.faces * 3;

        objReader.close();

        return modelInfo;
    }

    /**
     * Extracts model data.
     */
    private void extractOBJData(Context appContext, int resId, float positions[][], float texels[][], float normals[][], int faces[][]) throws IOException {
        // Counters
        int p = 0;
        int t = 0;
        int n = 0;
        int f = 0;

        //open OBJ file
        BufferedReader objReader = new BufferedReader(new InputStreamReader(appContext.getResources().openRawResource(resId)));

        //read OBJ file
        String line;
        String type;
        String analyzedLine;
        StringTokenizer spaceTokenizer;
        while ((line = objReader.readLine()) != null) {
            type = line.substring(0, 2); // first 2 char-s

            if (type.equals("v ")) {
                analyzedLine = line.substring(2, line.length());
                spaceTokenizer = new StringTokenizer(analyzedLine, " ");

                for (int i = 0; i < 3; i++) {
                    positions[p][i] = Float.parseFloat(spaceTokenizer.nextToken());
                }

                p++;
            } else if (type.equals("vt")) {
                analyzedLine = line.substring(2, line.length());
                spaceTokenizer = new StringTokenizer(analyzedLine, " ");

                for (int i = 0; i < 2; i++) {
                    texels[t][i] = Float.parseFloat(spaceTokenizer.nextToken());
                }

                t++;
            } else if (type.equals("vn")) {
                analyzedLine = line.substring(2, line.length());
                spaceTokenizer = new StringTokenizer(analyzedLine, " ");

                for (int i = 0; i < 3; i++) {
                    normals[n][i] = Float.parseFloat(spaceTokenizer.nextToken());
                }

                n++;
            } else if (type.equals("f ")) {
                analyzedLine = line.substring(2, line.length());
                spaceTokenizer = new StringTokenizer(analyzedLine, " ");
                StringTokenizer dashTokenizer;

                for (int i = 0; i < 9; i += 3) {
                    String space = spaceTokenizer.nextToken();
                    dashTokenizer = new StringTokenizer(space, "/");

                    faces[f][i] = Integer.parseInt(dashTokenizer.nextToken());
                    faces[f][i + 1] = Integer.parseInt(dashTokenizer.nextToken());
                    faces[f][i + 2] = Integer.parseInt(dashTokenizer.nextToken());

                    /*if (faces[f][i] == 0 || faces[f][i + 1] == 0 || faces[f][i + 2] == 0) {
                        System.out.println("Erroneous line: " + space);
                        throw new RuntimeException("Erroneous line: " + space);
                    }*/
                }

                f++;
            }
        }

        objReader.close();
    }

    /**
     * Write the data from {@code faces}, {@code positions}, {@code texels} and {@code normals} into {@code this.positions}, {@code this.texels} and {@code this.normals}.
     * @param faces
     * @param positions
     * @param texels
     * @param normals
     */
    private void writeModelData(int faces[][], float positions[][], float texels[][], float normals[][]) {
        // Write the highest and the lowest values.
        highest = findHighestVertex(positions, modelInfo.positions);
        lowest = findLowestVertex(positions, modelInfo.positions);

        // Current index.
        int index = 0;
        // write positions
        for (int i = 0; i < modelInfo.faces; i++) {
            int vA = faces[i][0] - 1;
            int vB = faces[i][3] - 1;
            int vC = faces[i][6] - 1;

            for (int j = 0; j < 3; j++) {
                this.positions[index + j] = positions[vA][j];
                this.positions[index + 3 + j] = positions[vB][j];
                this.positions[index + 6 + j] = positions[vC][j];
            }

            index += 9;
        }

        index = 0;
        // write texels
        for (int i = 0; i < modelInfo.faces; i++) {
            int vtA = faces[i][1] - 1;
            int vtB = faces[i][4] - 1;
            int vtC = faces[i][7] - 1;

            for (int j = 0; j < 2; j++) {
                this.texels[index + j] =  texels[vtA][j];
                this.texels[index + 2 + j] =  texels[vtB][j];
                this.texels[index + 4 + j] =  texels[vtC][j];
            }

            index += 6;
        }

        index = 0;
        // write normals
        for (int i = 0; i < modelInfo.faces; i++) {
            int vnA = faces[i][2] - 1;
            int vnB = faces[i][5] - 1;
            int vnC = faces[i][8] - 1;

            for (int j = 0; j < 3; j++) {
                this.normals[index + j] = normals[vnA][j];
                this.normals[index + 3 + j] = normals[vnB][j];
                this.normals[index + 6 + j] = normals[vnC][j];
            }

            index += 9;
        }
    }

    float[] findLowestVertex(float positions[][], int positionsNum) {
        float[] lowest = positions[0];

        for (int i = 1; i < positionsNum; i++) {
            if (positions[i][1] < lowest[1]) {
                lowest = positions[i];
            }
        }

        return lowest;
    }

    float[] findHighestVertex(float positions[][], int positionsNum) {
        float[] highest = positions[0];

        for (int i = 1; i < positionsNum; i++) {
            if (positions[i][1] > highest[1]) {
                highest = positions[i];
            }
        }

        return highest;
    }

    public float[] getNormals() {
        return normals;
    }

    public float[] getTexels() {
        return texels;
    }

    public float[] getPositions() {
        return positions;
    }

    public float[] getHighest() {
        return highest;
    }

    public float[] getLowest() {
        return lowest;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    /**
     * Holds the number of vertices, positions, texels, normals, faces.
     */
    public class ModelInfo {
        private int vertices;
        private int positions;
        private int texels;
        private int normals;
        private int faces;

        public int getPositions() {
            return positions;
        }

        public int getVertices() {
            return vertices;
        }

        public int getTexels() {
            return texels;
        }

        public int getNormals() {
            return normals;
        }

        public int getFaces() {
            return faces;
        }
    }
}
