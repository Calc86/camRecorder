package ru.xsrv.recorder.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by calc on 22.07.14.
 *
 */
public class AddDialogOld extends JDialog {
    private JTextField textField;

    private URI url = null;

    public AddDialogOld(Frame owner) {
        super(owner, "Add", true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //getContentPane().setBor
        //background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Url(http, rtsp):");
        textField = new JTextField("http://", 30);
        //testButton = new JButton("Test");
        JButton addButton = new JButton("Add");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    url = new URI(textField.getText());
                    AddDialogOld.this.dispose();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(AddDialogOld.this, "Wrong url", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        getContentPane().add(BorderLayout.WEST, label);
        getContentPane().add(BorderLayout.CENTER, textField);
        //getContentPane().add(BorderLayout.EAST, testButton);
        getContentPane().add(BorderLayout.SOUTH, addButton);
        //setC

        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    public URI getUrl() {
        return url;
    }
}
