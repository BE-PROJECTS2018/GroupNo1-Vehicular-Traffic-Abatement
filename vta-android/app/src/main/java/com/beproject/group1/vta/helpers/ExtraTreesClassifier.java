package com.beproject.group1.vta.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Scanner;

/**
 * Created by pavan on 8/3/18.
 */

public class ExtraTreesClassifier {
    public static String model_name = "v1-1-geohash-1";
    public static String map_prefix = "v1-1-geohash-1";
    private class Tree {
        private int[] childrenLeft;
        private int[] childrenRight;
        private double[] thresholds;
        private int[] indices;
        private double[][] classes;

        private int predict (double[] features, int node) {
            if (this.thresholds[node] != -2) {
                if (features[this.indices[node]] <= this.thresholds[node]) {
                    return this.predict(features, this.childrenLeft[node]);
                } else {
                    return this.predict(features, this.childrenRight[node]);
                }
            }
            return ExtraTreesClassifier.findMax(this.classes[node]);
        }
        private int predict (double[] features) {
            return this.predict(features, 0);
        }
    }

    private List<Tree> forest;
    private int nClasses;
    private int nEstimators;

    public ExtraTreesClassifier (String file) throws FileNotFoundException {
        String jsonStr = new Scanner(new File(file)).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Tree>>(){}.getType();
        this.forest = gson.fromJson(jsonStr, listType);
        this.nEstimators = this.forest.size();
        this.nClasses = this.forest.get(0).classes[0].length;
    }

    private static int findMax(double[] nums) {
        int index = 0;
        for (int i = 0; i < nums.length; i++) {
            index = nums[i] > nums[index] ? i : index;
        }
        return index;
    }

    public int predict(double[] features) {
        double[] classes = new double[this.nClasses];
        for (int i = 0; i < this.nEstimators; i++) {
            classes[this.forest.get(i).predict(features, 0)]++;
        }
        return ExtraTreesClassifier.findMax(classes);
    }
}
