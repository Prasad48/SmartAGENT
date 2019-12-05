package com.bhavaniprasad.smartagent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Dbhandler {

    @SerializedName("dependencies")
    @Expose
    private List<description> descriptionArray ;

    public List<description> getDescriptionArray() {
        return descriptionArray;
    }

    public void setDescriptionArray(List<description> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }
}
