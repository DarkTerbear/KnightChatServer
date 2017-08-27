package com.knightchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;

public class KnightChatServer {

    /**
     * Server listens on port 1337
     */
    private static final int PORT = 1337;

    /**
     * Set of names in the chatroom. Maintained to prevent name duplicates
     */
    private static HashSet<String> names = new HashSet<>();

    /**
     * Set of all PrintWriters for all clients. Maintained for simpler broadcasting
     */
    private static HashSet<PrintWriter> writers = new HashSet<>();

    /**
     * Application start point. Starts listening on port, spawns handler threads.
     * @param args
     */
    public static void main(String[] args) {
        boolean serverStarted = false;
        System.out.println("INFO: Starting KnightChat server on port " + PORT + "...");
        try {
            ServerSocket listener = new ServerSocket(PORT);
            try {
                while (true) {
                    if (!serverStarted) {
                        System.out.println("INFO: KnightChat server successfully started on port " + PORT + "...");
                        serverStarted = true;
                    }
                    new Handler(listener.accept()).start();
                }
            } finally {
                listener.close();
                System.out.println("INFO: KnightChat server stopped");
            }
        } catch (IOException e) {
            System.out.println("FATAL: Server startup failed. Cannot listen on port " + PORT);
            e.printStackTrace();
        }

    }

    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            System.out.println("INFO: User " + name + " has joined. Welcome!");
                            names.add(name);
                            for (PrintWriter writer : writers) {
                                writer.println("INFO " + name + " has joined. Welcome!");
                            }

                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }

                    if (input.startsWith("/")) {
                        if (input.substring(1).equals("users")) {
                            Iterator iterator = names.iterator();
                            String outString = "INFO " + "Users in this server: ";
                            while(iterator.hasNext()) {
                                outString += iterator.next() + ", ";
                            }
                            outString = outString.substring(0, outString.length() - 2);
                            out.println(outString);
                        }
                    } else {
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                        System.out.println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println("INFO: User " + name + " is logging out.");
                    for (PrintWriter writer : writers) {
                        writer.println("INFO " + name + " disconnected");
                    }
                    names.remove(name);
                } else System.out.println("WARN: User w/o name is logging out");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}