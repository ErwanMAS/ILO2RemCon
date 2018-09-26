package com.hp.ilo2.virtdevs;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;





public class VProgressBar
  extends Canvas
{
  private int progressWidth;
  private int progressHeight;
  private float percentage;
  private Image offscreenImg;
  private Graphics offscreenG;
  private Color progressColor = Color.red;
  private Color progressBackground = Color.white;
  





  public VProgressBar(int paramInt1, int paramInt2)
  {
    Font localFont = new Font("Dialog", 0, 15);
    setFont(localFont);
    
    this.progressWidth = paramInt1;
    this.progressHeight = paramInt2;
    setSize(paramInt1, paramInt2);
  }
  







  public VProgressBar(int paramInt1, int paramInt2, Color paramColor1, Color paramColor2, Color paramColor3)
  {
    Font localFont = new Font("Dialog", 0, 12);
    setFont(localFont);
    
    this.progressWidth = paramInt1;
    this.progressHeight = paramInt2;
    this.progressColor = paramColor2;
    this.progressBackground = paramColor3;
    setSize(paramInt1, paramInt2);
    
    setBackground(paramColor1);
  }
  








  public void updateBar(float paramFloat)
  {
    this.percentage = paramFloat;
    repaint();
  }
  




  public void setCanvasColor(Color paramColor)
  {
    setBackground(paramColor);
  }
  



  public void setProgressColor(Color paramColor)
  {
    this.progressColor = paramColor;
  }
  



  public void setBackGroundColor(Color paramColor)
  {
    this.progressBackground = paramColor;
  }
  








  public void paint(Graphics paramGraphics)
  {
    int i = 0;
    int j = 0;
    int k = 4;
    
    if (this.offscreenImg == null) {
      this.offscreenImg = createImage(this.progressWidth - k, this.progressHeight - k);
    }
    this.offscreenG = this.offscreenImg.getGraphics();
    
    i = this.offscreenImg.getWidth(this);
    j = this.offscreenImg.getHeight(this);
    
    this.offscreenG.setColor(this.progressBackground);
    this.offscreenG.fillRect(0, 0, i, j);
    
    this.offscreenG.setColor(this.progressColor);
    this.offscreenG.fillRect(0, 0, (int)(i * this.percentage), j);
    this.offscreenG.drawString(Integer.toString((int)(this.percentage * 100.0F)) + "%", i / 2 - 8, j / 2 + 5);
    
    this.offscreenG.clipRect(0, 0, (int)(i * this.percentage), j);
    this.offscreenG.setColor(this.progressBackground);
    this.offscreenG.drawString(Integer.toString((int)(this.percentage * 100.0F)) + "%", i / 2 - 8, j / 2 + 5);
    
    paramGraphics.setColor(this.progressBackground);
    paramGraphics.draw3DRect(getSize().width / 2 - this.progressWidth / 2, 0, this.progressWidth - 1, this.progressHeight - 1, false);
    
    paramGraphics.drawImage(this.offscreenImg, k / 2, k / 2, this);
  }
  



  public void update(Graphics paramGraphics)
  {
    paint(paramGraphics);
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/VProgressBar.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */