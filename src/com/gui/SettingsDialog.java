package com.gui;

import com.model.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textFFmpegPath;
    private JTextField textVlcPath;
    private JSpinner spinnerSeconds;

    private Settings settings = Settings.getInstance();

    public SettingsDialog(Frame owner) {
        super(owner);
        setContentPane(contentPane);
        setModal(true);
        setResizable(false);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        //  call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        textFFmpegPath.setText(settings.getFfmpegPath());
        textVlcPath.setText(settings.getVlcPath());
        spinnerSeconds.setValue(settings.getSeconds());

        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private void onOK() {
        // add your code here

        settings.setVlcPath(textVlcPath.getText());
        settings.setFfmpegPath(textFFmpegPath.getText());
        settings.setSeconds((Integer)spinnerSeconds.getValue());

        try {
            settings.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        /*SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);*/
    }

    public void setData(SettingsDialog data) {
    }

    public void getData(SettingsDialog data) {
    }

    public boolean isModified(SettingsDialog data) {
        return false;
    }
}
