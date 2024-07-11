package com.example.testemail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class EmailActivity extends AppCompatActivity {

    private EditText recipientEmailEditText, subjectEditText, messageEditText;
    private Button sendEmailButton, viewReceivedEmailsButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recipientEmailEditText = findViewById(R.id.recipientEmailEditText);
        subjectEditText = findViewById(R.id.subjectEditText);
        messageEditText = findViewById(R.id.messageEditText);
        sendEmailButton = findViewById(R.id.sendEmailButton);
        viewReceivedEmailsButton = findViewById(R.id.viewReceivedEmailsButton);

        sendEmailButton.setOnClickListener(v -> sendMessage());

        viewReceivedEmailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmailActivity.this, ReceiveEmailActivity.class);
            startActivity(intent);
        });
    }

    private void sendMessage() {
        String recipientEmail = recipientEmailEditText.getText().toString();
        String subject = subjectEditText.getText().toString();
        String message = messageEditText.getText().toString();
        String senderId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").orderByChild("email").equalTo(recipientEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String recipientId = userSnapshot.getKey();
                        String publicKey = userSnapshot.child("publicKey").getValue(String.class);

                        try {
                            // Encrypt message
                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT)));
                            cipher.init(Cipher.ENCRYPT_MODE, key);
                            String encryptedMessage = Base64.encodeToString(cipher.doFinal(message.getBytes()), Base64.DEFAULT);

                            // Save encrypted message to Firebase
                            DatabaseReference emailRef = mDatabase.child("emails").push();
                            emailRef.child("senderId").setValue(senderId);
                            emailRef.child("recipientId").setValue(recipientId);
                            emailRef.child("subject").setValue(subject);
                            emailRef.child("message").setValue(encryptedMessage);
                            emailRef.child("timestamp").setValue(System.currentTimeMillis());

                            Toast.makeText(EmailActivity.this, "Message sent!", Toast.LENGTH_SHORT).show();

                            // Navigate to email list screen
                            startActivity(new Intent(EmailActivity.this, ReceiveEmailActivity.class));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(EmailActivity.this, "Recipient not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}
