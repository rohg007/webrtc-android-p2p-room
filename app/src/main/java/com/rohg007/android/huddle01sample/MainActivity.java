package com.rohg007.android.huddle01sample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.rohg007.android.huddle01sample.models.RoomConnection;
import com.rohg007.android.huddle01sample.viewmodels.RoomConnectionViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RoomConnectionViewModel roomConnectionViewModel = new ViewModelProvider(this).get(RoomConnectionViewModel.class);

        Button startButton = findViewById(R.id.create_room_button);
        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("roomName", Utils.getRandomString());
            startActivity(intent);
        });

        EditText roomNameEditText = findViewById(R.id.room_name_edt);
        Button joinButton = findViewById(R.id.join_room_button);

        joinButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CallActivity.class);
            String room = roomNameEditText.getText().toString();
            roomConnectionViewModel.check(room).observe(this, aBoolean -> {
                if(aBoolean!=null) {
                    if (aBoolean) {
                        intent.putExtra("roomName", room);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Room doesn't exist", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
}