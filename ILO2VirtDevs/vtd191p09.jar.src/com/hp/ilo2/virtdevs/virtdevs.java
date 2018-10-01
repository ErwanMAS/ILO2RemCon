package com.hp.ilo2.virtdevs;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

public class virtdevs extends Applet implements java.awt.event.ActionListener, java.awt.event.ItemListener, Runnable {
    public static final int UNQF_HIDEFLP = 1;
    static final int ImageDone = Component.ALLBITS | Component.PROPERTIES | Component.HEIGHT | Component.WIDTH;
    public static boolean cdimg_support = true;
    public static int UID;
    public static Properties prop;
    protected boolean stopFlag = false;
    protected boolean running = false;
    Checkbox readOnlyCheckbox;
    int cdromIndex = 0;
    int floppyIndex = 0;
    int uniqueFeatures = 0;
    boolean forceConfig = false;
    boolean thread_init = false;
    int connectionCount = 0;
    byte[] pre = new byte[16];
    byte[] key = new byte[16];
    int fdport = 17988;
    int fdCboxChecked = 0;
    int cdCboxChecked = 0;
    Image[] images;
    String host;
    String baseURL;
    String servername;
    String configuration;
    String appletParamFloppy;
    String appletParamCdrom;
    String appletParamDevice;
    java.awt.Frame parent;
    String hostAddress;
    ChiselBox cdromChiselBox;
    ChiselBox floppyChiselBox;
    Choice cdDriveList;
    Button cdStartButton;
    Button cdBrowse;
    CheckboxGroup cdGroup;
    Checkbox cdCboxDev;
    Checkbox cdCboxImg;
    TextField cdChooseFile;
    Label cdLabel;
    Choice fdDriveList;
    String fdSelected;
    String cdSelected;
    Button fdStartButton;
    Button fdBrowse;
    TextField fdChooseFile;
    CheckboxGroup fdGroup;
    Checkbox fdCboxDev;
    Checkbox fdCboxImg;
    Button fdcrImage;
    Connection fdConnection;
    Connection cdConnection;
    Thread fdThread;
    Thread cdThread;
    private boolean cdConnected = false;
    private boolean fdConnected = false;
    private Panel statusBar;
    private Panel cdIcons;
    private Panel fdIcons;
    private Image cdStartImage;
    private Image cdStopImage;
    private Image ciStartImage;
    private Image currentImage;
    private Canvas cdCanvas;
    private Image fdStartImage;
    private Image fdStopImage;
    private Image fiStartImage;
    private Image floppyImage;
    private Canvas fdCanvas;
    private Label statLabel;
    private Image greenDot;
    private Image grayDot;
    private Image cdActiveImg;
    private Canvas cdActive;
    private Image fdActiveImg;
    private Canvas fdActive;

    private Image getImage(String path) {
        ClassLoader localClassLoader = getClass().getClassLoader();
        return getImage(localClassLoader.getResource("com/hp/ilo2/virtdevs/" + path));
    }

    public void init() {
        if (UID == 0) UID = hashCode();

        this.images = new Image[8];
        this.images[0] = getImage("cdstart.gif");
        this.images[1] = getImage("cdstop.gif");
        this.images[2] = getImage("active.gif");
        this.images[3] = getImage("inactive.gif");
        this.images[4] = null;
        this.images[5] = getImage("fdstart.gif");
        this.images[6] = getImage("fdstop.gif");
        this.images[7] = getImage("fistart.gif");

        URL localURL = getDocumentBase();
        this.hostAddress = getParameter("hostAddress");
        if (this.hostAddress == null)
            this.hostAddress = localURL.getHost();
        this.baseURL = (localURL.getProtocol() + "://" + localURL.getHost());
        if (localURL.getPort() != -1)
            this.baseURL = (this.baseURL + ":" + localURL.getPort());
        this.baseURL += "/";

        String info0Pre = getParameter("INFO0");
        if (info0Pre != null) {
            try {
                for (int i = 0; i < 16; i++) {
                    this.pre[i] = ((byte) Integer.parseInt(info0Pre.substring(2 * i, 2 * i + 2), 16));

                    this.key[i] = 0;
                }
            } catch (NumberFormatException localNumberFormatException1) {
                D.println(D.FATAL, "Couldn't parse INFO0: " + localNumberFormatException1);
            }
        }
        try {
            this.fdport = Integer.parseInt(getParameter("INFO1"));
        } catch (NumberFormatException localNumberFormatException2) {
            D.println(D.FATAL, "Couldn't parse INFO1: " + localNumberFormatException2);
        }

        this.configuration = getParameter("INFO2");
        if (this.configuration == null) {
            this.configuration = "auto";
        }
        this.servername = getParameter("INFO3");

        this.appletParamFloppy = getParameter("floppy");
        this.appletParamCdrom = getParameter("cdrom");
        this.appletParamDevice = getParameter("device");
        String config = getParameter("config");
        if (config != null) {
            this.configuration = config;
            this.forceConfig = true;
        }

        String uniqueFeatures = getParameter("UNIQUE_FEATURES");
        try {
            if (uniqueFeatures != null)
                this.uniqueFeatures = Integer.parseInt(uniqueFeatures);
        } catch (NumberFormatException e) {
            D.println(D.FATAL, "Couldn't parse UNIQUE_FEATURES: " + e);
        }

        this.parent = new java.awt.Frame();
    }

    public void start() {
        Thread localThread = new Thread(this);
        localThread.start();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException localInterruptedException) {
            System.out.println("Exception: " + localInterruptedException);
        }

        if (uiInit(this.images)) {
            setConfig(this.configuration);
            if (this.forceConfig)
                updateConfig();
            //show();
            setVisible(true);
            if (this.appletParamFloppy != null) doFloppy(this.appletParamFloppy);
            if (this.appletParamCdrom != null) do_cdrom(this.appletParamCdrom);
        }
    }

    public void stop() {
        D.println(D.VERBOSE, "Stop " + this);
        if (this.fdConnection != null) {
            try {
                this.fdConnection.close();
                this.fdThread = null;
            } catch (IOException e) {
                D.println(D.VERBOSE, e.toString());
            }
        }
        if (this.cdConnection != null) {
            try {
                this.cdConnection.close();
                this.cdThread = null;
            } catch (IOException e) {
                D.println(D.VERBOSE, e.toString());
            }
        }
    }

    public void destroy() {
        Thread localThread = new Thread(this);
        localThread.start();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            System.out.println("Exception: " + e);
        }
    }

    public synchronized void run() {
        if (!this.thread_init) {
            prop = new Properties();
            try {
                prop.load(new java.io.FileInputStream(System.getProperty("user.home") + System.getProperty("file.separator") + ".java" + System.getProperty("file.separator") + "hp.properties"));


            } catch (Exception e) {

                System.out.println("Exception: " + e);
            }
            cdimg_support = Boolean.valueOf(prop.getProperty("com.hp.ilo2.virtdevs.cdimage", "true"));


            MediaAccess localMediaAccess = new MediaAccess();
            localMediaAccess.setup_DirectIO();
            this.thread_init = true;
        } else {
            MediaAccess.cleanup();
            this.thread_init = false;
        }
    }

    private boolean uiInit(Image[] images) {
        MediaAccess localMediaAccess = new MediaAccess();

        GridBagLayout localGridBagLayout = new GridBagLayout();
        GridBagConstraints localGridBagConstraints = new GridBagConstraints();

        setLayout(localGridBagLayout);
        setBackground(Color.lightGray);


        this.statusBar = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));


        Panel localPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        if ((this.servername == null) || (this.servername.equals("host is unnamed")))
            this.servername = this.host;
        Label localLabel = new Label("Virtual Media: " + this.servername);
        localLabel.setFont(new Font("Arial", Font.BOLD, 12));
        localPanel.add(localLabel);
        localPanel.setForeground(Color.white);
        localPanel.setBackground(new Color(0, 102, 153));


        if (cdimg_support) {
            this.cdGroup = new CheckboxGroup();
            this.cdCboxDev = new Checkbox("Local Media Drive:", this.cdGroup, true);
            this.cdCboxDev.addItemListener(this);

            this.cdCboxImg = new Checkbox("Local Image File:", this.cdGroup, false);
            this.cdCboxImg.addItemListener(this);
        } else {
            this.cdLabel = new Label("Local Media Drive:");
        }

        this.fdCboxChecked = 0;

        this.cdChooseFile = new TextField(15);
        this.cdChooseFile.setEditable(false);
        this.cdChooseFile.addActionListener(this);


        this.cdDriveList = new Choice();
        this.cdDriveList.add("None");

        this.fdDriveList = new Choice();
        this.fdDriveList.add("None");
        int n;
        if (this.appletParamCdrom != null) {
            n = localMediaAccess.devtype(this.appletParamCdrom);
            if ((n != BaseMediaAccess.NoRootDir) && (n != BaseMediaAccess.CDROM)) {
                new VErrorDialog(this.parent, "Device '" + this.appletParamCdrom + "' is not a CD/DVD-ROM");
                this.appletParamCdrom = null;
            }
        }
        if (this.appletParamFloppy != null) {
            n = localMediaAccess.devtype(this.appletParamFloppy);
            if ((n != BaseMediaAccess.NoRootDir) && (n != BaseMediaAccess.Removable)) {
                new VErrorDialog(this.parent, "Device '" + this.appletParamFloppy + "' is not a floppy/USBkey");
                this.appletParamFloppy = null;
            }
        }
        if (this.appletParamDevice != null) {
            n = localMediaAccess.devtype(this.appletParamDevice);
            if (n == BaseMediaAccess.CDROM) {
                this.appletParamCdrom = this.appletParamDevice;
            } else if (n == BaseMediaAccess.Removable) {
                this.appletParamFloppy = this.appletParamDevice;
            } else {
                new VErrorDialog(this.parent, "Device '" + this.appletParamDevice + "' is neither a floppy/USBkey nor a CD/DVD-ROM");
            }
        }


        int fdDeviceIndex = 0;
        int cdDeviceIndex = 0;
        String[] deviceNames = localMediaAccess.devices();
        if (deviceNames != null) {
            for (String deviceName : deviceNames) {
                D.println(D.VERBOSE, "init:deviceName = " + deviceName);
                int devtype = localMediaAccess.devtype(deviceName);
                if (devtype == BaseMediaAccess.CDROM) {
                    this.cdDriveList.add(deviceName);
                    cdDeviceIndex++;
                    if (deviceName.equals(this.appletParamCdrom)) this.cdromIndex = cdDeviceIndex;
                }
                if (devtype == BaseMediaAccess.Removable) {
                    this.fdDriveList.add(deviceName);
                    fdDeviceIndex++;
                    if (deviceName.equals(this.appletParamFloppy)) this.floppyIndex = fdDeviceIndex;
                }
            }
        }

        this.cdDriveList.select(this.cdromIndex);
        this.cdDriveList.addItemListener(this);

        this.fdDriveList.select(this.floppyIndex);
        this.fdDriveList.addItemListener(this);


        this.cdStartButton = new Button("");
        this.cdStartButton.setLabel("   Connect   ");
        this.cdStartButton.addActionListener(this);

        this.cdBrowse = new Button("Browse");
        this.cdBrowse.setEnabled(false);
        this.cdBrowse.addActionListener(this);

        this.cdIcons = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        this.greenDot = images[2];
        prepareImage(this.greenDot, this.cdIcons);
        this.grayDot = images[3];
        prepareImage(this.grayDot, this.cdIcons);


        this.cdStartImage = images[0];
        prepareImage(this.cdStartImage, this.cdIcons);
        this.cdStopImage = images[1];
        prepareImage(this.cdStopImage, this.cdIcons);
        this.ciStartImage = images[7];
        prepareImage(this.ciStartImage, this.cdIcons);
        this.currentImage = this.cdStopImage;
        this.cdActiveImg = this.grayDot;

        this.cdIcons.add(this.cdCanvas = new Canvas() {
            public void paint(Graphics paramAnonymousGraphics) {
                super.paint(paramAnonymousGraphics);
                if (virtdevs.this.currentImage != null) {
                    virtdevs.this.waitImage(virtdevs.this.currentImage, this);
                    paramAnonymousGraphics.setColor(Color.lightGray);
                    paramAnonymousGraphics.fillRect(2, 2, 36, 36);
                    paramAnonymousGraphics.drawImage(virtdevs.this.currentImage, 4, 4, null);
                }

            }
        });
        this.cdCanvas.setBackground(Color.red);
        this.cdCanvas.setSize(40, 40);
        this.cdCanvas.setVisible(true);

        this.cdIcons.add(this.cdActive = new Canvas() {
            public void paint(Graphics paramAnonymousGraphics) {
                super.paint(paramAnonymousGraphics);
                if (virtdevs.this.cdActiveImg != null) {
                    virtdevs.this.waitImage(virtdevs.this.cdActiveImg, this);
                    paramAnonymousGraphics.drawImage(virtdevs.this.cdActiveImg, 10, 10, null);
                }

            }
        });
        this.cdActive.setBackground(Color.lightGray);
        this.cdActive.setSize(40, 40);
        this.cdActive.setVisible(true);

        this.fdGroup = new CheckboxGroup();
        this.fdCboxDev = new Checkbox("Local Media Drive:", this.fdGroup, true);
        this.fdCboxDev.addItemListener(this);

        this.fdCboxImg = new Checkbox("Local Image File:", this.fdGroup, false);
        this.fdCboxImg.addItemListener(this);

        this.fdCboxChecked = 0;


        this.fdStartButton = new Button("");
        this.fdStartButton.setLabel("   Connect   ");

        this.fdBrowse = new Button("Browse");
        this.fdBrowse.addActionListener(this);
        this.fdBrowse.setEnabled(false);
        this.fdStartButton.addActionListener(this);

        this.fdIcons = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        this.fdStartImage = images[5];
        prepareImage(this.fdStartImage, this.fdIcons);
        this.fdStopImage = images[6];
        prepareImage(this.fdStopImage, this.fdIcons);
        this.fiStartImage = images[7];
        prepareImage(this.fiStartImage, this.fdIcons);
        this.floppyImage = this.fdStopImage;
        this.fdActiveImg = this.grayDot;
        this.fdIcons.add(this.fdCanvas = new Canvas() {
            public void paint(Graphics paramAnonymousGraphics) {
                super.paint(paramAnonymousGraphics);
                if (virtdevs.this.floppyImage != null) {
                    virtdevs.this.waitImage(virtdevs.this.floppyImage, this);
                    paramAnonymousGraphics.setColor(Color.lightGray);
                    paramAnonymousGraphics.fillRect(2, 2, 36, 36);
                    paramAnonymousGraphics.drawImage(virtdevs.this.floppyImage, 4, 4, null);
                }

            }
        });
        this.fdCanvas.setBackground(Color.red);
        this.fdCanvas.setSize(40, 40);
        this.fdCanvas.setVisible(true);

        this.fdIcons.add(this.fdActive = new Canvas() {
            public void paint(Graphics paramAnonymousGraphics) {
                super.paint(paramAnonymousGraphics);
                if (virtdevs.this.fdActiveImg != null) {
                    virtdevs.this.waitImage(virtdevs.this.fdActiveImg, this);
                    paramAnonymousGraphics.drawImage(virtdevs.this.fdActiveImg, 10, 10, null);
                }

            }
        });
        this.fdActive.setBackground(Color.lightGray);
        this.fdActive.setSize(40, 40);
        this.fdActive.setVisible(true);


        this.fdChooseFile = new TextField(15);
        this.fdChooseFile.setEditable(false);
        this.fdChooseFile.addActionListener(this);
        if ((this.floppyIndex == 0) && (this.appletParamFloppy != null)) {
            this.fdChooseFile.setText(this.appletParamFloppy);
        }

        this.readOnlyCheckbox = new Checkbox("Force read-only access", false);
        this.readOnlyCheckbox.addItemListener(this);

        localGridBagConstraints.anchor = 11;
        localGridBagConstraints.fill = 2;
        localGridBagConstraints.weightx = 0.0D;
        localGridBagConstraints.weighty = 0.0D;
        add(localPanel, localGridBagConstraints, 0, 0, 4, 1);


        this.cdromChiselBox = new ChiselBox("Virtual CD/DVD-ROM");
        this.cdromChiselBox.content.setLayout(new GridBagLayout());
        localGridBagConstraints.anchor = 18;
        localGridBagConstraints.fill = 1;
        localGridBagConstraints.weightx = 100.0D;
        localGridBagConstraints.weighty = 60.0D;
        add(this.cdromChiselBox, localGridBagConstraints, 0, 3, 3, 1);

        int i = 0;
        localGridBagConstraints.fill = 2;
        localGridBagConstraints.anchor = 17;
        if (cdimg_support) {
            this.cdromChiselBox.cadd(this.cdCboxDev, localGridBagConstraints, 0, i, 1, 1);
        } else {
            this.cdromChiselBox.cadd(this.cdLabel, localGridBagConstraints, 0, i, 1, 1);
        }

        localGridBagConstraints.fill = 2;
        localGridBagConstraints.anchor = 17;
        this.cdromChiselBox.cadd(this.cdDriveList, localGridBagConstraints, 1, i, 1, 1);

        localGridBagConstraints.fill = 2;
        localGridBagConstraints.anchor = 17;
        this.cdromChiselBox.cadd(this.cdStartButton, localGridBagConstraints, 2, i, 1, 1);

        localGridBagConstraints.fill = 0;
        localGridBagConstraints.anchor = 13;
        this.cdromChiselBox.cadd(this.cdIcons, localGridBagConstraints, 3, i, 1, 2);

        if (cdimg_support) {
            localGridBagConstraints.fill = 2;
            localGridBagConstraints.anchor = 17;
            this.cdromChiselBox.cadd(this.cdCboxImg, localGridBagConstraints, 0, i + 1, 1, 1);

            localGridBagConstraints.anchor = 17;
            localGridBagConstraints.fill = 2;
            this.cdromChiselBox.cadd(this.cdChooseFile, localGridBagConstraints, 1, i + 1, 1, 1);
            if ((this.cdromIndex == 0) && (this.appletParamCdrom != null)) {
                this.cdChooseFile.setText(this.appletParamCdrom);
            }

            localGridBagConstraints.fill = 0;
            localGridBagConstraints.anchor = 17;
            this.cdromChiselBox.cadd(this.cdBrowse, localGridBagConstraints, 2, i + 1, 1, 1);
        }


        this.floppyChiselBox = new ChiselBox("Virtual Floppy/USBKey");
        this.floppyChiselBox.content.setLayout(new GridBagLayout());
        localGridBagConstraints.anchor = 18;
        localGridBagConstraints.fill = 1;
        localGridBagConstraints.weightx = 100.0D;
        localGridBagConstraints.weighty = 60.0D;
        add(this.floppyChiselBox, localGridBagConstraints, 0, 2, 3, 1);

        i = 0;
        localGridBagConstraints.fill = 2;
        localGridBagConstraints.anchor = 17;
        this.floppyChiselBox.cadd(this.fdCboxDev, localGridBagConstraints, 0, i, 1, 1);

        localGridBagConstraints.anchor = 17;
        localGridBagConstraints.fill = 2;
        this.floppyChiselBox.cadd(this.fdDriveList, localGridBagConstraints, 1, i, 1, 1);

        localGridBagConstraints.fill = 0;
        localGridBagConstraints.anchor = 17;
        this.floppyChiselBox.cadd(this.fdStartButton, localGridBagConstraints, 2, i, 1, 1);

        localGridBagConstraints.fill = 0;
        localGridBagConstraints.anchor = 13;
        this.floppyChiselBox.cadd(this.fdIcons, localGridBagConstraints, 3, i, 1, 2);

        localGridBagConstraints.fill = 2;
        localGridBagConstraints.anchor = 17;
        this.floppyChiselBox.cadd(this.fdCboxImg, localGridBagConstraints, 0, i + 1, 1, 1);

        localGridBagConstraints.anchor = 17;
        localGridBagConstraints.fill = 2;
        this.floppyChiselBox.cadd(this.fdChooseFile, localGridBagConstraints, 1, i + 1, 1, 1);

        localGridBagConstraints.fill = 0;
        localGridBagConstraints.anchor = 17;
        this.floppyChiselBox.cadd(this.fdBrowse, localGridBagConstraints, 2, i + 1, 1, 1);

        localGridBagConstraints.anchor = 17;
        this.floppyChiselBox.cadd(this.readOnlyCheckbox, localGridBagConstraints, 0, i + 2, 1, 1);


        i = 4;
        this.statLabel = new Label("Select a local drive from the list");
        this.statLabel.setFont(new Font("Arial", Font.BOLD, 12));
        this.statusBar.add(this.statLabel);

        localGridBagConstraints.anchor = 17;
        localGridBagConstraints.fill = 2;
        localGridBagConstraints.weightx = 120.0D;
        localGridBagConstraints.weighty = 0.0D;
        add(this.statusBar, localGridBagConstraints, 0, i, 1, 1);

        this.fdcrImage = new Button("Create Disk Image");
        this.fdcrImage.addActionListener(this);

        localGridBagConstraints.anchor = 13;
        localGridBagConstraints.fill = 0;
        localGridBagConstraints.weightx = 8.0D;
        add(this.fdcrImage, localGridBagConstraints, 2, i, 1, 1);


        java.awt.event.MouseAdapter local5 = new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent paramAnonymousMouseEvent) {
                if ((paramAnonymousMouseEvent.getModifiers() & 0x2) != 0) {
                    D.debug += 1;
                    System.out.println("Debug set to " + D.debug);
                }
                if ((paramAnonymousMouseEvent.getModifiers() & 0x8) != 0) {
                    D.debug -= 1;
                    System.out.println("Debug set to " + D.debug);
                }
            }
        };
        addMouseListener(local5);
        localLabel.addMouseListener(local5);
        this.statusBar.addMouseListener(local5);

        if ((this.uniqueFeatures & UNQF_HIDEFLP) == UNQF_HIDEFLP) {
            this.floppyChiselBox.setVisible(false);
        }
        return true;
    }

    private void add(Component component, GridBagConstraints constraints, int gridx, int gridy, int gridwidth, int gridheight) {
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.gridwidth = gridwidth;
        constraints.gridheight = gridheight;
        add(component, constraints);
    }

    public void itemStateChanged(ItemEvent paramItemEvent) {
        String str;
        if (paramItemEvent.getItemSelectable() == this.cdDriveList) {
            str = this.cdDriveList.getSelectedItem();
            if (str.equals("None")) {
                this.statLabel.setText("Select a local drive from the list");
            } else {
                this.statLabel.setText("Press Connect to start");
            }
        }
        if (paramItemEvent.getItemSelectable() == this.fdDriveList) {
            str = this.fdDriveList.getSelectedItem();
            if (str.equals("None")) {
                this.statLabel.setText("Select a local drive from the list");
            } else {
                this.statLabel.setText("Press Connect to start");
            }
        }

        if (paramItemEvent.getSource() == this.fdCboxDev) {
            if (!this.fdConnected) {
                this.fdChooseFile.setEditable(false);
                this.fdBrowse.setEnabled(false);
                this.fdDriveList.setEnabled(true);
                this.fdStartButton.setEnabled(true);
                this.fdCboxChecked = 0;
            }
        } else if ((paramItemEvent.getSource() == this.fdCboxImg) &&
                (!this.fdConnected)) {
            this.fdChooseFile.setEditable(true);
            this.fdBrowse.setEnabled(true);
            this.fdDriveList.setEnabled(false);
            this.fdStartButton.setEnabled(true);
            this.fdCboxChecked = 1;
        }


        if (paramItemEvent.getSource() == this.cdCboxDev) {
            if (!this.cdConnected) {
                this.cdChooseFile.setEditable(false);
                this.cdBrowse.setEnabled(false);
                this.cdDriveList.setEnabled(true);
                this.cdStartButton.setEnabled(true);
                this.cdCboxChecked = 0;
            }
        } else if ((paramItemEvent.getSource() == this.cdCboxImg) &&
                (!this.cdConnected)) {
            this.cdChooseFile.setEditable(true);
            this.cdBrowse.setEnabled(true);
            this.cdDriveList.setEnabled(false);
            this.cdStartButton.setEnabled(true);
            this.cdCboxChecked = 1;
        }


        if (paramItemEvent.getSource() == this.readOnlyCheckbox) {
            D.println(D.VERBOSE, "Read only = " + this.readOnlyCheckbox.getState());
            if (this.fdConnection != null) {
                this.fdConnection.setWriteProt(this.readOnlyCheckbox.getState());
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent paramActionEvent) {
        D.println(D.VERBOSE, "ActonPerformed " + paramActionEvent + " " + paramActionEvent.getSource());
        if ((this.cdDriveList.getSelectedItem().equals("None")) && (this.cdChooseFile.getText().length() == 0)) {
            this.statLabel.setText("Select a local drive from the list");


        } else if (paramActionEvent.getSource() == this.cdStartButton) {
            if (!cdimg_support) {
                this.cdSelected = this.cdDriveList.getSelectedItem();
            } else if (this.cdCboxDev.getState()) {
                this.cdSelected = this.cdDriveList.getSelectedItem();
            } else {
                this.cdSelected = this.cdChooseFile.getText();
            }
            do_cdrom(this.cdSelected);
        }


        if (((this.fdCboxDev.getState()) && (this.fdDriveList.getSelectedItem().equals("None"))) || ((this.fdCboxImg.getState()) && (this.fdChooseFile.getText().length() == 0))) {
            this.statLabel.setText("Select a local drive or Image");
        } else if (paramActionEvent.getSource() == this.fdStartButton) {
            if (this.fdCboxDev.getState()) {
                this.fdSelected = this.fdDriveList.getSelectedItem();
            } else {
                this.fdSelected = this.fdChooseFile.getText();
            }
            doFloppy(this.fdSelected);
        }
        VFileDialog localVFileDialog;
        String str;
        if (paramActionEvent.getSource() == this.fdBrowse) {

            D.println(D.VERBOSE, "actionPerformed:fdSelected = " + this.fdSelected);
            localVFileDialog = new VFileDialog("Choose Disk Image File");
            str = localVFileDialog.getString();
            if (str != null)
                this.fdChooseFile.setText(str);
            this.fdSelected = this.fdChooseFile.getText();
            D.println(D.VERBOSE, "FDIO.actionPerformed:fdSelected = " + this.fdSelected);


            if (this.fdThread != null) changeDisk(this.fdConnection, this.fdSelected);
        } else if (paramActionEvent.getSource() == this.fdChooseFile) {
            this.fdSelected = this.fdChooseFile.getText();
            if (this.fdThread != null) changeDisk(this.fdConnection, this.fdSelected);
            D.println(D.VERBOSE, "actionPerformed(2):fdSelected = " + this.fdSelected);
        } else if (paramActionEvent.getSource() == this.cdBrowse) {
            D.println(D.VERBOSE, "actionPerformed:cdSelected = " + this.cdSelected);
            localVFileDialog = new VFileDialog("Choose CD/DVD-ROM Image File");
            str = localVFileDialog.getString();
            if (str != null)
                this.cdChooseFile.setText(str);
            this.cdSelected = this.cdChooseFile.getText();
            D.println(D.VERBOSE, "FDIO.actionPerformed:cdSelected = " + this.cdSelected);


            if (this.cdThread != null) changeDisk(this.cdConnection, this.cdSelected);
        } else if (paramActionEvent.getSource() == this.cdChooseFile) {
            this.cdSelected = this.fdChooseFile.getText();
            D.println(D.VERBOSE, "actionPerformed(2):cdSelected = " + this.cdSelected);
            if (this.cdThread != null) changeDisk(this.cdConnection, this.cdSelected);
        } else if (paramActionEvent.getSource() == this.fdcrImage) {
            new CreateImage(this.parent);
        }
    }

    private void doFloppy(String paramString) {
        if (!this.fdConnected) {
            try {
                this.fdConnection = new Connection(this.hostAddress, this.fdport, Connection.FLOPPY, paramString, 0, this.pre, this.key, this);

            } catch (Exception e) {

                new VErrorDialog(this.parent, e.getMessage());
                return;
            }
            this.fdConnection.setWriteProt(this.readOnlyCheckbox.getState());

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int response;
            try {
                response = this.fdConnection.connect();
            } catch (Exception e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                D.println(D.FATAL, "Couldn't connect!\n");
                System.out.println(e.getMessage());
                new VErrorDialog(this.parent, "Could not connect Virtual Media. iLO Virtual Media service may be disabled.");
                return;
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            if (response == 33) {
                new VErrorDialog(this.parent, "Another virtual media client is connected.");
                return;
            }
            if (response == 34) {
                String str;
                if (rekey("vtdframe.htm")) {
                    str = "Invalid Login.  Try again.";
                } else {
                    str = "Invalid Login.";
                }
                new VErrorDialog(this.parent, str);
                return;
            }
            if (response == 35) {
                new VErrorDialog(this.parent, "iLO is not Licenced for Virtual Media.");
                return;
            }
            if (response == 37) {
                new VErrorDialog(this.parent, "The Virtual Device is not configured as a floppy drive.");
                this.configuration = "cdrom";
                setConfig(this.configuration);
                return;
            }
            if (response != 0) {
                new VErrorDialog(this.parent, "Unexpected HELLO response (" + Integer.toHexString(response) + ").  Connection Failed.");


                return;
            }

            this.fdThread = new Thread(this.fdConnection, "fdConnection");
            this.fdThread.start();

            if (this.fdCboxChecked == 0) {
                this.floppyImage = this.fdStartImage;
            } else {
                this.floppyImage = this.fiStartImage;
            }

            this.fdActiveImg = this.greenDot;
            this.fdStartButton.setLabel("Disconnect");

            this.fdcrImage.setEnabled(false);
            this.connectionCount += 1;
            this.fdCboxDev.setEnabled(false);
            this.fdDriveList.setEnabled(false);
            this.fdCboxImg.setEnabled(false);

            this.statLabel.setText("Virtual Media Connected");
            this.fdcrImage.setEnabled(false);
            this.fdCanvas.setBackground(Color.green);
            this.fdCanvas.invalidate();
            this.fdActive.repaint();

            repaint();
            this.fdConnected = true;
            if (this.configuration.equals("auto")) {
                setConfig("floppy");
            }
        } else {
            this.fdStartButton.setEnabled(false);
            try {
                this.fdConnection.close();
            } catch (Exception localException3) {
                D.println(D.FATAL, "Exception during close: " + localException3);
            }
        }
    }

    public void do_cdrom(String paramString) {
        if (!this.cdConnected) {
            try {
                this.cdConnection = new Connection(this.hostAddress, this.fdport, Connection.CDROM, paramString, 0, this.pre, this.key, this);
            } catch (Exception localException1) {
                new VErrorDialog(this.parent, localException1.getMessage());
                return;
            }
            this.cdConnection.setWriteProt(true);
            int i;
            try {
                i = this.cdConnection.connect();
            } catch (Exception localException2) {
                D.println(D.FATAL, "Couldn't connect!\n");
                System.out.println(localException2.getMessage());
                new VErrorDialog(this.parent, "Could not connect Virtual Media. iLO Virtual Media service may be disabled.");
                return;
            }

            if (i == 33) {
                new VErrorDialog(this.parent, "Another virtual media client is connected.");
                return;
            }
            if (i == 34) {
                String str;
                if (rekey("vtdframe.htm")) {
                    str = "Invalid Login.  Try again.";
                } else {
                    str = "Invalid Login.";
                }
                new VErrorDialog(this.parent, str);
                return;
            }
            if (i == 35) {
                new VErrorDialog(this.parent, "iLO is not Licenced for Virtual Media.");
                return;
            }
            if (i == 37) {
                new VErrorDialog(this.parent, "The Virtual Device is not configured as a CD/DVD-ROM drive.");
                this.configuration = "floppy";
                setConfig(this.configuration);
                return;
            }
            if (i != 0) {
                new VErrorDialog(this.parent, "Unexpected HELLO response (" + Integer.toHexString(i) + ").  Connection Failed.");


                return;
            }

            this.cdThread = new Thread(this.cdConnection, "cdConnection");
            this.cdThread.start();


            this.fdcrImage.setEnabled(false);
            this.connectionCount += 1;
            this.cdDriveList.setEnabled(false);
            this.cdStartButton.setLabel("Disconnect");
            this.statLabel.setText("Virtual Media Connected");
            this.fdcrImage.setEnabled(false);

            if (cdimg_support) {
                this.cdCboxDev.setEnabled(false);
                this.cdCboxImg.setEnabled(false);
            }

            if (this.cdCboxChecked == 0) {
                this.currentImage = this.cdStartImage;
            } else {
                this.currentImage = this.ciStartImage;
            }


            this.cdActiveImg = this.greenDot;
            this.cdCanvas.setBackground(Color.green);
            this.cdCanvas.invalidate();
            this.cdActive.repaint();

            repaint();
            this.cdConnected = true;
            if (this.configuration.equals("auto")) {
                setConfig("cdrom");
            }
        } else {
            try {
                this.cdConnection.close();
            } catch (Exception localException3) {
                D.println(D.FATAL, "Exception during close: " + localException3);
            }
        }
    }

    public void paint(Graphics paramGraphics) {
        super.paint(paramGraphics);
    }

    public void update(Graphics paramGraphics) {
        paint(paramGraphics);
    }

    private void waitImage(Image paramImage, ImageObserver paramImageObserver) {
        long l = System.currentTimeMillis();
        int i;
        do {
            i = checkImage(paramImage, paramImageObserver);
            if ((i & (Component.ERROR | Component.ABORT)) != 0)
                break;
            Thread.yield();
        } while ((System.currentTimeMillis() - l <= 2000L) && (i & ImageDone) != ImageDone);
    }

    private void setConfig(String paramString) {
        if (paramString.equals("floppy")) {
            this.fdStartButton.setEnabled(true);

            if (this.fdCboxChecked == 1) {
                this.fdCboxImg.setEnabled(true);
                this.fdCboxDev.setEnabled(false);
            } else {
                this.fdCboxDev.setEnabled(true);
                this.fdCboxImg.setEnabled(false);
            }
            this.fdDriveList.setEnabled(false);

            this.floppyChiselBox.setEnabled(true);
            this.readOnlyCheckbox.setEnabled(false);
            this.cdStartButton.setEnabled(false);
            this.cdDriveList.setEnabled(false);
            this.cdromChiselBox.setEnabled(false);
            if (cdimg_support) {
                this.cdChooseFile.setEditable(false);
                this.cdBrowse.setEnabled(false);
                this.cdCboxDev.setEnabled(false);
                this.cdCboxImg.setEnabled(false);
            }
        } else if (paramString.equals("cdrom")) {
            this.fdStartButton.setEnabled(false);
            this.fdCboxDev.setEnabled(false);
            this.fdCboxImg.setEnabled(false);
            this.fdDriveList.setEnabled(false);
            this.fdChooseFile.setEditable(false);
            this.fdBrowse.setEnabled(false);
            this.readOnlyCheckbox.setEnabled(false);
            this.floppyChiselBox.setEnabled(false);
            this.cdStartButton.setEnabled(true);

            if (this.cdCboxChecked == 1) {
                this.cdCboxImg.setEnabled(true);
                this.cdCboxDev.setEnabled(false);
            } else {
                this.cdCboxDev.setEnabled(true);
                this.cdCboxImg.setEnabled(false);
            }
            this.cdDriveList.setEnabled(false);
            this.cdromChiselBox.setEnabled(true);
            if (!cdimg_support) {
            }
        } else {
            this.fdStartButton.setEnabled(true);
            this.fdCboxDev.setEnabled(true);
            this.fdCboxImg.setEnabled(true);
            this.fdChooseFile.setEditable(false);
            this.readOnlyCheckbox.setEnabled(true);
            this.floppyChiselBox.setEnabled(true);
            this.cdStartButton.setEnabled(true);
            this.cdromChiselBox.setEnabled(true);
            if (cdimg_support) {
                this.cdCboxDev.setEnabled(true);
                this.cdCboxImg.setEnabled(true);
                this.cdChooseFile.setEditable(false);
                this.cdBrowse.setEnabled(false);
            }
            if (this.fdCboxChecked == 0) {
                this.fdDriveList.setEnabled(true);
                this.fdChooseFile.setEditable(false);
                this.fdBrowse.setEnabled(false);
            } else {
                this.fdDriveList.setEnabled(false);
                this.fdChooseFile.setEditable(true);
                this.fdBrowse.setEnabled(true);
            }

            if (this.cdCboxChecked == 0) {
                this.cdDriveList.setEnabled(true);
                this.cdChooseFile.setEditable(false);
                this.cdBrowse.setEnabled(false);
            } else {
                this.cdDriveList.setEnabled(false);
                this.cdChooseFile.setEditable(true);
                this.cdBrowse.setEnabled(true);
            }
        }
    }

    private void updateConfig() {
        try {
            URL localURL = new URL(this.baseURL + "modusb.cgi?usb=" + this.configuration);
            java.net.URLConnection localURLConnection = localURL.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(localURL.openStream()));


            String str;


            while ((str = br.readLine()) != null) {
                D.println(D.VERBOSE, "updcfg: " + str);
            }
            br.close();
        } catch (Exception e) {
            new VErrorDialog(this.parent, "Error updating device configuraiton (" + e + ")");

            e.printStackTrace();
        }
    }

    private boolean rekey(String paramString) {
        String str2 = null;
        try {
            D.println(D.VERBOSE, "Downloading new key: " + this.baseURL + paramString);
            URL localURL = new URL(this.baseURL + paramString);
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(localURL.openStream()));

            String str1;
            while ((str1 = localBufferedReader.readLine()) != null) {

                D.println(D.FATAL, "rekey: " + str1);
                if (str1.startsWith("info0=\"")) {
                    str2 = str1.substring(7, 39);
                    break;
                }
            }
            localBufferedReader.close();
        } catch (Exception localException) {
            D.println(D.FATAL, "rekey: " + localException);
            new VErrorDialog(this.parent, "Error retrieving new key");
            return false;
        }
        if (str2 == null) {
            new VErrorDialog(this.parent, "Error retrieving new key");
            return false;
        }
        try {
            for (int i = 0; i < 16; i++) {
                this.pre[i] = ((byte) Integer.parseInt(str2.substring(2 * i, 2 * i + 2), 16));
                this.key[i] = 0;
            }
        } catch (NumberFormatException e) {
            D.println(D.FATAL, "Couldn't parse new key: " + e);
            new VErrorDialog(this.parent, "Error parsing new key");
            return false;
        }
        return true;
    }

    private void changeDisk(Connection paramConnection, String paramString) {
        try {
            paramConnection.change_disk(paramString);
        } catch (IOException localIOException) {
            new VErrorDialog(this.parent, "Can't change disk (" + localIOException + ")");
        }
    }

    public void fdDisconnect() {
        this.fdThread = null;
        if (this.fdCboxChecked == 0) {
            this.fdDriveList.setEnabled(true);
            this.fdChooseFile.setEditable(false);
            this.fdBrowse.setEnabled(false);
        } else {
            this.fdDriveList.setEnabled(false);
            this.fdChooseFile.setEditable(true);
            this.fdBrowse.setEnabled(true);
        }
        this.fdStartButton.setLabel("   Connect   ");
        if (!this.cdConnected) {
            this.fdcrImage.setEnabled(true);
        }
        this.fdCboxDev.setEnabled(true);
        this.fdCboxImg.setEnabled(true);
        this.statLabel.setText("Virtual Media Disconnected");
        this.floppyImage = this.fdStopImage;
        this.fdActiveImg = this.grayDot;
        this.fdCanvas.setBackground(Color.red);
        this.fdCanvas.invalidate();
        this.fdActive.repaint();

        repaint();
        this.fdConnected = false;
        this.fdStartButton.setEnabled(true);
        if (this.configuration.equals("auto")) {
            setConfig(this.configuration);
        }
        if (--this.connectionCount == 0) {
            this.fdcrImage.setEnabled(true);
        }
    }

    public void cdDisconnect() {
        if (cdimg_support) {
            this.cdCboxDev.setEnabled(true);
            this.cdCboxImg.setEnabled(true);
        }
        this.cdThread = null;
        if (this.cdCboxChecked == 0) {
            this.cdDriveList.setEnabled(true);
            this.cdChooseFile.setEditable(false);
            this.cdBrowse.setEnabled(false);
        } else {
            this.cdDriveList.setEnabled(false);
            this.cdChooseFile.setEditable(true);
            this.cdBrowse.setEnabled(true);
        }
        this.cdStartButton.setLabel("   Connect   ");
        if (!this.fdConnected) {
            this.fdcrImage.setEnabled(true);
        }
        this.statLabel.setText("Virtual Media Disconnected");
        this.currentImage = this.cdStopImage;
        this.cdActiveImg = this.grayDot;
        this.cdCanvas.setBackground(Color.red);
        this.cdCanvas.invalidate();
        this.cdActive.repaint();

        repaint();
        this.cdConnected = false;
        this.cdStartButton.setEnabled(true);
        if (this.configuration.equals("auto")) {
            setConfig(this.configuration);
        }
        if (--this.connectionCount == 0) {
            this.fdcrImage.setEnabled(true);
        }
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/virtdevs.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */