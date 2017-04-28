package com.mihalic.franck.cardless_poker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class ConnectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        initClientButtonManagement();
        initServerButtonManagement();

        // Version Name display
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            TextView versionTV = (TextView) findViewById(R.id.versionName);
            String fullVersionName=getResources().getText(R.string.version)+" "+version;
            versionTV.setText(fullVersionName);
        } catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    private void initClientButtonManagement() {
        final Button mainButton = (Button) findViewById(R.id.clientButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(ConnectActivity.this, ClientActivity.class);
                startActivity( intent );
                finish();
            }
        });
    }

    private void initServerButtonManagement() {
        final Button mainButton = (Button) findViewById(R.id.serverButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(ConnectActivity.this, MainActivity.class);
                startActivity( intent );
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(ConnectActivity.this)
                .setTitle("Confirmation")
                .setMessage("Are you sure to want to leave ?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        intent = new Intent(ConnectActivity.this, ConnectActivity.class);
                        startActivity( intent );
                        finish();
                    }
                })
                .show();
    }
}
