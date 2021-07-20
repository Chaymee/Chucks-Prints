package ChucksPrints;

import com.solacesystems.jcsmp.*;

import java.util.concurrent.CountDownLatch;

public class BasicSubscriber {

    private static final String SAMPLE_NAME = main.class.getSimpleName();
    private static final String TOPIC_PREFIX = "test1";

    public static void run() throws JCSMPException, InterruptedException{
        JCSMPSession session = null;
        XMLMessageConsumer cons = null;
        String host = "192.168.1.11";
        String username = "admin";
        String vpnname = "default";
        String password = "admin";
        System.out.println("Hello World");

        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpnname);
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();

        final CountDownLatch latch = new CountDownLatch(1);


        cons= session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if(msg instanceof TextMessage) {
                    System.out.printf("TextMessage recieved: '%s'%n",
                            ((TextMessage)msg).getText());
                } else {
                    System.out.println("Message recieved.");
                }
                System.out.printf("Message Dump:%n%s%n", msg.dump());
                latch.countDown();
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer recieved exception: %s%n", e);
                latch.countDown();

            }
        });


        Topic topic = JCSMPFactory.onlyInstance().createTopic(TOPIC_PREFIX);
        System.out.printf("Setting topic subscription '%s'...\n", topic.getName());
        session.addSubscription(topic);

        cons.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.printf("%nWoke from waiting%n");
        }

        cons.close();
        System.out.println("Exiting.");
        session.closeSession();
    }
}
