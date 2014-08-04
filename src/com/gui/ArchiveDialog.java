package com.gui;

import com.model.Archive;
import com.model.Cam;
import com.model.Model;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

public class ArchiveDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonPlay;
    private JButton buttonApply;
    private JButton buttonReset;
    private JButton buttonReturn;
    private JTextField textFrom;
    private JTextField textTo;
    private JToolBar toolBar;
    private JPanel panelArchive;
    private JComboBox<Cam> comboBoxCam;

    public ArchiveDialog(JFrame owner) {
        super(owner);
        setContentPane(contentPane);
        setModal(true);
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

        // call onCancel() when cross is clicked
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

        setToolButton(buttonPlay, "ic_action_play", "Play");
        setToolButton(buttonApply, "ic_action_accept", "Apply");
        setToolButton(buttonReset, "ic_action_refresh", "Reset");
        setToolButton(buttonReturn, "ic_action_forward", "Return");


        List<Cam> list;
        try {
            list = Model.selectAll(new Cam());

            for(Cam c : list){
                comboBoxCam.addItem(c);
            }

            fillArchive();

            /*if(list.size() != 0){
                fillArchive((Cam) comboBoxCam.getSelectedItem());
            }*/

        } catch (SQLException e) {
            e.printStackTrace();
        }

        comboBoxCam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fillArchive();
            }
        });

        display();
    }

    private void display(){
        setPreferredSize(new Dimension(500, 400));
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private void fillArchive(){
        Cam cam = (Cam) comboBoxCam.getSelectedItem();

        panelArchive.removeAll();
        panelArchive.revalidate();
        JLabel label;
        try {
            List<Archive> list = Model.select(new Archive(), "CID=" + cam.getId());
            for(Archive a : list){
                label = new JLabel(a.getId() + "");
                panelArchive.add(label);
            }
            panelArchive.revalidate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void setToolButton(final JButton button, final String image, final String toolTip){
        String imgLocation = "res/images/" + image + ".png";

        button.setToolTipText(toolTip);
        button.setIcon(new ImageIcon(imgLocation, toolTip));
    }

    private void createUIComponents() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        panelArchive = new JPanel(new WrapLayout());
        //panelArchive.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        //panelArchive.setPreferredSize(new Dimension(500, 400));
    }
}
