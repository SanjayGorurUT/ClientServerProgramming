import java.util.Scanner;
import java.io.*;
import java.net.*;

public class BookClient implements Runnable {

    int tcpPort;
    int udpPort;
    String commandFile;
    int clientId;
    DatagramSocket datagramSocket;
    Socket tcpSocket;
    BufferedReader serverReader;
    PrintWriter serverWriter;
    boolean shutdown;

    public BookClient(String commandFile, int clientId) {
        this.tcpPort = 7000;
        this.udpPort = 8000;
        this.commandFile = commandFile;
        this.clientId = clientId;
        try {
            this.datagramSocket = new DatagramSocket();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        this.tcpSocket = null;
        this.serverReader = null;
        this.serverWriter = null;
        this.shutdown = false;
    }
    public static void main(String[] args) {
        int clientId;

        if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments: command-file, clientId");
            System.out.println("\t(1) command-file: file with commands to the server");
            System.out.println("\t(2) clientId: an integer between 1..9");
            System.exit(-1);
        }

        String commandFile = args[0];
        clientId = Integer.parseInt(args[1]);
        String hostAddress = "localhost";

        BookClient bookClient = new BookClient(commandFile, clientId); 
        Thread t = new Thread(bookClient);
        t.start();
    }

    public void run() {
        try {
            File createFile = new File("out_" + clientId + ".txt");
            createFile.createNewFile();
            FileWriter writeFile = new FileWriter(createFile);
            DatagramPacket packet;
            DatagramPacket dataPacket;
            InetAddress ia;
            ia = InetAddress.getByName("localhost");
            String message = "connect";
            packet = new DatagramPacket(message.getBytes(), message.getBytes().length, ia, udpPort);
            datagramSocket.send(packet);
            byte[] buf = new byte[1024];
            dataPacket = new DatagramPacket(buf, buf.length, ia, udpPort);
            datagramSocket.receive(dataPacket);

            Scanner sc = new Scanner(new FileReader(commandFile));

            while (sc.hasNextLine() && shutdown == false) {
                String cmd = sc.nextLine();
                String[] tokens = cmd.split(" ");
                
                if(tcpSocket == null && datagramSocket != null)
                {   
                    packet = new DatagramPacket(cmd.getBytes(), cmd.getBytes().length, ia, udpPort);
                    datagramSocket.send(packet);
                    buf = new byte[1024];
                    dataPacket = new DatagramPacket(buf, buf.length, ia, udpPort);
                    datagramSocket.receive(dataPacket);
                    String response = new String(dataPacket.getData());
                    int i = response.length() - 1;
                    for(i = i; i >= 0; i--) {
                        if(Character.isLetterOrDigit(response.charAt(i)) || response.charAt(i) == '\"')
                        {
                            break;
                        }
                    }
                    response = new String(response.substring(0, i + 1));
                    if (tokens[0].equals("set-mode")) {
                        String newMode = response.substring(response.lastIndexOf(" ") + 1, response.length());
                        if(newMode.equals("TCP"))
                        {
                            datagramSocket = null;
                            tcpSocket = new Socket("localhost", tcpPort);
                        }
                        writeFile.write(response + "\n");
                    } else if (tokens[0].equals("begin-loan")) {
                        writeFile.write(response + "\n");
                    } else if (tokens[0].equals("end-loan")) {
                        writeFile.write(response +  "\n");
                    } else if (tokens[0].equals("get-loans")) {
                        String[] loanRecords = response.split("&");
                        for (String record: loanRecords) {
                            writeFile.write(record + "\n");
                        }
                    } else if (tokens[0].equals("get-inventory")) {
                        String[] inventoryRecords = response.split("&");
                        for (String record: inventoryRecords) {
                            writeFile.write(record + "\n");
                        }
                    } else if (tokens[0].equals("exit")) {
                        writeFile.close();
                        shutdown = true;
                    } else {
                        System.out.println("ERROR: No such command");
                    }
                }
                else if(datagramSocket == null && tcpSocket != null)
                {
                    PrintWriter clientWriter = new PrintWriter(tcpSocket.getOutputStream(), true);
                    clientWriter.println(cmd);
                    BufferedReader readServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                    String response = readServer.readLine();
                    if (tokens[0].equals("set-mode")) {
                        String newMode = response.substring(response.lastIndexOf(" ") + 1, response.length());
                        if(newMode.equals("UDP"))
                        {
                            datagramSocket = new DatagramSocket();
                            tcpSocket = null;
                        }
                        writeFile.write(response + "\n");
                    } else if (tokens[0].equals("begin-loan")) {
                        writeFile.write(response + "\n");
                    } else if (tokens[0].equals("end-loan")) {
                        writeFile.write(response + "\n");
                    } else if (tokens[0].equals("get-loans")) {
                        String[] loanRecords = response.split("&");
                        for (String record: loanRecords) {
                            writeFile.write(record + "\n");
                        }
                    } else if (tokens[0].equals("get-inventory")) {
                        String[] inventoryRecords = response.split("&");
                        for (String record: inventoryRecords) {
                            writeFile.write(record + "\n");
                        }
                    } else if (tokens[0].equals("exit")) {
                        writeFile.close();
                        shutdown = true;
                        tcpSocket.close();
                    } else {
                        System.out.println("ERROR: No such command");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
