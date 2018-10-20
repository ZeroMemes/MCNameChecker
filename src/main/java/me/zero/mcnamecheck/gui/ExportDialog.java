package me.zero.mcnamecheck.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.zero.mcnamecheck.UsernameContainer;
import me.zero.mcnamecheck.UsernameData;
import me.zero.mcnamecheck.util.Util;
import org.oxbow.swingbits.dialog.task.TaskDialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.zero.mcnamecheck.UsernameData.CheckStatus.*;

public class ExportDialog extends JDialog {

    private static final String[] FORMAT_EXTENSIONS = {
            "txt", "csv", "json"
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JPanel contentPane;

    private JCheckBox checkBoxAvailable;
    private JCheckBox checkBoxUnavailable;
    private JCheckBox checkBoxFailed;
    private JCheckBox checkBoxUnchecked;

    private JComboBox comboBoxMigration;

    private JComboBox comboBoxFormat;
    private JTextField textFieldFile;
    private JButton buttonChooseFile;

    private JButton buttonExport;
    private JButton buttonCancel;

    private File file;

    private final UsernameContainer container;

    ExportDialog(UsernameContainer c) {
        this.container = c;

        setContentPane(contentPane);
        setModal(true);
        setTitle("Export Usernames");
        getRootPane().setDefaultButton(buttonExport);

        this.buttonChooseFile.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(false);
            int action = fileChooser.showDialog(this.contentPane, "Choose");
            if (action == JFileChooser.APPROVE_OPTION) {
                this.file = fileChooser.getSelectedFile();
                update();
            }
        });

        this.comboBoxFormat.addActionListener(event -> update());

        this.buttonExport.addActionListener(event -> {
            if (this.file == null)
                return;

            Path outputPath = Paths.get(this.file.getAbsolutePath() + getExtension());

            List<UsernameData> toExport = this.container.getUsernameData().stream()
                    .filter(u -> {
                        if (this.checkBoxAvailable.isSelected() && u.checkStatus == AVAILABLE) {
                            return true;
                        }
                        if (this.checkBoxUnavailable.isSelected() && u.checkStatus == UNAVAILABLE) {
                            switch (this.comboBoxMigration.getSelectedIndex()) {
                                case 0:
                                    return true;
                                case 1:
                                    return !u.unmigrated;
                                case 2:
                                    return u.unmigrated;
                            }
                        }
                        if (this.checkBoxFailed.isSelected() && u.checkStatus == FAILED) {
                            return true;
                        }
                        if (this.checkBoxUnchecked.isSelected() && u.checkStatus == UNCHECKED) {
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            try {
                switch (this.comboBoxFormat.getSelectedIndex()) {
                    case 0: { // Raw
                        this.writeRaw(outputPath, toExport);
                        break;
                    }
                    case 1: { // CSV
                        this.writeCSV(outputPath, toExport);
                        break;
                    }
                    case 2: { // JSON
                        this.writeJSON(outputPath, toExport);
                        break;
                    }
                }
            } catch (Exception e) {
                TaskDialogs.showException(e);
            }

            dispose();
        });

        this.buttonCancel.addActionListener(event -> dispose());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.contentPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void update() {
        if (this.file != null) {
            this.textFieldFile.setText(this.file.getAbsolutePath() + getExtension());
        }
    }

    private String getExtension() {
        return "." + FORMAT_EXTENSIONS[this.comboBoxFormat.getSelectedIndex()];
    }

    private void writeRaw(Path path, List<UsernameData> usernames) throws IOException {
        List<String> lines = new ArrayList<>();

        for (UsernameData.CheckStatus status : UsernameData.CheckStatus.values()) {
            lines.add(Util.capitalize(status.name()) + ":");
            usernames.stream().filter(u -> u.checkStatus == status).map(u -> " " + u.username).forEach(lines::add);
        }

        Files.write(path, lines);
    }

    private void writeCSV(Path path, List<UsernameData> usernames) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("username,status,migrated");
        usernames.forEach(u -> lines.add(String.join(",",
                u.username,
                Util.capitalize(u.checkStatus.toString()),
                u.checkStatus == UNAVAILABLE ? String.valueOf(!u.unmigrated) : "N/A")
        ));

        Files.write(path, lines);
    }

    private void writeJSON(Path path, List<UsernameData> usernames) throws IOException {
        JsonArray array = new JsonArray();

        usernames.forEach(u -> {
            JsonObject object = new JsonObject();
            object.addProperty("username", u.username);
            object.addProperty("status", Util.capitalize(u.checkStatus.toString()));
            if (u.checkStatus == UNAVAILABLE) {
                object.addProperty("migrated", !u.unmigrated);
            }
            array.add(object);
        });

        Files.write(path, Collections.singletonList(GSON.toJson(array)));
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonExport = new JButton();
        buttonExport.setText("Export");
        panel2.add(buttonExport, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Check Status"));
        checkBoxAvailable = new JCheckBox();
        checkBoxAvailable.setSelected(true);
        checkBoxAvailable.setText("Available");
        panel4.add(checkBoxAvailable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxUnavailable = new JCheckBox();
        checkBoxUnavailable.setSelected(true);
        checkBoxUnavailable.setText("Unavailable");
        panel4.add(checkBoxUnavailable, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxFailed = new JCheckBox();
        checkBoxFailed.setText("Failed");
        panel4.add(checkBoxFailed, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxUnchecked = new JCheckBox();
        checkBoxUnchecked.setText("Unchecked");
        panel4.add(checkBoxUnchecked, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Choose the username types that should be exported");
        panel4.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder("Migration Status"));
        comboBoxMigration = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Any");
        defaultComboBoxModel1.addElement("Migrated");
        defaultComboBoxModel1.addElement("Unmigrated");
        comboBoxMigration.setModel(defaultComboBoxModel1);
        comboBoxMigration.setSelectedIndex(0);
        panel5.add(comboBoxMigration, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Only applies to accounts that have been checked and are unavailable");
        panel5.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 4, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder("Export Settings"));
        final JLabel label3 = new JLabel();
        label3.setText("Format");
        panel6.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxFormat = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Raw");
        defaultComboBoxModel2.addElement("CSV");
        defaultComboBoxModel2.addElement("JSON");
        comboBoxFormat.setModel(defaultComboBoxModel2);
        panel6.add(comboBoxFormat, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("File");
        panel6.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldFile = new JTextField();
        textFieldFile.setEditable(false);
        textFieldFile.setEnabled(true);
        textFieldFile.setText("");
        panel6.add(textFieldFile, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseFile = new JButton();
        buttonChooseFile.setText("Choose");
        panel6.add(buttonChooseFile, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() { return contentPane; }
}
