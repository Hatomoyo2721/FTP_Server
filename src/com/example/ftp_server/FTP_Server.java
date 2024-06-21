package com.example.ftp_server;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class FTP_Server extends JFrame {

    private final JTextArea consoleTextArea;
    private final DefaultListModel<String> fileListModel;
    private final JList<String> fileList;
    private final File downloadDirectory;
    private final File tempDirectory;
    private final FTPServerBackend serverBackend;
    private final ExecutorService executorService;

    public FTP_Server() {
        setTitle("FTP Server");
        setSize(900, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setWrapStyleWord(true);

        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
        consoleScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        fileList.setPreferredSize(new Dimension(400, fileList.getPreferredSize().height));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(consoleScrollPane, BorderLayout.CENTER);
        panel.add(fileScrollPane, BorderLayout.EAST);

        add(panel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        add(createStatusBar(), BorderLayout.NORTH);

        downloadDirectory = new File("downloaded_files");
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdir();
        }

        tempDirectory = new File("temp_files");
        if (!tempDirectory.exists()) {
            tempDirectory.mkdir();
        }

        serverBackend = new FTPServerBackend(this, downloadDirectory, tempDirectory);
        executorService = Executors.newSingleThreadExecutor();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> shutdownServer());
        fileMenu.add(exitMenuItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton openButton = new JButton("Open Selected File");
        openButton.addActionListener(e -> openSelectedFile());

        JButton downloadButton = new JButton("Download Selected File");
        downloadButton.addActionListener(e -> downloadSelectedFile());

        buttonPanel.add(openButton);
        buttonPanel.add(downloadButton);

        return buttonPanel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Server is running...");
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "FTP Server v1.0\nDeveloped by SomeoneME", 
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public void startServer() {
        serverBackend.startServer();
    }

    public void appendToConsole(String message) {
        SwingUtilities.invokeLater(() -> consoleTextArea.append(message + "\n"));
    }

    private void openSelectedFile() {
        FileHandler.openSelectedFile(fileList, tempDirectory, this);
    }

    private void downloadSelectedFile() {
        FileHandler.downloadSelectedFile(fileList, tempDirectory, downloadDirectory, this);
    }

    public void addFileToList(String fileName) {
        SwingUtilities.invokeLater(() -> fileListModel.addElement(fileName));
    }

    public boolean confirmSaveFile(String fileName) {
        return true; // Automatically save to temp directory
    }

    public static void main(String[] args) {
        FTP_Server serverGUI = new FTP_Server();
        SwingUtilities.invokeLater(() -> {
            serverGUI.setVisible(true);
            serverGUI.startServer();
        });

        // Adding shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SwingUtilities.invokeLater(serverGUI::shutdownServer)));
    }

    @Override
    public void dispose() {
        super.dispose();
        serverBackend.shutdownThreadPool();
        FileHandler.deleteTempDirectory(tempDirectory);
    }

    public void shutdownServer() {
        SwingUtilities.invokeLater(() -> {
            int response = JOptionPane.showConfirmDialog(this,
                    "Do you want to shut down the server?",
                    "Shutdown Server",
                    JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                // Initiate shutdown
                initiateShutdown();
            }
        });
    }

    private void initiateShutdown() {
        executorService.submit(() -> {
            serverBackend.shutdownThreadPool();
            FileHandler.deleteTempDirectory(tempDirectory);
            appendToConsole("\nServer shutting down...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
    }
}
