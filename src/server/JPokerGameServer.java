package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JPokerGameServer implements AutoCloseable {
    private static final String CONNECTION_FACTORY_JNDI = "jms/JPoker24ConnectionFactory";
    private static final String GAME_QUEUE_JNDI    = "jms/JPoker24GameQueue";
    private static final String GAME_TOPIC_JNDI    = "jms/JPoker24GameTopic";

    private static final String JOIN_PREFIX        = "JOIN:";
    private static final String ANSWER_PREFIX      = "ANSWER:";
    private static final String GAME_START_PREFIX  = "GAME_START:";
    private static final String GAME_WINNER_PREFIX = "GAME_WINNER:";
    private static final String GAME_NO_WINNER     = "GAME_NO_WINNER";

    private static final int LOBBY_TIMEOUT_SECONDS      = 10;
    private static final int MIN_PLAYERS_FOR_TIMED_START = 2;
    private static final int MAX_LOBBY_SIZE              = 4;
    private static final int NUM_CARDS                   = 4;
    private static final int MAX_CARD_VALUE              = 13;

    private final String host;
    private final Set<String> lobbyPlayers = new LinkedHashSet<>();
    // True only while a lobby is forming; resets the moment a game session launches.
    private boolean lobbyActive;
    private long firstJoinTime = -1;

    // username -> their active GameSession.
    private final Map<String, GameSession> activeSessions = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingStart;

    private DatabaseService db;
    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue gameQueue;
    private Topic gameTopic;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer topicProducer;

    public JPokerGameServer(String host) throws NamingException, JMSException {
        this.host = host;
        try {
            db = new DatabaseService();
        } catch (Exception e) {
            System.err.println("Warning: could not connect to database. Stats will not be recorded: " + e.getMessage());
        }
        createJndiContext();
        lookupConnectionFactory();
        lookupQueue();
        lookupTopic();
        createConnection();
        createSession();
        createConsumer();
        createTopicProducer();
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";

        try (JPokerGameServer gameServer = new JPokerGameServer(host)) {
            System.out.println("JPoker game server listening for lobby joins...");
            gameServer.run();
        } catch (Exception e) {
            System.err.println("Failed to start JPoker game server: " + e.getMessage());
        }
    }

    public void run() throws JMSException {
        while (true) {
            Message message = consumer.receive();
            if (message instanceof TextMessage) {
                String body = ((TextMessage) message).getText();
                handleMessage(body);
            }
        }
    }

    private synchronized void handleMessage(String body) {
        if (body == null) {
            return;
        }

        if (body.startsWith(JOIN_PREFIX)) {
            handleJoinMessage(body);
        } else if (body.startsWith(ANSWER_PREFIX)) {
            handleAnswerMessage(body);
        }
    }

    private void handleJoinMessage(String body) {
        String username = body.substring(JOIN_PREFIX.length()).trim();
        if (username.isEmpty()) {
            return;
        }

        if (lobbyActive) {
            System.out.println("Ignoring join from " + username + " because a lobby is already forming.");
            return;
        }

        boolean joined = lobbyPlayers.add(username);
        if (joined) {
            System.out.println(username + " joined lobby (" + lobbyPlayers.size() + "/" + MAX_LOBBY_SIZE + ")");

            if (lobbyPlayers.size() == 1) {
                firstJoinTime = System.currentTimeMillis();
                pendingStart = scheduler.schedule(this::timedStartCallback, LOBBY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                System.out.println("Lobby timer started (" + LOBBY_TIMEOUT_SECONDS + "s).");
            }
        }

        tryStartGame();
    }

    private void handleAnswerMessage(String body) {
        // format: ANSWER:username|expression
        String payload = body.substring(ANSWER_PREFIX.length());
        int sep = payload.indexOf('|');
        if (sep < 0) {
            return;
        }

        String username   = payload.substring(0, sep).trim();
        String expression = payload.substring(sep + 1).trim();

        GameSession session = activeSessions.get(username);
        if (session == null) {
            System.out.println("Ignoring answer from " + username + " (not in any active game).");
            return;
        }

        if (session.submissions.containsKey(username)) {
            System.out.println("Ignoring duplicate answer from " + username + ".");
            return;
        }

        session.submissions.put(username, expression);
        System.out.println("Answer from " + username
                + " (" + session.submissions.size() + "/" + session.players.size() + "): " + expression);

        if (session.submissions.size() >= session.players.size()) {
            resolveGame(session);
        }
    }

    private void resolveGame(GameSession session) {
        String winner = null;
        String winningExpression = null;

        for (Map.Entry<String, String> entry : session.submissions.entrySet()) {
            if (ExpressionValidator.validate(entry.getValue(), session.cards)) {
                winner = entry.getKey();
                winningExpression = entry.getValue();
                break;
            }
        }

        float elapsedSeconds = (System.currentTimeMillis() - session.startTime) / 1000.0f;

        if (winner != null) {
            System.out.println("Winner: " + winner + " | expression: " + winningExpression
                    + " | time: " + elapsedSeconds + "s");
            publishToTopic(GAME_WINNER_PREFIX + winner + "|" + winningExpression);
        } else {
            System.out.println("No winner this round.");
            publishToTopic(GAME_NO_WINNER);
        }

        logGameResult(session.players, winner, elapsedSeconds);
        closeSession(session);
    }

    private void timedStartCallback() {
        synchronized (this) {
            System.out.println("Lobby timer fired.");
            tryStartGame();
        }
    }

    private void tryStartGame() {
        if (lobbyActive) {
            return;
        }

        int size = lobbyPlayers.size();
        boolean fullLobby = size >= MAX_LOBBY_SIZE;
        boolean timedOut  = size >= MIN_PLAYERS_FOR_TIMED_START
                && firstJoinTime >= 0
                && (System.currentTimeMillis() - firstJoinTime) >= LOBBY_TIMEOUT_SECONDS * 1000L;

        if (!fullLobby && !timedOut) {
            return;
        }

        int[] cards = dealCards();
        Set<String> players = new LinkedHashSet<>(lobbyPlayers);
        GameSession session = new GameSession(players, cards);
        for (String player : players) {
            activeSessions.put(player, session);
        }

        String reason = fullLobby ? "full lobby" : "timer elapsed";
        System.out.println("Starting game (" + reason + ") with players: " + players
                + " | cards: " + Arrays.toString(cards));

        String playerList = String.join(",", players);
        String cardList   = joinInts(cards);
        publishToTopic(GAME_START_PREFIX + playerList + "|" + cardList);

        // Reset lobby immediately so a new one can open.
        lobbyActive = false;
        lobbyPlayers.clear();
        firstJoinTime = -1;

        if (pendingStart != null && !pendingStart.isDone()) {
            pendingStart.cancel(false);
            pendingStart = null;
        }
    }

    private void logGameResult(Set<String> players, String winner, float elapsedSeconds) {
        if (db == null) return;
        try {
            db.recordGameResult(players, winner, elapsedSeconds);
            System.out.println("Leaderboard updated.");
        } catch (Exception e) {
            System.err.println("Failed to update leaderboard: " + e.getMessage());
        }
    }

    private void closeSession(GameSession session) {
        for (String player : session.players) {
            activeSessions.remove(player);
        }
        System.out.println("Session closed. " + activeSessions.size() + " active player(s) remain in other sessions.");
    }

    private int[] dealCards() {
        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= MAX_CARD_VALUE; i++) {
            values.add(i);
        }
        Collections.shuffle(values);
        int[] cards = new int[NUM_CARDS];
        for (int i = 0; i < NUM_CARDS; i++) {
            cards[i] = values.get(i);
        }
        return cards;
    }

    private static String joinInts(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private void publishToTopic(String text) {
        try {
            TextMessage msg = session.createTextMessage(text);
            topicProducer.send(msg);
            System.out.println("Published: " + text);
        } catch (JMSException e) {
            System.err.println("Failed to publish to topic: " + e.getMessage());
        }
    }

    private void createJndiContext() throws NamingException {
        System.setProperty("org.omg.CORBA.ORBInitialHost", host);
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        jndiContext = new InitialContext();
    }

    private void lookupConnectionFactory() throws NamingException {
        connectionFactory = (ConnectionFactory) jndiContext.lookup(CONNECTION_FACTORY_JNDI);
    }

    private void lookupQueue() throws NamingException {
        gameQueue = (Queue) jndiContext.lookup(GAME_QUEUE_JNDI);
    }

    private void lookupTopic() throws NamingException {
        gameTopic = (Topic) jndiContext.lookup(GAME_TOPIC_JNDI);
    }

    private void createConnection() throws JMSException {
        connection = connectionFactory.createConnection();
        connection.start();
    }

    private void createSession() throws JMSException {
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    private void createConsumer() throws JMSException {
        consumer = session.createConsumer(gameQueue);
    }

    private void createTopicProducer() throws JMSException {
        topicProducer = session.createProducer(gameTopic);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();

        if (topicProducer != null) {
            try { topicProducer.close(); } catch (JMSException ignored) {}
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException ignored) {
                // no-op
            }
        }

        if (session != null) {
            try {
                session.close();
            } catch (JMSException ignored) {
                // no-op
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ignored) {
                // no-op
            }
        }
    }

    private static final class GameSession {
        final Set<String> players;
        final int[] cards;
        final Map<String, String> submissions = new LinkedHashMap<>();
        final long startTime = System.currentTimeMillis();

        GameSession(Set<String> players, int[] cards) {
            this.players = Collections.unmodifiableSet(new LinkedHashSet<>(players));
            this.cards   = cards;
        }
    }
}

