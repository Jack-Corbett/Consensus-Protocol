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

        try {
            coordSocket = new Socket("localhost", coordinatorPort);
            coordIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
            coordOut = new PrintWriter(coordSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Could not establish connection to the coordinator");
        }

        connect();
    }

    private void connect() {
        join();

        // Get other participant details

        // Get Vote options

        // Message other participants
    }

    private void join() {
        coordOut.println("JOIN" + port);
    }

    /**
     * The Token Prototype.
     */
    abstract class Token {
        String message;
    }

    public static void main(String[] args) {
        if (args.length == 4) {
            new Participant(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            System.err.println("Not enough arguments provided");
        }
    }
}
