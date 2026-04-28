package ui;

import javax.swing.*;

public abstract class BasePanel extends JPanel {
    protected BasePanel() {
        setOpaque(false);
    }

    /** Called when this panel becomes visible/active. Override to set message handler. */
    public void onShow() {}
}
