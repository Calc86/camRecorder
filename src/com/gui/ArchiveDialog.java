package com.gui;

import com.model.Archive;
import com.model.Cam;
import com.model.Model;
import com.video.FFmpeg;
import com.video.M3U;
import com.video.Vlc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DateFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ArchiveDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonPlay;
    private JButton buttonMergeAndPlay;
    private JButton buttonReturn;
    private JToolBar toolBar;
    private JPanel panelArchive;
    private JComboBox<Cam> comboBoxCam;
    private JSpinner spinnerTimeFrom;
    private JSpinner spinnerTimeTo;

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
        setToolButton(buttonMergeAndPlay, "ic_action_copy", "Merge and play");
        setToolButton(buttonReturn, "ic_action_forward", "Return");

        buttonPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                M3U m3u = new M3U();
                Cam cam = (Cam) comboBoxCam.getSelectedItem();
                try {
                    String m3uPath = m3u.create(cam.getId(), getList());
                    Vlc.play(m3uPath);
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        buttonMergeAndPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //fillArchive();
                FFmpeg FFmpeg = new FFmpeg();
                try {
                    //todo: бывает что ffmpeg "заедает" на этой операции
                    FFmpeg.createConcatFile(getList());
                    FFmpeg.concat();
                    Vlc.play("concat.mp4");
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });


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

    private List<Archive> getList() throws SQLException {
        Cam cam = (Cam) comboBoxCam.getSelectedItem();
        Date from = (Date)spinnerTimeFrom.getValue();
        Date to = (Date)spinnerTimeTo.getValue();

        return Model.select(new Archive(),
                "CID=" + cam.getId() +
                        " and start>=" + from.getTime() +
                        " and start<=" + to.getTime()
        );
    }

    private void fillArchive(){
        panelArchive.removeAll();
        panelArchive.repaint();
        JLabel label;
        try {
            List<Archive> list = getList();
            SimpleDateFormat date = new SimpleDateFormat("[HH:mm:ss]");

            for(Archive archive : list){
                Date d = new Date(archive.getStart());
                label = new JLabel(archive.getId() + ":" + date.format(d));
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

        spinnerTimeFrom = new JSpinner( new SpinnerDateModel());
        JSpinner.DateEditor timeEditorFrom = new JSpinner.DateEditor(spinnerTimeFrom, "yyyy/MM/dd HH:mm:ss");
        spinnerTimeFrom.setEditor(timeEditorFrom);
        spinnerTimeFrom.setValue(new Date());

        spinnerTimeTo = new JSpinner( new SpinnerDateModel());
        JSpinner.DateEditor timeEditorTo = new JSpinner.DateEditor(spinnerTimeTo, "yyyy/MM/dd HH:mm:ss");
        spinnerTimeTo.setEditor(timeEditorTo);
        spinnerTimeTo.setValue(new Date());

        spinnerTimeFrom.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fillArchive();
            }
        });

        spinnerTimeTo.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fillArchive();
            }
        });
    }
}
