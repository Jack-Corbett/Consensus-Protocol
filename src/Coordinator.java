import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * The coordinator in a consensus vote
 */
public class Coordinator {

    private int expectedParticipants;
    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>(expectedParticipants));
    private ArrayList<String> failedParticipants;
    private ArrayList<String> outcomes;
    private ArrayList<String> options;

    /**
     * Instantiate a coordinator
     * @param port The port this coordinator should listen on for the participants to join
     * @param expectedParticipants The number of participants the coordinator is expecting to join
     * @param options The voting options to be given to the participants to decide upon
     */
    private Coordinator(int port, int expectedParticipants, ArrayList<String> options) {
        Tokeniser tokeniser = new Tokeniser();
        outcomes = new ArrayList<>();
        failedParticipants = new ArrayList<>();
        this.expectedParticipants = expectedParticipants;
        this.options = options;

        try {
            System.out.println("Waiting for " + expectedParticipants + " participant(s) to join");
            ServerSocket listener = new ServerSocket(port);

            // Accept participants until the expected number is reached
            while (participants.size() < expectedParticipants) {
                Socket participantSocket = listener.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(participantSocket.getOutputStream()));

                // Read the join token
                Token token = tokeniser.getToken(in.readLine());

                if (token instanceof JoinToken) {
                    JoinToken joinToken = ((JoinToken) token);
                    register(joinToken.port, out);

                    // Start a new thread to listen for the outcome from this participant
                    System.out.println("Listening for outcome from participant: " + joinToken.port);
                    new Thread (new CoordinatorListener(this, joinToken.port, in, tokeniser)).start();
                } else {
                    System.err.println("Participant failed to join");
                }
            }
            System.out.println("All participants have joined");

            // When all have joined send the lists of participants and vote options to the participants
            sendParticipants();
            sendVoteOptions();
        } catch (IOException e) {
            System.err.println("Coordinator server socket closed");
        }
    }

    /**
     * Send each participant the port numbers of the other participants so they can connect to each other directly
     */
    private synchronized void sendParticipants() {
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
        }
    }

    /**
     * Send each participant the vote options
     */
    private synchronized void sendVoteOptions() {
        for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
            PrintWriter out = participant.getValue();

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
     * Register an outcome with the coordinator
     * @param outcome The vote the participant decided on based on all votes
     * @param contributors A list of participants who's votes were considered in deciding the outcome
     */
    synchronized void registerOutcome(String outcome, ArrayList<String> contributors) {
        outcomes.add(outcome);
        System.out.println("Outcome received: " + outcome + " based on votes from: " + contributors);
        if (outcomes.size() == participants.size()) {
            printOutcome();
        }
    }

    /**
     * Output the outcome or restart voting if it was a fail or not all participants returned the same outcome
     */
    private synchronized void printOutcome() {
        // Check all participants agree
        if (outcomes.stream().distinct().limit(2).count() <= 1) {
            // If they agree it was a fail restart the vote
            if (outcomes.get(0).equals("FAIL")) {
                restartVote();
            } else {
                System.out.println("FINAL OUTCOME: " + outcomes.get(0));
                System.exit(0);
            }
        } else {
            System.err.println("Outcomes received did not all match: " + outcomes);
        }
    }

    /**
     * Restart the vote in the event of a fail outcome from all participants
     */
    private synchronized void restartVote() {
        // Discard the current outcomes
        outcomes.clear();
        // Remove a random option
        options.remove(new Random().nextInt(options.size()));
        System.out.println("Triggering voting restart");

        StringBuilder failures = new StringBuilder();
        for (String participant : failedParticipants) {
            failures.append(participant).append(" ");
        }

        // Send the restart message to all participants
        for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
            participant.getValue().println("RESTART " + failures);
        }
        failedParticipants.clear();
        // Resend the vote options
        sendVoteOptions();
    }

    /**
     * Register a participant failure to remove them from the participants map so we don't wait for an outcome from them
     * @param name The port of the participant that has died
     */
    synchronized void registerFailure(String name) {
        // Remove the failed participant so we don't wait for it to send an outcome
        participants.remove(name);
        failedParticipants.add(name);
        // Check if you can output the final outcome in case a participant failed after all of the others had reported back
        if (outcomes.size() == participants.size() && !outcomes.isEmpty()) printOutcome();
    }

    /**
     * Register the participant by adding them to the participants map
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

    /**
     * Start a coordinator
     * @param args Coordinator port, Expected number of participants, Vote options
     */
    public static void main(String[] args) {
        if (args.length > 2) {
            ArrayList<String> options = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
            new Coordinator(Integer.parseInt(args[0]), Integer.parseInt(args[1]), options);
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
