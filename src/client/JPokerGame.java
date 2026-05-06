package client;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.SwingUtilities;

public class JPokerGame implements AutoCloseable {
    private static final String CONNECTION_FACTORY_JNDI = "jms/JPoker24ConnectionFactory";
    private static final String GAME_QUEUE_JNDI    = "jms/JPoker24GameQueue";
    private static final String GAME_TOPIC_JNDI    = "jms/JPoker24GameTopic";

    private static final String GAME_START_PREFIX  = "GAME_START:";
    private static final String GAME_WINNER_PREFIX = "GAME_WINNER:";
    private static final String GAME_NO_WINNER     = "GAME_NO_WINNER";

    private final String host;

    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue gameQueue;
    private Topic gameTopic;
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer topicConsumer;

    public JPokerGame(String host) throws NamingException, JMSException {
        this.host = host;
        createJndiContext();
        lookupConnectionFactory();
        lookupQueue();
        lookupTopic();
        createConnection();
        createSession();
        createProducer();
    }

    public void requestNewGame(String username) throws JMSException {
        TextMessage message = session.createTextMessage("JOIN:" + username);
        producer.send(message);
    }

    public void submitAnswer(String username, String expression) throws JMSException {
        TextMessage message = session.createTextMessage("ANSWER:" + username + "|" + expression);
        producer.send(message);
    }

    public void subscribeToGameEvents(String username, GameListener listener) throws JMSException {
        boolean[] inGame = {false};

        topicConsumer = session.createConsumer(gameTopic);
        topicConsumer.setMessageListener(message -> {
            if (!(message instanceof TextMessage)) return;
            try {
                String body = ((TextMessage) message).getText();
                if (body == null) return;

                if (body.startsWith(GAME_START_PREFIX)) {
                    // GAME_START:p1,p2,...|v1,v2,...
                    String payload = body.substring(GAME_START_PREFIX.length());
                    int sep = payload.indexOf('|');
                    if (sep < 0) return;
                    String[] players  = payload.substring(0, sep).split(",");
                    String[] cardStrs = payload.substring(sep + 1).split(",");
                    boolean mine = false;
                    for (String p : players) {
                        if (p.trim().equals(username)) { mine = true; break; }
                    }
                    if (!mine) return;
                    int[] cards = new int[cardStrs.length];
                    for (int i = 0; i < cardStrs.length; i++) {
                        cards[i] = Integer.parseInt(cardStrs[i].trim());
                    }
                    inGame[0] = true;
                    SwingUtilities.invokeLater(() -> listener.onGameStart(players, cards));

                } else if (body.startsWith(GAME_WINNER_PREFIX)) {
                    if (!inGame[0]) return;
                    // GAME_WINNER:username|expression
                    String payload = body.substring(GAME_WINNER_PREFIX.length());
                    int sep = payload.indexOf('|');
                    String winner     = sep >= 0 ? payload.substring(0, sep) : payload;
                    String expression = sep >= 0 ? payload.substring(sep + 1) : "";
                    inGame[0] = false;
                    SwingUtilities.invokeLater(() -> listener.onGameWinner(winner, expression));

                } else if (body.equals(GAME_NO_WINNER)) {
                    if (!inGame[0]) return;
                    inGame[0] = false;
                    SwingUtilities.invokeLater(listener::onNoWinner);
                }

            } catch (JMSException | NumberFormatException ignored) {}
        });
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

    private void createProducer() throws JMSException {
        producer = session.createProducer(gameQueue);
    }

    @Override
    public void close() {
        if (topicConsumer != null) {
            try { topicConsumer.close(); } catch (JMSException ignored) {}
        }
        if (producer != null) {
            try { producer.close(); } catch (JMSException ignored) {}
        }
        if (session != null) {
            try { session.close(); } catch (JMSException ignored) {}
        }
        if (connection != null) {
            try { connection.close(); } catch (JMSException ignored) {}
        }
    }
}
