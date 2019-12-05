package com.bhavaniprasad.smartagent;

public class description {

    private String id;
    private String name;
    private String type;
    private int sizeInBytes;
    private String cdn_path;

    public description(String id, String name, String type, int sizeInBytes, String cdn_path) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.cdn_path = cdn_path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getCdn_path() {
        return cdn_path;
    }

    public void setCdn_path(String cdn_path) {
        this.cdn_path = cdn_path;
    }
}