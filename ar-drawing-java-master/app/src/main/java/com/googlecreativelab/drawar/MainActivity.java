package com.googlecreativelab.drawar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3f;

public class MainActivity extends AppCompatActivity {
    private FirebaseDatabase database;
    private Long lastRoom ;
    private DatabaseReference room;
    private DatabaseReference rooms;
    private Long codeRoom;
    private EditText connectCode;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        connectCode = findViewById(R.id.room);
        database = FirebaseDatabase.getInstance();
        room = database.getReference("Last-Code");


        room.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Long value = dataSnapshot.getValue(Long.class);
                lastRoom = value;
                if (!connected) {
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    connected = true;
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });


    }
    public void newHost(View v){
        if (connected) {
            if (lastRoom != null) {
                lastRoom += 1;
                room.setValue(lastRoom);
                codeRoom = lastRoom;
            }
            rooms = database.getReference("roomList").child(Long.toString(codeRoom));
            ArrayList<ArrayList<ArrayList<Vector3f>>> listPeople = new ArrayList<>();
            ArrayList<ArrayList<Vector3f>> strock = new ArrayList<>();
            ArrayList<Vector3f> tmp = new ArrayList<>();
            tmp.add(new Vector3f(0, 0, 0));
            strock.add(tmp);
            listPeople.add(strock);
            rooms.setValue(listPeople);
            Intent intent = new Intent(this, DrawAR.class);
            intent.putExtra("code", String.valueOf(codeRoom));
            this.finish();
            startActivity(intent);
        }
    }
    public void connect(View v){
        if (connected) {
            String c = connectCode.getText().toString();

            Intent intent = new Intent(this, DrawAR.class);
            intent.putExtra("code", c);
            startActivity(intent);
        }
    }
}
