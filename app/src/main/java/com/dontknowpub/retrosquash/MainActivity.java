package com.dontknowpub.retrosquash;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;



public class MainActivity extends Activity {
    Canvas canvas;
    SquashCourtView squashCourtView;

    //Sound
    //initialize sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //For getting display details like the number of pixels
    Display display;
    Point size;
    int screenWidth;
    int screenHeight;

    //Game objects
    int racketWidth;
    int racketHeight;
    Point racketPosition;

    Point ballPosition;
    int ballWidth;

    //for all ball movements
    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    //for racket movement
    boolean racketIsMovingLeft;
    boolean racketIsMovingRight;

    //stats
    Long lastFrameTime;
    int fps;
    int score;
    int lives;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        squashCourtView = new SquashCourtView(this);
        setContentView(squashCourtView);

        //Sound code
        soundPool = new SoundPool(10,AudioManager.STREAM_MUSIC, 0);
        try {
            //Create objects of the 2 required classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create our three fx in memory ready for use
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);
        } //end of try
        catch (IOException e ){
            // catch exceptions here
        } //end of catch

        // Could this be an object with getters and setters
        // Don't want just anyone changing screen size
        // Get the screen size in pixels
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // The game objects
        racketPosition = new Point();
        racketPosition.x = screenWidth/2;
        racketPosition.y = screenHeight - 20;
        racketWidth = screenWidth /8;
        racketHeight = 10;

        ballWidth = screenWidth / 35;
        ballPosition = new Point();
        ballPosition.x = screenWidth /2;
        ballPosition.y = 1+ballWidth;

        lives = 3;

    } //ends onCreate
    class SquashCourtView extends SurfaceView implements Runnable{
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSquash;

        public SquashCourtView(Context context){
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballIsMovingDown = true;

            //Send the ball in random direction
            Random randomNumber = new Random();
            int ballDirection = randomNumber.nextInt(3);
            switch (ballDirection){
                case 0:
                     ballIsMovingLeft = true;
                    ballIsMovingRight = false;
                    break;

                case 1:
                    ballIsMovingRight = true;
                    ballIsMovingLeft = false;
                    break;
                case 2:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;

            }//end switch
        } //ends SquashCourView

        @Override
        public  void run() {
            while(playingSquash) {
                updateCourt();
                drawCourt();
                controlFPS();
            } //end while
        }// end run

        public void updateCourt() {
            if (racketIsMovingRight) {
                racketPosition.x = racketPosition.x + 10;
            }// if racketIsMovingRight

            if (racketIsMovingLeft) {
                racketPosition.x = racketPosition.x - 10;
            } // end if racketIsMovingLeft

            // detect collisions

            //hit right of screen
            if(ballPosition.x + ballWidth > screenWidth) {
                ballIsMovingLeft = true;
                ballIsMovingRight = false;
                soundPool.play(sample1,1, 1, 0, 0, 0, 1);
            }// end if ballPosition is greater then screenWidth

            //hit left of screen
            if (ballPosition < 0) {
                ballIsMovingLeft = false;
                ballIsMovingRight = true;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }//end if ball position left

            //Edge of ball has hit bottom of screen
            if(ballPosition.y > screenHeight - ballWidth) {
                lives = lives - 1;
                if(lives == 0){
                    lives = 3;
                    score = 0;
                    soundPool.play(sample4, 1, 1, 0, 0, 1);
                }//end if lives = 0

                ballPosition.y = 1 + ballWidth; //back to top of screen

                //what horizontal direction should we use
                //for the next falling ball
                Random randomNumber = new Random();
                int startX = randomNumber.nextInt(screenWidth - ballWidth) + 1;
                ballPosition.x = startX + ballWidth;

                int ballDirection = randomNumber.nextInt(3);
                switch (ballDirection){
                    case 0:
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                        break;

                    case 1:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = true;
                        break;

                    case 2:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = false;
                        break;

                }// end switch

            } //end of if ballPosition y

            // ball hits the top of the screen
            if(ballPosition.y <= 0){
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPosition.y = 1;
                soundPool(sample2, 1, 1, 0, 0, 1);
            }//end if for ball podition moving down

            // depending upon the two directions we should
            // be moving in adjust our x any positions
            if(ballIsMovingDown) {
                ballPosition.y += 6;
            }//end ball moving down

            if(ballIsMovingUp){
                ballPosition.y -= 10;
            }//end ball moving up

            if (ballIsMovingLeft){
                ballPosition.x -= 12;
            }// end ball moving left

            if(ballIsMovingRight){
                ballPosition.x += 12;
            }// end ball moving right

            //Has the ball hit the racket
            if(ballPosition.y + ballWidth >= (racketPosition.y - racketHeight /2)){
                int halfRacket = racketWidth / 2;
                if (ballPosition.x + ballWidth > (racketPosition.x - halfRacket)
                        && ballPosition.x - ballWidth < (racketPosition.x + halfRacket)){
                    //rebound the ball vertically and play a sound
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;
                    //now decide how to rebound the ball horizontally
                    if (ballPosition.x > racketPosition.x) {
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    }//end if horizontal ball rebound
                    else{
                        ballIsMovingRight = false;
                        ballIsMovingLeft = true;
                    }//end else
                }// end ball position x
            }//end if ball hits the racket


        }// end update Court

    } //ends SquashCourtView Class


} //ends mainActivity


