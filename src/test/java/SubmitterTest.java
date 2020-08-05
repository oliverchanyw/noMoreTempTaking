import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class SubmitterTest {

	static final String OLI_ID = "7563012";
	static final String OLI_PIN = "1212";

	private Submitter s;

    @Before
	public void initialize() {
		s = new Submitter();
	}

    @Test
    public void sendOneOliver() throws ParseException {
    	assertTrue(s.submit(OLI_ID, OLI_PIN, new SimpleDateFormat("dd/MM/yyyy").parse("01/08/2020"), true));
	}

	@Test
	public void sendOneWeekOliver() throws ParseException {
		assertTrue(s.submitWithin(OLI_ID, OLI_PIN, new SimpleDateFormat("dd/MM/yyyy").parse("01/08/2020"), new Date()));
	}
}
