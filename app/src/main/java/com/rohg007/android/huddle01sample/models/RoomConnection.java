package com.rohg007.android.huddle01sample.models;

public class RoomConnection {

    private String participant1 = "";
    private String participant2 = "";

    public RoomConnection() {
    }

    public RoomConnection(String participant1, String participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
    }

    public String getParticipant1() {
        return participant1;
    }

    public void setParticipant1(String participant1) {
        this.participant1 = participant1;
    }

    public String getParticipant2() {
        return participant2;
    }

    public void setParticipant2(String participant2) {
        this.participant2 = participant2;
    }
}
