import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import static java.lang.System.exit;
import static java.lang.System.in;

public class Node {

    String host = "localhost";
    int port = 6090;
    int nNode;
    int nodeId;
    Memory memory;
    Communication_impl communicationImpl;
    Utility utility;
    Registry registry;

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

        utility = new Utility(memory, communicationImpl);

        waitEndStarting();
        StartTask();

        readInputFromUser();
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

    public boolean readInputFromUser(){

        System.out.println(" p - to print memory");
        System.out.println(" q - to quit");

        //Ecrire des messages jusqu'à ce que l'on quitte 'q'
        Scanner scanner = new Scanner(System.in);
        String userMessage;
        while ((userMessage = scanner.nextLine()) != null) {

            if(userMessage.equals("q")) break;
            if(userMessage.equals("p")) {
                memory.printMemory();
                continue;
            }

            System.out.println("Node " + nodeId + " : " + userMessage);
            processInput(userMessage);

        }

        return true;
    }

    public void processInput(String input){
        String[] cmd = input.split(" ");
        String op1 = cmd[0];
        String op2 = cmd[2];
        String symb = cmd[1];

        int index = Integer.parseInt(op1);
        int value = Integer.parseInt(op2);
        if(symb.equals("=")){

            communicationImpl.AcquireMutexOnAllNodes(index);
            memory.setValue(index, value);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Put : " + memory.getValue(index) + " at index : " + index);
            communicationImpl.ReleaseMutexOnAllNodes(index);

        }
    }


}
