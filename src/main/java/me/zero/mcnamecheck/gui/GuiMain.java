package me.zero.mcnamecheck.gui;

import com.google.common.collect.Lists;
import me.zero.mcnamecheck.Main;
import me.zero.mcnamecheck.UsernameContainer;
import me.zero.mcnamecheck.UsernameData;
import me.zero.mcnamecheck.util.UIHelper;
import me.zero.mcnamecheck.util.Util;
import org.oxbow.swingbits.dialog.task.TaskDialogs;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

        this.buttonExport.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(false);
            int action = fileChooser.showSaveDialog(this.contentPane);
            if (action == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.write(
                            Paths.get(fileChooser.getSelectedFile().getAbsolutePath() + ".txt"),
                            this.container.getUsernameData().stream()
                                    .filter(u -> u.checkStatus == AVAILABLE)
                                    .map(u -> u.username)
                                    .collect(Collectors.toList())
                    );
                } catch (Exception e) {
                    TaskDialogs.showException(e);
                }
            }

            this.container.getUsernameData().stream()
                    .filter(u -> u.checkStatus == AVAILABLE)
                    .map(u -> u.username)
                    .forEach(System.out::println);
        });

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
                                    // Set everything to unavailable
                                    partition.forEach(u -> u.checkStatus = UNAVAILABLE);
                                    // Then go back and set anything that is available to available
                                    Util.getAbsentNames(names, profiles).forEach(name ->
                                            this.container.getByName(name).checkStatus = AVAILABLE);
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
                                setForeground(Color.ORANGE);
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

        int checked   = (int) usernames.stream().filter(u -> u.checkStatus == AVAILABLE || u.checkStatus == UNAVAILABLE).count();
        int available = (int) usernames.stream().filter(u -> u.checkStatus == AVAILABLE).count();
        int failed    = (int) usernames.stream().filter(u -> u.checkStatus == FAILED).count();

        NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());


        this.buttonCheck   .setText(this.running ? "Cancel" : "Check");
        this.labelStatus   .setText(this.status.name().substring(0, 1) + this.status.name().substring(1).toLowerCase());

        this.labelTotal    .setText(format.format(usernames.size()));
        this.labelChecked  .setText(format.format(checked));
        this.labelAvailable.setText(format.format(available));
        this.labelFailed   .setText(format.format(failed));
        this.labelSuccess  .setText(format.format(this.requestSuccess));
        this.labelFailure  .setText(format.format(this.requestFailure));
    }

    private enum RunStatus {
        IDLE, SENDING, WAITING
    }
}
