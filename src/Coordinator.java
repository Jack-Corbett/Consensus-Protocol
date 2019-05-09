import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Coordinator {

    private int maxParticipants;
    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>(maxParticipants));
    private ArrayList<String> outcomes;

    private Coordinator(int port, int maxParticipants, ArrayList<String> options) {
        Tokeniser tokeniser = new Tokeniser();
        outcomes = new ArrayList<>();
        this.maxParticipants = maxParticipants;

        try {
            System.out.println("Waiting for " + maxParticipants + " participant(s) to join");
            ServerSocket listener = new ServerSocket(port);

            // Accept participants until the maximum is reached
            while (participants.size() < maxParticipants) {
                Socket participantSocket = listener.accept();

                // Read the join token
                BufferedReader in = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(participantSocket.getOutputStream()));

                Token token = tokeniser.getToken(in.readLine());

                if (token instanceof JoinToken) {
                    JoinToken joinToken = ((JoinToken) token);
                    register(joinToken.port, out);

                    System.out.println("Listening for outcome from participant: " + joinToken.port);
                    new Thread (new CoordinatorListener(this, in, tokeniser)).start();
                } else {
                    System.err.println("Participant failed to join");
                }
            }
            System.out.println("All participants have joined");

            sendVoteDetails(options);
        } catch (IOException e) {
            System.err.println("Coordinator server socket closed");
        }
    }

    synchronized void registerOutcome(String outcome) {
        outcomes.add(outcome);
        if (outcomes.size() == maxParticipants) {
            // TODO check they all match
            System.out.println("OUTCOME: " + outcomes.get(0));
        }
    }

    private synchronized void sendVoteDetails(ArrayList<String> options) {
        // Once all of the participants have joined, send them all the details and vote options
        for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
            PrintWriter out = participant.getValue();

            // Compile a list of the other participants
            StringBuilder participantList = new StringBuilder();
            for (Map.Entry<String, PrintWriter> entry : participants.entrySet()) {
                if (entry.getKey().equals(participant.getKey())) continue;
                participantList.append(entry.getKey()).append(" ");
            }

            // Send the participant list
            System.out.println("Sending participant list to: " + participant.getKey() + " - " + participantList);
            out.println("DETAILS " + participantList);
            out.flush();

            // Compile a list of vote options
            StringBuilder optionsList = new StringBuilder();
            for (String option : options) {
                optionsList.append(option).append(" ");
            }

            // Send the vote options
            System.out.println("Sending vote options to: " + participant.getKey() + " - " + optionsList);
            out.println("VOTE_OPTIONS " + optionsList);
            out.flush();
        }
    }

    /**
     * Register the participant by adding them to the hash map
     */
    private void register(String name, PrintWriter out) {
        if (participants.containsKey(name)) {
            System.err.println("Participant failed to join as it's port is already in use");
            return;
        }
        try {
            participants.put(name, out);
        } catch (NullPointerException e) {
            System.err.println("Failed to register new participant");
        }
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
