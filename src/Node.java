import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static java.lang.System.exit;

public class Node {

    String host = "localhost";
    int port = 6090;
    int nNode;
    int nodeId;

    public static void main(String [] args) throws RemoteException {

        if(args.length < 2){
            System.err.println("./Node <number_node> <node_id>");
            exit(-1);
        }

        Node node = new Node(args);
        exit(0);
    }

    Node(String [] args) throws RemoteException {

        nNode = Integer.parseInt(args[0]);
        nodeId = Integer.parseInt(args[1]);

        System.out.println("Node is starting (id=" + nodeId + ")");

        Registry registry = LocateRegistry.getRegistry(host, port);

        Memory memory = new Memory();

        Communication_impl communicationImpl = new Communication_impl(memory, nNode, nodeId);
        Communication_itf communicationItf = (Communication_itf) UnicastRemoteObject.exportObject(communicationImpl, 0);
        registry.rebind("Node" + nodeId, communicationItf);

        Utility utility = new Utility(memory, communicationImpl);
    }
}
