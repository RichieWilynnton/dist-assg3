package client;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JPokerGame implements AutoCloseable {
    private static final String CONNECTION_FACTORY_JNDI = "jms/JPoker24ConnectionFactory";
    private static final String GAME_QUEUE_JNDI = "jms/JPoker24GameQueue";

    private final String host;

    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue gameQueue;
    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public JPokerGame(String host) throws NamingException, JMSException {
        this.host = host;
        createJndiContext();
        lookupConnectionFactory();
        lookupQueue();
        createConnection();
        createSession();
        createProducer();
    }

    public void requestNewGame(String username) throws JMSException {
        TextMessage message = session.createTextMessage();
        message.setText("JOIN:" + username);
        producer.send(message);
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

    private void createProducer() throws JMSException {
        producer = session.createProducer(gameQueue);
    }

    @Override
    public void close() {
        if (producer != null) {
            try {
                producer.close();
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
