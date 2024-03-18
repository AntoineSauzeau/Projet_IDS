import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Node {

    String host = "";
    int port = 0;

    public static void main(String [] args) {

    }

    Node() throws RemoteException {

        Registry registry = LocateRegistry.getRegistry(host, port);

        Memory memory = new Memory();
        Communication_impl communicationImpl = new Communication_impl(memory);
        Communication_itf communicationItf = (Communication_itf) UnicastRemoteObject.exportObject(communicationImpl, 0);


    }
}
