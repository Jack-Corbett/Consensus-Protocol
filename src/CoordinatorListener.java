import java.io.BufferedReader;
import java.io.IOException;

/**
 * A listener thread to wait for the outcome from a participant
 */
class CoordinatorListener implements Runnable {
    private Coordinator coordinator;
    private String name;
    private BufferedReader in;
    private Tokeniser tokeniser;

    /**
     * Instantiates a coordinator listener
     * @param coordinator A reference to the coordinator this listener belongs to
     * @param name The identifier of the socket we are listening for
     * @param in A buffered reader for the socket
     * @param tokeniser A reference to the tokeniser object for parsing the received messages
     */
    CoordinatorListener(Coordinator coordinator, String name, BufferedReader in, Tokeniser tokeniser) {
        this.coordinator = coordinator;
        this.name = name;
        this.in = in;
        this.tokeniser = tokeniser;
    }

    /**
     * Wait for the participant outcome and pass it on to the coordinator
     */
    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                // Get outcome from participants
                Token token = tokeniser.getToken(message);
                if (token instanceof OutcomeToken) {
                    OutcomeToken outcomeToken = ((OutcomeToken) token);
                    coordinator.registerOutcome(outcomeToken.outcome, outcomeToken.participants);
                }
            }
            throw new IOException();
        } catch (IOException e) {
            // Register the failure with the coordinator to remove the participant from the participants map
            System.out.println("Connection to participant: " + name + " has been lost");
            coordinator.registerFailure(name);
        }
    }
}