import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Coordinator {

    private int port;
    private int maxParticipants;
    private int numOfParticipants = 0;
    private ArrayList<String> options;
    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>(maxParticipants));

    private Coordinator(int port, int maxParticipants, ArrayList<String> options) {
        this.port = port;
        this.maxParticipants = maxParticipants;
        this.options = options;

        try {
            ServerSocket listener = new ServerSocket(port);
            while (true) {
                Socket participant = listener.accept();
                new ParticipantThread(participant).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Each participant has it's own thread for connecting to the coordinator
     */
    private class ParticipantThread extends Thread {
        private Socket participant;
        private String name;
        private BufferedReader in;
        private PrintWriter out;

        ParticipantThread(Socket participant) throws IOException {
            this.participant = participant;
            in = new BufferedReader(new InputStreamReader(participant.getInputStream()));
            out = new PrintWriter(participant.getOutputStream(), true);
        }

        public void run() {
            try {
                Token token = null;
                token = getToken(in.readLine());

                // If the first request isn't to join, close the connection
                if (!(token instanceof JoinToken)) {
                    participant.close();
                    return;
                }
                // Check the registration request
                if (!(register(name = ((JoinToken) token).port, out))) {
                    participant.close();
                    return;
                }
                // Loop processing request
                token = getToken(in.readLine());
                while (true) {

                }
                // If the connection drops, unregister it

            } catch (IOException e) {
                System.err.println("I/O Exception - participant connection has been closed");
            }
        }

        private Token getToken(String message) {
            StringTokenizer st = new StringTokenizer(message);

            // Return null if the message is empty
            if (!(st.hasMoreTokens())) return null;

            String firstToken = st.nextToken();
            if (firstToken.equals("JOIN")) {
                    if (st.hasMoreTokens())
                        return new JoinToken(message, st.nextToken());
                    else
                        return null;
                }
            /*if (firstToken.equals("YELL")) {
                    String msg = "";
                    while (sTokenizer.hasMoreTokens())
                        msg += " " + sTokenizer.nextToken();
                    return new YellToken(message, msg);
                }
            if (firstToken.equals("TELL")) {
                    String name = sTokenizer.nextToken();
                    String msg = "";
                    while (sTokenizer.hasMoreTokens())
                        msg += " " + sTokenizer.nextToken();
                    return new TellToken(message, name, msg);
                }
            if (firstToken.equals("EXIT"))
                    return new ExitToken(message);*/
            return null; // Ignore request
        }

        /**
         * The Token Prototype.
         */
        abstract class Token {
            String message;
        }

        /**
         * Syntax: JOIN &lt;name&gt;
         */
        class JoinToken extends Token {
            String port;

            JoinToken(String message, String port) {
                this.message = message;
                this.port = port;
            }
        }
    }

    /**
     * Register the participant and send them the details and vote options
     */
    private boolean register(String name, PrintWriter out) {
        if (numOfParticipants >= maxParticipants) {
            return false;
        } if (participants.containsKey(name)) {
            System.err.println("Port is already in use");
            return false;
        }
        try {
            participants.put(name, out);
        } catch (NullPointerException e) {
            return false;
        }
        numOfParticipants ++;

        // Send the other participants to the newly registered participants
        StringBuilder participantList = new StringBuilder();
        for (Map.Entry<String, PrintWriter> entry : participants.entrySet()) {
            if (entry.getKey().equals(name)) break;
            participantList.append(entry.getKey()).append(" ");
        }
        out.println("DETAILS " + participantList);

        // Vote options
        StringBuilder optionsList = new StringBuilder();
        for (String option : options) {
            optionsList.append(option).append(" ");
        }
        out.println("OPTIONS " + optionsList);

        return true;
    }

    synchronized void broadcast() {

    }

    public static void main(String[] args) {
        if (args.length > 2) {
            ArrayList<String> options = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
            new Coordinator(Integer.parseInt(args[0]), Integer.parseInt(args[1]), options);
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
