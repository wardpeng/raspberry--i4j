import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class SerialTestGET implements SerialPortEventListener
{
	SerialPort serialPort;
	static int sensorValue = 0;
	/** The port we’re normally going to use. */
	private static final String PORT_NAMES[] = { "/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
	};
	/**
	 * A BufferedReader which will be fed by a InputStreamReader converting the
	 * bytes into characters making the displayed results codepage independent
	 */
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public void initialize()
	{
		// the next line is for Raspberry Pi and
		// gets us into the while loop and was suggested here was suggested
		// http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
		System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}
		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();
			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		}
		catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port. This will prevent
	 * port locking on platforms like Linux.
	 */
	public synchronized void close()
	{
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent)
	{
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine = input.readLine();
				// System.out.println(inputLine);
				sendGet(inputLine);
			}
			catch (Exception e) {
				System.err.println(e.toString());
				serialPort.removeEventListener();
				serialPort.close();
			}
		}
		// Ignore all the other eventTypes, but you should consider the other
		// ones.
	}

	// HTTP GET request
	public void sendGet(String inputLine) throws Exception
	{
		try {
			// if difference is more than 3 then send data to SAP HANA
			if (inputLine != null && inputLine.length() > 0
					&& Math.abs(sensorValue - Integer.parseInt(inputLine)) > 3) {
				sensorValue = Integer.parseInt(inputLine);
				// Considering that A001 sensor is connection with this
				// raspberry pie for now
				// we can even pass this with command line but for
				// simplicityhardcoding it
				// Replace with your HANA server URL and port number
				String url = "http:///demoApp/demo01/app01/services/putSensorReading.xsjs?id=A001&value=";
				url = url + inputLine;
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				// optional default is GET
				con.setRequestMethod("GET");
				// add request header
				// con.setRequestProperty("User-Agent", USER_AGENT);
				int responseCode = con.getResponseCode();
				if (responseCode == 200) {
					System.out.println("OK value:" + inputLine);
				} else {
					System.out.println("Error: Response code " + responseCode);
				}
			}
			System.out.println("OK value: Less than 3");
		}
		catch (Exception e) {
			System.err.println(e.toString());
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	public static void main(String[] args) throws Exception
	{
		SerialTestGET main = new SerialTestGET();
		main.initialize();
		Thread t = new Thread()
		{
			public void run()
			{
				// the following line will keep this app alive for 1000 seconds,
				// waiting for events to occur and responding to them (printing
				// incoming messages to console).
				try {
					Thread.sleep(1000000);
				}
				catch (InterruptedException ie) {
				}
			}
		};
		t.start();
		System.out.println("Started");
	}
}