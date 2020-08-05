import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ManualSubmit implements Runnable {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    public String ID;
    public String PIN;

    public static void main(String[] args) throws IOException, ParseException {
        BufferedReader in = new BufferedReader(new FileReader("id.txt"));
        String line = in.readLine();

        while (line != null) {
            String[] kv = line.split(",");
            ManualSubmit slave = new ManualSubmit(kv[0], "1212");
            Thread thread = new Thread(slave);
            thread.start();
            line = in.readLine();
            System.out.println("Started thread for " + kv[1]);
        }
    }

    public ManualSubmit(String ID, String PIN) {
        this.ID = ID;
        this.PIN = PIN;
    }

    @Override
    /**
     * Manages 1 client connection.
     */
    public void run() {
        try {
            Submitter submitter = new Submitter();
            submitter.setPin(this.ID, this.PIN);
            Date start = DATE_FORMAT.parse("03/13/2020");
            submitter.submitWithin(this.ID, this.PIN, start, new Date());
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
