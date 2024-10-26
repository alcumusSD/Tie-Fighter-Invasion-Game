package com.example.dodgegame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Declare GameSurface Instance Defined in Inner Class
    ConstraintLayout constraintLayout;
    GameSurface gameSurface;
    TextView textView;
    MediaPlayer mediaPlayer, mediaPlayer2;
    Bitmap ball;
    Bitmap enemy;
    ArrayList<Enemy> tiefighter = new ArrayList<>();
    int score = 0;
    int enemyX;
    int enemyY;
    int speed = -5;
    int lives = 10;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize GameSurface in Context (In this case, the MainActivity)
        gameSurface = new GameSurface(this);
        // Instead of using the standard XML, we are employing the "Canvas" created in the GameSurface instance
        setContentView(gameSurface);
        mediaPlayer = MediaPlayer.create(this, R.raw.battlemusic);
        mediaPlayer2 = MediaPlayer.create(this, R.raw.arrowhit);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    // LifeCycle Methods employed to call the GameSurface pause/resume and therefore
    // ensure our game does not crash if/when the application is paused/resumed
    @Override
    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }

    // Define GameSurface
    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener {
        // Almost all of these variables are required anytime you are implementing a SurfaceView
        Thread gameThread;  // required for functionality
        SurfaceHolder holder; // required for functionality
        volatile boolean running = false; // variable shared amongst threads; required for functionality
        Bitmap ball, background, enemy;
        int ballX;
        Paint paintProperty; // required for functionality
        int screenWidth, screenHeight; // required for functionality
        float totalFlip = 0f;
        private ArrayList<Enemy> enemies = new ArrayList<>();
        private long lastSpawnTime = 0;
        private int spawnInterval = 2000; // Spawn interval in milliseconds

        public GameSurface(Context context) {
            super(context);
            // Initialize holder
            holder = getHolder();

            // Initialize resources
            background = BitmapFactory.decodeResource(getResources(), R.drawable.starwarsbackground);
            ball = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.xwing),
                    300, 300, false);
            enemy = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.tiefighter),
                    300, 300, false);


            // Retrieve screensize
            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;
            paintProperty = new Paint();

            // Needed if using MotionSensors to create the image movement
            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // to create movement based on change in z-axis
            // added a *-1 so it moves in direction of phone tilt instead inverted
            totalFlip = event.values[0] * speed;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void run() {
            Canvas canvas;
            Drawable d = getResources().getDrawable(R.drawable.starwarsbackground, null);

            while (running) {
                if (!holder.getSurface().isValid())
                    continue;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSpawnTime > spawnInterval) {
                    spawnEnemy();
                    lastSpawnTime = currentTime;
                }

                canvas = holder.lockCanvas(null);
                d.setBounds(getLeft(), getTop(), getRight(), getBottom());
                d.draw(canvas);
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(100);
                canvas.drawText("Lives: " + lives, 10, 2500, paint);
                canvas.drawText("Score: " + score, 1000, 2500, paint);


                // Define the spacing required to accommodate the image and screen size so images do not exceed bounds of the view
                float ballImageHorizontalSpacing = (screenWidth / 2.0f) - (ball.getWidth() / 2.0f);
                float ballImageVerticalSpacing = (screenHeight / 2.0f) - (ball.getHeight() / 2.0f);
                canvas.drawBitmap(ball, ballImageHorizontalSpacing + ballX, ballImageVerticalSpacing + 600, null);

                for (int i = 0; i < enemies.size(); i++) {
                    Enemy enemy = enemies.get(i);
                    enemy.move();
                    enemy.draw(canvas);

                    if (checkCollision((int) (ballImageHorizontalSpacing + ballX), (int) (ballImageVerticalSpacing + 700), ball, enemy)) {
                        lives--;
                        Log.d("tag", "Lives: " + lives);
                        enemies.remove(i);
                        i--;
                        mediaPlayer2.start();
                        if(lives == 0)
                        {
                            canvas.drawText("Lives:" + lives, 17, 2500, paint);
                            canvas.drawText("GAME OVER!", 500, 2700, paint);
                            running = false;
                        }
                    }

                    if (enemy.getyPos() > screenHeight) {
                        score++;
                        Log.d("Score","Score: " + score);
                        enemies.remove(i);
                        i--;
                    }
                }

                // With Sensors
                if (ballX + totalFlip < (int) ballImageHorizontalSpacing && ballX + totalFlip > -1 * (int) ballImageHorizontalSpacing) {
                    ballX += totalFlip;
                }
                holder.unlockCanvasAndPost(canvas);
            }
        }

        private void spawnEnemy() {
            int x = (int) (Math.random() * (screenWidth - enemy.getWidth()));
            enemies.add(new Enemy(x, 0, enemy));
        }

        public void resume()
        {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause()
        {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean checkCollision(int ballX, int ballY, Bitmap ballBitmap, Enemy enemy)
        {
            Rect ballRect = new Rect(ballX, ballY, (int) ballX + ballBitmap.getWidth(), (int) ballY + ballBitmap.getHeight());
            Rect enemyRect = new Rect(enemy.getxPos(), enemy.getyPos(), enemy.getxPos() + enemy.getBitMap().getWidth(), enemy.getyPos() + enemy.getBitMap().getHeight());
            return Rect.intersects(ballRect, enemyRect);
        }

        public boolean onTouchEvent(MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.d("tag", "Hello");
                if(score >= 5)
                {
                    speed = -20;
                    Log.d("tag", String.valueOf(speed));
                    return true;
                }
            }
            return false;
        }
    }
}
