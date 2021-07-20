package ChucksPrints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;

public class PrintProgress {
    String location;
    String path;
    Double progress;
    Double timestamp;

    public void Process() {

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