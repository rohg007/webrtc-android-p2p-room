package com.rohg007.android.huddle01sample.viewmodels;

import android.app.Application;

import com.rohg007.android.huddle01sample.models.RoomConnection;
import com.rohg007.android.huddle01sample.repositories.RoomRepository;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class RoomConnectionViewModel extends AndroidViewModel {

    private RoomRepository roomRepository;

    public RoomConnectionViewModel(@NonNull Application application) {
        super(application);
        roomRepository = new RoomRepository(application);
    }

    public void addDataToFirebase(String room, String participant){
        roomRepository.addDataToFirebase(room, participant);
    }

    public void removeDataFromFirebase(String room){
        roomRepository.removeDataFromFirebase(room);
    }

    public MutableLiveData<RoomConnection> getDataFromFirebase(String room){
        return roomRepository.getDataFromFirebase(room);
    }

    public MutableLiveData<Boolean> check(String room){
        return roomRepository.check(room);
    }

    public MutableLiveData<Boolean> deleteMutableLiveData(){
        return roomRepository.getDeleteMutableLiveData();
    }
}
