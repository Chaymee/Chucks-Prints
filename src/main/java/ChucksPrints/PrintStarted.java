package ChucksPrints;


/**
 * PrintStarted class implements Runnable so that its run method can be called by executor service to complete work
 *  in a new thread from the threadpool
 *
 * PrintStarted data represents the json layout for an event pushed to the /event/PrintStarted topic
 */
public class PrintStarted implements Runnable{
    String name;
    String path;
    String origin;
    int size;
    String owner;
    String user;
    String _event;
    int _timestamp;
    String topic;
    int printerNum;

    // Overloaded constructor used to make messages for publishing
    public PrintStarted(String name, String path ) {
        this.name = name;
        this.path = path;
    }

    // topic is not part of the json payload so I manually set the topic for an object of the printStarted class
    //  Later I can parse this topic to figure out what printer it came from
    public void setTopic(String topic) {
        this.topic = topic;
    }



    // Basic run function to take the incoming event what was converted from json to java object and provide
    //  an alert to the user.  At this point you would add business logic to update some office billboard or
    //  send a slack message or email or some other form of alert or update
    @Override
    public void run() {
        printerNum = Integer.parseInt(topic.substring(17,18));
        System.out.println("Printer " + printerNum + ": Starting to print " + this.name + " from " + this.path + " on topic " + this.topic);

    }
};


