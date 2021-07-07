package com.rohg007.android.huddle01sample.models;

public class RoomConnection {

    private Participant participant1;
    private Participant participant2;

    public RoomConnection() {
    }

    public RoomConnection(Participant participant1, Participant participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
    }

    public Participant getParticipant1() {
        return participant1;
    }

    public void setParticipant1(Participant participant1) {
        this.participant1 = participant1;
    }

    public Participant getParticipant2() {
        return participant2;
    }

    public void setParticipant2(Participant participant2) {
        this.participant2 = participant2;
    }
}
