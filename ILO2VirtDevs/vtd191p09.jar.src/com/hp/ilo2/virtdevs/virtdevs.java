package com.hp.ilo2.virtdevs;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

public class virtdevs extends Applet implements java.awt.event.ActionListener, java.awt.event.ItemListener, Runnable {
    static int UID;
    static Properties prop;

    private int uniqueFeatures = 0;
    private static final int UNQF_HIDEFLP = 1;

    private boolean forceConfig = false;
    private boolean thread_init = false;

    private int connectionCount = 0;
    private byte[] pre = new byte[16];
    private byte[] key = new byte[16];
    private int fdport = 17988;

    Checkbox readOnlyCheckbox;
    private String host;
    private String baseURL;
    private String serverName;
    private String configuration;
    private String appletParamFloppy;
    private String appletParamCdrom;
    private java.awt.Frame parent;
    private String hostAddress;
    private ChiselBox cdromChiselBox;
    private ChiselBox floppyChiselBox;

    private Button cdStartButton;
    private Button cdBrowse;
    private Checkbox cdCboxImg;
    private TextField cdChooseFile;
    String cdSelected;

    private String fdSelected;
    private Button fdStartButton;
    private Button fdBrowse;
    private TextField fdChooseFile;
    private Checkbox fdCboxImg;
    private Button fdcrImage;

    private Connection fdConnection;
    Connection cdConnection;

    private Thread fdThread;
    private Thread cdThread;

    private boolean cdConnected = false;
    private boolean fdConnected = false;

    private Label statLabel;

    private Canvas cdActiveCanvas;
    private Canvas fdActiveCanvas;

    public void init() {
        if (UID == 0) UID = hashCode();

        URL docBase = getDocumentBase();
        this.hostAddress = getParameter("hostAddress");
        if (this.hostAddress == null)
            this.hostAddress = docBase.getHost();
        this.baseURL = (docBase.getProtocol() + "://" + docBase.getHost());
        if (docBase.getPort() != -1)
            this.baseURL = (this.baseURL + ":" + docBase.getPort());
        this.baseURL += "/";

        String info0Pre = getParameter("INFO0");
        if (info0Pre != null) {
            try {
                for (int i = 0; i < 16; i++) {
                    this.pre[i] = ((byte) Integer.parseInt(info0Pre.substring(2 * i, 2 * i + 2), 16));

                    this.key[i] = 0;
                }
            } catch (NumberFormatException e) {
                D.println(D.FATAL, "Couldn't parse INFO0: " + e);
            }
        }
        try {
            this.fdport = Integer.parseInt(getParameter("INFO1"));
        } catch (NumberFormatException e) {
            D.println(D.FATAL, "Couldn't parse INFO1: " + e);
        }

        this.configuration = getParameter("INFO2");
        if (this.configuration == null) {
            this.configuration = "auto";
        }
        this.serverName = getParameter("INFO3");

        this.appletParamFloppy = getParameter("floppy");
        this.appletParamCdrom = getParameter("cdrom");
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
        Thread thread = new Thread(this);
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (uiInit()) {
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
        Thread thread = new Thread(this);
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        if (!this.thread_init) {
            prop = new Properties();
            try {
                prop.load(new java.io.FileInputStream(System.getProperty("user.home") + System.getProperty("file.separator") + ".java" + System.getProperty("file.separator") + "hp.properties"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.thread_init = true;
        } else {
            this.thread_init = false;
        }
    }

    private boolean uiInit() {
        new MediaAccess();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        setLayout(new GridBagLayout());
        setBackground(SystemColor.window);

        Panel connectionPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        if ((this.serverName == null) || (this.serverName.equals("host is unnamed")))
            this.serverName = this.host;
        Label connectionLabel = new Label("Virtual Media: " + this.serverName);
        connectionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        connectionPanel.add(connectionLabel);
        connectionPanel.setForeground(Color.white);
        connectionPanel.setBackground(new Color(0, 102, 153));





        this.cdCboxImg = new Checkbox("Local Image File:", null, true);
        this.cdCboxImg.addItemListener(this);
        //this.cdLabel = new Label("Local Media Drive:");

        this.cdChooseFile = new TextField(15);
        this.cdChooseFile.setEditable(false);
        this.cdChooseFile.addActionListener(this);


        this.cdStartButton = new Button("");
        this.cdStartButton.setLabel("   Connect   ");
        this.cdStartButton.addActionListener(this);

        this.cdBrowse = new Button("Browse");
        this.cdBrowse.setEnabled(true);
        this.cdBrowse.addActionListener(this);

        Panel cdIcons = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        cdIcons.add(this.cdActiveCanvas = new Canvas());
        this.cdActiveCanvas.setBackground(Color.red);
        this.cdActiveCanvas.setSize(40, 40);
        this.cdActiveCanvas.setVisible(true);

        this.fdCboxImg = new Checkbox("Local Image File:", null, true);
        this.fdCboxImg.addItemListener(this);

        this.fdStartButton = new Button("");
        this.fdStartButton.setLabel("   Connect   ");

        this.fdBrowse = new Button("Browse");
        this.fdBrowse.addActionListener(this);
        this.fdBrowse.setEnabled(false);
        this.fdStartButton.addActionListener(this);

        Panel fdIcons = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));


        fdIcons.add(this.fdActiveCanvas = new Canvas());
        this.fdActiveCanvas.setBackground(Color.red);
        this.fdActiveCanvas.setSize(40, 40);
        this.fdActiveCanvas.setVisible(true);


        this.fdChooseFile = new TextField(15);
        this.fdChooseFile.setEditable(false);
        this.fdChooseFile.addActionListener(this);

        this.readOnlyCheckbox = new Checkbox("Force read-only access", false);
        this.readOnlyCheckbox.addItemListener(this);

        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.0D;
        gridBagConstraints.weighty = 0.0D;
        add(connectionPanel, gridBagConstraints, 0, 0, 4, 1);


        this.cdromChiselBox = new ChiselBox("Virtual CD/DVD-ROM");
        this.cdromChiselBox.content.setLayout(new GridBagLayout());
        gridBagConstraints.anchor = 18;
        gridBagConstraints.fill = 1;
        gridBagConstraints.weightx = 100.0D;
        gridBagConstraints.weighty = 60.0D;
        add(this.cdromChiselBox, gridBagConstraints, 0, 3, 3, 1);

        gridBagConstraints.fill = 2;
        gridBagConstraints.anchor = 17;
        this.cdromChiselBox.cadd(this.cdStartButton, gridBagConstraints, 2, 0, 1, 1);

        gridBagConstraints.fill = 0;
        gridBagConstraints.anchor = 13;
        this.cdromChiselBox.cadd(cdIcons, gridBagConstraints, 3, 0, 1, 2);

        gridBagConstraints.fill = 2;
        gridBagConstraints.anchor = 17;
        this.cdromChiselBox.cadd(this.cdCboxImg, gridBagConstraints, 0, 0, 1, 1);

        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = 2;
        this.cdromChiselBox.cadd(this.cdChooseFile, gridBagConstraints, 1, 0, 1, 1);

        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        this.cdromChiselBox.cadd(this.cdBrowse, gridBagConstraints, 2, 1, 1, 1);


        this.floppyChiselBox = new ChiselBox("Virtual Floppy/USBKey");
        this.floppyChiselBox.content.setLayout(new GridBagLayout());
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 100.0D;
        gridBagConstraints.weighty = 60.0D;
        add(this.floppyChiselBox, gridBagConstraints, 0, 2, 3, 1);

        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        this.floppyChiselBox.cadd(this.fdStartButton, gridBagConstraints, 2, 0, 1, 1);

        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        this.floppyChiselBox.cadd(fdIcons, gridBagConstraints, 3, 0, 1, 2);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        this.floppyChiselBox.cadd(this.fdCboxImg, gridBagConstraints, 0, 0, 1, 1);

        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        this.floppyChiselBox.cadd(this.fdChooseFile, gridBagConstraints, 1, 0, 1, 1);

        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        this.floppyChiselBox.cadd(this.fdBrowse, gridBagConstraints, 2, 1, 1, 1);

        gridBagConstraints.anchor = GridBagConstraints.WEST;
        this.floppyChiselBox.cadd(this.readOnlyCheckbox, gridBagConstraints, 0, 1, 1, 1);


        Panel statusBar = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        this.statLabel = new Label("Select a local drive from the list");
        this.statLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusBar.add(this.statLabel);

        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 120.0D;
        gridBagConstraints.weighty = 0.0D;
        add(statusBar, gridBagConstraints, 0, 4, 1, 1);

        this.fdcrImage = new Button("Create Disk Image");
        this.fdcrImage.addActionListener(this);

        gridBagConstraints.anchor = 13;
        gridBagConstraints.fill = 0;
        gridBagConstraints.weightx = 8.0D;
        add(this.fdcrImage, gridBagConstraints, 2, 4, 1, 1);


        java.awt.event.MouseAdapter secretDebug = new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent paramAnonymousMouseEvent) {
                if ((paramAnonymousMouseEvent.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                    D.debug += 1;
                    System.out.println("Debug set to " + D.debug);
                }
                if ((paramAnonymousMouseEvent.getModifiers() & InputEvent.ALT_MASK) != 0) {
                    D.debug -= 1;
                    System.out.println("Debug set to " + D.debug);
                }
            }
        };
        addMouseListener(secretDebug);
        connectionLabel.addMouseListener(secretDebug);
        statusBar.addMouseListener(secretDebug);

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
        if (paramItemEvent.getSource() == this.fdCboxImg && !this.fdConnected) {
            this.fdChooseFile.setEditable(true);
            this.fdBrowse.setEnabled(true);
            this.fdStartButton.setEnabled(true);
        }

        if (paramItemEvent.getSource() == this.cdCboxImg && !this.cdConnected) {
            this.cdChooseFile.setEditable(true);
            this.cdBrowse.setEnabled(true);
            this.cdStartButton.setEnabled(true);
        }

        if (paramItemEvent.getSource() == this.readOnlyCheckbox) {
            D.println(D.VERBOSE, "Read only = " + this.readOnlyCheckbox.getState());
            if (this.fdConnection != null) {
                this.fdConnection.setWriteProtect(this.readOnlyCheckbox.getState());
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent event) {
        D.println(D.VERBOSE, "ActionPerformed " + event + " " + event.getSource());

        if (event.getSource() == this.cdStartButton) {
            if (this.cdChooseFile.getText().length() == 0) {
                this.statLabel.setText("Select an image");
            } else {
                this.cdSelected = this.cdChooseFile.getText();
                do_cdrom(this.cdSelected);
            }
        }

        if (event.getSource() == this.fdStartButton) {
            if (this.fdChooseFile.getText().length() == 0) {
                this.statLabel.setText("Select an image");
            } else {
                this.fdSelected = this.fdChooseFile.getText();
                doFloppy(this.fdSelected);
            }
        }

        VFileDialog fileDialog;
        String path;

        if (event.getSource() == this.fdBrowse) {
            D.println(D.VERBOSE, "actionPerformed:fdSelected = " + this.fdSelected);
            fileDialog = new VFileDialog("Choose Disk Image File");
            path = fileDialog.getString();
            if (path != null)
                this.fdChooseFile.setText(path);
            this.fdSelected = this.fdChooseFile.getText();
            D.println(D.VERBOSE, "FDIO.actionPerformed:fdSelected = " + this.fdSelected);
            if (this.fdThread != null) changeDisk(this.fdConnection, this.fdSelected);
        }

        if (event.getSource() == this.fdChooseFile) {
            this.fdSelected = this.fdChooseFile.getText();
            if (this.fdThread != null) changeDisk(this.fdConnection, this.fdSelected);
            D.println(D.VERBOSE, "actionPerformed(2):fdSelected = " + this.fdSelected);
        }

        if (event.getSource() == this.cdBrowse) {
            D.println(D.VERBOSE, "actionPerformed:cdSelected = " + this.cdSelected);
            fileDialog = new VFileDialog("Choose CD/DVD-ROM Image File");
            path = fileDialog.getString();
            if (path != null)
                this.cdChooseFile.setText(path);
            this.cdSelected = this.cdChooseFile.getText();
            D.println(D.VERBOSE, "FDIO.actionPerformed:cdSelected = " + this.cdSelected);
            if (this.cdThread != null) changeDisk(this.cdConnection, this.cdSelected);
        }

        if (event.getSource() == this.cdChooseFile) {
            this.cdSelected = this.fdChooseFile.getText();
            D.println(D.VERBOSE, "actionPerformed(2):cdSelected = " + this.cdSelected);
            if (this.cdThread != null) changeDisk(this.cdConnection, this.cdSelected);
        }
    }

    private void doFloppy(String paramString) {
        if (!this.fdConnected) {
            try {
                this.fdConnection = new Connection(this.hostAddress, this.fdport, Connection.FLOPPY, paramString, this.pre, this.key, this);
            } catch (Exception e) {
                new VErrorDialog(this.parent, e.getMessage());
                return;
            }
            this.fdConnection.setWriteProtect(this.readOnlyCheckbox.getState());

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
                if (rekey()) {
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

            this.fdStartButton.setLabel("Disconnect");

            this.fdcrImage.setEnabled(false);
            this.connectionCount += 1;
            this.fdCboxImg.setEnabled(false);

            this.statLabel.setText("Virtual Media Connected");

            this.fdActiveCanvas.setBackground(Color.green);
            this.fdActiveCanvas.repaint();

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

    void do_cdrom(String paramString) {
        if (!this.cdConnected) {
            try {
                this.cdConnection = new Connection(this.hostAddress, this.fdport, Connection.CDROM, paramString, this.pre, this.key, this);
            } catch (Exception e) {
                new VErrorDialog(this.parent, e.getMessage());
                return;
            }
            this.cdConnection.setWriteProtect(true);
            int response;
            try {
                response = this.cdConnection.connect();
            } catch (Exception e) {
                D.println(D.FATAL, "Couldn't connect!\n");
                System.out.println(e.getMessage());
                new VErrorDialog(this.parent, "Could not connect Virtual Media. iLO Virtual Media service may be disabled.");
                return;
            }

            if (response == 33) {
                new VErrorDialog(this.parent, "Another virtual media client is connected.");
                return;
            }
            if (response == 34) {
                String str;
                if (rekey()) {
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
                new VErrorDialog(this.parent, "The Virtual Device is not configured as a CD/DVD-ROM drive.");
                this.configuration = "floppy";
                setConfig(this.configuration);
                return;
            }
            if (response != 0) {
                new VErrorDialog(this.parent, "Unexpected HELLO response (" + Integer.toHexString(response) + ").  Connection Failed.");
                return;
            }

            this.cdThread = new Thread(this.cdConnection, "cdConnection");
            this.cdThread.start();

            this.connectionCount += 1;
            this.cdStartButton.setLabel("Disconnect");
            this.statLabel.setText("Virtual Media Connected");
            this.fdcrImage.setEnabled(false);

            this.cdCboxImg.setEnabled(false);

            this.cdActiveCanvas.setBackground(Color.green);
            this.cdActiveCanvas.repaint();

            repaint();
            this.cdConnected = true;
            if (this.configuration.equals("auto")) {
                setConfig("cdrom");
            }
        } else {
            try {
                this.cdConnection.close();
            } catch (Exception e) {
                D.println(D.FATAL, "Exception during close: " + e);
            }
        }
    }

    @Override
    public void paint(Graphics paramGraphics) {
        super.paint(paramGraphics);
    }

    public void update(Graphics paramGraphics) {
        paint(paramGraphics);
    }

    private void setConfig(String paramString) {
        if (paramString.equals("floppy")) {
            this.fdStartButton.setEnabled(true);

            this.fdCboxImg.setEnabled(true);

            this.floppyChiselBox.setEnabled(true);
            this.readOnlyCheckbox.setEnabled(false);
            this.cdStartButton.setEnabled(false);
            this.cdromChiselBox.setEnabled(false);

            this.cdChooseFile.setEditable(false);
            this.cdBrowse.setEnabled(false);
            this.cdCboxImg.setEnabled(false);

        } else if (paramString.equals("cdrom")) {
            this.fdStartButton.setEnabled(false);
            this.fdCboxImg.setEnabled(false);
            this.fdChooseFile.setEditable(false);
            this.fdBrowse.setEnabled(false);
            this.readOnlyCheckbox.setEnabled(false);
            this.floppyChiselBox.setEnabled(false);
            this.cdStartButton.setEnabled(true);

            this.cdCboxImg.setEnabled(true);

            this.cdromChiselBox.setEnabled(true);
        } else {
            this.fdStartButton.setEnabled(true);
            this.fdCboxImg.setEnabled(true);
            this.fdChooseFile.setEditable(false);
            this.readOnlyCheckbox.setEnabled(true);
            this.floppyChiselBox.setEnabled(true);
            this.cdStartButton.setEnabled(true);
            this.cdromChiselBox.setEnabled(true);

            this.cdCboxImg.setEnabled(true);
            this.cdChooseFile.setEditable(false);
            this.cdBrowse.setEnabled(false);

            this.fdChooseFile.setEditable(true);
            this.fdBrowse.setEnabled(true);

            this.cdChooseFile.setEditable(true);
            this.cdBrowse.setEnabled(true);

        }
    }

    private void updateConfig() {
        try {
            URL localURL = new URL(this.baseURL + "modusb.cgi?usb=" + this.configuration);
            BufferedReader br = new BufferedReader(new InputStreamReader(localURL.openStream()));

            String str;

            while ((str = br.readLine()) != null) {
                D.println(D.VERBOSE, "updcfg: " + str);
            }
            br.close();
        } catch (Exception e) {
            new VErrorDialog(this.parent, "Error updating device configuration (" + e + ")");

            e.printStackTrace();
        }
    }

    private boolean rekey() {
        String str2 = null;
        try {
            D.println(D.VERBOSE, "Downloading new key: " + this.baseURL + "vtdframe.htm");
            URL localURL = new URL(this.baseURL + "vtdframe.htm");
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
        } catch (Exception e) {
            D.println(D.FATAL, "rekey: " + e);
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

    void fdDisconnect() {
        this.fdThread = null;

        this.fdChooseFile.setEditable(true);
        this.fdBrowse.setEnabled(true);

        this.fdStartButton.setLabel("   Connect   ");
        if (!this.cdConnected) {
            this.fdcrImage.setEnabled(true);
        }
        this.fdCboxImg.setEnabled(true);
        this.statLabel.setText("Virtual Media Disconnected");
        this.fdActiveCanvas.repaint();

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

    void cdDisconnect() {
        this.cdCboxImg.setEnabled(true);
        this.cdThread = null;

        this.cdChooseFile.setEditable(true);
        this.cdBrowse.setEnabled(true);

        this.cdStartButton.setLabel("   Connect   ");
        if (!this.fdConnected) {
            this.fdcrImage.setEnabled(true);
        }
        this.statLabel.setText("Virtual Media Disconnected");
        this.cdActiveCanvas.repaint();

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