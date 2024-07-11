package com.example.testemail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton, receivedEmailsButton, logoutButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        logoutButton = findViewById(R.id.logoutButton);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> registerUser());
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Email and Password must not be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, proceed to email screen
                        startActivity(new Intent(MainActivity.this, EmailActivity.class));
                    } else {
                        // Login failed, show error
                        Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Email and Password must not be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration successful, generate RSA keys
                        generateRSAKeys(task.getResult().getUser().getUid(), email);
                    } else {
                        // Registration failed, show error
                        Toast.makeText(MainActivity.this, "Registration Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generateRSAKeys(String userId, String email) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            String publicKey = Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT);
            String privateKey = Base64.encodeToString(pair.getPrivate().getEncoded(), Base64.DEFAULT);

            // Save keys to Firebase
            DatabaseReference userRef = mDatabase.child("users").child(userId);
            userRef.child("publicKey").setValue(publicKey);
            userRef.child("privateKey").setValue(privateKey);
            userRef.child("email").setValue(email);

            // Proceed to email screen
            startActivity(new Intent(MainActivity.this, EmailActivity.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Clear the back stack and start the login activity
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
