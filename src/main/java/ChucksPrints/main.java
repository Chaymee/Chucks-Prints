package ChucksPrints;


import com.solacesystems.jcsmp.JCSMPException;

import java.io.IOException;
import java.util.Scanner;


class main {

    // Graceful shutdown var shared across thread memory
    public volatile Boolean isShutdown = false;


    private static String host = "tcp://192.168.1.11";
    private static String username = "admin";
    private static String vpnname = "";
    private static String password = "admin";
    private static String topic = "octoprint/#";

    public static void main(String[] args) {




        // Prompt user to enter selection for which application they would like to start
        // Parse std in to get their selection and spin it off to a new thread.

        Scanner stdin = new Scanner(System.in);
        int userAction;

        System.out.println("*************************************");
        System.out.println("Welcome to Chucks Prints POC Client.");
        System.out.println("What service would you like to explore?");
        System.out.println("Please select one of the following by entering the number\n" +
                "and pressing return on your keyboard. The default when no number is entered is Printer monitoring.");
        System.out.println("   1. Printer monitoring - MQTT via Paho.");
        System.out.println("   2. Power usage - Via JCSMP.");
        System.out.println("   3. Resource Report - Power and Filament usage report.");
        System.out.println("   4. Printer Simulator - Publish MQTT with Paho.");
        System.out.println("   5. Power simulator - Publish with JCSMP");
        System.out.println("   6. Exit - end all tasks and shut down gracefully");
        userAction = Integer.parseInt(stdin.next());
        System.out.printf("Recieved request for action: %d from user.\n", userAction);


        // Determine which user action was selected
        switch (userAction) {
            case 1:
                System.out.println("Starting Printer Monitor, please enter topic. Leave blank for default topic: ");
                String tempin;
                tempin = stdin.nextLine();
                if (!tempin.isBlank())
                    topic = tempin;
                System.out.println("Topic: " + topic);
                // Create a new thread to run the selected task for fun.
                Runnable mqttDirectSubscriber1 = new MQTTDirectSubscriber(host, username, password, topic);
                new Thread(mqttDirectSubscriber1).start();
                break;
            case 2:
                System.out.println("You have selected option 2: Power Usage analysis with JCSMP.");
                System.out.println("!!! This functionality is not yet completely implemented.");
                DirectSubscriber directSubscriber = new DirectSubscriber();
                try {
                    directSubscriber.run(host, vpnname, username, password);
                } catch (JCSMPException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            // Extra program modes to provide inspiration.  I probably will never implement these
            case 3:
                System.out.println("  Resource report requested");
                System.out.println("!!! This functionality has not yet been developed. It is included as inspiration !!!");
                break;
            case 4:
                System.out.println("  Printer Simulator requested");
                System.out.println("!!! This functionality has not yet been developed. It is in progress !!!");

                break;
            case 5:
                System.out.println("  Power Simulator requested");
                System.out.println("!!! This functionality has not yet been developed. It is included as inspiration !!!");
                break;
            case 6:
                System.out.println("  Exit requested");
                System.exit(0);
            default:
                System.out.println(" No action requested, exiting.");
                break;
        }
    }
}