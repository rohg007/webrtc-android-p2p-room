package com.rohg007.android.huddle01sample.viewmodels;

import android.app.Application;

import com.rohg007.android.huddle01sample.repositories.CameraRepository;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class CameraViewModel extends AndroidViewModel {

    private CameraRepository cameraRepository;

    public CameraViewModel(@NonNull Application application) {
        super(application);
        cameraRepository = CameraRepository.getCameraRepository();
    }

    public void toggle(){
        cameraRepository.toggle();
    }

    public MutableLiveData<Boolean> getCameraState(){
        return cameraRepository.getCameraState();
    }
}
