package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class Theme {
    public static final Color BG_TOP = new Color(16, 20, 32);
    public static final Color BG_BOTTOM = new Color(34, 42, 66);
    public static final Color PANEL_BG = new Color(26, 32, 50);
    public static final Color PANEL_BORDER = new Color(44, 56, 86);
    public static final Color TEXT_PRIMARY = new Color(238, 244, 255);
    public static final Color TEXT_MUTED = new Color(168, 184, 210);
    public static final Color ACCENT = new Color(255, 183, 3);
    public static final Color ACCENT_DARK = new Color(204, 133, 0);
    public static final Color ERROR = new Color(231, 76, 60);

    private static final Font BASE_FONT = new Font("Trebuchet MS", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Trebuchet MS", Font.BOLD, 20);

    private Theme() {}

    public static void applyToFrame(JFrame frame) {
        frame.getContentPane().setBackground(BG_BOTTOM);
    }

    public static void stylePanel(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(PANEL_BG);
        panel.setBorder(new CompoundBorder(new LineBorder(PANEL_BORDER, 1, true), new EmptyBorder(12, 12, 12, 12)));
    }

    public static void styleTitle(JLabel label) {
        label.setForeground(TEXT_PRIMARY);
        label.setFont(TITLE_FONT);
    }

    public static void styleLabel(JLabel label) {
        label.setForeground(TEXT_MUTED);
        label.setFont(BASE_FONT);
    }

    public static void stylePrimaryButton(AbstractButton button) {
        button.setBackground(ACCENT);
        button.setForeground(new Color(25, 30, 44));
        button.setFocusPainted(false);
        button.setFont(BASE_FONT.deriveFont(Font.BOLD));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        button.setBackground(new Color(40, 50, 78));
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setFont(BASE_FONT);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public static void styleReadyButtonIdle(AbstractButton button) {
        button.setBackground(new Color(40, 50, 78));
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setFont(BASE_FONT);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public static void styleReadyButtonActive(AbstractButton button) {
        button.setBackground(ACCENT);
        button.setForeground(new Color(25, 30, 44));
        button.setFocusPainted(false);
        button.setFont(BASE_FONT.deriveFont(Font.BOLD, 16f));
        button.setBorder(new EmptyBorder(12, 22, 12, 22));
    }

    public static void styleField(JTextField field) {
        field.setBackground(new Color(22, 28, 44));
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(BASE_FONT);
        field.setBorder(compoundInputBorder());
    }

    public static void stylePasswordField(JPasswordField field) {
        field.setBackground(new Color(22, 28, 44));
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(BASE_FONT);
        field.setBorder(compoundInputBorder());
    }

    public static void styleTextArea(JTextArea area) {
        area.setBackground(new Color(22, 28, 44));
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setFont(BASE_FONT);
        area.setBorder(compoundInputBorder());
    }

    public static void styleList(JList<?> list) {
        list.setBackground(new Color(22, 28, 44));
        list.setForeground(TEXT_PRIMARY);
        list.setSelectionBackground(ACCENT_DARK);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(BASE_FONT);
        list.setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    public static void styleScrollPane(JScrollPane pane) {
        pane.getViewport().setBackground(new Color(22, 28, 44));
        pane.setBorder(new LineBorder(PANEL_BORDER, 1, true));
    }

    public static void styleStatusLabel(JLabel label) {
        label.setForeground(TEXT_MUTED);
        label.setFont(BASE_FONT);
    }

    private static Border compoundInputBorder() {
        return new CompoundBorder(new LineBorder(PANEL_BORDER, 1, true), new EmptyBorder(6, 8, 6, 8));
    }
}
