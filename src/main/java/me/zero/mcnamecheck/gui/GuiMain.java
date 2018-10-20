package me.zero.mcnamecheck.gui;

import com.google.common.collect.Lists;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.mojang.api.profiles.Profile;
import me.zero.mcnamecheck.Main;
import me.zero.mcnamecheck.UsernameContainer;
import me.zero.mcnamecheck.UsernameData;
import me.zero.mcnamecheck.util.UIHelper;
import me.zero.mcnamecheck.util.Util;
import org.oxbow.swingbits.dialog.task.TaskDialogs;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static javax.swing.JOptionPane.*;
import static me.zero.mcnamecheck.UsernameData.CheckStatus.*;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class GuiMain {

    public JPanel contentPane;

    private JList<UsernameData> usernamesList;

    private JButton buttonImport;
    private JButton buttonExport;
    private JButton buttonClear;
    private JButton buttonFilter;
    private JButton buttonClean;
    private JButton buttonCheck;

    private JLabel labelTotal;
    private JLabel labelChecked;
    private JLabel labelAvailable;
    private JLabel labelUnmigrated;
    private JLabel labelFailed;
    private JLabel labelStatus;

    private JLabel labelSuccess;
    private JLabel labelFailure;

    private JRadioButton radioButtonColoredTable;

    /**
     * The amount of successful requests
     */
    private int requestSuccess;

    /**
     * The amount of failed requests
     */
    private int requestFailure;

    /**
     * The current running status, displayed in the gui
     */
    private RunStatus status;

    /**
     * Volatile flag to control running state of the name lookup thread
     */
    private volatile boolean running;

    private final UsernameContainer container;

    public GuiMain(UsernameContainer c) {
        this.container = c;

        this.status = RunStatus.IDLE;
        this.update();

        this.buttonImport.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(false);
            int action = fileChooser.showOpenDialog(this.contentPane);
            if (action == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.readAllLines(fileChooser.getSelectedFile().toPath()).forEach(this.container::addUsername);
                    this.update();
                } catch (Exception e) {
                    TaskDialogs.showException(e);
                }
            }
        });

        this.buttonExport.addActionListener(event ->
                UIHelper.openWindow(new ExportDialog(this.container)));

        this.buttonFilter.addActionListener(event ->
                UIHelper.openWindow(new FilterDialog(test -> {
                    this.container.filterByName(test);
                    this.update();
                })));

        this.buttonClear.addActionListener(event -> {
            if (this.container.size() > 0) {
                int result = JOptionPane.showConfirmDialog(
                        this.contentPane,
                        "Clearing will remove all usernames from the list. Are you sure you want to do this?",
                        "Confirm Action",
                        YES_NO_OPTION,
                        QUESTION_MESSAGE
                );

                if (result == YES_OPTION) {
                    this.container.clear();
                    this.update();
                }
            }
        });

        this.buttonClean.addActionListener(event ->
                UIHelper.openWindow(new CleanDialog(test -> {
                    this.container.filter(test);
                    this.update();
                })));

        this.buttonCheck.addActionListener(event -> {
            if (!this.running) {
                this.running = true;
                new Thread(() -> {
                    this.requestSuccess = this.requestFailure = 0;
                    this.update();

                    List<List<UsernameData>> partitioned = Lists.partition(this.container.getUsernameData(), 100);
                    for (List<UsernameData> partition : partitioned) {
                        if (!this.running) {
                            return;
                        }

                        this.status = RunStatus.SENDING;
                        this.update();

                        Main.INSTANCE.profileRepository.findProfilesByNames(
                                partition.stream().map(u -> u.username).toArray(String[]::new),
                                (names, profiles) -> {
                                    List<Profile> profileList = Arrays.asList(profiles);

                                    // Set everything to unavailable
                                    partition.forEach(u -> {
                                        u.checkStatus = UNAVAILABLE;

                                        Profile profile = profileList.stream().filter(p -> u.username.equalsIgnoreCase(p.getName())).findFirst().orElse(null);

                                        if (profile != null) {
                                            u.checkStatus = UNAVAILABLE;
                                            u.unmigrated = profile.isLegacy();
                                        } else {
                                            u.checkStatus = AVAILABLE;
                                        }
                                    });
                                    this.requestSuccess++;
                                },
                                e -> {
                                    e.printStackTrace();
                                    partition.forEach(u -> u.checkStatus = FAILED);
                                    this.requestFailure++;
                                }
                        );

                        if (!this.running) {
                            return;
                        }

                        this.status = RunStatus.WAITING;
                        this.update();

                        try {
                            Thread.sleep(1100);
                        } catch (Exception e) {
                            TaskDialogs.showException(e);
                            this.status = RunStatus.IDLE;
                            this.running = false;
                            this.update();
                            break;
                        }
                    }

                    this.status = RunStatus.IDLE;
                    this.running = false;
                    this.update();
                }).start();
            } else {
                this.status = RunStatus.IDLE;
                this.running = false;
            }
            this.update();
        });

        this.usernamesList.setFocusable(false);
        this.usernamesList.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public final Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UsernameData) {
                    UsernameData user = (UsernameData) value;
                    setText(user.username);

                    if (GuiMain.this.radioButtonColoredTable.isSelected()) {
                        switch (user.checkStatus) {
                            case AVAILABLE:
                                setForeground(Color.GREEN);
                                break;
                            case UNAVAILABLE:
                                setForeground(user.unmigrated ? Color.BLUE : Color.ORANGE);
                                break;
                            case FAILED:
                                setForeground(Color.RED);
                                break;
                            case UNCHECKED:
                                setForeground(Color.BLACK);
                                break;
                        }
                    }
                }
                return c;
            }
        });
    }

    /**
     * Updates all of the UI elements with their current values.
     */
    private void update() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::update);
            return;
        }

        this.usernamesList.setListData(this.container.getUsernameData().toArray(new UsernameData[0]));

        List<UsernameData> usernames = this.container.getUsernameData();

        int checked = (int) usernames.stream().filter(u -> u.checkStatus == AVAILABLE || u.checkStatus == UNAVAILABLE).count();
        int available = (int) usernames.stream().filter(u -> u.checkStatus == AVAILABLE).count();
        int unmigrated = (int) usernames.stream().filter(u -> u.checkStatus == UNAVAILABLE && u.unmigrated).count();
        int failed = (int) usernames.stream().filter(u -> u.checkStatus == FAILED).count();

        NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());

        this.buttonCheck.setText(this.running ? "Cancel" : "Check");
        this.labelStatus.setText(Util.capitalize(this.status.name()));

        this.labelTotal.setText(format.format(usernames.size()));
        this.labelChecked.setText(format.format(checked));
        this.labelAvailable.setText(format.format(available));
        this.labelUnmigrated.setText(format.format(unmigrated));
        this.labelFailed.setText(format.format(failed));
        this.labelSuccess.setText(format.format(this.requestSuccess));
        this.labelFailure.setText(format.format(this.requestFailure));
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
        contentPane.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder("Usernames"));
        usernamesList = new JList();
        scrollPane1.setViewportView(usernamesList);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(9, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(150, -1), null, 0, false));
        buttonImport = new JButton();
        buttonImport.setText("Import");
        buttonImport.setToolTipText("Import a list of names from a file");
        panel1.add(buttonImport, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCheck = new JButton();
        buttonCheck.setText("Check");
        buttonCheck.setToolTipText("Check the list of names");
        panel1.add(buttonCheck, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonFilter = new JButton();
        buttonFilter.setText("Filter");
        buttonFilter.setToolTipText("Filter the list of names using RegEx");
        panel1.add(buttonFilter, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Requests"));
        final JLabel label1 = new JLabel();
        label1.setText("Success");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelSuccess = new JLabel();
        labelSuccess.setText("0");
        panel2.add(labelSuccess, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFailure = new JLabel();
        labelFailure.setText("0");
        panel2.add(labelFailure, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Failure");
        panel2.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonExport = new JButton();
        buttonExport.setText("Export");
        buttonExport.setToolTipText("Export the available names to a file");
        panel1.add(buttonExport, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder("Status"));
        final JLabel label3 = new JLabel();
        label3.setText("Total");
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelTotal = new JLabel();
        labelTotal.setText("0");
        panel3.add(labelTotal, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Checked");
        panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Available");
        panel3.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelAvailable = new JLabel();
        labelAvailable.setText("0");
        panel3.add(labelAvailable, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelChecked = new JLabel();
        labelChecked.setText("0");
        panel3.add(labelChecked, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Failed");
        panel3.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelFailed = new JLabel();
        labelFailed.setText("0");
        panel3.add(labelFailed, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelStatus = new JLabel();
        labelStatus.setText("");
        panel3.add(labelStatus, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Status");
        panel3.add(label7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Unmigrated");
        panel3.add(label8, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelUnmigrated = new JLabel();
        labelUnmigrated.setText("0");
        panel3.add(labelUnmigrated, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonClean = new JButton();
        buttonClean.setText("Clean");
        buttonClean.setToolTipText("Clean out usernames with a condition");
        panel1.add(buttonClean, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonClear = new JButton();
        buttonClear.setText("Clear");
        buttonClear.setToolTipText("Clear the list of names");
        panel1.add(buttonClear, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Settings"));
        radioButtonColoredTable = new JRadioButton();
        radioButtonColoredTable.setText("Colored Table");
        panel4.add(radioButtonColoredTable, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() { return contentPane; }

    private enum RunStatus {
        IDLE, SENDING, WAITING
    }

}
