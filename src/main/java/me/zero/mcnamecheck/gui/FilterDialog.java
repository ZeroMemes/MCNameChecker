package me.zero.mcnamecheck.gui;

import org.oxbow.swingbits.dialog.task.TaskDialogs;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilterDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textFieldRegex;
    private JRadioButton radioButtonMatches;
    private JRadioButton radioButtonContains;
    private JRadioButton radioButtonNegate;

    FilterDialog(Consumer<Predicate<String>> callback) {
        setContentPane(this.contentPane);
        setModal(true);
        getRootPane().setDefaultButton(this.buttonOK);

        this.radioButtonMatches.setSelected(true);

        this.radioButtonMatches.addActionListener(event ->
                this.radioButtonContains.setSelected(!this.radioButtonMatches.isSelected()));

        this.radioButtonContains.addActionListener(event ->
                this.radioButtonMatches.setSelected(!this.radioButtonContains.isSelected()));

        this.buttonOK.addActionListener(event -> {
            try {
                Pattern pattern = Pattern.compile(this.textFieldRegex.getText());
                callback.accept(element -> this.isValid(pattern, element));
                dispose();
            } catch (Exception e) {
                TaskDialogs.showException(e);
            }
        });

        this.buttonCancel.addActionListener(event -> dispose());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.contentPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private boolean isValid(Pattern pattern, String element) {
        Matcher matcher = pattern.matcher(element);
        return !this.radioButtonNegate.isSelected() == (this.radioButtonMatches.isSelected() ? matcher.matches() : matcher.find());
    }
}
