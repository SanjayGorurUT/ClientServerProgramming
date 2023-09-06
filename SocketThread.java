import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public class SocketThread implements Runnable {

    ServerSocket serverSocket;
    BookServer bookServer;
    ReentrantLock lock;

    public SocketThread(BookServer bookServer, ServerSocket serverSocket) {
        this.bookServer = bookServer;
        this.serverSocket = serverSocket;
    }

    public void run() {
        while (true) {
            try {
                Socket clientSocket = this.serverSocket.accept();
                bookServer.lock.lock();
                bookServer.sockets.add(clientSocket);
                bookServer.lock.unlock();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    
    
}
