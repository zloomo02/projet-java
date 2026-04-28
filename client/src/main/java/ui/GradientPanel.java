package ui;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    public GradientPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, Theme.BG_TOP, 0, h, Theme.BG_BOTTOM);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(255, 255, 255, 18));
        for (int y = 24; y < h; y += 28) {
            for (int x = 16; x < w; x += 36) {
                g2.fillOval(x, y, 2, 2);
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
