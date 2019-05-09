import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

class Tokeniser {

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
                    ArrayList<String> participants = new ArrayList<>();
                    while (st.hasMoreTokens()) participants.add(st.nextToken());
                    return new OutcomeToken(message, outcome, participants);
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
                    System.err.println("No votes received from other participants. Either all participants have failed " +
                            "or the connection has been lost");
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
 *
 */
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

