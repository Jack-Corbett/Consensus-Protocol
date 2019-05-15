import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A participant in the consensus vote
 */
public class Participant {

    private int port;
    private int failureCondition;
    private int failureCount;

    private AtomicBoolean outcomeSent = new AtomicBoolean(false);
    private Set<String> currentParticipants = Collections.synchronizedSet(new HashSet<>());
    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> votes = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> votesCache = Collections.synchronizedMap(new HashMap<>());

    // Coordinator details
    private BufferedReader coordIn;
    private PrintWriter coordOut;

    private Tokeniser tokeniser;

    /**
     * Instantiates a participant
     * @param coordinatorPort The port the coordinator is listening on
     * @param port The port this participant should listen on
     * @param timeout How long to wait after hearing no messages from other participants before closing
     * @param failureCondition 0 - no failure, 1 - after sending it's vote to some but not all other participants,
     *                         2 -  fails before deciding on the outcome
     */
    private Participant(int coordinatorPort, int port, int timeout, int failureCondition) {
        tokeniser = new Tokeniser();
        this.port = port;
        this.failureCondition = failureCondition;
        failureCount = 0;

        try {
            Socket coordSocket = new Socket("localhost", coordinatorPort);
            coordIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
            coordOut = new PrintWriter(new OutputStreamWriter(coordSocket.getOutputStream()));

            // Add this as it's own current participant
            currentParticipants.add(Integer.toString(port));

            // Listen for incoming connections from the other participants
            new Thread(() -> {
                try {
                    System.out.println("Listening for other participants");
                    ServerSocket listener = new ServerSocket(port);
                    while (true) {
                        Socket participantSocket = listener.accept();
                        participantSocket.setSoTimeout(timeout);
                        new Thread(new ParticipantListener(this, participantSocket, tokeniser)).start();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to start thread for new participant connection");
                }
            }).start();

            join();
        } catch (IOException e) {
            System.err.println("Failed to connect to the coordinator");
        }
    }

    /**
     * Connect to the coordinator
     */
    private synchronized void join() {
        System.out.println("Joining");
        // Send join message to the coordinator
        coordOut.println("JOIN " + port);
        coordOut.flush();

        getParticipantDetails();
        getVoteOptions();
    }

    /**
     * Receive the details of the other participants from the coordinator
     */
    private synchronized void getParticipantDetails() {
        Token token;
        try {
            // Details token identifying the other participants
            token = tokeniser.getToken(coordIn.readLine());
            if (token instanceof DetailsToken) {
                System.out.println("Connecting to other participants");

                // For each participant, set up a socket to connect to them
                for (String participant : ((DetailsToken) token).participants) {
                    Socket participantSocket = new Socket("localhost", Integer.parseInt(participant));

                    PrintWriter participantOut = new PrintWriter(participantSocket.getOutputStream(), true);

                    System.out.println("Adding participant: " + participant);
                    // Add the name to the starting participants list
                    currentParticipants.add(participant);
                    // Add the name and output channel to the participants hash map
                    participants.put(participant, participantOut);
                }
            } else {
                System.err.println("Failed to receive other participants details");
            }
        } catch (IOException e) {
            System.err.println("Failed to establish connection to another participant");
        }
    }

    /**
     * Receive the vote options from the coordinator
     */
    private synchronized void getVoteOptions() {
        Token token;
        try {
            // Get vote options and decide randomly which to vote for
            token = tokeniser.getToken(coordIn.readLine());
            if (token instanceof VoteOptionsToken) {
                System.out.println("Received vote options");

                ArrayList<String> voteOptions = ((VoteOptionsToken) token).voteOptions;
                String vote = voteOptions.get(new Random().nextInt(voteOptions.size()));
                System.out.println("Participant has decided to vote for: " + vote);

                // Add this participants vote
                votes.put(Integer.toString(port), vote);
            } else {
                System.err.println("Failed to receive vote options");
            }

            sendVotes();
        } catch (IOException e) {
            System.err.println();
        }
    }

    /**
     * Send votes to all other participants
     */
    private synchronized void sendVotes() {
        // If this is the only participant left there is no point in sending out votes so just send the outcome
        if (currentParticipants.size() == 1) {
            sendOutcome();
            System.exit(0);
        }
        System.out.println("Sending vote to other participants");
        int count = 0;

        StringBuilder voteList = new StringBuilder();
        for (Map.Entry<String, String> vote : votes.entrySet()) {
            voteList.append(vote.getKey()).append(" ").append(vote.getValue()).append(" ");
        }
        String message = "VOTE " + voteList;

        for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
            participant.getValue().println(message);
            count ++;
            if (failureCondition == 1 && count == 1) System.exit(0);
        }
        if (failureCondition == 2) System.exit(0);
    }

    /**
     * Record a vote received by a participant listener, send the outcome back to the coordinator if we have received
     * a vote from all of the participants
     * @param votes The votes received
     */
    synchronized void registerVote(HashMap<String, String> votes) {
        if (!outcomeSent.get()) {
            System.out.println("Registering vote");
            this.votes.putAll(votes);
            // If we have received a vote from all participants calculate the outcome
            if (this.votes.keySet().equals(currentParticipants)) {
                sendOutcome();
            }
        } else if (failureCount == 0) {
            System.out.println("Caching vote");
            votesCache.putAll(votes);
        }
    }

    /**
     * Register that a participant has failed
     */
    synchronized void registerFailure() {
        failureCount++;
        if (failureCount == currentParticipants.size() - 1) {
            sendOutcome();
        } else {
            sendVotes();
        }
    }

    /**
     * Send the outcome of the vote to the coordinator
     */
    private synchronized void sendOutcome() {
        outcomeSent.set(true);

        // Majority vote the votes this participant has received
        String decision = majorityVote(votes.values());

        StringBuilder contributors = new StringBuilder();
        for (String participant : votes.keySet()) {
            contributors.append(participant).append(" ");
        }

        if (decision != null) {
            System.out.println("Vote decision: " + decision);
            // Finally send the outcome to the coordinator
            coordOut.println("OUTCOME " + decision + " " + contributors);
            coordOut.flush();
        } else {
            System.out.println("Vote decision: FAIL");
            // Report the tie to the coordinator so it can resend the vote options
            coordOut.println("OUTCOME FAIL " + contributors);
            coordOut.flush();
            // Wait for the restart message from the coordinator
            new Thread(this::waitForRestart).start();
        }
    }

    /**
     * Run in a separate thread, this waits for the restart message from the coordinator as this is sent once all
     * participants have reported a fail (no majority)
     */
    private void waitForRestart() {
        Token token;
        try {
            // Restart token
            String message = coordIn.readLine();
            token = tokeniser.getToken(message);
            if (token instanceof RestartToken) {
                restartVote(((RestartToken) token).failures);
            } else {
                System.err.println("Restart message not received from coordinator");
            }
        } catch (IOException e) {
            System.err.println("Failed to read restart message");
        }
    }

    /**
     * Restarts the vote by clearing the current votes and receiving the new vote options
     */
    private synchronized void restartVote(ArrayList<String> failures) {
        System.out.println("Restarting Vote");
        outcomeSent.set(false);
        failureCount = 0;
        for (String participant : failures) currentParticipants.remove(participant);
        votes.clear();
        votes.putAll(votesCache);
        votesCache.clear();
        getVoteOptions();
    }

    /**
     * Majority vote the votes received to decide on a vote outcome
     * @param values A collection of all votes received
     * @return The majority element or null if there isn't a majority
     */
    private String majorityVote(Collection<String> values) {
        ArrayList<String> votes = new ArrayList<>(values);
        String element = null;
        int counter = 0;
        int index = 0;

        while (index < votes.size()) {
            if (counter == 0) {
                element = votes.get(index);
                counter++;
            } else if (element.equals(votes.get(index))) {
                counter++;
            } else {
                counter--;
            }
            index++;
        }

        // No majority element found
        if (counter == 0) {
            return null;
        }

        index = -1;
        counter = 0;
        while (++index < votes.size()) {
            if (element.equals(votes.get(index))) {
                counter++;
            }
        }

        if (counter > votes.size() / 2)
            return element;

        return null;
    }

    /**
     * Start a participant
     * @param args Coordinator port, Participant port, Timeout in milliseconds, Failure condition
     */
    public static void main(String[] args) {
        if (args.length == 4) {
            new Participant(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
