package com.hp.ilo2.virtdevs;

import java.awt.*;

public class ChiselBox extends Panel {
    Panel content;
    String name;

    private static final int inx = 8;
    private static final int iny = 8;

    private boolean enabled = true;
    private final Color backgroundColor = SystemColor.window;

    public ChiselBox() {
        this.content = new Panel();
        add(this.content);
        setBackground(this.backgroundColor);
    }

    public ChiselBox(String paramString) {
        this.content = new Panel();
        add(this.content);
        this.name = paramString;
        setBackground(this.backgroundColor);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        repaint();
    }

    public void cadd(Component paramComponent, GridBagConstraints paramGridBagConstraints, int gridx, int gridy, int gridwidth, int gridheight) {
        paramGridBagConstraints.gridx = gridx;
        paramGridBagConstraints.gridy = gridy;
        paramGridBagConstraints.gridwidth = gridwidth;
        paramGridBagConstraints.gridheight = gridheight;
        this.content.add(paramComponent, paramGridBagConstraints);
    }


    public void paint(Graphics g) {
        Dimension ownSize = getSize();
        g.setFont(new Font("Arial", Font.BOLD, 12));

        Color localColor1 = this.backgroundColor.darker();
        Color localColor2 = this.backgroundColor.brighter();

        int inx = ChiselBox.inx;
        int iny = ChiselBox.iny;
        int k = ownSize.width - ChiselBox.inx;
        int m = ownSize.height - ChiselBox.iny;


        g.setColor(localColor1);
        g.drawLine(inx, iny, k, iny);
        g.drawLine(inx, iny, inx, m);
        g.drawLine(inx + 1, m - 1, k - 1, m - 1);
        g.drawLine(k - 1, iny + 1, k - 1, m - 1);

        g.setColor(localColor2);
        g.drawLine(inx + 1, iny + 1, k - 2, iny + 1);
        g.drawLine(inx + 1, iny + 1, inx + 1, m - 2);
        g.drawLine(inx, m, k, m);
        g.drawLine(k, iny, k, m);

        FontMetrics localFontMetrics = g.getFontMetrics();
        int n = localFontMetrics.stringWidth(this.name);
        int i1 = localFontMetrics.getHeight() - localFontMetrics.getDescent();
        g.setColor(this.backgroundColor);
        g.fillRect(2 * ChiselBox.inx, iny, n + 8, 2);

        if (this.enabled) {
            g.setColor(Color.black);
            g.drawString(this.name, 2 * ChiselBox.inx + 4, ChiselBox.iny + i1 / 2 - 1);
        } else {
            g.setColor(this.backgroundColor.brighter());
            g.drawString(this.name, 1 + 2 * ChiselBox.inx + 4, 1 + ChiselBox.iny + i1 / 2 - 1);
            g.setColor(this.backgroundColor.darker());
            g.drawString(this.name, 2 * ChiselBox.inx + 4, ChiselBox.iny + i1 / 2 - 1);
        }

        this.content.setBounds(2 * ChiselBox.inx, ChiselBox.iny + i1, ownSize.width - 4 * ChiselBox.inx, ownSize.height - 2 * (ChiselBox.iny / 2 + i1));


        this.content.paint(this.content.getGraphics());
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/ChiselBox.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */