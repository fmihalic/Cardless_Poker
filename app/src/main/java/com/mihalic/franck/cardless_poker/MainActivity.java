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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    List<Integer> myPokerDeckArray;
    List<Player> myPokerPlayerArray;
    int randomCard1;
    int randomCard2;
    int randomCard3;
    int randomCard4;
    int randomCard5;

    int randomCard1Player1;
    int randomCard2Player1;
    int randomCard1Player2;
    int randomCard2Player2;
    int randomCard1Player3;
    int randomCard2Player3;
    int randomCard1Player4;
    int randomCard2Player4;
    int randomCard1Player5;
    int randomCard2Player5;
    int randomCard1Player6;
    int randomCard2Player6;
    int randomCard1Player7;
    int randomCard2Player7;
    int randomCard1Player8;
    int randomCard2Player8;
    int randomCard1Player9;
    int randomCard2Player9;
    int randomCard1Player10;
    int randomCard2Player10;

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
            // todo manage socket exception here.
        }

        // Initialize Button management
        initButtonManagement();

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
                    if(playerNumber==0){
                        playerNumber=assignUser();
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
                    img1.setImageResource(randomCard1);
                    img2.setImageResource(randomCard2);
                    img3.setImageResource(randomCard3);
                    tv.setText(R.string.buttonTurn);
                    // if turn
                } else if (buttonTextValue.equals(getResources().getString(R.string.buttonTurn))) {
                    img4.setImageResource(randomCard4);
                    tv.setText(R.string.buttonRiver);
                    // if river
                } else if (buttonTextValue.equals(getResources().getString(R.string.buttonRiver))) {
                    img5.setImageResource(randomCard5);
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

    private int assignUser() {
        // TODO: 25/04/2017 : manager user connect and disconnect. for now assign +1 to user number
        Player newPlayer=new Player();
        newPlayer.setNumber(myPokerPlayerArray.size()+1);
        newPlayer.setActive(true);
        myPokerPlayerArray.add(newPlayer);
        return newPlayer.getNumber();
    }
    private void assignCards(PrintWriter os, int playerNumber) {
        if (playerNumber==1){
            os.println(randomCard1Player1);
            os.println(randomCard2Player1);
        } else if (playerNumber==2){
            os.println(randomCard1Player2);
            os.println(randomCard2Player2);
        } else if (playerNumber==3){
            os.println(randomCard1Player3);
            os.println(randomCard2Player3);
        } else if (playerNumber==4){
            os.println(randomCard1Player4);
            os.println(randomCard2Player4);
        } else if (playerNumber==5){
            os.println(randomCard1Player5);
            os.println(randomCard2Player5);
        } else if (playerNumber==6){
            os.println(randomCard1Player6);
            os.println(randomCard2Player6);
        } else if (playerNumber==7){
            os.println(randomCard1Player7);
            os.println(randomCard2Player7);
        } else if (playerNumber==8){
            os.println(randomCard1Player8);
            os.println(randomCard2Player8);
        } else if (playerNumber==9){
            os.println(randomCard1Player9);
            os.println(randomCard2Player9);
        } else if (playerNumber==10){
            os.println(randomCard1Player10);
            os.println(randomCard2Player10);
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

        // Get a random card in the deck for each card : Flop / Turn / River
        setRandomCard1(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1));
        setRandomCard2(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2));
        setRandomCard3(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard3));
        setRandomCard4(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard4));
        setRandomCard5(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard5));

        // Get 2 random cards in the deck for ALL possible players (10 max).
        setRandomCard1Player1(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player1));
        setRandomCard2Player1(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player1));
        setRandomCard1Player2(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player2));
        setRandomCard2Player2(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player2));
        setRandomCard1Player3(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player3));
        setRandomCard2Player3(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player3));
        setRandomCard1Player4(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player4));
        setRandomCard2Player4(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player4));
        setRandomCard1Player5(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player5));
        setRandomCard2Player5(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player5));
        setRandomCard1Player6(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player6));
        setRandomCard2Player6(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player6));
        setRandomCard1Player7(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player7));
        setRandomCard2Player7(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player7));
        setRandomCard1Player8(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player8));
        setRandomCard2Player8(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player8));
        setRandomCard1Player9(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player9));
        setRandomCard2Player9(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player9));
        setRandomCard1Player10(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard1Player10));
        setRandomCard2Player10(myPokerDeckArray.get(new Random().nextInt(myPokerDeckArray.size())));
        myPokerDeckArray.remove(myPokerDeckArray.indexOf(randomCard2Player10));
    }
    public void setRandomCard1(int randomCard1) {
        this.randomCard1 = randomCard1;
    }
    public void setRandomCard2(int randomCard2) {
        this.randomCard2 = randomCard2;
    }
    public void setRandomCard3(int randomCard3) {
        this.randomCard3 = randomCard3;
    }
    public void setRandomCard4(int randomCard4) {
        this.randomCard4 = randomCard4;
    }
    public void setRandomCard5(int randomCard5) {
        this.randomCard5 = randomCard5;
    }
    public void setRandomCard1Player1(int randomCard) {
        this.randomCard1Player1 = randomCard;
    }
    public void setRandomCard2Player1(int randomCard) {
        this.randomCard2Player1 = randomCard;
    }
    public void setRandomCard1Player2(int randomCard) {
        this.randomCard1Player2 = randomCard;
    }
    public void setRandomCard2Player2(int randomCard) {
        this.randomCard2Player2 = randomCard;
    }
    public void setRandomCard1Player3(int randomCard) {
        this.randomCard1Player3 = randomCard;
    }
    public void setRandomCard2Player3(int randomCard) {
        this.randomCard2Player3 = randomCard;
    }
    public void setRandomCard1Player4(int randomCard) {
        this.randomCard1Player4 = randomCard;
    }
    public void setRandomCard2Player4(int randomCard) {
        this.randomCard2Player4 = randomCard;
    }
    public void setRandomCard1Player5(int randomCard) {
        this.randomCard1Player5 = randomCard;
    }
    public void setRandomCard2Player5(int randomCard) {
        this.randomCard2Player5 = randomCard;
    }
    public void setRandomCard1Player6(int randomCard) {
        this.randomCard1Player6 = randomCard;
    }
    public void setRandomCard2Player6(int randomCard) {
        this.randomCard2Player6 = randomCard;
    }
    public void setRandomCard1Player7(int randomCard) {
        this.randomCard1Player7 = randomCard;
    }
    public void setRandomCard2Player7(int randomCard) {
        this.randomCard2Player7 = randomCard;
    }
    public void setRandomCard1Player8(int randomCard) {
        this.randomCard1Player8 = randomCard;
    }
    public void setRandomCard2Player8(int randomCard) {
        this.randomCard2Player8 = randomCard;
    }
    public void setRandomCard1Player9(int randomCard) {
        this.randomCard1Player9 = randomCard;
    }
    public void setRandomCard2Player9(int randomCard) {
        this.randomCard2Player9 = randomCard;
    }
    public void setRandomCard1Player10(int randomCard) {
        this.randomCard1Player10 = randomCard;
    }
    public void setRandomCard2Player10(int randomCard) {
        this.randomCard2Player10 = randomCard;
    }
}
