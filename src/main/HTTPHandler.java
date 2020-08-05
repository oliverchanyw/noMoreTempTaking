package main;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;

public class HTTPHandler implements Runnable {

    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    // port to listen connection
    static final int PORT = 8080;

    private Socket connect; // Client Connection via Socket Class
    private Submitter submitter;

    public HTTPHandler(Socket c) {
        connect = c;
        submitter = new Submitter();
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("HTTPHandler started.\nListening for connections on port : " + PORT + " ...\n");

            // keep accepting new connections
            while (true) {
                HTTPHandler handler = new HTTPHandler(serverConnect.accept()); // blocks until connection received
                System.out.println(String.format("Connecton opened. (%tT)", new Date()));
                Thread thread = new Thread(handler); // 1 thread per handled request
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("HTTPHandler Connection error : " + e.getMessage());
        }
    }

    @Override
    /**
     * Manages 1 client connection.
     */
    public void run() {

        try ( BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
              PrintWriter out = new PrintWriter(connect.getOutputStream());  // character output stream to client (for headers)
              BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream()) // binary output stream to client (for requested data)
              ) {

            String input = in.readLine(); // get first line of the request from the client
            StringTokenizer parse = new StringTokenizer(input);

            String method = parse.nextToken().toUpperCase();
            String path = parse.nextToken().toLowerCase();

            switch (method) {
                case "POST":
                    handleSubmit(in, out, dataOut, path);
                    break;
                case "GET":
                case "HEAD":
                    deliverFile(out, dataOut, path, method);
                    break;
                default:
                    methodNotSupported(out, dataOut, method);
            }

            connect.close();
        } catch (IOException ioe) {
            System.err.println("HTTPHandler error : " + ioe);
        } finally {
            System.out.println("Connection closed.\n");
        }
    }

    /**
     * Reads a file into an array.
     * @param file The file to be read
     * @return an array containing the file's data.
     * @throws IOException
     */
    private byte[] readFileData(File file) throws IOException {
        byte[] fileData = new byte[(int) file.length()];

        try (FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
        }

        return fileData;
    }

    // return supported MIME Types
    private String getContentType(String path) {
        return (path.endsWith(".htm")  ||  path.endsWith(".html")) ? "text/html" : "text/plain";
    }

    /**
     * Sends the client a FNF page.
     * @param out char output stream to the client (for headers)
     * @param dataOut binary output stream to client (for requested data)
     * @param path the file they initially wanted
     * @throws IOException
     */
    private void fileNotFound(PrintWriter out, OutputStream dataOut, String path) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        String content = "text/html";
        byte[] fileData = readFileData(file);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + (int) file.length());
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, (int) file.length());
        dataOut.flush();

        System.out.println("File " + path + " not found");
    }

    /**
     * Sends the client a Not Implemented page.
     * @param out char output stream to the client (for headers)
     * @param dataOut binary output stream to client (for requested data)
     * @param method the method they called
     * @throws IOException
     */
    private void methodNotSupported(PrintWriter out, OutputStream dataOut, String method) throws IOException {
        System.out.println("501 Not Implemented : " + method + " method.");

        // we return the not supported file to the client
        File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
        int fileLength = (int) file.length();
        String contentMimeType = "text/html";
        //read content to return to client
        byte[] fileData = readFileData(file);

        // we send HTTP Headers with data to client
        out.println("HTTP/1.1 501 Not Implemented");
        out.println("Date: " + new Date());
        out.println("Content-type: " + contentMimeType);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
    }

    /**
     * Gives a client the file they want, for a GET/HEAD request.
     * @param out char output stream to the client (for headers)
     * @param dataOut binary output stream to client (for requested data)
     * @param path the file they initially wanted
     * @param method the method they called
     */
    private void deliverFile(PrintWriter out, OutputStream dataOut, String path, String method) throws IOException {
        if (path.endsWith("/")) path += DEFAULT_FILE;

        try {
            File file = new File(WEB_ROOT, path);
            int fileLength = (int) file.length();
            String content = getContentType(path);

            // send HTTP Headers
            out.println("HTTP/1.1 200 OK");
            out.println("Date: " + new Date());
            out.println("Content-type: " + content);
            out.println("Content-length: " + fileLength);
            out.println(); // blank line between headers and content, very important !
            out.flush(); // flush character output stream buffer

            if (method.equals("GET")) { // GET method so we return content
                byte[] fileData = readFileData(file);
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
                System.out.println("File " + path + " of type " + content + " returned");
            }
        } catch (FileNotFoundException fnfe) {
            fileNotFound(out, dataOut, path);
        }
    }


    /**
     *
     * @param in
     * @param out char output stream to the client (for headers)
     * @param dataOut binary output stream to client (for requested data)
     * @param path the local URL path
     */
    private void handleSubmit(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut, String path) {
       if (!"/submit".equals(path)) return;

       String line;
       int contentLength = 0;
       char[] bodyArr = null;
       try {
           line = in.readLine();
           while (line.length() != 0) {
               System.out.println(line);
               if (line.startsWith("Content-Length:")) {
                   contentLength = Integer.parseInt(line.substring(16));
                   bodyArr = new char[contentLength];
               }
               line = in.readLine();
           }
           in.read(bodyArr, 0, contentLength);

           String[] body = new String(bodyArr).split("&");
           Map<String, String> params = new HashMap<>();
           for (String param : body) {
               String[] kv = param.split("=");
               params.put(kv[0], kv[1]);
           }


            boolean pass = submitter.submitWithin(
                    params.get("picker"),
                    params.get("pin"),
                    DATE_FORMAT.parse(params.get("startDate").replace("%2F", "/")),
                    DATE_FORMAT.parse(params.get("endDate").replace("%2F", "/"))
            );

           if (pass) {
               out.println("HTTP/1.1 303 See Other");
               out.println("Location: success.html");
               out.println(); // blank line between headers and content, very important !
               out.flush(); // flush character output stream buffer
           }

           System.out.println(pass ? "All passed." : "Something failed");

       } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}