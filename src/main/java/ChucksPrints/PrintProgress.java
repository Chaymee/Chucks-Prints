package ChucksPrints;


/**
 * PrintProgress class implements Runnable so that its run method can be called by executorservice to complete work
 *  in a new thread from the threadpool
 *
 *  PrintProgress class data represents the json paylod from a /progress/printing topic message
 */
public class PrintProgress implements Runnable {
    String location;
    String path;
    Double progress;
    Double timestamp;
    String topic;
    int printerNum;

    // Topic is not a part of the json payload for a progress/printing event.  I manually set the topic so that I can
    //  parse it later to get the printer number.
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Basic run function for printProgress that can be called by executorservice.
     *
     * This would be the location for logic to update an office billboard or publish an alert via slack or email.
     */
    @Override
    public void run() {
        printerNum = Integer.parseInt(topic.substring(17,18));
        System.out.println(" Printer " + printerNum + " print progress: " + progress + "%");
    }
}
    /* Extended version of the PrintProgress messages.
       Commented out after testing

    String location;
    List<String> path;
    Double progress;
    Printer_data printerData;
    Time _timestamp;
}
class Printer_data {
    State state;
    Job job;
    String currentZ;
    Progress progress;
    Offsets offsets;
    Resend resend;
}



class State {
    String text;
    Flags flags;
    String error;

}
class Flags {
    Boolean operational;
    Boolean printing;
    Boolean cancelling;
    Boolean pausing;
    Boolean resuming;
    Boolean finishing;
    Boolean closedOrError;
    Boolean error;
    Boolean paused;
    Boolean ready;
    Boolean sdReady;
}

class Job {
    File file;
    Double estimatedPrintTime;
    Double averagePrintTime;
    Double lastTime;
    Filament filament;
    String user;
}
class File {
    String name;
    String path;
    String display;
    String origin;
    int size;
    Time date;
}
class Filament {
    Tool0 tool;
}
class Tool0 {
    double length;
    double volume;

}
class Progress {
    Double completion;
    int filepos;
    int printTime;
    int printTimeLeft;
    String printTimeLeftOrigin;
}
class Offsets {

}
class Resend {
    int count;
    int transmitted;
    double ratio;
}
     */