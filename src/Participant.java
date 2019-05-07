import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Participant {

    private int port;
    private int coordinatorPort;
    private int timeout;
    private int failureCondition;

    private Socket coordSocket;
    private BufferedReader coordIn;
    private PrintWriter coordOut;

    private Participant(int coordinatorPort, int port, int timeout, int failureCondition) {
        this.coordinatorPort = coordinatorPort;
        this.port = port;
        this.timeout = timeout;
        this.failureCondition = failureCondition;
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
        coordOut.println("JOIN " + port);

        try {
            // Get other participant details
            System.out.println(coordIn.readLine());

            // Get Vote options
            System.out.println(coordIn.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The Token Prototype.
     */
    abstract class Token {
        String message;
    }

    class VoteToken extends Token {
        String participant;
        String vote;

        VoteToken(String message, String participant, String vote) {
            this.message = message;
            this.participant = participant;
            this.vote = vote;
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
