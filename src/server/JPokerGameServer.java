package server;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JPokerGameServer implements AutoCloseable {
    private static final String CONNECTION_FACTORY_JNDI = "jms/JPoker24ConnectionFactory";
    private static final String GAME_QUEUE_JNDI = "jms/JPoker24GameQueue";
    private static final String JOIN_PREFIX = "JOIN:";

    private final String host;
    private final Set<String> lobbyPlayers = new LinkedHashSet<>();

    private boolean gameInProgress;

    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue gameQueue;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public JPokerGameServer(String host) throws NamingException, JMSException {
        this.host = host;
        createJndiContext();
        lookupConnectionFactory();
        lookupQueue();
        createConnection();
        createSession();
        createConsumer();
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
        if (body == null || !body.startsWith(JOIN_PREFIX)) {
            return;
        }

        String username = body.substring(JOIN_PREFIX.length()).trim();
        if (username.isEmpty()) {
            return;
        }

        if (gameInProgress) {
            System.out.println("Ignoring join from " + username + " because a game is already in progress.");
            return;
        }

        boolean joined = lobbyPlayers.add(username);
        if (joined) {
            System.out.println(username + " joined lobby (" + lobbyPlayers.size() + "/4)");
        }

        if (lobbyPlayers.size() >= 4) {
            gameInProgress = true;
            System.out.println("Starting JPoker game with players: " + lobbyPlayers);
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

    @Override
    public void close() {
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
}
