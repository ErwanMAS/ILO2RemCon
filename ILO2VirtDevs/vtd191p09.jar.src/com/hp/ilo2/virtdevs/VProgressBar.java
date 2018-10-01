package com.hp.ilo2.virtdevs;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;


public class VProgressBar extends Canvas {
    private int progressWidth;
    private int progressHeight;
    private float percentage;
    private Image offscreenImg;
    private Color progressColor = Color.red;
    private Color progressBackground = Color.white;


    public VProgressBar(int paramInt1, int paramInt2) {
        Font localFont = new Font("Dialog", Font.PLAIN, 15);
        setFont(localFont);

        this.progressWidth = paramInt1;
        this.progressHeight = paramInt2;
        setSize(paramInt1, paramInt2);
    }


    public VProgressBar(int paramInt1, int paramInt2, Color paramColor1, Color paramColor2, Color paramColor3) {
        Font localFont = new Font("Dialog", Font.PLAIN, 12);
        setFont(localFont);

        this.progressWidth = paramInt1;
        this.progressHeight = paramInt2;
        this.progressColor = paramColor2;
        this.progressBackground = paramColor3;
        setSize(paramInt1, paramInt2);

        setBackground(paramColor1);
    }


    public void updateBar(float paramFloat) {
        this.percentage = paramFloat;
        repaint();
    }


    public void setCanvasColor(Color paramColor) {
        setBackground(paramColor);
    }


    public void setProgressColor(Color paramColor) {
        this.progressColor = paramColor;
    }


    public void setBackGroundColor(Color paramColor) {
        this.progressBackground = paramColor;
    }


    public void paint(Graphics paramGraphics) {
        int k = 4;

        if (this.offscreenImg == null) {
            this.offscreenImg = createImage(this.progressWidth - k, this.progressHeight - k);
        }
        Graphics offscreenG = this.offscreenImg.getGraphics();

        int i = this.offscreenImg.getWidth(this);
        int j = this.offscreenImg.getHeight(this);

        offscreenG.setColor(this.progressBackground);
        offscreenG.fillRect(0, 0, i, j);

        offscreenG.setColor(this.progressColor);
        offscreenG.fillRect(0, 0, (int) (i * this.percentage), j);
        offscreenG.drawString(Integer.toString((int) (this.percentage * 100.0F)) + "%", i / 2 - 8, j / 2 + 5);

        offscreenG.clipRect(0, 0, (int) (i * this.percentage), j);
        offscreenG.setColor(this.progressBackground);
        offscreenG.drawString(Integer.toString((int) (this.percentage * 100.0F)) + "%", i / 2 - 8, j / 2 + 5);

        paramGraphics.setColor(this.progressBackground);
        paramGraphics.draw3DRect(getSize().width / 2 - this.progressWidth / 2, 0, this.progressWidth - 1, this.progressHeight - 1, false);

        paramGraphics.drawImage(this.offscreenImg, k / 2, k / 2, this);
    }


    public void update(Graphics paramGraphics) {
        paint(paramGraphics);
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/VProgressBar.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */