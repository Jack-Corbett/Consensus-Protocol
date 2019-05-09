import java.io.BufferedReader;
import java.io.IOException;

/**
 * This is used to listen for the outcome from the participants
 */
class CoordinatorListener implements Runnable {
    private Coordinator coordinator;
    private BufferedReader in;
    private Tokeniser tokeniser;

    CoordinatorListener(Coordinator coordinator ,BufferedReader in, Tokeniser tokeniser) {
        this.coordinator = coordinator;
        this.in = in;
        this.tokeniser = tokeniser;
    }

    @Override
    public void run() {
        try {
            // Get outcome from participants
            Token token = tokeniser.getToken(in.readLine());
            if (token instanceof OutcomeToken) {
                OutcomeToken outcomeToken = ((OutcomeToken) token);
                coordinator.registerOutcome(outcomeToken.outcome);
            }
        } catch (IOException e) {
            System.err.println("Failed to wait for outcome from participant");
        }
    }
}