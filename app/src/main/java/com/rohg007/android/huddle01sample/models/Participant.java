package com.rohg007.android.huddle01sample.models;

public class Participant {

    private String id;
    private String deviceName;

    public Participant() {
    }

    public Participant(String id, String deviceName) {
        this.id = id;
        this.deviceName = deviceName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

}
