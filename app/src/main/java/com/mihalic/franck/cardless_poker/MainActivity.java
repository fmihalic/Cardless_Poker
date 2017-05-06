package com.mihalic.franck.cardless_poker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    List<Integer> myPokerDeckArray;
    List<Player> myPokerPlayerArray;
    int SecureRandomCard1;
    int SecureRandomCard2;
    int SecureRandomCard3;
    int SecureRandomCard4;
    int SecureRandomCard5;

    int SecureRandomCard1Player1;
    int SecureRandomCard2Player1;
    int SecureRandomCard1Player2;
    int SecureRandomCard2Player2;
    int SecureRandomCard1Player3;
    int SecureRandomCard2Player3;
    int SecureRandomCard1Player4;
    int SecureRandomCard2Player4;
    int SecureRandomCard1Player5;
    int SecureRandomCard2Player5;
    int SecureRandomCard1Player6;
    int SecureRandomCard2Player6;
    int SecureRandomCard1Player7;
    int SecureRandomCard2Player7;
    int SecureRandomCard1Player8;
    int SecureRandomCard2Player8;
    int SecureRandomCard1Player9;
    int SecureRandomCard2Player9;
    int SecureRandomCard1Player10;
    int SecureRandomCard2Player10;

    NsdManager.RegistrationListener mRegistrationListener;
    NsdManager mNsdManager;
    private static final String TAG = "MainActivity";
    String mServiceName;
    NsdServiceInfo serviceInfo;
    private ServerSocket serverSocket;
    Handler updateConversationHandler;
    Thread serverThread = null;
    TextView text;
    public static final int SOCKETSERVERPORT = 6000;
    int handNumber;
    int dealerPlayerNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        handNumber=0;
        dealerPlayerNumber=0;

        // Initialize poker deck and cards
        initDeck();

        // init user Array
        myPokerPlayerArray=new ArrayList<>();

        try {
            // Initialize a server socket on the next available port.
            ServerSocket mServerSocket = new ServerSocket(0);
            int mLocalPort = mServerSocket.getLocalPort();
            // Register NSD Network Service Discovery
            registerService(mLocalPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize Button management
        initButtonManagement();

        // Initialize Restart Button management
        initRestartButtonManagement();

        text = (TextView) findViewById(R.id.text2);
        updateConversationHandler = new Handler();
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }


    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ServerThread implements Runnable {
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SOCKETSERVERPORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter outPut;
        private CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                this.outPut = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(this.clientSocket.getOutputStream())),
                        true);
             } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // read 1st line containing user number
                    Integer playerNumber = Integer.valueOf(input.readLine());
                    String androidID = input.readLine();
                    if(playerNumber==0){
                        playerNumber=assignUser(androidID);
                    }
                    outPut.println(playerNumber);
                    outPut.println(handNumber);
                    assignCards(outPut, playerNumber);
                    outPut.println(dealerPlayerNumber);
                    input.close();
                    outPut.close();
                    this.clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Confirmation")
                .setMessage("Are you sure to want to go back to main menu ?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        intent = new Intent(MainActivity.this, ConnectActivity.class);
                        startActivity( intent );
                        finish();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void registerService(int port) {

        // Create the NsdServiceInfo object, and populate it.
        serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("CardlessPokerServer");
        serviceInfo.setServiceType("_http._tcp");
        serviceInfo.setPort(port);
        initializeRegistrationListener();
        mNsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.e(TAG, "Registration failed!");
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.i(TAG, "Service has been unregistered.");
            }
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                Log.e(TAG, "Unregistration failed!");
            }
        };
    }

    private void initButtonManagement() {
        final Button mainButton = (Button) findViewById(R.id.mainButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Get ImageView of each card
                final ImageView img1= (ImageView) findViewById(R.id.card1);
                final ImageView img2= (ImageView) findViewById(R.id.card2);
                final ImageView img3= (ImageView) findViewById(R.id.card3);
                final ImageView img4= (ImageView) findViewById(R.id.card4);
                final ImageView img5= (ImageView) findViewById(R.id.card5);

                // Add 1.5 seconds delay on button to avoid multiple click that cause crash
                mainButton.setEnabled(false);
                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainButton.setEnabled(true);
                            }
                        });
                    }
                }, 1000);

                // Get button values
                TextView tv = (TextView)findViewById(R.id.mainButton);
                String buttonTextValue = mainButton.getText().toString();

                // if flop
                if (buttonTextValue.equals(getResources().getString(R.string.buttonFlop))){
                    img1.setImageResource(SecureRandomCard1);
                    img2.setImageResource(SecureRandomCard2);
                    img3.setImageResource(SecureRandomCard3);
                    tv.setText(R.string.buttonTurn);
                    // if turn
                } else if (buttonTextValue.equals(getResources().getString(R.string.buttonTurn))) {
                    img4.setImageResource(SecureRandomCard4);
                    tv.setText(R.string.buttonRiver);
                    // if river
                } else if (buttonTextValue.equals(getResources().getString(R.string.buttonRiver))) {
                    img5.setImageResource(SecureRandomCard5);
                    tv.setText(R.string.buttonStart);
                    // if start
                } else {
                    img1.setImageResource(R.drawable.card_back);
                    img2.setImageResource(R.drawable.card_back);
                    img3.setImageResource(R.drawable.card_back);
                    img4.setImageResource(R.drawable.card_back);
                    img5.setImageResource(R.drawable.card_back);
                    initDeck();
                    tv.setText(R.string.buttonFlop);
                }
            }
        });
    }

    private void initRestartButtonManagement() {
        final Button restartButton = (Button) findViewById(R.id.buttonRestart);
        restartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Get ImageView of each card
                final ImageView img1= (ImageView) findViewById(R.id.card1);
                final ImageView img2= (ImageView) findViewById(R.id.card2);
                final ImageView img3= (ImageView) findViewById(R.id.card3);
                final ImageView img4= (ImageView) findViewById(R.id.card4);
                final ImageView img5= (ImageView) findViewById(R.id.card5);

                // Add 1 second delay on button to avoid multiple click that cause crash
                restartButton.setEnabled(false);
                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                restartButton.setEnabled(true);
                            }
                        });
                    }
                }, 1000);

                final Button mainButton = (Button) findViewById(R.id.mainButton);
                // Add 1 second delay on button to avoid multiple click that cause crash
                mainButton.setEnabled(false);
                Timer buttonTimer2 = new Timer();
                buttonTimer2.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainButton.setEnabled(true);
                            }
                        });
                    }
                }, 1000);

                img1.setImageResource(R.drawable.card_back);
                img2.setImageResource(R.drawable.card_back);
                img3.setImageResource(R.drawable.card_back);
                img4.setImageResource(R.drawable.card_back);
                img5.setImageResource(R.drawable.card_back);
                initDeck();
                mainButton.setText(R.string.buttonFlop);
            }
        });
    }

    private int assignUser(String androidID) {
        // Verify if user exists in the poker player using the android ID.
        for (Player player : myPokerPlayerArray) {
            if (player.getAndroidID().equals(androidID)){
                return player.getNumber();
            }
        }
        // If user not found, create a new user.
        Player newPlayer=new Player();
        newPlayer.setNumber(myPokerPlayerArray.size()+1);
        newPlayer.setAndroidID(androidID);
        myPokerPlayerArray.add(newPlayer);
        return newPlayer.getNumber();
    }
    private void assignCards(PrintWriter os, int playerNumber) {
        if (playerNumber==1){
            os.println(SecureRandomCard1Player1);
            os.println(SecureRandomCard2Player1);
        } else if (playerNumber==2){
            os.println(SecureRandomCard1Player2);
            os.println(SecureRandomCard2Player2);
        } else if (playerNumber==3){
            os.println(SecureRandomCard1Player3);
            os.println(SecureRandomCard2Player3);
        } else if (playerNumber==4){
            os.println(SecureRandomCard1Player4);
            os.println(SecureRandomCard2Player4);
        } else if (playerNumber==5){
            os.println(SecureRandomCard1Player5);
            os.println(SecureRandomCard2Player5);
        } else if (playerNumber==6){
            os.println(SecureRandomCard1Player6);
            os.println(SecureRandomCard2Player6);
        } else if (playerNumber==7){
            os.println(SecureRandomCard1Player7);
            os.println(SecureRandomCard2Player7);
        } else if (playerNumber==8){
            os.println(SecureRandomCard1Player8);
            os.println(SecureRandomCard2Player8);
        } else if (playerNumber==9){
            os.println(SecureRandomCard1Player9);
            os.println(SecureRandomCard2Player9);
        } else if (playerNumber==10){
            os.println(SecureRandomCard1Player10);
            os.println(SecureRandomCard2Player10);
        }
    }


    private void initDeck() {
        handNumber++;
        TextView handNumberTV = (TextView) findViewById(R.id.handNumber);
        String fullHandNumber=getResources().getText(R.string.handNumber)+String.valueOf(handNumber);
        handNumberTV.setText(fullHandNumber);

        dealerPlayerNumber++;
        if (myPokerPlayerArray==null || dealerPlayerNumber>myPokerPlayerArray.size()){
            dealerPlayerNumber=1;
        }

        myPokerDeckArray=new ArrayList<>();
        // Add int value of drawable image in an array list
        myPokerDeckArray.add(R.drawable.card_2c);
        myPokerDeckArray.add(R.drawable.card_2d);
        myPokerDeckArray.add(R.drawable.card_2h);
        myPokerDeckArray.add(R.drawable.card_2s);
        myPokerDeckArray.add(R.drawable.card_3c);
        myPokerDeckArray.add(R.drawable.card_3d);
        myPokerDeckArray.add(R.drawable.card_3h);
        myPokerDeckArray.add(R.drawable.card_3s);
        myPokerDeckArray.add(R.drawable.card_4c);
        myPokerDeckArray.add(R.drawable.card_4d);
        myPokerDeckArray.add(R.drawable.card_4h);
        myPokerDeckArray.add(R.drawable.card_4s);
        myPokerDeckArray.add(R.drawable.card_5c);
        myPokerDeckArray.add(R.drawable.card_5d);
        myPokerDeckArray.add(R.drawable.card_5h);
        myPokerDeckArray.add(R.drawable.card_5s);
        myPokerDeckArray.add(R.drawable.card_6c);
        myPokerDeckArray.add(R.drawable.card_6d);
        myPokerDeckArray.add(R.drawable.card_6h);
        myPokerDeckArray.add(R.drawable.card_6s);
        myPokerDeckArray.add(R.drawable.card_7c);
        myPokerDeckArray.add(R.drawable.card_7d);
        myPokerDeckArray.add(R.drawable.card_7h);
        myPokerDeckArray.add(R.drawable.card_7s);
        myPokerDeckArray.add(R.drawable.card_8c);
        myPokerDeckArray.add(R.drawable.card_8d);
        myPokerDeckArray.add(R.drawable.card_8h);
        myPokerDeckArray.add(R.drawable.card_8s);
        myPokerDeckArray.add(R.drawable.card_9c);
        myPokerDeckArray.add(R.drawable.card_9d);
        myPokerDeckArray.add(R.drawable.card_9h);
        myPokerDeckArray.add(R.drawable.card_9s);
        myPokerDeckArray.add(R.drawable.card_tc);
        myPokerDeckArray.add(R.drawable.card_td);
        myPokerDeckArray.add(R.drawable.card_th);
        myPokerDeckArray.add(R.drawable.card_ts);
        myPokerDeckArray.add(R.drawable.card_jc);
        myPokerDeckArray.add(R.drawable.card_jd);
        myPokerDeckArray.add(R.drawable.card_jh);
        myPokerDeckArray.add(R.drawable.card_js);
        myPokerDeckArray.add(R.drawable.card_qc);
        myPokerDeckArray.add(R.drawable.card_qd);
        myPokerDeckArray.add(R.drawable.card_qh);
        myPokerDeckArray.add(R.drawable.card_qs);
        myPokerDeckArray.add(R.drawable.card_kc);
        myPokerDeckArray.add(R.drawable.card_kd);
        myPokerDeckArray.add(R.drawable.card_kh);
        myPokerDeckArray.add(R.drawable.card_ks);
        myPokerDeckArray.add(R.drawable.card_ac);
        myPokerDeckArray.add(R.drawable.card_ad);
        myPokerDeckArray.add(R.drawable.card_ah);
        myPokerDeckArray.add(R.drawable.card_as);

        // Get a SecureRandom card in the deck for each card : Flop / Turn / River
        setSecureRandomCard1(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1));
        setSecureRandomCard2(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2));
        setSecureRandomCard3(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard3));
        setSecureRandomCard4(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard4));
        setSecureRandomCard5(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard5));

        // Get 2 SecureRandom cards in the deck for ALL possible players (10 max).
        setSecureRandomCard1Player1(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player1));
        setSecureRandomCard2Player1(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player1));
        setSecureRandomCard1Player2(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player2));
        setSecureRandomCard2Player2(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player2));
        setSecureRandomCard1Player3(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player3));
        setSecureRandomCard2Player3(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player3));
        setSecureRandomCard1Player4(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player4));
        setSecureRandomCard2Player4(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player4));
        setSecureRandomCard1Player5(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player5));
        setSecureRandomCard2Player5(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player5));
        setSecureRandomCard1Player6(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player6));
        setSecureRandomCard2Player6(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player6));
        setSecureRandomCard1Player7(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player7));
        setSecureRandomCard2Player7(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player7));
        setSecureRandomCard1Player8(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player8));
        setSecureRandomCard2Player8(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player8));
        setSecureRandomCard1Player9(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player9));
        setSecureRandomCard2Player9(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player9));
        setSecureRandomCard1Player10(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard1Player10));
        setSecureRandomCard2Player10(myPokerDeckArray.get(new SecureRandom().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(SecureRandomCard2Player10));
    }
    public void setSecureRandomCard1(int SecureRandomCard1) {
        this.SecureRandomCard1 = SecureRandomCard1;
    }
    public void setSecureRandomCard2(int SecureRandomCard2) {
        this.SecureRandomCard2 = SecureRandomCard2;
    }
    public void setSecureRandomCard3(int SecureRandomCard3) {
        this.SecureRandomCard3 = SecureRandomCard3;
    }
    public void setSecureRandomCard4(int SecureRandomCard4) {
        this.SecureRandomCard4 = SecureRandomCard4;
    }
    public void setSecureRandomCard5(int SecureRandomCard5) {
        this.SecureRandomCard5 = SecureRandomCard5;
    }
    public void setSecureRandomCard1Player1(int SecureRandomCard) {
        this.SecureRandomCard1Player1 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player1(int SecureRandomCard) {
        this.SecureRandomCard2Player1 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player2(int SecureRandomCard) {
        this.SecureRandomCard1Player2 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player2(int SecureRandomCard) {
        this.SecureRandomCard2Player2 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player3(int SecureRandomCard) {
        this.SecureRandomCard1Player3 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player3(int SecureRandomCard) {
        this.SecureRandomCard2Player3 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player4(int SecureRandomCard) {
        this.SecureRandomCard1Player4 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player4(int SecureRandomCard) {
        this.SecureRandomCard2Player4 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player5(int SecureRandomCard) {
        this.SecureRandomCard1Player5 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player5(int SecureRandomCard) {
        this.SecureRandomCard2Player5 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player6(int SecureRandomCard) {
        this.SecureRandomCard1Player6 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player6(int SecureRandomCard) {
        this.SecureRandomCard2Player6 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player7(int SecureRandomCard) {
        this.SecureRandomCard1Player7 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player7(int SecureRandomCard) {
        this.SecureRandomCard2Player7 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player8(int SecureRandomCard) {
        this.SecureRandomCard1Player8 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player8(int SecureRandomCard) {
        this.SecureRandomCard2Player8 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player9(int SecureRandomCard) {
        this.SecureRandomCard1Player9 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player9(int SecureRandomCard) {
        this.SecureRandomCard2Player9 = SecureRandomCard;
    }
    public void setSecureRandomCard1Player10(int SecureRandomCard) {
        this.SecureRandomCard1Player10 = SecureRandomCard;
    }
    public void setSecureRandomCard2Player10(int SecureRandomCard) {
        this.SecureRandomCard2Player10 = SecureRandomCard;
    }
}
