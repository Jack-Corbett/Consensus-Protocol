import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * A listener thread to handle incoming votes from another participant
 */
class ParticipantListener implements Runnable {
    private Participant participant;
    private Socket socket;
    private Tokeniser tokeniser;

    /**
     * Instantiates a participant listener
     * @param participant A reference to the participant this listener belongs to
     * @param socket The socket connection to the other participant
     * @param tokeniser A reference to the tokeniser object for parsing the received messages
     */
    ParticipantListener(Participant participant, Socket socket, Tokeniser tokeniser) {
        this.participant = participant;
        this.socket = socket;
        this.tokeniser = tokeniser;
    }

    /**
     * Loops while the buffered reader reads an input and pass the vote on to the participant
     */
    @Override
    public void run() {
        String message;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while ((message = in.readLine()) != null) {
                // Read the vote token and register it with the participant
                Token token = tokeniser.getToken(message);
                if (token instanceof VoteToken) {
                    VoteToken voteToken = ((VoteToken) token);
                    participant.registerVote(voteToken.votes);
                }
            }
            throw new IOException();
        } catch (SocketTimeoutException e) {
            // If the connection times out kill the participant as an outcome must have been decided as no votes have been sent
            System.exit(0);
        } catch (IOException e) {
            // This means a participant failed so we need to do another round of voting
            System.err.println("Connection to a participant has been lost");
            participant.registerFailure();
        }
    }
}
