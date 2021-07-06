import java.lang.System;
import java.util.HashMap;
import java.io. *;
import java.net. *;

//
// This is an implementation of a simplified version of a command
// line ftp client. The program always takes two arguments
//

public class CSftp {
    static final int DEFAULT_PORT_NUMBER = 21;
    static final int MAX_LEN = 255;
    static final int MAX_BYTE_ARRAY = 1243483647;
    static final int ARG_CNT = 2;

    public static void main(String[] args) {
        byte cmdString[] = new byte[MAX_LEN];

        // Get command line arguments and connected to FTP
        // If the arguments are invalid or there aren't enough of them
        // then exit.

        if (args.length < ARG_CNT - 1) {
            System.out.print("Usage: cmd ServerAddress ServerPort\n\r");
            return;
        }

        // Configure Port Number
        String hostName = args[0];
        int portNumber = DEFAULT_PORT_NUMBER;
        if (args.length == ARG_CNT) {
            portNumber = Integer.parseInt(args[1]);
        }

        // Start processing the command here.
        try {
            // Initialize Socket and Connect
            Socket echoSocket = new Socket(hostName, portNumber);
            echoSocket.setSoTimeout(20000);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            printMessages(in);

            try {
                for (int len = 1; len > 0;) {
                    System.out.print("csftp> ");
                    len = System. in.read(cmdString);
                    if (len <= 0) 
                        break;
                    
                    // Process Commands
                    String stringArguments = new String(cmdString, 0, len).trim();
                    String[] splitArguments = stringArguments.split(" ");
                    runCommands(splitArguments, in, out);
                }
            } catch (IOException e) {
                System.err.println("0xFFFE Input error while reading commands, terminating. \r\n");
                return;
            }
        } catch (IOException e) {
            System.out.print("0xFFFC Control connection to " + hostName + " on port " + portNumber + " failed to open.\r\n");
        }
    }

    /**
   * Action to run the commands to the connected server
   * @param splitArguments  Is a String array containing the arguments that the user has entered
   * @param in              Reference to BufferedReader Object 
   * @param out             Reference to the PrinterWriter Object
   * 
   */
    public static void runCommands(String[] splitArguments, BufferedReader in, PrintWriter out) {
        int argumentCount = splitArguments.length;
        String info = "";
        String command = splitArguments[0].toLowerCase();
        if (commandChecker(splitArguments)) {
            if (argumentCount > 1) {
                info = splitArguments[1];
            }
            try {
                actions(command, info, in, out);
            } catch (Exception e) {
                System.out.println("0xFFFF Processing error. " + e);
            }
        }
    }

    /**
   * Helper to take care of the different commands that the user has inputted.
   * @param command     Refers to what operation the user has inputted
   * @param info        Refers to any additionaly information for the command
   * @param in          Reference to BufferedReader Object 
   * @param out         Reference to the PrinterWriter Object
   * 
   */
    private static void actions(String command, String info, BufferedReader in, PrintWriter out) {
        try {
            switch (command) {
                case "user":
                    out.print("USER " + info + "\r\n");
                    out.flush();
                    if (!out.checkError()) {
                        System.out.println("--> " + "USER" + " " + info);
                        printMessages(in);
                    } else {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                        System.exit(0);
                    }
                    break;
                case "pw":
                    out.print("PASS " + info + "\r\n");
                    out.flush();
                    if (!out.checkError()) {
                        System.out.println("--> " + "PASS" + " " + info);
                        printMessages(in);
                    } else {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                        System.exit(0);
                    }
                    break;
                case "quit":
                    out.print("QUIT" + "\r\n");
                    out.flush();
                    if (!out.checkError()) {
                        System.out.println("--> " + "QUIT");
                        printMessages(in);
                        System.exit(0);
                    } else {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                        System.exit(0);
                    }
                    break;
                case "get":
                    try {
                        out.print("PASV" + "\r\n");
                        out.flush();
                        System.out.println("--> " + "PASV");
                        String response = in.readLine();
                        System.out.println("<-- " + response);

                        PrintWriter passiveOut;
                        BufferedReader passiveIn;
                        InputStream readBytes;
                        try {
                            HashMap<String, Object> connectionMap = createNewConnection(response);
                            passiveOut = (PrintWriter)connectionMap.get("out");
                            passiveIn = (BufferedReader)connectionMap.get("in");
                            Socket passiveSocket = (Socket)connectionMap.get("socket");
                            readBytes = passiveSocket.getInputStream();
                        } catch (IOException e) {
                            return;
                        }
                        if (!response.startsWith("530")) {
                            out.print("TYPE I" + "\r\n");
                            out.flush();
                            System.out.println("--> " + "TYPE I");
                            System.out.println("<-- " + in.readLine());
                            out.print("RETR " + info + "\r\n");
                            out.flush();
                            System.out.println("--> " + "RETR " + info);
                            response = in.readLine();
                            System.out.println("<-- " + response);
                            makeFile(info, readBytes);
                            System.out.println("<-- " + in.readLine());
                        }
                    } catch (NullPointerException e) {
                        return;
                    } catch (IOException exception) {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                    }
                    break;
                case "features":
                    try {
                        out.print("FEAT" + "\r\n");
                        out.flush();
                        System.out.println("--> " + "FEAT");
                        // List Features
                        printMessages( in);
                    } catch (IOException exception) {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                        System.exit(0);
                    }
                    break;
                case "cd":
                    out.print("CWD " + info + "\r\n");
                    out.flush();
                    if (!out.checkError()) {
                        System.out.println("--> " + "CWD" + " " + info);
                        printMessages(in);
                    } else {
                        System.out.println("0xFFFD  Control connection I/O error, closing control connection.");
                        System.exit(0);
                    }
                    break;
                case "dir":
                    out.print("PASV" + "\r\n");
                    out.flush();
                    System.out.println("--> " + "PASV");
                    String response = in.readLine();
                    System.out.println("<-- " + response);
                    try {
                        PrintWriter passiveOut;
                        BufferedReader passiveIn;
                        try {
                            HashMap<String, Object> connectionMap = createNewConnection(response);
                            passiveOut = (PrintWriter)connectionMap.get("out");
                            passiveIn = (BufferedReader)connectionMap.get("in");
                        } catch (IOException e) {
                            return;
                        }
                        if (!response.startsWith("530")) {
                            if (!out.checkError()) {
                                out.print("LIST" + "\r\n");
                                out.flush();
                                System.out.println("--> " + "LIST");
                                printMessages(in);
                                try {
                                    printDataBlockMessages(passiveIn);
                                } catch (IOException e) {
                                    System.out.println("0x3A7 Data transfer connection I/O error, closing data connection");
                                    passiveOut.print("QUIT" + "\r\n");
                                    passiveOut.flush();
                                    return;
                                }
                                System.out.println("<-- " + in.readLine());
                            } else {
                                System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                                System.exit(0);
                            }
                        }
                    } catch (NullPointerException e) {
                        return;
                    }
                    break;
            }
        } catch (IOException e) {
            System.out.println("0xFFFF Processing error. " + e);
        }
    }

    /**
   * Helper to print messages from the server
   * @param in      Reference to BufferedReader object 
   * 
   * @throws IOException
   */
    private static void printMessages(BufferedReader in)throws IOException {
        try {
            StringBuffer stringBuffer = new StringBuffer("");
            String response = in.readLine();
            String responseCode = response.substring(0,3);
            String endingCode = responseCode + " ";

            stringBuffer.append(response);

            String s = stringBuffer.toString();
            while (!s.contains(endingCode)) {
                s = stringBuffer.append("\n" + in.readLine()).toString();
            }

            System.out.println("<-- " + stringBuffer);
        } catch (IOException e) {
            System.out.println("0x3A7 Data transfer connection I/O error, closing data connection");
            throw new IOException(e);
        }
    }

    /**
   * Helper to print block messages regarding any Data Transactions from the server
   * @param in          Reference to BufferedReader object 
   * 
   * @throws IOException
   */
    private static void printDataBlockMessages(BufferedReader in)throws IOException {
        try {
            StringBuffer stringBuffer = new StringBuffer("");
            stringBuffer.append( in.readLine());

            while ( in.ready()) {
                stringBuffer.append("\n" + in.readLine());
            }
            System.out.println("<-- " + stringBuffer);
        } catch (IOException e) {
            System.out.println("0x3A7 Data transfer connection I/O error, closing data connection");
            throw new IOException(e);
        }
    }

    /**
   * Using the response from the server, the function creates a new connection with a server
   * @param response    is the response from the server containing information about the new address
   * 
   * @return            HashMap<String, Object>, 
   *                    returns a hashmap of the PrintWriter, BufferedReader and Socket of the newly connected server. 
   */
    private static HashMap<String, Object> createNewConnection(String response)throws IOException {
        HashMap<String, Object> connectionMap = new HashMap<String, Object>();
        if (response.startsWith("227")) {
            HashMap<String, Object> hostInfoMap = parseNewHost(response);
            String hostName = (String)hostInfoMap.get("host");
            int portNum = (int)hostInfoMap.get("port");
            try {
                Socket newSocket = new Socket(hostName, portNum);
                newSocket.setSoTimeout(10000);
                PrintWriter out = new PrintWriter(newSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
                connectionMap.put("in", in);
                connectionMap.put("out", out);
                connectionMap.put("socket", newSocket);
            } catch (IOException exception) {
                System.out.println("0xFFFC Control connection to " + hostName + " on port " + portNum + " failed to open");
                throw new IOException(exception);
            }
        }
        return connectionMap;
    }

    /**
   * Using the response from the server, the function parses information for the new IP and host number.
   * @param response    is the response from the server containing information about the new address
   * 
   * @return            HashMap<String, Object>
   *                    returns a hashmap of the IP address and Port Number
   */
    private static HashMap<String, Object> parseNewHost(String response) {
        response = response.trim().split("\\(")[1];
        String[] parsedArray = response.replace(")", "").replace(".", "").split(",");
        String newHost = parsedArray[0] + "." + parsedArray[1] + "." + parsedArray[2] + "." + parsedArray[3];
        int newPortNum = (Integer.parseInt(parsedArray[4]) * 256) + Integer.parseInt(parsedArray[5]);
        HashMap<String, Object> newHostInfoMap = new HashMap<String, Object>();
        newHostInfoMap.put("host", newHost);
        newHostInfoMap.put("port", newPortNum);

        return newHostInfoMap;
    }

    /**
   * Checker to see that the user has entered valid commands and arguments.
   * Returns True if the entered has entered a valid command and the correct number of arguments.
   * 
   * @param splitArguments   Is a String array containing the arguments that the user has entered
   * 
   * @return            Boolean
   */
    private static Boolean commandChecker(String[] splitArguments) {
        String command = splitArguments[0].toLowerCase().replace(" ", "");
        int arguments = splitArguments.length;
        Boolean valid = false;
        switch (command) {
            case "user":
                if (arguments == 2) {
                    valid = true;
                }
                break;
            case "pw":
                if (arguments == 2) {
                    valid = true;
                }
                break;
            case "quit":
                if (arguments == 1) {
                    valid = true;
                }
                break;
            case "get":
                if (arguments == 2) {
                    valid = true;
                }
                break;
            case "features":
                if (arguments == 1) {
                    valid = true;
                }
                break;
            case "cd":
                if (arguments == 2) {
                    valid = true;
                }
                break;
            case "dir":
                if (arguments == 1) {
                    valid = true;
                }
                break;
            default:
                System.out.println("0x001 Invalid command. \r\n");
                return valid;
        }
        if (!valid) {
            System.out.print("0x002 Incorrect number of arguments.\r\n");
        }
        return valid;
    }

    /**
   * Helper to create File on Local device in current working directory
   * @param fileName    The Name of the Pathfile  
   * @param in          Input Stream object
   *        
   */
    private static void makeFile(String fileName, InputStream in) {
        File file = new File("./" + fileName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(out);

            byte[] contents = new byte[MAX_BYTE_ARRAY];
            int read = 0;

            try {
                while ((read = in.read(contents)) != -1) {
                    System.out.println(contents);
                    bos.write(contents, 0, read);
                }
            } catch (IOException e) {
                System.err.printf("0x3A7 Data transfer connection I/O error, closing dataconnection.");
            }
            out.close();
            in.close();
        } catch (IOException e) {
            System.out.printf("0x38E Access to local file %s denied.", fileName);
        }
    }
}
