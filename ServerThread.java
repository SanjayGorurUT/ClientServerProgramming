import java.util.*;
import java.io.*;
import java.net.*;

public class ServerThread implements Runnable {
    
    DatagramSocket datagramSocket;
    ServerSocket serverSocket;
    BookServer bookServer;

    public ServerThread(BookServer bookServer, String mode, int tcpPort, int udpPort)
    {
        this.bookServer = bookServer;
        try
        {
            if(mode.equals("u"))
            {
                datagramSocket = new DatagramSocket(udpPort);
                serverSocket = null;
            }
            else if(mode.equals("t"))
            {
                datagramSocket = null;
                serverSocket = new ServerSocket(tcpPort);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DatagramPacket dataPacket;
        byte[] buf;
        while(true)
        {
            if(serverSocket == null && datagramSocket != null)
            {
                buf = new byte[1024];
                dataPacket = new DatagramPacket(buf, buf.length);
                try {
                    datagramSocket.receive(dataPacket);
                    String request = new String(dataPacket.getData());
                    int i = request.length() - 1;
                    for(i = i; i >= 0; i--) {
                        if(Character.isLetterOrDigit(request.charAt(i))) {
                            break;
                        }
                    }
                    request = new String(request.substring(0, i + 1));
                    bookServer.lock.lock();
                    String response = bookServer.processInput(request);
                    bookServer.lock.unlock();
                    InetAddress ia = InetAddress.getByName("localhost");
                    DatagramPacket packet = new DatagramPacket(response.getBytes(), response.getBytes().length, ia, dataPacket.getPort());
                    datagramSocket.send(packet);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            else if (serverSocket != null && datagramSocket == null) {
                    bookServer.lock.lock();
                    List<Socket> dead = new ArrayList<>();
                    for (Socket clientSocket: bookServer.sockets) {
                        try
                        {
                            BufferedReader readClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            String request = readClient.readLine();
                            int i = request.length() - 1;
                            for(i = i; i >= 0; i--)
                            {
                                if(Character.isLetterOrDigit(request.charAt(i)))
                                {
                                    break;
                                }
                            }
                            request = new String(request.substring(0, i + 1)); 
                            String response = bookServer.processInput(request);
                            PrintWriter serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                            if(response.equals("The communication mode is set to UDP"))
                            {
                                dead.add(clientSocket);
                            }
                            serverWriter.println(response);
                            serverWriter.flush();
                        }
                        catch(Exception e)
                        {

                        }
                    } 
                    bookServer.sockets.removeAll(dead);
                    bookServer.lock.unlock();
            }


        }
    }
}
