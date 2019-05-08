import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Participant {

    private int port;
    private int coordinatorPort;
    private int timeout;
    private int failureCondition;
    private HashMap<String, String> votes;

    private Map<String, Socket> participants = Collections.synchronizedMap(new HashMap<>());

    private Socket coordSocket;
    private BufferedReader coordIn;
    private PrintWriter coordOut;

    private Participant(int coordinatorPort, int port, int timeout, int failureCondition) {
        this.coordinatorPort = coordinatorPort;
        this.port = port;
        this.timeout = timeout;
        this.failureCondition = failureCondition;
        votes = new HashMap<>();
        run();
    }

    private void run() {
        try {
            coordSocket = new Socket("localhost", coordinatorPort);
            coordIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
            coordOut = new PrintWriter(coordSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Could not establish connection to the coordinator");
        }

        connect();

        // Decide how to vote

        // Send and receive votes

        // Majority vote them

        // Finally send the outcome to the coordinator
    }

    private void connect() {
        // Send join message to the coordinator
        coordOut.println("JOIN " + port);

        try {
            // Get other participant details
            Token token;
            // Details token identifying the other participants
            token = getToken(coordIn.readLine());
            if (token != null) {
                for (String participant : ((DetailsToken) token).participants) {

                    // Add the pairing to the participant hash map
                    participants.put(participant, );
                }
            }


            // Get Vote options
            System.out.println(coordIn.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Token getToken(String message) {
        StringTokenizer st = new StringTokenizer(message);

        // Return null if the message is empty
        if (!(st.hasMoreTokens())) return null;

        String firstToken = st.nextToken();
        switch (firstToken) {
            case "DETAILS":
                if (st.hasMoreTokens()) {
                    ArrayList<String> participants = new ArrayList<>();
                    while (st.hasMoreTokens()) participants.add(st.nextToken());
                    return new DetailsToken(message, participants);
                } else {
                    return new DetailsToken(message, new ArrayList<>());
                }
            case "VOTE_OPTIONS":
                if (st.hasMoreTokens()) {
                    ArrayList<String> voteOptions = new ArrayList<>();
                    while (st.hasMoreTokens()) voteOptions.add(st.nextToken());
                    return new VoteOptionsToken(message, voteOptions);
                } else {
                    System.err.println("No vote options provided by coordinator");
                    return null;
                }
            case "VOTE":
                if (st.hasMoreTokens()) {
                    HashMap<String, String> votesReceived = new HashMap<>();
                    while (st.hasMoreTokens()) votesReceived.put(st.nextToken(), st.nextToken());
                    return new VoteToken(message, votesReceived);
                } else {
                    System.err.println("No votes received from other participants. Either all participants have failed " +
                            "or the connection has been lost");
                    return null;
                }
        }
        return null;
    }

    /**
     * The Token Prototype.
     */
    abstract class Token {
        String message;
    }

    class DetailsToken extends Token {
        ArrayList<String> participants;

        DetailsToken(String message, ArrayList<String> participants) {
            this.message = message;
            this.participants = participants;
        }
    }

    class VoteOptionsToken extends Token {
        ArrayList<String> voteOptions;

        VoteOptionsToken(String message, ArrayList<String> voteOptions) {
            this.message = message;
            this.voteOptions = voteOptions;
        }
    }

    class VoteToken extends Token {
        HashMap<String, String> votes;

        VoteToken(String message, HashMap<String, String> votes) {
            this.message = message;
            this.votes = votes;
        }
    }

    public static void main(String[] args) {
        if (args.length == 4) {
            new Participant(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
