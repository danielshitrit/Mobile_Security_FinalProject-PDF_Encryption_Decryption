package com.example.PdfEncryptionAndDecrytion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get references to the Encrypt and Decrypt buttons in the layout

        Button Encrypt = findViewById(R.id.button1);
        Button Decrypt = findViewById(R.id.button2);
        // Set up a click listener for the Encrypt button

        Encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the EncryptActivity when the Encrypt button is clicked

                startActivity(new Intent(MainActivity.this, EncryptActivity.class));
            }
        });
        // Set up a click listener for the Decrypt button

        Decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the DecryptActivity when the Decrypt button is clicked

                startActivity(new Intent(MainActivity.this,DecryptActivity.class));
            }
        });

    }
}