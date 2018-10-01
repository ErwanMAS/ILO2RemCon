package com.hp.ilo2.virtdevs;

import java.awt.*;

public class CreateImage extends java.awt.Dialog implements java.awt.event.ActionListener, java.awt.event.WindowListener, java.awt.event.TextListener, java.awt.event.ItemListener, Runnable {
  java.awt.Choice fdDrive;
  java.awt.TextField ImgFile;
  java.awt.Button browse;
  java.awt.Button create;
  java.awt.Button cancel;
  java.awt.Button dimg;
  VProgressBar progress;
  boolean canceled = false;
  boolean diskimage = true;
  boolean iscdrom = false;
  java.awt.Frame frame;
  String[] dev;
  int[] devt;
  boolean defaultRemovable = false;
  int retrycount = 10;
  private java.awt.Label statLabel;
  
  public CreateImage(java.awt.Frame paramFrame)
  {
    super(paramFrame, "Create Media Image");
    int i = 1;
    int j = 0;
    
    this.frame = paramFrame;
    
    java.awt.Panel localPanel = new java.awt.Panel(new java.awt.FlowLayout(2, 10, 5));
    

    setSize(400, 185);
    setResizable(false);
    setModal(false);
    addWindowListener(this);
    
    java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
    

    setLayout(new java.awt.GridBagLayout());
    setBackground(java.awt.Color.lightGray);
    
    ChiselBox localChiselBox = new ChiselBox("Create Disk Image");
    localChiselBox.content.setLayout(new java.awt.GridBagLayout());
    localGridBagConstraints.anchor = 11;
    localGridBagConstraints.fill = 1;
    localGridBagConstraints.weightx = 100.0D;
    localGridBagConstraints.weighty = 100.0D;
    add(localChiselBox, localGridBagConstraints, 0, 0, 1, 1);
    
    localGridBagConstraints.fill = 0;
    localGridBagConstraints.weightx = 100.0D;
    localGridBagConstraints.weighty = 100.0D;
    localGridBagConstraints.anchor = 13;
    java.awt.Label localLabel = new java.awt.Label("Drive");
    localChiselBox.cadd(localLabel, localGridBagConstraints, 0, 0, 1, 1);
    
    localLabel = new java.awt.Label("Image File");
    localChiselBox.cadd(localLabel, localGridBagConstraints, 0, 1, 1, 1);
    
    localGridBagConstraints.fill = 2;
    localGridBagConstraints.anchor = 17;
    this.fdDrive = new java.awt.Choice();
    
    MediaAccess localMediaAccess = new MediaAccess();
    this.dev = localMediaAccess.devices();
    this.devt = new int[this.dev.length];
    for (int k = 0; k < this.dev.length; k++)
    {
      this.devt[k] = localMediaAccess.devtype(this.dev[k]);
      
      if (this.devt[k] == 2) {
        this.fdDrive.add(this.dev[k]);
        i = 0;
        this.defaultRemovable = true;
      }
      if ((this.devt[k] == 5) && (virtdevs.cdimg_support)) {
        this.fdDrive.add(this.dev[k]);
        if (k == 0) {
          this.iscdrom = true;
        } else if (!this.defaultRemovable) {
          this.iscdrom = true;
          j = 1;
        }
        i = 0;
      }
    }
    if (i != 0)
      this.fdDrive.add("None");
    localMediaAccess = null;
    this.fdDrive.addItemListener(this);
    localChiselBox.cadd(this.fdDrive, localGridBagConstraints, 1, 0, 1, 1);
    
    this.ImgFile = new java.awt.TextField();
    this.ImgFile.addTextListener(this);
    localChiselBox.cadd(this.ImgFile, localGridBagConstraints, 1, 1, 1, 1);
    
    this.progress = new VProgressBar(350, 25, java.awt.Color.lightGray, java.awt.Color.blue, java.awt.Color.white);
    

    localGridBagConstraints.anchor = 10;
    localChiselBox.cadd(this.progress, localGridBagConstraints, 0, 2, 3, 1);
    
    localGridBagConstraints.fill = 2;
    
    this.dimg = new java.awt.Button("Disk >> Image");
    localChiselBox.cadd(this.dimg, localGridBagConstraints, 2, 0, 1, 1);
    this.dimg.addActionListener(this);
    
    this.statLabel = new java.awt.Label("                                                                                 ");
    this.statLabel.setFont(new java.awt.Font("Arial", Font.BOLD, 12));
    localPanel.add(this.statLabel);
    
    this.browse = new java.awt.Button("Browse");
    localChiselBox.cadd(this.browse, localGridBagConstraints, 2, 1, 1, 1);
    this.browse.addActionListener(this);
    
    this.create = new java.awt.Button("Create");
    this.create.setEnabled(false);
    localPanel.add(this.create);
    this.create.addActionListener(this);
    
    this.cancel = new java.awt.Button("Cancel");
    localPanel.add(this.cancel);
    this.cancel.addActionListener(this);
    
    localGridBagConstraints.fill = 2;
    localGridBagConstraints.weighty = 0.0D;
    add(localPanel, localGridBagConstraints, 0, 1, 1, 1);
    
    setVisible(true);
    
    if (j != 0) {
      this.dimg.setLabel("Disk >> Image");
      this.diskimage = true;
      this.dimg.setEnabled(false);
    } else {
      this.dimg.setEnabled(true);
    }
    this.dimg.repaint();
  }
  

  void add(Component paramComponent, java.awt.GridBagConstraints paramGridBagConstraints, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    paramGridBagConstraints.gridx = paramInt1;
    paramGridBagConstraints.gridy = paramInt2;
    paramGridBagConstraints.gridwidth = paramInt3;
    paramGridBagConstraints.gridheight = paramInt4;
    add(paramComponent, paramGridBagConstraints);
  }
  
  public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
  {
    Object localObject1 = paramActionEvent.getSource();
    Object localObject2; if (localObject1 == this.browse) {
      this.statLabel.setText(" ");
      this.progress.updateBar(0.0F);
      localObject2 = new VFileDialog("Create Disk Image");
      String str = ((VFileDialog)localObject2).getString();
      if (str != null) {
        this.ImgFile.setText(str);
        if (!this.fdDrive.getSelectedItem().equals("None"))
          this.create.setEnabled(true);
      }
    }
    if (localObject1 == this.create) {
      this.create.setEnabled(false);
      this.browse.setEnabled(false);
      this.fdDrive.setEnabled(false);
      this.ImgFile.setEnabled(false);
      this.dimg.setEnabled(false);
      if (this.diskimage) {
        this.statLabel.setText("Creating image file, please wait...");
      } else
        this.statLabel.setText("Creating disk, please wait...");
      localObject2 = new Thread(this);
      ((Thread)localObject2).start();
    }
    if (localObject1 == this.dimg) {
      this.statLabel.setText(" ");
      this.progress.updateBar(0.0F);
      this.diskimage = (!this.diskimage);
      if (this.diskimage) {
        this.dimg.setLabel("Disk >> Image");
      } else {
        this.dimg.setLabel("Image >> Disk");
      }
      this.dimg.repaint();
    }
    if (localObject1 == this.cancel) {
      this.statLabel.setText(" ");
      this.progress.updateBar(0.0F);
      this.canceled = true;
      dispose();
    }
  }
  
  public void textValueChanged(java.awt.event.TextEvent paramTextEvent)
  {
    Object localObject = paramTextEvent.getSource();
    if (localObject == this.ImgFile) {
      this.statLabel.setText(" ");
      this.progress.updateBar(0.0F);
      if ((!this.ImgFile.getText().equals("")) && (!this.fdDrive.getSelectedItem().equals("None"))) {
        this.create.setEnabled(true);
      } else {
        this.create.setEnabled(false);
      }
    }
  }
  
  public void itemStateChanged(java.awt.event.ItemEvent paramItemEvent) {
    Object localObject = paramItemEvent.getSource();
    
    if (localObject == this.fdDrive) {
      this.statLabel.setText(" ");
      this.progress.updateBar(0.0F);
      String str = this.fdDrive.getSelectedItem();
      int i = 0;
      for (; (i < this.dev.length) && (!str.equals(this.dev[i])); i++) {}
      
      if (i < this.dev.length) {
        this.iscdrom = (this.devt[i] == 5);
      } else {
        this.iscdrom = false;
        this.create.setEnabled(false);
      }
      if (this.iscdrom) {
        this.dimg.setLabel("Disk >> Image");
        this.diskimage = true;
        this.dimg.setEnabled(false);
      } else {
        this.dimg.setEnabled(true);
      }
      this.dimg.repaint();
    }
  }
  
  public int cdrom_testunitready(MediaAccess paramMediaAccess) {
    byte[] arrayOfByte1 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    
    byte[] arrayOfByte2 = new byte[8];
    byte[] arrayOfByte3 = new byte[3];
    
    int i = paramMediaAccess.scsi(arrayOfByte1, 1, 8, arrayOfByte2, arrayOfByte3);
    if (i >= 0)
      i = SCSI.mk_int32(arrayOfByte2, 0) * SCSI.mk_int32(arrayOfByte2, 4);
    return i;
  }
  
  public int cdrom_startstopunit(MediaAccess paramMediaAccess)
  {
    byte[] arrayOfByte1 = { 27, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 };
    
    byte[] arrayOfByte2 = new byte[8];
    byte[] arrayOfByte3 = new byte[3];
    
    int i = paramMediaAccess.scsi(arrayOfByte1, 1, 8, arrayOfByte2, arrayOfByte3);
    
    if (i >= 0)
      i = SCSI.mk_int32(arrayOfByte2, 0) * SCSI.mk_int32(arrayOfByte2, 4);
    return i;
  }
  
  public long cdrom_size(MediaAccess paramMediaAccess)
  {
    byte[] arrayOfByte1 = { 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    
    byte[] arrayOfByte2 = new byte[8];
    byte[] arrayOfByte3 = new byte[3];
    
    long l = paramMediaAccess.scsi(arrayOfByte1, 1, 8, arrayOfByte2, arrayOfByte3);
    if (l >= 0L)
      l = SCSI.mk_int32(arrayOfByte2, 0) * SCSI.mk_int32(arrayOfByte2, 4);
    return l;
  }
  
  public void cdrom_read(MediaAccess paramMediaAccess, long paramLong, int paramInt, byte[] paramArrayOfByte)
    throws java.io.IOException
  {
    byte[] arrayOfByte1 = new byte[12];
    byte[] arrayOfByte2 = new byte[3];
    

    int i = (int)(paramLong / 2048L);
    arrayOfByte1[0] = 40;
    arrayOfByte1[1] = 0;
    arrayOfByte1[2] = ((byte)(i >> 24 & 0xFF));
    arrayOfByte1[3] = ((byte)(i >> 16 & 0xFF));
    arrayOfByte1[4] = ((byte)(i >> 8 & 0xFF));
    arrayOfByte1[5] = ((byte)(i >> 0 & 0xFF));
    arrayOfByte1[6] = 0;
    arrayOfByte1[7] = ((byte)(paramInt / 2048 >> 8 & 0xFF));
    arrayOfByte1[8] = ((byte)(paramInt / 2048 >> 0 & 0xFF));
    arrayOfByte1[9] = 0;
    arrayOfByte1[10] = 0;
    arrayOfByte1[11] = 0;
    
    int j = paramMediaAccess.scsi(arrayOfByte1, 1, paramInt, paramArrayOfByte, arrayOfByte2);
    if (j == -1)
      throw new java.io.IOException("Error reading CD-ROM.");
    if (arrayOfByte2[0] != 0) {
      throw new java.io.IOException("Error reading CD-ROM.  Sense data (" + D.hex(arrayOfByte2[0], 1) + "/" + D.hex(arrayOfByte2[1], 2) + "/" + D.hex(arrayOfByte2[2], 2) + ")");
    }
  }
  
  public void cdrom_read_retry(MediaAccess paramMediaAccess, long paramLong, int paramInt, byte[] paramArrayOfByte)
    throws java.io.IOException
  {
    byte[] arrayOfByte1 = new byte[12];
    byte[] arrayOfByte2 = new byte[3];
    byte[] arrayOfByte3 = new byte[12];
    

    int i = 0;int j = 0;
    int k = 0;
    

    int n = (int)(paramLong / 2048L);
    arrayOfByte1[0] = 40;
    arrayOfByte1[1] = 0;
    arrayOfByte1[2] = ((byte)(n >> 24 & 0xFF));
    arrayOfByte1[3] = ((byte)(n >> 16 & 0xFF));
    arrayOfByte1[4] = ((byte)(n >> 8 & 0xFF));
    arrayOfByte1[5] = ((byte)(n >> 0 & 0xFF));
    arrayOfByte1[6] = 0;
    arrayOfByte1[7] = ((byte)(paramInt / 2048 >> 8 & 0xFF));
    arrayOfByte1[8] = ((byte)(paramInt / 2048 >> 0 & 0xFF));
    arrayOfByte1[9] = 0;
    arrayOfByte1[10] = 0;
    arrayOfByte1[11] = 0;
    int m;
    do {
      long l1 = System.currentTimeMillis();
      m = paramMediaAccess.scsi(arrayOfByte1, 1, paramInt, paramArrayOfByte, arrayOfByte2);
      long l2 = System.currentTimeMillis();
      
      if (m < 0) {
        cdrom_testunitready(paramMediaAccess);
        cdrom_startstopunit(paramMediaAccess);
        m = -1;
      }
      
      if (arrayOfByte2[1] == 41) {
        m = -1;
      }
      if ((arrayOfByte2[0] == 3) || (arrayOfByte2[0] == 4))
      {
        if ((arrayOfByte2[1] == 2) && (arrayOfByte2[2] == 0)) {
          arrayOfByte3[0] = 43;
          arrayOfByte3[1] = 0;
          arrayOfByte3[2] = arrayOfByte1[2];
          arrayOfByte3[3] = arrayOfByte1[3];
          arrayOfByte3[4] = arrayOfByte1[4];
          arrayOfByte3[5] = arrayOfByte1[5];
          arrayOfByte3[6] = 0;
          arrayOfByte3[7] = 0;
          arrayOfByte3[8] = 0;
          arrayOfByte3[9] = 0;
          arrayOfByte3[10] = 0;
          arrayOfByte3[11] = 0;
          
          m = paramMediaAccess.scsi(arrayOfByte3, 1, paramInt, paramArrayOfByte, arrayOfByte2);
          cdrom_testunitready(paramMediaAccess);


        }
        else if (arrayOfByte2[1] == 17) {
          cdrom_testunitready(paramMediaAccess);
          cdrom_startstopunit(paramMediaAccess);

        }
        else
        {
          cdrom_testunitready(paramMediaAccess);
        }
        
        m = -1;
      }
    } while ((m < 0) && (k++ < this.retrycount));
    
    if (k >= this.retrycount) {
      D.println(D.FATAL, "RETRIES FAILED ! ");
    }
  }
  


  public void run()
  {
    int i = 0;
    long l2 = 0L;
    String str = this.ImgFile.getText();
    
    int m = 0;
    
    if (str.equals("")) {
      this.browse.setEnabled(true);
      this.fdDrive.setEnabled(true);
      this.ImgFile.setEnabled(true);
      this.dimg.setEnabled(true);
      return;
    }
    
    MediaAccess localMediaAccess1 = new MediaAccess();
    MediaAccess localMediaAccess2 = new MediaAccess();
    try {
      int k;
      if (this.iscdrom)
      {
        k = localMediaAccess1.open(this.fdDrive.getSelectedItem(), 1);
        if (k < 0) {
          m = 1;
          new VErrorDialog("Could not open CDROM (" + localMediaAccess1.dio.sysError(-k) + ")", false);
          throw new java.io.IOException("Couldn't open cdrom " + k);
        }
        cdrom_testunitready(localMediaAccess1);
        l2 = cdrom_size(localMediaAccess1);
        i = 65536;
      } else {
        k = localMediaAccess1.open(this.fdDrive.getSelectedItem(), 1);
        l2 = localMediaAccess1.size();
        i = localMediaAccess1.dio.BytesPerSec * localMediaAccess1.dio.SecPerTrack;
      }
    }
    catch (java.io.IOException localIOException1) {}
    

    if ((!this.diskimage) && (localMediaAccess1.wp())) {
      new VErrorDialog(this.frame, "Diskette in drive " + this.fdDrive.getSelectedItem() + " is write protected.");
      


      m = 1;
      this.create.setEnabled(true);
      this.browse.setEnabled(true);
      this.fdDrive.setEnabled(true);
      this.ImgFile.setEnabled(true);
      this.dimg.setEnabled(true);
      try {
        localMediaAccess1.close();
      }
      catch (java.io.IOException localIOException2) {}
      return;
    }
    
    setCursor(java.awt.Cursor.getPredefinedCursor(3));
    
    long l1 = l2;
    if ((i == 0) || (l1 == 0L)) {
      new VErrorDialog(this.frame, "Unable to determine disk geometry. Make sure that a disk is in the drive.");
      
      m = 1;
      i = 0;
      l1 = 0L;
    }
    else
    {
      try {
        localMediaAccess2.open(str, this.diskimage ? 2 : 0);
      } catch (java.io.IOException localIOException3) {
        new VErrorDialog(this.frame, "Unable to open file" + str + ".");
      }
    }
    

    long l3 = 0L;
    byte[] arrayOfByte = new byte[i];
    int n = 0;
    try {
      do {
        int j = i < l1 ? i : (int)l1;
        if (this.diskimage) {
          if (this.iscdrom) {
            cdrom_read_retry(localMediaAccess1, l3, j, arrayOfByte);
          } else {
            localMediaAccess1.read(l3, j, arrayOfByte);
          }
          localMediaAccess2.write(l3, j, arrayOfByte);
        } else {
          localMediaAccess2.read(l3, j, arrayOfByte);
          localMediaAccess1.write(l3, j, arrayOfByte);
        }
        l3 += j;
        l1 -= j;
        

        if ((!this.diskimage) && ((float)l3 / (float)l2 >= 0.95D)) {
          this.progress.updateBar(0.95F);
        }
        else {
          this.progress.updateBar((float)l3 / (float)l2);
        }
        if (l1 <= 0L) break; } while (!this.canceled);











    }
    catch (java.io.IOException localIOException4)
    {










      m = 1;
      new VErrorDialog(this.frame, "Error during " + (this.diskimage ? "image" : "diskette") + " creation (" + localIOException4 + ")");
    }
    


    setCursor(java.awt.Cursor.getPredefinedCursor(0));
    
    if (m == 0) {
      try {
        localMediaAccess1.close();
        localMediaAccess2.close();
      } catch (java.io.IOException localIOException5) {
        D.println(D.FATAL, "Closing: " + localIOException5);
      }
      
      this.progress.updateBar((float)l3 / (float)l2);
      
      if (this.diskimage) {
        this.statLabel.setText("Image file was created successfully.");
      } else {
        this.statLabel.setText("Disk was created successfully.");
      }
    } else {
      this.statLabel.setText(" ");
    }
    
    this.create.setEnabled(true);
    this.browse.setEnabled(true);
    this.fdDrive.setEnabled(true);
    this.ImgFile.setEnabled(true);
    if (this.iscdrom) {
      this.dimg.setEnabled(false);
    } else {
      this.dimg.setEnabled(true);
    }
  }
  
  public void windowClosing(java.awt.event.WindowEvent paramWindowEvent) {
    this.canceled = true;
    dispose();
  }
  
  public void windowActivated(java.awt.event.WindowEvent paramWindowEvent) {}
  
  public void windowClosed(java.awt.event.WindowEvent paramWindowEvent) {}
  
  public void windowDeactivated(java.awt.event.WindowEvent paramWindowEvent) {}
  
  public void windowDeiconified(java.awt.event.WindowEvent paramWindowEvent) {}
  
  public void windowIconified(java.awt.event.WindowEvent paramWindowEvent) {}
  
  public void windowOpened(java.awt.event.WindowEvent paramWindowEvent) {}
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/CreateImage.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */