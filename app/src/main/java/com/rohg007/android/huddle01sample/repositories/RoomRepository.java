package com.rohg007.android.huddle01sample.repositories;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohg007.android.huddle01sample.models.Participant;
import com.rohg007.android.huddle01sample.models.RoomConnection;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class RoomRepository {

    private final DatabaseReference reference;
    MutableLiveData<Boolean> deleteMutableLiveData;

    public RoomRepository(Application application){
        reference = FirebaseDatabase.getInstance().getReference();
        deleteMutableLiveData = new MutableLiveData<>(false);
    }

    public void addDataToFirebase(String room, String participant){
        reference.child(room).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() == null){
                    RoomConnection roomConnection = new RoomConnection();
                    roomConnection.setParticipant1(new Participant(Build.ID, Build.DEVICE));
                    reference.child(room).setValue(roomConnection);
                } else {
                    RoomConnection roomConnection = snapshot.getValue(RoomConnection.class);
                    assert roomConnection != null;
                    if(roomConnection.getParticipant2()==null)
                        roomConnection.setParticipant2(new Participant(Build.ID, Build.DEVICE));
                    reference.child(room).setValue(roomConnection);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("db error", error.getMessage());
            }
        });

    }

    public MutableLiveData<RoomConnection> getDataFromFirebase(String room){
        MutableLiveData<RoomConnection> roomConnectionMutableLiveData = new MutableLiveData<>();
        reference.child(room).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    roomConnectionMutableLiveData.setValue(snapshot.getValue(RoomConnection.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return roomConnectionMutableLiveData;
    }

    public void removeDataFromFirebase(String room){
        reference.child(room).removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                deleteMutableLiveData.setValue(true);
            }
        });
    }

    public MutableLiveData<Boolean> check(String room){
        MutableLiveData<Boolean> mutableLiveData = new MutableLiveData<>();
        reference.child(room).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    mutableLiveData.setValue(true);
                } else {
                    mutableLiveData.setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return mutableLiveData;
    }

    public MutableLiveData<Boolean> getDeleteMutableLiveData() {
        return deleteMutableLiveData;
    }
}
