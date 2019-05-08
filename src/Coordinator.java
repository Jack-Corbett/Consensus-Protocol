import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Coordinator {

    private int maxParticipants;
    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>(maxParticipants));

    private Coordinator(int port, int maxParticipants, ArrayList<String> options) {
        this.maxParticipants = maxParticipants;

        try {
            System.out.println("Waiting for " + maxParticipants + " participants to join");
            ServerSocket listener = new ServerSocket(port);
            // Accept participants until the maximum is reached
            while (participants.size() < maxParticipants - 1) {
                Socket participant = listener.accept();
                new Thread (new ParticipantThread(participant)).start();
            }

            // TODO wait here until they are all in

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("All participants have joined");

            // Once all of the participants have joined, send them all the details and vote options
            for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
                PrintWriter out = participant.getValue();

                // Other participants
                StringBuilder participantList = new StringBuilder();
                for (Map.Entry<String, PrintWriter> entry : participants.entrySet()) {
                    if (entry.getKey().equals(participant.getKey())) break;
                    participantList.append(entry.getKey()).append(" ");
                }
                System.out.println("Sending participant list to: " + participant.getKey());
                out.println("DETAILS " + participantList);

                // Vote options
                StringBuilder optionsList = new StringBuilder();
                for (String option : options) {
                    optionsList.append(option).append(" ");
                }
                System.out.println("Sending vote options to: " + participant.getKey());
                out.println("VOTE_OPTIONS " + optionsList);
            }
        } catch (IOException e) {
            System.err.println("Coordinator server socket closed");
        }
    }

    /**
     * Each participant has it's own thread for connecting to the coordinator
     */
    private class ParticipantThread implements Runnable {
        private Socket participant;
        private String name;
        private BufferedReader in;
        private PrintWriter out;

        ParticipantThread(Socket participant) throws IOException {
            System.out.println("A participant is joining");
            this.participant = participant;
            in = new BufferedReader(new InputStreamReader(participant.getInputStream()));
            out = new PrintWriter(participant.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                Token token;
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

                System.out.println("Waiting for outcome from participant: " + name);

                // Get outcome from participants
                // token = getToken(in.readLine());

                // Save the result to a concurrent data structure
                // System.out.println(token != null ? ((OutcomeToken) token).outcome : null);

            } catch (IOException e) {
                // If the connection drops, unregister it
                System.err.println("Participant " + name + " connection lost");
                participants.remove(name);
            }
        }

        private Token getToken(String message) {
            StringTokenizer st = new StringTokenizer(message);

            // Return null if the message is empty
            if (!(st.hasMoreTokens())) return null;

            String firstToken = st.nextToken();
            switch (firstToken) {
                case "JOIN":
                    if (st.hasMoreTokens()) return new JoinToken(message, st.nextToken());
                    else return null;
                case "OUTCOME":
                    if (st.hasMoreTokens()) {
                        String outcome = st.nextToken();
                        ArrayList<String> participants = new ArrayList<>();
                        while (st.hasMoreTokens()) participants.add(st.nextToken());
                        return new OutcomeToken(message, outcome, participants);
                    }
                    break;
            }
            return null;
        }

        /**
         * Token Prototype
         */
        abstract class Token {
            String message;
        }

        /**
         * Syntax: JOIN <port>;
         */
        class JoinToken extends Token {
            String port;

            JoinToken(String message, String port) {
                this.message = message;
                this.port = port;
            }
        }

        /**
         * Syntax: OUTCOME <outcome> |<contributing participants>|
         */
        class OutcomeToken extends Token {
            String outcome;
            ArrayList<String> participants;

            OutcomeToken(String message, String outcome, ArrayList<String> participants) {
                this.message = message;
                this.outcome = outcome;
                this.participants = participants;
            }
        }
    }

    /**
     * Register the participant and send them the details and vote options
     */
    private boolean register(String name, PrintWriter out) {
        if (participants.containsKey(name)) {
            System.err.println("Port is already in use");
            return false;
        }
        try {
            participants.put(name, out);
        } catch (NullPointerException e) {
            return false;
        }
        return true;
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
