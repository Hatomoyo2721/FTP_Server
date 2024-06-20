package com.example.ftp_server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FTPServerBackend {

    private static final int PORT = 4321;
    private final FTP_Server serverGUI;
    private final File downloadDirectory;
    private final File tempDirectory;
    private final ExecutorService threadPool;

    public FTPServerBackend(FTP_Server serverGUI, File downloadDirectory, File tempDirectory) {
        this.serverGUI = serverGUI;
        this.downloadDirectory = downloadDirectory;
        this.tempDirectory = tempDirectory;
        this.threadPool = Executors.newWorkStealingPool();
    }

    public void startServer() {
        new Thread(this::runServer).start();
    }

    private void runServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();            
                    serverGUI.appendToConsole("Client connected from IP: " + clientSocket.getInetAddress().getHostAddress());
                    Future<?> submit = threadPool.submit(() -> handleClientConnection(clientSocket));
                } catch (IOException e) {
                    serverGUI.appendToConsole("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            serverGUI.appendToConsole("Error starting server: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())) {

            String type = dataInputStream.readUTF();
            if ("FILE".equals(type)) {
                String fileName = dataInputStream.readUTF();
                serverGUI.appendToConsole("Receiving file: " + fileName);

                byte[] fileData = FileHandler.receiveFileToMemory(dataInputStream, dataOutputStream, fileName, serverGUI);
                if (fileData != null) {
                    serverGUI.appendToConsole("File received. Saving to temp directory.");
                    File tempFile = new File(tempDirectory, fileName);
                    FileHandler.saveFileFromMemory(fileData, tempFile, serverGUI);
                    serverGUI.addFileToList(fileName);
                }
            } else {
                serverGUI.appendToConsole("Unknown request from client. Closing connection.\n");
            }
        } catch (IOException e) {
//            serverGUI.appendToConsole("Error handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                serverGUI.appendToConsole("Error closing client socket: " + e.getMessage() + "\n");
            }
        }
    }

    public void shutdownThreadPool() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}
