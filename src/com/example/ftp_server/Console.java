package com.example.ftp_server;

import javax.swing.*;

public class Console {

    public static void appendToConsole(JTextArea consoleTextArea, String message) {
        SwingUtilities.invokeLater(() -> {
            consoleTextArea.append(message + "\n");
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        });
    }
}
