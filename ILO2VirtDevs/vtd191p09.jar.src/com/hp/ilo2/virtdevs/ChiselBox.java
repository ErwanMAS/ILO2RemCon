package com.hp.ilo2.virtdevs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Panel;

public class ChiselBox extends Panel {
    public Panel content;
    public String name;
    int inx = 8;
    int iny = 8;
    boolean raised = false;
    boolean enabled = true;
    Color bg = Color.lightGray;

    public ChiselBox() {
        this.content = new Panel();
        add(this.content);
        setBackground(this.bg);
    }

    public ChiselBox(String paramString) {
        this.content = new Panel();
        add(this.content);
        this.name = paramString;
        setBackground(this.bg);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        repaint();
    }


    public void cadd(Component paramComponent, GridBagConstraints paramGridBagConstraints, int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
        paramGridBagConstraints.gridx = paramInt1;
        paramGridBagConstraints.gridy = paramInt2;
        paramGridBagConstraints.gridwidth = paramInt3;
        paramGridBagConstraints.gridheight = paramInt4;
        this.content.add(paramComponent, paramGridBagConstraints);
    }


    public void paint(Graphics paramGraphics) {
        Dimension localDimension = getSize();


        Color localColor1 = this.raised ? this.bg.brighter() : this.bg.darker();
        Color localColor2 = this.raised ? this.bg.darker() : this.bg.brighter();

        int i = this.inx;
        int j = this.iny;
        int k = localDimension.width - this.inx;
        int m = localDimension.height - this.iny;


        paramGraphics.setColor(localColor1);
        paramGraphics.drawLine(i, j, k, j);
        paramGraphics.drawLine(i, j, i, m);
        paramGraphics.drawLine(i + 1, m - 1, k - 1, m - 1);
        paramGraphics.drawLine(k - 1, j + 1, k - 1, m - 1);

        paramGraphics.setColor(localColor2);
        paramGraphics.drawLine(i + 1, j + 1, k - 2, j + 1);
        paramGraphics.drawLine(i + 1, j + 1, i + 1, m - 2);
        paramGraphics.drawLine(i, m, k, m);
        paramGraphics.drawLine(k, j, k, m);

        FontMetrics localFontMetrics = paramGraphics.getFontMetrics();
        int n = localFontMetrics.stringWidth(this.name);
        int i1 = localFontMetrics.getHeight() - localFontMetrics.getDescent();
        paramGraphics.setColor(this.bg);
        paramGraphics.fillRect(2 * this.inx, j, n + 8, 2);

        if (this.enabled) {
            paramGraphics.setColor(Color.black);
            paramGraphics.drawString(this.name, 2 * this.inx + 4, this.iny + i1 / 2 - 1);
        } else {
            paramGraphics.setColor(this.bg.brighter());
            paramGraphics.drawString(this.name, 1 + 2 * this.inx + 4, 1 + this.iny + i1 / 2 - 1);
            paramGraphics.setColor(this.bg.darker());
            paramGraphics.drawString(this.name, 2 * this.inx + 4, this.iny + i1 / 2 - 1);
        }

        this.content.setBounds(2 * this.inx, this.iny + i1, localDimension.width - 4 * this.inx, localDimension.height - 2 * (this.iny / 2 + i1));


        this.content.paint(this.content.getGraphics());
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/ChiselBox.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */