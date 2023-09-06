import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.*;

public class BookServer {
    Map<String, BookPair> inventory;
    Map<String, ArrayList<LoanPair>> userToLoan;
    Map<Integer, String> idToUser;   
    int loanNumber;
    ArrayList<Socket> sockets;
    ReentrantLock lock;

    public BookServer()
    {
        inventory = new LinkedHashMap<String, BookPair>();
        userToLoan = new HashMap<String, ArrayList<LoanPair>>();
        idToUser = new HashMap<Integer, String>();
        loanNumber = 1;
        sockets = new ArrayList<Socket>();
        lock = new ReentrantLock();
    }

    public void loadInventory(String inputFile) {
        try {
            File file = new File(inputFile);
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                String wholeLine = scan.nextLine();
                String name = wholeLine.substring(1, wholeLine.lastIndexOf(" ") - 1);
                int quantity = Integer.parseInt(wholeLine.substring(wholeLine.lastIndexOf(" ") + 1));
                inventory.put(name, new BookPair(quantity));
            }
            scan.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String processInput(String command) {
        Scanner scan = new Scanner(command);
        String requestType = scan.next();
        int loanId;
        String username;
        String book;
        String newMode;
        String response  = "";
        switch (requestType) {
            case "connect":
                response = "connected";
                break;
            case "set-mode":
                newMode = scan.next();
                String findMode = "";
                if(newMode.equals("t")) {
                    findMode = "TCP";
                }
                else if(newMode.equals("u")) {
                    findMode = "UDP";
                }
                response = "The communication mode is set to " + findMode;
                break;
            case "begin-loan":
                username = scan.next();
                book = scan.nextLine();
                book = parseString(book);
                if(!inventory.containsKey(book))
                {
                    response = "Request Failed - We do not have this book";
                }
                else if(inventory.get(book).getQuantity() == 0)
                {
                    response = "Request Failed - Book not available";
                }
                else
                {
                    inventory.get(book).beginLoan();
                    if(!userToLoan.containsKey(username))
                    {
                        userToLoan.put(username, new ArrayList<LoanPair>());
                    }
                    userToLoan.get(username).add(new LoanPair(loanNumber, book));
                    idToUser.put(loanNumber, username);
                    response = "Your request has been approved, " + Integer.toString(loanNumber) + " " + username + " \"" + book + "\"";
                    loanNumber++;
                }
                break;
            case "end-loan":
                loanId = Integer.parseInt(scan.nextLine().substring(1));
                if (!idToUser.containsKey(loanId)) {
                    response = loanId + " not found, no such borrow record";
                    break;
                }
                else {
                    response = loanId + " is returned";
                }
                username = idToUser.get(loanId);
                String currBook = "";
                idToUser.remove(loanId);
                for (int k = 0; k < userToLoan.get(username).size(); k++) {
                    if (userToLoan.get(username).get(k).getLoanId() == loanId) {
                        currBook = userToLoan.get(username).get(k).getBook();
                        userToLoan.get(username).remove(userToLoan.get(username).get(k));
                        break;
                    }
                }
                inventory.get(currBook).endLoan();
                if(userToLoan.get(username).size() <= 0)
                {
                    userToLoan.remove(username);
                }
                break;
            case "get-loans":
                username = scan.nextLine();
                username = parseString(username);
                if (!userToLoan.containsKey(username)) {
                    response = "No record found for " + username;
                }
                else {
                    for (LoanPair pair: userToLoan.get(username)) {
                        response += pair.getLoanId() + " \"" + pair.getBook() + "\"&";
                    }
                    response = response.substring(0, response.length()-1);
                }
                break;
            case "get-inventory":
                for (String key: inventory.keySet()) {
                    response += "\"" + key + "\" " + inventory.get(key).getQuantity() + "&";
                }
                response = response.substring(0, response.length()-1);
                break;
            case "exit":
                try {
                    File f = new File("inventory.txt");
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    FileWriter fileWriter = new FileWriter(f);
                    String inventoryString = "";
                    for (String key: inventory.keySet()) {
                        inventoryString += "\"" + key + "\" " + inventory.get(key).getQuantity() + "\n";
                    }
                    inventoryString = inventoryString.substring(0, inventoryString.length()-1);
                    fileWriter.write(inventoryString);
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        scan.close();
        return response;
    }

    public static void main(String[] args) {
        BookServer bookServer = new BookServer();

        if (args.length != 1) {
            System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
            System.exit(-1);
        }
        bookServer.loadInventory(args[0]);

        ServerThread udpServer = new ServerThread(bookServer, "u", 7000, 8000);
        ServerThread tcpServer = new ServerThread(bookServer, "t", 7000, 8000);
        SocketThread tcpServerHelper = new SocketThread(bookServer, tcpServer.serverSocket);
        Thread udpThread = new Thread(udpServer);
        Thread tcpThread = new Thread(tcpServer);
        Thread tcpThreadHelper = new Thread(tcpServerHelper);

        tcpThreadHelper.start();
        udpThread.start();
        tcpThread.start();
    }

    public String parseString(String input) {
        int i = 0;
        int j = input.length() - 1;
        for(; i < input.length(); i++)
        {
            if(Character.isLetter(input.charAt(i)))
            {
                break;
            }
        }
        for(; j >= 0; j--)
        {
            if(Character.isLetter(input.charAt(j)))
            {
                break;
            }
        }
        return input.substring(i, j + 1);
    }
}
