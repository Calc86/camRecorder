package com.gui;

import com.onvif.JAXBContext;
import com.onvif.cam.Cam;
import com.onvif.cam.Profile;
import org.onvif.ver10.media.wsdl.GetSnapshotUriResponse;
import org.onvif.ver10.media.wsdl.GetStreamUriResponse;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class AddDialog1 extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane;
    private JTextField textFieldUrl;
    private JTextField textFieldOnvifUrl;
    private JButton buttonGet;
    private JComboBox<OnvifProfile> comboBoxSources;

    private URI url = null;

    public AddDialog1(Frame owner) {
        super(owner, "Add", true);

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

        buttonGet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onGet();
            }
        });

        comboBoxSources.setEnabled(false);

        comboBoxSources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onComboBoxSources();
            }
        });

        setPreferredSize(new Dimension(600, 200));
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private void onOK() {
        try {
            url = new URI(textFieldUrl.getText());
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(this, "Wrong url" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
    }

    private void onGet(){
        try {
            JAXBContext.create();
        } catch (JAXBException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Cam cam;

        try {
            cam = new Cam(textFieldOnvifUrl.getText());
            cam.init();

        } catch (ConnectException e){
            JOptionPane.showMessageDialog(this, e.getClass().getName() + ": " + e.getMessage(), "Connect Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (MalformedURLException e){
            JOptionPane.showMessageDialog(this, e.getClass().getName() + ": " + e.getMessage(), "Url Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getClass().getName() + ": " + e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Profile> profiles = new ArrayList<Profile>();

        for(org.onvif.ver10.schema.Profile p : cam.getProfiles()){
            GetStreamUriResponse stream = null;
            GetSnapshotUriResponse snap = null;
            try {
                stream = cam.getStreamUri(p);
                snap = cam.getSnapshotUri(p);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Profile profile = new Profile();
            profile.setName(p.getName());
            profile.setEncoding(p.getVideoEncoderConfiguration().getEncoding().value());
            profile.setWidth(p.getVideoEncoderConfiguration().getResolution().getWidth());
            profile.setHeight(p.getVideoEncoderConfiguration().getResolution().getHeight());
            profile.setQuality(p.getVideoEncoderConfiguration().getQuality());

            Profile.URI video = profile.new URI();
            profile.setVideo(video);
            Profile.URI snapshot = profile.new URI();
            profile.setSnap(snapshot);

            if(stream != null){
                video.setUri(stream.getMediaUri().getUri());
                if(stream.getMediaUri().getTimeout() != null)
                    video.setTtl(stream.getMediaUri().getTimeout().toString());
                video.setInvalidAfterConnection(stream.getMediaUri().isInvalidAfterConnect());
                video.setInvalidAfterReboot(stream.getMediaUri().isInvalidAfterReboot());
            }

            if(snap != null){
                snapshot.setUri(snap.getMediaUri().getUri());
                if(snap.getMediaUri().getTimeout() != null)
                    snapshot.setTtl(snap.getMediaUri().getTimeout().toString());
                snapshot.setInvalidAfterConnection(snap.getMediaUri().isInvalidAfterConnect());
                snapshot.setInvalidAfterReboot(snap.getMediaUri().isInvalidAfterReboot());
            }

            profiles.add(profile);
        }

        comboBoxSources.removeAll();
        for(Profile profile : profiles){
            comboBoxSources.addItem(new OnvifProfile(profile));
        }

        if(comboBoxSources.getItemCount() > 0) comboBoxSources.setEnabled(true);
        else comboBoxSources.setEnabled(false);
        onComboBoxSources();
    }

    public void onComboBoxSources(){
        OnvifProfile onvif = (OnvifProfile)comboBoxSources.getSelectedItem();
        textFieldUrl.setText(onvif.getProfile().getVideo().getUri());
    }

    private class OnvifProfile{
        private Profile profile;

        private OnvifProfile(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return profile.getName() + " (" + profile.getEncoding() + ") "
                    + profile.getWidth() + "x" + profile.getHeight()
                    + ": " + profile.getVideo().getUri()
                    + ", TTL: " + profile.getVideo().getTtl();
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public URI getUrl() {
        return url;
    }

    /*public static void main(String[] args) {
        AddDialog1 dialog = new AddDialog1();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }*/
}
