package me.zero.mcnamecheck.util;

import java.awt.*;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class UIHelper {

    private UIHelper() {}

    public static void openWindow(Window window) {
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
