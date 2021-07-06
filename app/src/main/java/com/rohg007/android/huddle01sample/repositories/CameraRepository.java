package com.rohg007.android.huddle01sample.repositories;

import java.util.concurrent.CancellationException;

import androidx.lifecycle.MutableLiveData;

public class CameraRepository {

    // false - front facing camera
    // true - back facing camera

    private static CameraRepository cameraRepository;
    private MutableLiveData<Boolean> cameraState;

    public static CameraRepository getCameraRepository(){
        if(cameraRepository==null)
            cameraRepository = new CameraRepository();
        return cameraRepository;
    }

    private CameraRepository(){
        cameraState = new MutableLiveData<Boolean>(false);
    }

    public void toggle(){
        cameraState.setValue(!cameraState.getValue());
    }

    public MutableLiveData<Boolean> getCameraState(){
        return cameraState;
    }
}
