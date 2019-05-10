import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {

    private int port;
    private int timeout;
    private String vote;

    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> votes = Collections.synchronizedMap(new HashMap<>());

    // Coordinator details
    private BufferedReader coordIn;
    private PrintWriter coordOut;

    private Tokeniser tokeniser;

    private Participant(int coordinatorPort, int port, int timeout, int failureCondition) {
        tokeniser = new Tokeniser();
        this.port = port;
        this.timeout = timeout;

        try {
            Socket coordSocket = new Socket("localhost", coordinatorPort);
            coordIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
            coordOut = new PrintWriter(new OutputStreamWriter(coordSocket.getOutputStream()));

            // Listen for incoming connections from the other participants
            new Thread(() -> {
                try {
                    System.out.println("Listening for other participants");
                    ServerSocket listener = new ServerSocket(port);
                    while (true) {
                        Socket participantSocket = listener.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));
                        new Thread(new ParticipantListener(this, in, tokeniser)).start();
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

    private synchronized void join() {
        System.out.println("Joining");
        // Send join message to the coordinator
        coordOut.println("JOIN " + port);
        coordOut.flush();

        Token token;
        try {
            // Details token identifying the other participants
            token = tokeniser.getToken(coordIn.readLine());
            if (token instanceof DetailsToken) {
                System.out.println("Connecting to other participants");

                // For each participant, set up a socket to connect to them
                for (String participant : ((DetailsToken) token).participants) {
                    Socket participantSocket = new Socket("localhost", Integer.parseInt(participant));
                    // participantSocket.setSoTimeout(timeout);

                    PrintWriter participantOut = new PrintWriter(participantSocket.getOutputStream(), true);

                    System.out.println("Adding participant: " + participant);
                    // Add the name and output channel to the participants hash map
                    participants.put(participant, participantOut);
                }
            } else {
                System.err.println("Failed to receive other participants details");
            }

            // Get vote options and decide randomly which to vote for
            token = tokeniser.getToken(coordIn.readLine());
            if (token instanceof VoteOptionsToken) {
                System.out.println("Received vote options");

                ArrayList<String> voteOptions = ((VoteOptionsToken) token).voteOptions;
                vote = voteOptions.get(new Random().nextInt(voteOptions.size()));
                System.out.println("Participant has decided to vote for: " + vote);
            } else {
                System.err.println("Failed to receive vote options");
            }

            sendVotes();
        } catch (IOException e) {
            System.err.println("Failed to setup connection");
            e.printStackTrace();
        }
    }

    private synchronized void sendVotes() {
        System.out.println("Sending vote to other participants");
        for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
            String message = "VOTE " + port +  " " + vote;
            participant.getValue().println(message);
        }
    }

    synchronized void registerVote(HashMap<String, String> votes) {
        System.out.println("Registering vote");
        this.votes.putAll(votes);
        if (this.votes.size() == participants.size()) {
            sendOutcome();
        }
    }

    private synchronized void sendOutcome() {
        // Add this participants vote
        votes.put(Integer.toString(port), vote);

        // Majority vote the votes this participant has received
        String decision = majorityVote(votes.values());
        System.out.println("Vote decision: " + decision);

        // Finally send the outcome to the coordinator
        coordOut.println("OUTCOME " + decision);
        coordOut.flush();
    }

    private String majorityVote(Collection<String> votes) {
        String element = null;
        String[] values = votes.toArray(new String[0]);
        int length = values.length;
        int counter = 0;
        int index = 0;

        while (index < length) {
            if (counter == 0) {
                element = values[index];
                counter++;
            } else if (element.equals(values[index])) {
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
        while (++index < length) {
            if (element.equals(values[index])) {
                counter++;
            }
        }

        if (counter > length / 2)
            return element;

        return null;
    }

    public static void main(String[] args) {
        if (args.length == 4) {
            new Participant(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
