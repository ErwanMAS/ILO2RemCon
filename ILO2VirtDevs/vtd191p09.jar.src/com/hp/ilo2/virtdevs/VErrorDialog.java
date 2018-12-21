package com.hp.ilo2.virtdevs;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VErrorDialog extends Dialog {
  public VErrorDialog(Frame owner, String paramString) {
    super(owner, "Error", true);
    uiInit(paramString);
  }
  
  public VErrorDialog(String message, boolean isModal) {
    super(new Frame(), "Error", isModal);
    uiInit(message);
  }
  
  private void uiInit(String message) {
    GridBagLayout layout = new GridBagLayout();
    setLayout(layout);

    setBackground(java.awt.Color.lightGray);
    setSize(300, 150);

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = 0;
    constraints.anchor = 10;
    constraints.weightx = 100.0D;
    constraints.weighty = 100.0D;
    constraints.gridx = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;

    constraints.gridy = 0;
      TextArea messageTextArea = new TextArea(message, 5, 40, 1);
      messageTextArea.setEditable(false);

      add(messageTextArea, constraints);

      Button ok = new Button("    Ok    ");
      ok.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          dispose();
        }
      });
    
    constraints.gridy = 1;
      add(ok, constraints);

    setVisible(true);
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/VErrorDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */