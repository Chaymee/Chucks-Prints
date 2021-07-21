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



/*
A version of the Paho MQTT subscriber in direct subscriber format.

Initially used for ease of use in testing and development.
May change subscriber strategy depending on requirements.
 */

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

    // Method to start a new thread to run this module
    public void run(){
        
        System.out.println("Initializing connection to broker at " + host );

        // Setup threads to handle certain events
        executorService = Executors.newFixedThreadPool(4);


        // Create or open log file to keep track of all messages coming into the subscriber.
        // In this particular case I am subscribing to all topics and treating this file as a database simulation
        // This process could be replaced by a different application that would actually receive every message,
        // format the messages into the desired schema and store them into the database.
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
            // Could add TLS to this broker and subscriber if it was moving out of testing phase.

            // Connect the client
            System.out.println("Connecting to Solace messaging at "+host);
            mqttClient.connect(connOpts);
            System.out.println("Connected");


            // MQTT uses callback instead of having to poll the broker continuously to check for new messages
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to message broker lost." + cause.getMessage());
                    isShutdown = true;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msgTime = new Timestamp(System.currentTimeMillis()).toString();
                    // Place message into log file.
                    logger.write("\nRecieved new message:\n" +
                            "\tTime recieved:     " + msgTime +
                            "\n\tTopic:           " + topic +
                            "\n\tMessage:         " + new String(message.getPayload()) +
                            "\n\tQos:             " + message.getQos() + "\n");
                    logger.flush();

                    // Create gson object to convert json payloads to java objects.
                    Gson gson = new Gson();

                    // Because I am slightly misusing this subscriber (by subscribing at such a high level to topics
                    //  I need to check for certain topics before performing an action with an incoming event.

                    if (topic.matches("octoprint/printer\\d/event/PrintStarted")) {
                        // Here I use regex to select PrintStarted messages from all printers (between 0 and 9)
                        try {
                            // Process print started event.
                            // Strange behavior - the printStarted event occurs after the print progress zero percent event
                            PrintStarted printStartedObject = gson.fromJson(new String(message.getPayload()), PrintStarted.class);
                            printStartedObject.setTopic(topic);
                            executorService.execute(printStartedObject);
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        }
                    } else if (topic.matches("octoprint/printer\\d/progress/printing")) {
                        // Here I use regex to select print progress messages from all printers (between 0 and 9)

                        try {
                            // Process print progress event.
                            // Strange behavior - the zero progress event occurs before the print started event.
                            PrintProgress printProgressObject = gson.fromJson(new String(message.getPayload()), PrintProgress.class);
                            printProgressObject.setTopic(topic);
                            executorService.execute(printProgressObject);
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Inside this method I could perform some action to confirm that I received an event from the broker
                    //  I am delivering events in format 0 so no confirmation is required.
                }
            });

            // Subscribe client to the topic filter and a QoS level of 0
            System.out.println("Subscribing client to topic: " + topic);
            mqttClient.subscribe(topic, 0);
            System.out.println("Subscribed");

            // endless loop runs till we say stop of we get an I/O interrupt from system (I will remove I/O interrupt option)
            while (System.in.available() == 0 && !isShutdown) {

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
