package de.tum.androidpraktikum.cardroarddatavisualizationjava.models;

import com.google.gson.annotations.SerializedName;

/**
 * A brewery unit (i.e. an aging vessel, a brewkettle, a bright beer vessel, etc.)
 */
public class Unit {
    @SerializedName("Name")
    public String name;
    @SerializedName("Temp")
    public int temperature;
    @SerializedName("Visco")
    public int viscosity;
    @SerializedName("Level")
    public int level;
    @SerializedName("Stage")
    public String stage;

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
