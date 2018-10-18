package me.zero.mcnamecheck;

import com.mojang.api.profiles.HttpProfileRepository;
import me.zero.mcnamecheck.gui.GuiMain;
import me.zero.mcnamecheck.util.UIHelper;

import javax.swing.*;

/**
 * @author Brady
 * @since 10/16/2018
 */
public enum Main {

    INSTANCE;

    public final HttpProfileRepository profileRepository;
    private final UsernameContainer usernames;
    public GuiMain gui;

    Main() {
        this.profileRepository = new HttpProfileRepository("minecraft");
        this.usernames = new UsernameContainer();
    }

    public void init() {
        if (this.gui != null) {
            return;
        }

        this.gui = new GuiMain(this.usernames);
        JFrame frame = new JFrame();
        frame.setTitle("Minecraft Name Checker");
        frame.setContentPane(gui.contentPane);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        UIHelper.openWindow(frame);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Main.INSTANCE.init();
    }
}
