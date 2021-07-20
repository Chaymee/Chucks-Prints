package ChucksPrints;

/* Data layout for PrintStarted events

 */

import java.sql.Time;

public class PrintStarted {
    String name;
    String path;
    String origin;
    int size;
    String owner;
    String user;
    String _event;
    int _timestamp;
    String topic;

    public void setTopic(String topic) {
        this.topic = topic;
    }


    public void Process() {
            System.out.println("Starting to print " + this.name + " from " + this.path + " on topic " + this.topic);


    }
};


