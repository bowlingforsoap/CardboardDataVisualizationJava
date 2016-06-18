package de.tum.androidpraktikum.cardroarddatavisualizationjava.models;

import com.google.gson.annotations.SerializedName;

/**
 * A brewery unit (i.e. an aging vessel, a brewkettle, a bright beer vessel, etc.)
 */
public class Unit {
    @SerializedName("Name")
    String name;
    @SerializedName("Temp")
    int temperature;
    @SerializedName("Visco")
    int viscosity;
    @SerializedName("Level")
    int level;
    @SerializedName("Stage")
    String stage;

    @Override
    public String toString() {
        return "Unit{" +
                "name='" + name + '\'' +
                ", temperature=" + temperature +
                ", viscosity=" + viscosity +
                ", level=" + level +
                ", stage='" + stage + '\'' +
                '}';
    }
}
