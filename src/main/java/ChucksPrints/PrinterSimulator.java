package ChucksPrints;


import com.google.gson.GsonBuilder;
import com.solacesystems.jcsmp.TextMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.lang.System.exit;

/**
 * A synthetic MQTT message publisher representing additional 3d printers
 *
 * Not sure if I can run this from the same instance of intellij
 */
public class PrinterSimulator implements Runnable{

    private String host;
    private String username;
    private String vpnname;
    private String password;
    private String startedTopic = "/event/PrintStarted";
    private String progressTopic = "/progress/printing";
    private String highLevelTopic = "octoprint/printer";
    private int printerNum;

    private int numberOfTimes;

    public PrinterSimulator(String host, String username, String password, int printerNum, int numberOfTimes) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.startedTopic = highLevelTopic + printerNum + startedTopic;
        this.progressTopic = highLevelTopic + printerNum + progressTopic;
        this.printerNum = printerNum;
        this.numberOfTimes = numberOfTimes;
    }


    @Override
    public void run() {

        // Build Gson object
        Gson gson = new GsonBuilder().create();


        // Build the client

        try {
            MqttClient mqttClient = new MqttClient(host, "Printer" + getClass().getSimpleName());


            MqttConnectOptions connectOptions =  new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());

            // Initiate client connection
            System.out.println("Starting mqtt connection to " + host);
            mqttClient.connect(connectOptions);
            System.out.println("Publisher connected");

            // Build an MQTT message from my java object
            for(int i = 0; i < numberOfTimes; i++) {
                // Publish PrintStarted message
                PrintStarted printStarted = new PrintStarted("3dPhoneMount.gcode", "~/3dPhoneMount.gcode");
                MqttMessage message = new MqttMessage(gson.toJson(printStarted).getBytes());
                message.setQos(0);
                System.out.println("Publishing message " + message);
                mqttClient.publish(startedTopic, message);

                // Publish percentage in increments of 5%
                PrintProgress printProgress = new PrintProgress(0.0);
                message.clearPayload();
                message.setPayload(gson.toJson(printProgress).getBytes());
                mqttClient.publish(progressTopic, message);
                for(double j = 5; j <=100; j+=5) {
                    Thread.sleep(3000);
                    message.clearPayload();
                    printProgress.setProgress(j);
                    message.setPayload(gson.toJson(printProgress).getBytes());
                    System.out.println("Publishing message " + message);
                    mqttClient.publish(progressTopic, message);
                }
            }
            // Disconnect client once we are finished printing
            mqttClient.disconnect();
            exit(0);

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
