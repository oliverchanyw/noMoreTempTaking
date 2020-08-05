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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class HTTPHandler implements Runnable {

    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    // port to listen connection
    static final int PORT = 8080;

    // Client Connection via Socket Class
    private Socket connect;

    public HTTPHandler(Socket c) {
        connect = c;
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
            String fileRequested = parse.nextToken().toLowerCase();

            // we support only GET and HEAD methods, we check
            if (!method.equals("GET")  &&  !method.equals("HEAD")) {
                methodNotSupported(out, dataOut, method);
            } else { // GET or HEAD method
                deliverFile(out, dataOut, fileRequested, method);
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
    private String getContentType(String fileRequested) {
        return (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html")) ? "text/html" : "text/plain";
    }

    /**
     * Sends the client a FNF page.
     * @param out char output stream to the client (for headers)
     * @param dataOut binary output stream to client (for requested data)
     * @param fileRequested the file they initially wanted
     * @throws IOException
     */
    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
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

        System.out.println("File " + fileRequested + " not found");
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
     * @param fileRequested the file they initially wanted
     * @param method the method they called
     */
    private void deliverFile(PrintWriter out, OutputStream dataOut, String fileRequested, String method) throws IOException {
        if (fileRequested.endsWith("/")) fileRequested += DEFAULT_FILE;

        try {
            File file = new File(WEB_ROOT, fileRequested);
            int fileLength = (int) file.length();
            String content = getContentType(fileRequested);

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
                System.out.println("File " + fileRequested + " of type " + content + " returned");
            }
        } catch (FileNotFoundException fnfe) {
            fileNotFound(out, dataOut, fileRequested);
        }
    }
}