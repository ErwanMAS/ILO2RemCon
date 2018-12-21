package com.hp.ilo2.virtdevs;

import java.awt.FileDialog;
import java.awt.event.WindowEvent;

public class VFileDialog extends java.awt.Frame implements java.awt.event.WindowListener
{
  private FileDialog fd;
  
  VFileDialog(String paramString)
  {
    super(paramString);
    addWindowListener(this);
    this.fd = new FileDialog(this, paramString);
    this.fd.setVisible(true);
  }
  
  public String getString()
  {
    String path = null;
    if ((this.fd.getDirectory() != null) && (this.fd.getFile() != null)) {
      path = this.fd.getDirectory() + this.fd.getFile();
    }
    return path;
  }
  
  public void windowClosing(WindowEvent paramWindowEvent) { setVisible(false); }
  
  public void windowActivated(WindowEvent paramWindowEvent) {}
  
  public void windowClosed(WindowEvent paramWindowEvent) {}
  
  public void windowDeactivated(WindowEvent paramWindowEvent) {}
  
  public void windowDeiconified(WindowEvent paramWindowEvent) {}
  
  public void windowIconified(WindowEvent paramWindowEvent) {}
  
  public void windowOpened(WindowEvent paramWindowEvent) {}
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/VFileDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */