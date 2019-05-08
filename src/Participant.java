import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {

    private int port;
    private String vote;

    private Map<String, PrintWriter> participants = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> votes = Collections.synchronizedMap(new HashMap<>());

    private Participant(int coordinatorPort, int port, int timeout, int failureCondition) {
        this.port = port;

        try {
            Socket coordSocket = new Socket("localhost", coordinatorPort);
            BufferedReader coordIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
            PrintWriter coordOut = new PrintWriter(coordSocket.getOutputStream(), true);

            // Send join message to the coordinator
            coordOut.println("JOIN " + port);

            // Get other participant details
            Token token;
            // Details token identifying the other participants
            token = getToken(coordIn.readLine());
            if (token != null) {
                for (String participant : ((DetailsToken) token).participants) {
                    Socket participantSocket = new Socket("localhost", Integer.parseInt(participant));
                    participantSocket.setSoTimeout(timeout);
                    PrintWriter out = new PrintWriter(participantSocket.getOutputStream(), true);
                    // Add the name and output channel to the participants hash map
                    participants.put(Integer.toString(port), out);
                }
            }

            // Get vote options and decide randomly which to vote for
            token = getToken(coordIn.readLine());
            if (token != null) {
                ArrayList<String> voteOptions = ((VoteOptionsToken) token).voteOptions;
                vote = voteOptions.get(new Random().nextInt(voteOptions.size()));
                System.out.println("Participant has decided to vote for: " + vote);
            }

            // Create a thread to start listening for new connections when voting begins
            new Thread (new ParticipantListener()).start();

            // VOTING now starts
            // This thread sends its votes to others
            for (Map.Entry<String, PrintWriter> participant : participants.entrySet()) {
                String message = "VOTE " + participant.getKey() +  " " + vote;
                System.out.println("Sending: " + message);
                participant.getValue().println(message);
            }

            // TODO This is just to test
            Thread.sleep(5000);

            System.out.println(votes.toString());

            // Majority vote the votes this participant has received
            String decision = majorityVote(votes.values());

            // Finally send the outcome to the coordinator
            coordOut.println("OUTCOME " + decision);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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

    /**
     * Loop, spawning a new participant thread for every participant connection
     */
    private class ParticipantListener implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("Listening for votes from other participants");
                ServerSocket listener = new ServerSocket(port);
                while (true) {
                    Socket participant = listener.accept();
                    new Thread(new ParticipantThread(participant)).start();
                }
            } catch (IOException e) {
                System.err.println("Failed to start new thread for new participant connection");
            }
        }
    }

    /**
     * Handle incoming votes from another participant
     */
    private class ParticipantThread implements Runnable {
        private Socket participant;

        ParticipantThread(Socket participant) {
            this.participant = participant;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(participant.getInputStream()));
                Token token = getVoteToken(in.readLine());
                if (token != null) {
                    votes.putAll(((VoteToken) token).votes);
                }
            } catch (IOException e) {
                System.err.println("Reading a participants vote failed");
            }
        }

        private Token getVoteToken(String message) {
            StringTokenizer st = new StringTokenizer(message);

            // Return null if the message is empty
            if (!(st.hasMoreTokens())) return null;

            String firstToken = st.nextToken();
            if ("VOTE".equals(firstToken)) {
                if (st.hasMoreTokens()) {
                    HashMap<String, String> votesReceived = new HashMap<>();
                    while (st.hasMoreTokens()) votesReceived.put(st.nextToken(), st.nextToken());
                    return new VoteToken(message, votesReceived);
                } else {
                    System.err.println("No votes received from other participants. Either all participants have failed " +
                            "or the connection has been lost");
                }
            }
            return null;
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
