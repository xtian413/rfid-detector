package com.example.nfcreaderwriter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the layout with the two buttons
        setContentView(R.layout.activity_main);

        Button createButton = findViewById(R.id.createButton);
        Button simulateButton = findViewById(R.id.simulateButton);

        // Set up the click listener for the "Create New User" button
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the CreateUserActivity when the button is clicked
                Intent intent = new Intent(MainActivity.this, CreateUserActivity.class);
                startActivity(intent);
            }
        });

        // Set up the click listener for the "Simulate" button
        simulateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SimulateActivity when the button is clicked
                Intent intent = new Intent(MainActivity.this, SimulateActivity.class);
                startActivity(intent);
            }
        });
    }
}