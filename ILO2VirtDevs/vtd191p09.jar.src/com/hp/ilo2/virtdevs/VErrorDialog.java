package com.hp.ilo2.virtdevs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;

public class VErrorDialog extends Dialog implements java.awt.event.ActionListener
{
  java.awt.TextArea txt;
  java.awt.Button ok;
  boolean disp;
  
  public VErrorDialog(java.awt.Frame paramFrame, String paramString)
  {
    super(paramFrame, "Error", true);
    ui_init(paramString);
  }
  
  public VErrorDialog(String paramString, boolean paramBoolean) {
    super(new java.awt.Frame(), "Error", paramBoolean);
    ui_init(paramString);
  }
  
  protected void ui_init(String paramString) {
    this.txt = new java.awt.TextArea(paramString, 5, 40, 1);
    
    this.txt.setEditable(false);
    this.ok = new java.awt.Button("    Ok    ");
    this.ok.addActionListener(this);
    
    java.awt.GridBagLayout localGridBagLayout = new java.awt.GridBagLayout();
    java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
    
    setLayout(localGridBagLayout);
    setBackground(java.awt.Color.lightGray);
    setSize(300, 150);
    
    localGridBagConstraints.fill = 0;
    localGridBagConstraints.anchor = 10;
    localGridBagConstraints.weightx = 100.0D;
    localGridBagConstraints.weighty = 100.0D;
    localGridBagConstraints.gridx = 0;
    localGridBagConstraints.gridy = 0;
    localGridBagConstraints.gridwidth = 1;
    localGridBagConstraints.gridheight = 1;
    
    add(this.txt, localGridBagConstraints);
    
    localGridBagConstraints.gridy = 1;
    add(this.ok, localGridBagConstraints);
    show();
  }
  

  public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
  {
    if (paramActionEvent.getSource() == this.ok) {
      dispose();
      this.disp = true;
    }
  }
  
  public boolean disposed() {
    return this.disp;
  }
  
  public void append(String paramString) {
    this.txt.append(paramString);
    this.txt.repaint();
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/VErrorDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */