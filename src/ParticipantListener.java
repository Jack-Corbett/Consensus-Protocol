import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handle incoming votes from another participant
 */
class ParticipantListener implements Runnable {
    private Participant participant;
    private String name;
    private BufferedReader in;
    private Tokeniser tokeniser;

    ParticipantListener(Participant participant, String name, BufferedReader in, Tokeniser tokeniser) {
        this.participant = participant;
        this.name = name;
        this.in = in;
        this.tokeniser = tokeniser;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                // Read the vote token and register it with the participant
                Token token = tokeniser.getToken(message);
                if (token instanceof VoteToken) {
                    VoteToken voteToken = ((VoteToken) token);
                    participant.registerVote(voteToken.votes);
                }
            }
        } catch (IOException e) {
            // This means a participant failed so we need to do another round of voting
            System.err.println("Connection to participant: " + name + "has been lost");
            participant.registerFailure(name);
        }
    }
}
