package com.rohg007.android.huddle01sample;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

class CustomSDPObserver implements SdpObserver {

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }

}
