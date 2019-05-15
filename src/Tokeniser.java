import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * A string tokeniser for messages passed between the coordinator and participants
 */
class Tokeniser {

    /**
     * Tokenise a message
     * @param message The message received
     * @return A token for the message
     */
    Token getToken(String message) {
        StringTokenizer st = new StringTokenizer(message);

        // Return null if the message is empty
        if (!(st.hasMoreTokens())) return null;

        String firstToken = st.nextToken();
        switch (firstToken) {
            case "JOIN":
                if (st.hasMoreTokens()) return new JoinToken(message, st.nextToken());
                else return null;
            case "OUTCOME":
                if (st.hasMoreTokens()) {
                    String outcome = st.nextToken();
                    ArrayList<String> contributors = new ArrayList<>();
                    while (st.hasMoreTokens()) contributors.add(st.nextToken());
                    return new OutcomeToken(message, outcome, contributors);
                }
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
                    System.err.println("No vote details included in vote message");
                }
            case "RESTART":
                // return new RestartToken();
                if (st.hasMoreTokens()) {
                    ArrayList<String> failedParticipants = new ArrayList<>();
                    while (st.hasMoreTokens()) failedParticipants.add(st.nextToken());
                    return new RestartToken(message, failedParticipants);
                } else {
                    return new RestartToken(message, new ArrayList<>());
                }
        }
        return null;
    }
}

/**
 * Token Prototype
 */
abstract class Token {
    String message;
}

/**
 * Syntax: JOIN <port>;
 */
class JoinToken extends Token {
    String port;

    JoinToken(String message, String port) {
        this.message = message;
        this.port = port;
    }
}

/**
 * Syntax: OUTCOME <outcome> |<contributing participants>|
 */
class OutcomeToken extends Token {
    String outcome;
    ArrayList<String> participants;

    OutcomeToken(String message, String outcome, ArrayList<String> participants) {
        this.message = message;
        this.outcome = outcome;
        this.participants = participants;
    }
}

/**
 * Syntax: DETAILS |<port>|
 */
class DetailsToken extends Token {
    ArrayList<String> participants;

    DetailsToken(String message, ArrayList<String> participants) {
        this.message = message;
        this.participants = participants;
    }
}

/**
 * Syntax: VOTE_OPTIONS |<port>|
 */
class VoteOptionsToken extends Token {
    ArrayList<String> voteOptions;

    VoteOptionsToken(String message, ArrayList<String> voteOptions) {
        this.message = message;
        this.voteOptions = voteOptions;
    }
}

/**
 * Syntax: VOTE |<port> <vote>|
 */
class VoteToken extends Token {
    HashMap<String, String> votes;

    VoteToken(String message, HashMap<String, String> votes) {
        this.message = message;
        this.votes = votes;
    }
}

/**
 * Syntax: RESTART |<port>|
 */
class RestartToken extends Token {
    ArrayList<String> failures;

    RestartToken(String message, ArrayList<String> failures) {
        this.message = message;
        this.failures = failures;
    }
}

