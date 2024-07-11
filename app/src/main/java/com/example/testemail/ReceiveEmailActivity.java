package com.example.testemail;

import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.ArrayList;
import java.util.List;

public class ReceiveEmailActivity extends AppCompatActivity {

    private ListView emailListView;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private List<String> emailList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_email);

        emailListView = findViewById(R.id.emailListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emailList);
        emailListView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadEmails();
    }

    private void loadEmails() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("emails").orderByChild("recipientId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        emailList.clear();
                        for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                            String senderId = emailSnapshot.child("senderId").getValue(String.class);
                            String encryptedMessage = emailSnapshot.child("message").getValue(String.class);

                            // Fetch recipient's private key
                            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot userSnapshot) {
                                    String privateKey = userSnapshot.child("privateKey").getValue(String.class);

                                    // Fetch sender's email
                                    mDatabase.child("users").child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot senderSnapshot) {
                                            String senderEmail = senderSnapshot.child("email").getValue(String.class);

                                            try {
                                                // Decrypt message
                                                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                                PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT)));
                                                cipher.init(Cipher.DECRYPT_MODE, key);
                                                String decryptedMessage = new String(cipher.doFinal(Base64.decode(encryptedMessage, Base64.DEFAULT)));

                                                emailList.add("From: " + senderEmail + "\n" + decryptedMessage);
                                                adapter.notifyDataSetChanged();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Handle error
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // Handle error
                                }
                            });
                        }
                    } else {
                        Toast.makeText(ReceiveEmailActivity.this, "No emails found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
        } else {
            Toast.makeText(ReceiveEmailActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }
}
