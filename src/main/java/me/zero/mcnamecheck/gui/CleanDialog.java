package me.zero.mcnamecheck.gui;

import me.zero.mcnamecheck.UsernameData;

import javax.swing.*;
import java.awt.event.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static me.zero.mcnamecheck.UsernameData.CheckStatus.*;

public final class CleanDialog extends JDialog {

    private JPanel contentPane;

    private JButton buttonOK;
    private JButton buttonCancel;

    private JRadioButton radioButtonAvailable;
    private JRadioButton radioButtonUnavailable;
    private JRadioButton radioButtonFailed;
    private JRadioButton radioButtonUnchecked;

    CleanDialog(Consumer<Predicate<UsernameData>> callback) {
        setContentPane(this.contentPane);
        setModal(true);
        getRootPane().setDefaultButton(this.buttonOK);

        this.buttonOK.addActionListener(event -> {
            callback.accept(u -> {
                if (this.radioButtonAvailable.isSelected() && u.checkStatus == AVAILABLE) {
                    return false;
                }
                if (this.radioButtonUnavailable.isSelected() && u.checkStatus == UNAVAILABLE) {
                    return false;
                }
                if (this.radioButtonFailed.isSelected() && u.checkStatus == FAILED) {
                    return false;
                }
                if (this.radioButtonUnchecked.isSelected() && u.checkStatus == UNCHECKED) {
                    return false;
                }
                return true;
            });
            dispose();
        });

        this.buttonCancel.addActionListener(event -> dispose());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.contentPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }
}
