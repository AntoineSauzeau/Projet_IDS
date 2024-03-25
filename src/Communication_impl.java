import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;

public class Communication_impl implements Communication_itf {

    Memory memory;
    int nNode;
    int nodeId;
    Registry registry;
    int localEltRequestIndex;
    long localRequestTimestamp;
    ArrayList<Integer> lNodeWaiting;
    Semaphore semaphore;
    long logicalTimestamp;

    public Communication_impl(Memory memory, int nNode, int nodeId, Registry registry){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
        this.registry = registry;

        this.lNodeWaiting = new ArrayList<>();
        this.semaphore = new Semaphore(0);
        logicalTimestamp = 0; //A voir
    }

    @Override
    public ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp, long logicalTimestamp) throws RemoteException {
        //Si ce node veut un verrou sur le même élément (conflit) et qu'il a commencé avant -> Echec
        if(localEltRequestIndex == index && requestTimestamp > localRequestTimestamp){
            lNodeWaiting.add(nodeWhoRequestId);
            return ResponseType.FAIL;
        }
        memory.lockElement(index);
        return ResponseType.OK;
    }

    @Override
    public void ReleaseMutexOnElement(int index, long logicalTimestamp) throws RemoteException {
        memory.releaseElement(index);
    }

    @Override
    public void PropagateModification(int index, int value, long logicalTimestamp) {
        System.out.println("Received modification of element ("+ index +") = " + value);
        memory.setValue(index, value);
    }

    public void AcquireMutexOnAllNodesLoop(int index) {
        localRequestTimestamp = System.currentTimeMillis();
        localEltRequestIndex = index;

        if(!AcquireMutexOnAllNodes(index)) {
            //On attend avant de retenter d'obtenir le verrou pour chaque node
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    boolean AcquireMutexOnAllNodes(int index) {
        System.out.println("Node " + nodeId + " trying to acquire mutex for element " + index);
        boolean returnValue = true;

        if(memory.isElementLocked(index)) returnValue = false;
        else {
            for (int i = 1; i <= nNode; i++) {
                if (i == nodeId) continue;

                Communication_itf node;
                ResponseType res;
                try {
                    node = (Communication_itf) registry.lookup("Node" + i);
                    res = node.AcquireMutexOnElement(nodeId, index, localRequestTimestamp, System.currentTimeMillis());
                } catch (NotBoundException | RemoteException e) {
                    continue; //Le noeud n'existe probablement plus
                }

                if (res == ResponseType.FAIL) {
                    returnValue = false;
                    break;
                }
            }
        }

        if(returnValue) System.out.println("Node " + nodeId + " succeed to acquire mutex for element " + index);
        else {
            System.out.println("Node " + nodeId + " fail to acquire mutex for element " + index + "(" + memory.isElementLocked(index)+")");
        }

        return returnValue;
    }

    public void ReleaseMutexOnAllNodes(int index) {
        System.out.println("Node " + nodeId + " start to release mutex for element " + index);

        memory.releaseElement(index);

        for(int i = 1; i <= nNode; i++) {
            if (i == nodeId) continue;

            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + i);
                node.PropagateModification(index, memory.getValue(index));
                node.ReleaseMutexOnElement(index);
            }
            catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        localEltRequestIndex = -1;
        System.out.println("Node " + nodeId + " has released mutex for element " + index);

        if(!lNodeWaiting.isEmpty()){
            Integer nodeToWakeUpId = lNodeWaiting.get(0);
            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + nodeToWakeUpId);
                node.WakeUp(lNodeWaiting, System.currentTimeMillis());
                lNodeWaiting.clear();
            } catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }
    }

    public void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long logicalTimestamp) {
        System.out.println("wake up node : " + nodeId);

        lNodeAlreadyWaiting.remove(0);
        lNodeWaiting = lNodeAlreadyWaiting;
        semaphore.release();
    }
}
