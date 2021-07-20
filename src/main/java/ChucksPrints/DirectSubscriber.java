

/*
A version of the subscriber in direct subscriber format.
Based on Solace sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp/blob/dbbd13a7af12da84d1215af8b1ce9c8b9d30e588/src/main/java/com/solace/samples/jcsmp/patterns/DirectSubscriber.java

Initially used for ease of use in testing and development.
May change subscriber strategy depending on requirements.
 */

package ChucksPrints;

import com.google.gson.GsonBuilder;
import com.solacesystems.jcsmp.*;
import com.google.gson.Gson;
import java.io.IOException;

public class DirectSubscriber {

    private static final String SUB_NAME = DirectSubscriber.class.getSimpleName();
    private static final String TOPIC_PREFIX = "chuck/prints/";
    private static final String API = "JCSMP"; //This is the solace native java api which connects on SMP port to broker

    // Create some variables in main memory(shared across threads)
    private static volatile int msgRecvCnt = 0;                 // keep track of number of messages read
    private static volatile boolean hasDetectedDiscard = false; // Set if we have missed/thrown away any messages
    private static volatile boolean isShutdown = false;         // To signal run loop that we are done

    public void run(String host, String vpn_name, String username, String password) throws JCSMPException, IOException, InterruptedException {
        System.out.println("Initializing connection to broker at " + host );


        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn_name);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        if (password != null)
            properties.setProperty(JCSMPProperties.PASSWORD, password);
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // Hard code that we would like to re-establish connection to subscribed topics

        // Setup some properties to support the reconnect mandate
        JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
        channelProps.setReconnectRetries(20);     // Will try to re-establish connection 20 times
        channelProps.setConnectRetriesPerHost(5); // We will give up on a particular host after 5 tries (good for fail-over?)
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);

        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs sessionEventArgs) {
                System.out.printf("!!! Handling session event: %s.%n", sessionEventArgs);
            }
        });
        session.connect();   // connect to the broker - we are not an active subscriber yet

        final XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage bytesXMLMessage) {
                msgRecvCnt++;   // increment our total message counter

                if (bytesXMLMessage.getDiscardIndication()) {
                    hasDetectedDiscard = true;  // Set a flag saying the producer is creating message at a rate sub cannot handle
                    // If this flag gets set we will want to consider multi-threading (making more instances) of the sub
                }

                if (bytesXMLMessage instanceof TextMessage) {
                    // I am doing this to facilitate incoming JSON payloads instead of XML payloads
                    System.out.println("Text data from recieved message: " + ((TextMessage) bytesXMLMessage).getText());
                } else {
                    System.out.printf("Message Dump:%n%s%n", bytesXMLMessage.dump());
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("!!! MessageListener's exception: %s.%n", e);
                if (e instanceof JCSMPTransportException) {
                    isShutdown = true;    // Tell application to stop because we cannot connect and have passed our re-connect limit
                    // If I wanted fail over I could initiate a new connection at this point to sister broker
                }

            }
        });

        // Lets start a subscription to a topic on our newly created connection
        //session.addSubscription(JCSMPFactory.onlyInstance().createTopic(TOPIC_PREFIX + "*/progress/>"));  // using wild cards to get progress messages from all
                                                                                                                //  printers posting progress data
        session.addSubscription(JCSMPFactory.onlyInstance().createTopic("octoprint/event/Connected"));  // using wild cards to get progress messages from all


        consumer.start();
        System.out.println(API + " " + SUB_NAME + " connected, and running.");

        // endless loop runs till we say stop of we get an I/O interrupt from system (I will remove I/O interrupt option)
        while (System.in.available() == 0 && !isShutdown) {
            Thread.sleep(60000);
            System.out.printf("%s %s Recieved msgs/min: %,d%n", API, SUB_NAME, msgRecvCnt);
            msgRecvCnt = 0;

            if (hasDetectedDiscard) {
                System.out.println("*** Egress discard detected ***: "
                                + SUB_NAME + " unable to keep up with full message rate");
                hasDetectedDiscard = false;
            }
        }
        isShutdown = true;
        session.closeSession();
        System.out.println("Main thread quitting.");
    }
}
