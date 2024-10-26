package com.example.dodgegame;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Enemy {
    private int xPos;
    private int yPos;
    private Bitmap bitMap;

    public Enemy(int xPos, int yPos, Bitmap bitMap)
    {
        this.xPos = xPos;
        this.yPos = yPos;
        this.bitMap = bitMap;
    }

    public void move()
    {
        yPos += 100;
    }
    public int getxPos()
    {
        return xPos;
    }

    public int getyPos()
    {
        return yPos;
    }

    public Bitmap getBitMap()
    {
        return bitMap;
    }

    public void draw(Canvas canvas)
    {
        canvas.drawBitmap(bitMap, xPos , yPos,  null);

    }

}
