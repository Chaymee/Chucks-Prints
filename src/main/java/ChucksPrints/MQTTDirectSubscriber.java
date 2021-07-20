

/*
A version of the subscriber in direct subscriber format.
Based on Solace sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp/blob/dbbd13a7af12da84d1215af8b1ce9c8b9d30e588/src/main/java/com/solace/samples/jcsmp/patterns/DirectSubscriber.java

Initially used for ease of use in testing and development.
May change subscriber strategy depending on requirements.
 */

package ChucksPrints;

import com.google.gson.JsonParseException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MQTTDirectSubscriber implements Runnable{


    // Create some variables in main memory(shared across threads)
    private static volatile int msgRecvCnt = 0;                 // keep track of number of messages read
    private static volatile boolean hasDetectedDiscard = false; // Set if we have missed/thrown away any messages
    private static volatile boolean isShutdown = false;         // To signal run loop that we are done

    private String host;
    private String username;
    private String vpnname;
    private String password;
    private String topic;
    private File topicLog;
    private FileWriter logger;
    private ExecutorService executorService;

    public MQTTDirectSubscriber(String host, String user, String password, String topic) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.topic = topic;
    }

    public void run(){
        
        System.out.println("Initializing connection to broker at " + host );

        // Setup threads to handle certain events
        executorService = Executors.newFixedThreadPool(4);


        try {
            topicLog = new File("MQTTDirectSubscriberTopic.log");
            if(topicLog.createNewFile()) {

                System.out.println("Created new file to write topic log into at " + topicLog.getAbsolutePath());
            } else {
                // File already existed
                System.out.println("Writing to existing topic log file at " + topicLog.getAbsolutePath());
            }
            logger = new FileWriter(topicLog);
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient(host, "mqttSubscriberApp");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            // Connect the client
            System.out.println("Connecting to Solace messaging at "+host);
            mqttClient.connect(connOpts);
            System.out.println("Connected");

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to message broker lost." + cause.getMessage());
                    isShutdown = true;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msgTime = new Timestamp(System.currentTimeMillis()).toString();
                    logger.write("\nRecieved new message:\n" +
                            "\tTime recieved:     " + msgTime +
                            "\n\tTopic:           " + topic +
                            "\n\tMessage:         " + new String(message.getPayload()) +
                            "\n\tQos:             " + message.getQos() + "\n");
                    logger.flush();
                    Gson gson = new Gson();
                    // Use executorservice to process these incoming messages.
                    if (topic.matches( "octoprint/printer\\d/progress/printing")) {

                        try {
                            System.out.println("Handling print progress event");
                            PrintProgress printProgressObject = gson.fromJson(new String(message.getPayload()), PrintProgress.class);
                            printProgressObject.Process();
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        }

                    } else if(topic.matches("octoprint/printer\\d/event/PrintStarted")) {
                        System.out.println("Handling print Started event");
                        try {
                            PrintStarted printStartedObject = gson.fromJson(new String(message.getPayload()), PrintStarted.class);
                            printStartedObject.setTopic(topic);
                            printStartedObject.Process();

                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            // Subscribe client to the topic filter and a QoS level of 0
            System.out.println("Subscribing client to topic: " + topic);
            mqttClient.subscribe(topic, 0);
            System.out.println("Subscribed");

// endless loop runs till we say stop of we get an I/O interrupt from system (I will remove I/O interrupt option)
            while (System.in.available() == 0 && !isShutdown) {


                if (hasDetectedDiscard) {
                    System.out.println("*** Egress discard detected ***: "
                            + topic + " unable to keep up with full message rate");
                    hasDetectedDiscard = false;
                }
            }
            isShutdown = true;
            mqttClient.disconnect();
            logger.close();
            System.out.println("Main thread quitting.");
            return;

        } catch (MqttException mex ) {
            System.out.printf("Reason: %s, msg: %s,\n loc: %s, cause: %s,\n exception: %s.\n",
                    mex.getReasonCode(), mex.getMessage(), mex.getLocalizedMessage(), mex.getCause(),
                    mex);
        } catch ( IOException ex) {
            ex.printStackTrace();
        }
    }
}
