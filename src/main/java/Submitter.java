import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

public class Submitter {

    private static String URL = "https://temptaking.ado.sg/group/MemberSubmitTemperature";
    private static String GROUPCODE = "33ba9f0d729b39833259929f7fb99241";
    private static double MAX_TEMP = 37.3;
    private static double MIN_TEMP = 34.9;


    final private Random random;
    final private Map<String, String> headers;

    public Submitter() {
        this.random = new Random();
        headers = new HashMap<>();
        setUpHTTP();
    }

    /**
     * Hits the server up for all AM PMs within the provided dates
     * @param memberId person submitting
     * @param pin 4 digit numeric
     * @param start when to fill from (inclusive)
     * @param end when to fill till (inclusive)
     * @return did all "OK"?
     */
    public boolean submitWithin(String memberId, String pin, Date start, Date end) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);

        while (!cal.getTime().after(end)) {
            boolean check = submit(memberId, pin, cal.getTime(), true) & submit(memberId, pin, cal.getTime(), false);

            if (!check) {
                System.out.println(String.format("Failed on: %s with PIN: %s, Date: %tF", memberId, pin, cal.getTime()));
                return false;
            }
            cal.add(Calendar.DATE, 1);
        }

        return true;
    }

    /**
     * Hits up the server to submit 1 entry
     * @param memberId person submitting
     * @param pin 4 digit numeric
     * @param date which day is the temperature for?
     * @param am is it AM? else PM.
     * @return did the server say "OK"?
     */
    public boolean submit(String memberId, String pin, Date date, boolean am) {
        try {
            Connection connection = Jsoup.connect(URL)
                    .headers(headers)
                    .header("Cookie", String.format("memberId=%s; JSESSIONID=d253b4542d544c667ab5b6bab410; loginToken=c3bbcfeadc97eb66d07f9299054aa1b9", memberId))
                    .requestBody(makeBody(date, am, memberId, randTemp(), pin))
                    .userAgent("Mozilla")
                    .method(Connection.Method.POST);

            Connection.Response res = connection.execute();

            if (res.statusCode() != 200) return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Rolls our random seed to give a plausible temperature.
     * @return the temperature in 2 d.p. as a String e.g. "36.4"
     */
    private String randTemp() {
        double temp = (MIN_TEMP + MAX_TEMP + this.random.nextGaussian() * (MAX_TEMP - MIN_TEMP)) / 2;
        temp = Math.min(Math.max(MIN_TEMP, temp), MAX_TEMP);
        return String.valueOf(temp).substring(0, 4); // 2 dp
    }

    /**
     * Called at Submitter creation. Picks out all the HTTP headers.
     */
    private void setUpHTTP() {
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Connection", "keep-alive");
        headers.put("DNT", "1");
        headers.put("Host", "temptaking.ado.sg");
        headers.put("Origin", "https://temptaking.ado.sg");
        headers.put("Referer", "https://temptaking.ado.sg/group/33ba9f0d729b39833259929f7fb99241");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    private String makeBody(Date date, boolean am, String memberId, String temperature, String pin) {
        String out =    "groupCode=" + GROUPCODE +
                        "&date=" + String.format("%td/%tm/%tY", date, date, date).replace("/", "%2F") +
                        "&meridies=" + (am ? "AM" : "PM") +
                        "&memberId=" + memberId +
                        "&temperature=" + temperature +
                        "&pin=" + pin;

        return out;
    }
}
