package com.mihalic.franck.cardless_poker;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    private NsdServiceInfo cardlessPokerServerService;
    private int randomCard1;
    private int randomCard2;
    private int playerNumber;
    private int handNumber;
    private static final int SERVER_PORT_EMULATED = 5000;
    // Attribute name for GOT fans only
    private static final int SERVER_PORT_REAL = 6000;
    private static final String SERVER_IP_EMULATED = "10.0.2.2";
    boolean isEmulated;
    boolean isDealer;
    boolean isHiddenCards=true;
    Handler timerHandler;
    ImageView img1;
    ImageView img2;
    ImageView imgDealer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Try to see if code is run on an emulator
        isEmulated=isEmulator();

        // Buttons management
        initShowHideCardsButtonManagement();
        initShowCardsButtonManagement();

        // Try to discover service CardlessPokerServer on network
        FindServicesNSD service = new FindServicesNSD((NsdManager)getSystemService(NSD_SERVICE), "_http._tcp");
        service.run();

        // Leave 2 sec to scan network
        sleep(2000);

        // Get the server service
        cardlessPokerServerService = service.getCardlessPokerServerService();

        // If no server found and not running on emulated device
        if (cardlessPokerServerService==null && !isEmulated){
            alertNoNetwork(String.valueOf(getResources().getText(R.string.dialogNoServerTitle)),
                    String.valueOf(getResources().getText(R.string.dialogNoServerMessage)));
        } else {
            // Real life or emulated device.
            // Call thread to reach server
            new Thread(new ClientThread()).start();
            // leave 0,1 second to get player number and cards
            sleep(100);
            // Update Player number on screen
            updatePlayerNumber();
            // Update Hand number on screen
            updateHandNumber();
            // Update dealer image on screen
            updateDealerImage();
            // Create an handler to call server every X seconds
            refreshDisplayTimer();
        }
    }

    private void initShowHideCardsButtonManagement() {
        final Button mainButton = (Button) findViewById(R.id.showHideCardButton);
        mainButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Thread(new ClientThread()).start();
                    // leave 0,1 sec to update card with call on server
                    sleep(100);
                    updateCards();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    hideCards();
                }
                updateHandNumber();
                updateDealerImage();
                return true;
            }
        });
    }

    private void initShowCardsButtonManagement() {
        final Button mainButton = (Button) findViewById(R.id.showCardButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new ClientThread()).start();
                // leave 0,1 sec to update card with call on server
                sleep(100);
                updateCards();
                updateHandNumber();
                updateDealerImage();
            }
        });
    }

    private void refreshDisplayTimer(){
        // Handler to update cards every X seconds
        // This will help when there is a new flop to display fresh cards without any action
        timerHandler = new Handler();
        final int delay = 3000; //milliseconds
        timerHandler.postDelayed(new Runnable(){
            public void run(){
                new Thread(new ClientThread()).start();
                // Leave 0,1 sec to update card with call on server
                sleep(100);

                // Check if it's a new hand (to auto hide cards);
                updateIsNewHand();
                if (isHiddenCards) {
                    hideCards();
                } else {
                    updateCards();
                }

                updateHandNumber();
                updateDealerImage();
                // Update Player number on screen
                updatePlayerNumber();
                timerHandler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void updateIsNewHand() {
        // If new hand, card are auto hidden
        TextView handNumberTV = (TextView) findViewById(R.id.handNumber);
        String str=(String)handNumberTV.getText();
        String substr=str.substring(str.indexOf("#") + 1);
        int handNumberDisplay = Integer.valueOf(substr);
        if (handNumberDisplay!=handNumber) {
            isHiddenCards = true;
        }
    }

    private class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                // Create Socket used for communication with server
                Socket socket;
                if (isEmulated){
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP_EMULATED);
                    socket = new Socket(serverAddr, SERVER_PORT_EMULATED);

                } else {
                    // TODO see what can be done with ports on real devices
                    InetAddress serverAddr = cardlessPokerServerService.getHost();
                    socket = new Socket(serverAddr, SERVER_PORT_REAL);
                }

                // PrintWriter used to send data to server
                PrintWriter output = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);

                // BufferedReader used to receive data from server
                BufferedReader input;
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send player number to server to get card.
                // If 0 it's an init and server will return a new player number
                output.println(playerNumber);
                playerNumber = Integer.valueOf(input.readLine());
                // Get hand number autoincrement by server
                handNumber = Integer.valueOf(input.readLine());
                // Get cards
                randomCard1 = Integer.valueOf(input.readLine());
                randomCard2 = Integer.valueOf(input.readLine());
                int dealerNumber=Integer.valueOf(input.readLine());
                // If dealer number sent by server equals current player, current player is the dealer
                isDealer = dealerNumber==playerNumber;
                // Cleaning
                input.close();
                output.close();
                socket.close();
            } catch (InterruptedIOException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrompu via InterruptedIOException");
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private void updateCards() {
        img1= (ImageView) findViewById(R.id.card1);
        img2= (ImageView) findViewById(R.id.card2);
        img1.setImageResource(randomCard1);
        img2.setImageResource(randomCard2);
        isHiddenCards=false;
    }

    private void hideCards() {
        img1= (ImageView) findViewById(R.id.card1);
        img2= (ImageView) findViewById(R.id.card2);
        img1.setImageResource(R.drawable.card_back);
        img2.setImageResource(R.drawable.card_back);
        isHiddenCards=true;
    }

    private void updateDealerImage() {
        imgDealer= (ImageView) findViewById(R.id.dealerImage);
        if (isDealer){
            imgDealer.setVisibility(View.VISIBLE);
        } else {
            imgDealer.setVisibility(View.INVISIBLE);
        }
    }

    private void updateHandNumber() {
        TextView handNumberTV = (TextView) findViewById(R.id.handNumber);
        String fullHandNumber=getResources().getText(R.string.handNumber)+String.valueOf(handNumber);
        handNumberTV.setText(fullHandNumber);
    }

    private void updatePlayerNumber() {
        TextView playerNumberTV = (TextView) findViewById(R.id.playerNumber);
        String fullPlayerNumber=getResources().getText(R.string.playerNumber)+String.valueOf(playerNumber);
        playerNumberTV.setText(fullPlayerNumber);
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void alertNoNetwork(String title, String message) {
        new AlertDialog.Builder(ClientActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        intent = new Intent(ClientActivity.this, ConnectActivity.class);
                        startActivity( intent );
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(ClientActivity.this)
                .setTitle("Confirmation")
                .setMessage("Are you sure to want to go back to main menu ?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        intent = new Intent(ClientActivity.this, ConnectActivity.class);
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

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
