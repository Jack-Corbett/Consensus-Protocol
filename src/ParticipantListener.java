import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handle incoming votes from another participant
 */
class ParticipantListener implements Runnable {
    private Participant participant;
    private BufferedReader in;
    private Tokeniser tokeniser;

    ParticipantListener(Participant participant, BufferedReader in, Tokeniser tokeniser) {
        this.participant = participant;
        this.in = in;
        this.tokeniser = tokeniser;
    }

    @Override
    public void run() {
        try {
            // Read the vote token and register it with the participant
            Token token = tokeniser.getToken(in.readLine());
            if (token instanceof VoteToken) {
                VoteToken voteToken = ((VoteToken) token);
                participant.registerVote(voteToken.votes);
            }
        } catch (IOException e) {
            System.err.println("Reading a participants vote failed");
        }
    }
}
