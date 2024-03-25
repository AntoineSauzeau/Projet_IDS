import java.rmi.NotBoundException;
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
    Memory memory;
    Communication_impl communicationImpl;

    public static void main(String [] args) throws RemoteException, InterruptedException {

        if(args.length < 2){
            System.err.println("./Node <number_node> <node_id>");
            exit(-1);
        }

        Node node = new Node(args);
        exit(0);
    }

    Node(String [] args) throws RemoteException, InterruptedException {

        nNode = Integer.parseInt(args[0]);
        nodeId = Integer.parseInt(args[1]);

        System.out.println("Node is starting (id=" + nodeId + ")");

        registry = LocateRegistry.getRegistry(host, port);

        memory = new Memory();

        communicationImpl = new Communication_impl(memory, nNode, nodeId, registry);
        Communication_itf communicationItf = (Communication_itf) UnicastRemoteObject.exportObject(communicationImpl, 0);
        registry.rebind("Node" + nodeId, communicationItf);

        Utility utility = new Utility(memory, communicationImpl);

        StartTask();
    }

    void waitEndStarting(){
        while(true){
            boolean finished = true;
            for(int i = 1; i <= nNode; i++) {
                if (i == nodeId) continue;

                try {
                    registry.lookup("Node" + i);
                } catch (RemoteException | NotBoundException e) {
                    finished = false;
                }
            }
            if(finished) return;
        }
    }



    void StartTask(){
        if(nodeId == 1){
            StartTaskX();
        }
        else if(nodeId == 2){
            StartTaskX();
        }
        else{
            StartTaskX();
        }
    }

    void StartTask1() {
        communicationImpl.AcquireMutexOnAllNodesLoop(0);
        memory.setValue(0, 5);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        communicationImpl.ReleaseMutexOnAllNodes(0);
    }

    void StartTask2(){
        communicationImpl.AcquireMutexOnAllNodesLoop(0);
        System.out.println(memory.getValue(0));
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        communicationImpl.ReleaseMutexOnAllNodes(0);
    }

    void StartTaskX(){
        for(int i = 0; i < 10; i++){
            communicationImpl.AcquireMutexOnAllNodesLoop(0);
            System.out.println(memory.getValue(0));
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            communicationImpl.ReleaseMutexOnAllNodes(0);
        }
    }
}
