import java.io.*;
import java.net.*;
import java.util.*;

public class DrawServer {
    private static final int PORT = 5000;
    private static final List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Draw server started on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            clients.add(out);

            new Thread(() -> {
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    Object obj;
                    while ((obj = in.readObject()) != null) {
                        broadcast(obj);
                    }
                } catch (Exception e) {
                    System.out.println("Client disconnected");
                } finally {
                    clients.remove(out);
                }
            }).start();
        }
    }

    private static void broadcast(Object obj) {
        synchronized (clients) {
            for (ObjectOutputStream out : clients) {
                try {
                    out.writeObject(obj);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
