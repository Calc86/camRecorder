package com.gui;

import com.Server;
import com.model.Cam;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by calc on 22.07.14.
 *
 */
public class MainFrame {
    private JFrame frame;
    private JTree tree;
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Камеры");
    private JToolBar tool;
    private Server server = new Server();


    public void buildGUI(){
        frame = new JFrame("Digital Video Recorder");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        tree = new JTree(root);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        buildTree();
        JScrollPane scrollTree = new JScrollPane(tree);

        tool = new JToolBar();
        //tool.setPreferredSize(new Dimension(300, 42));
        tool.setFloatable(false);
        tool.setRollover(true);
        buildTollBar();

        frame.getContentPane().add(BorderLayout.NORTH, tool);
        frame.getContentPane().add(BorderLayout.CENTER, scrollTree);
        //frame.setBounds(50, 50, 300, 300);
        frame.setPreferredSize(new Dimension(500, 500));
        frame.pack();
        tree.requestFocusInWindow();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void buildTollBar(){
        final JButton buttonAdd = createToolButton("ic_action_new", "Add cam");
        tool.add(buttonAdd);
        final JButton buttonRemove = createToolButton("ic_action_remove", "Remove cam");
        tool.add(buttonRemove);
        tool.addSeparator();

        final JButton buttonStart = createToolButton("ic_action_play", "Start server");
        tool.add(buttonStart);
        final JButton buttonStop = createToolButton("ic_action_stop", "Stop server");
        tool.add(buttonStop);
        buttonStop.setEnabled(false);

        tool.addSeparator();
        final JButton buttonArchive = createToolButton("ic_action_video", "View archive");
        tool.add(buttonArchive);
        buttonArchive.setEnabled(false);

        tool.addSeparator();
        final JButton buttonExit = createToolButton("ic_action_forward", "Exit");
        tool.add(buttonExit);



        buttonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddDialog dialog = new AddDialog(frame);

                URI url = dialog.getUrl();

                if(url == null) return;

                Cam cam = new Cam();
                cam.setUrl(url);
                try {
                    cam.insert();
                    buildTree();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        buttonRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

                if(node == null) return;

                int yes = JOptionPane.showConfirmDialog(frame, "Вы уверены?", "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(yes == JOptionPane.NO_OPTION) return;

                Object o = node.getUserObject();
                if(o.getClass().equals(Cam.class)){
                    Cam cam = (Cam)o;
                    try {
                        cam.delete();
                        buildTree();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        buttonStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                server.start();

                if(!server.isStop()){
                    buttonAdd.setEnabled(false);
                    buttonRemove.setEnabled(false);
                    buttonStart.setEnabled(false);
                    buttonStop.setEnabled(true);
                    buttonExit.setEnabled(false);
                    tree.setEnabled(false);

                    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                server.stop();

                if(server.isStop()){
                    buttonAdd.setEnabled(true);
                    buttonRemove.setEnabled(true);
                    buttonStart.setEnabled(true);
                    buttonStop.setEnabled(false);
                    buttonExit.setEnabled(true);
                    tree.setEnabled(true);

                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }
        });

        buttonExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
    }

    private JButton createToolButton(String image, String toolTip){
        String imgLocation = "res/images/" + image + ".png";
        URL imageURL = JToolBar.class.getResource(imgLocation);

        JButton button = new JButton();
        button.setToolTipText(toolTip);
        button.setIcon(new ImageIcon(imgLocation, toolTip));

        return button;
    }

    private void buildTree(){
        root.removeAllChildren();
        Cam cam = new Cam();

        try {
            List<Cam> list = cam.selectAll();

            for(Cam c: list){
                root.add(new DefaultMutableTreeNode(c, false));
            }
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.reload();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) {
            // handle exception
        }
        catch (ClassNotFoundException e) {
            // handle exception
        }
        catch (InstantiationException e) {
            // handle exception
        }
        catch (IllegalAccessException e) {
            // handle exception
        }
        MainFrame main = new MainFrame();
        main.buildGUI();
    }
}
