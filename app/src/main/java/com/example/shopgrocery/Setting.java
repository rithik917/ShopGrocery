package com.example.shopgrocery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class Setting extends AppCompatActivity {
    private ImageButton backbtn;
    SwitchCompat switchfm;
    TextView textNotification;
    private Boolean isChecked;
    public static final String notificationEnabled = "Notification Enabled";
    public static final String notificationDisabled = "Notification Disabled";
    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        backbtn = findViewById(R.id.backButton);
        switchfm = findViewById(R.id.switchfm);
        textNotification = findViewById(R.id.textNotification);
        sp=getSharedPreferences("SETTINGS_SP",MODE_PRIVATE);
        isChecked=sp.getBoolean("FCM_ENABLED",false);
        switchfm.setChecked(isChecked);
        if(isChecked){
            textNotification.setText(notificationEnabled);

        }
        else{
            textNotification.setText(notificationDisabled);
        }
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });
        switchfm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SubscibeToTopic();
                }
                else {
                    unsubscribeToTopic();
                }
            }
        });
    }

    private void SubscibeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
spEditor=sp.edit();
spEditor.putBoolean("FCM_ENABLED",true);
spEditor.apply();
Toast.makeText(Setting.this,notificationEnabled,Toast.LENGTH_SHORT);
textNotification.setText(notificationEnabled);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @org.jetbrains.annotations.NotNull Exception e) {
                Toast.makeText(Setting.this,e.getMessage(),Toast.LENGTH_SHORT);
            }
        });
    }

    private void unsubscribeToTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.FCM_TOPIC).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                spEditor=sp.edit();
                spEditor.putBoolean("FCM_ENABLED",false);
                spEditor.apply();
                Toast.makeText(Setting.this,notificationDisabled,Toast.LENGTH_SHORT);
                textNotification.setText(notificationDisabled);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @org.jetbrains.annotations.NotNull Exception e) {
                Toast.makeText(Setting.this,e.getMessage(),Toast.LENGTH_SHORT);
            }
        });
    }
}