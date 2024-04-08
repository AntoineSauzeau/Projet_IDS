import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Communication_impl implements Communication_itf {

    Memory memory;
    int nNode;
    int nodeId;
    Registry registry;
    int localEltRequestIndex;
    ArrayList<Integer> lNodeWaiting;
    Semaphore semaphore;
    long localLogicalTimestamp;
    long localRequestTimestamp;
    boolean waiting;

    public Communication_impl(Memory memory, int nNode, int nodeId, Registry registry){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
        this.registry = registry;

        this.lNodeWaiting = new ArrayList<>();
        this.semaphore = new Semaphore(0);
        localLogicalTimestamp = 0; //A voir
        waiting = false;
    }

    @Override
    public ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp) throws RemoteException {
        ResponseType res;

        //Si ce node veut un verrou sur le même élément (conflit) et qu'il a commencé avant -> Echec
        if(!waiting && localEltRequestIndex == index && ((requestTimestamp == localRequestTimestamp && nodeWhoRequestId < nodeId) || requestTimestamp < localRequestTimestamp)){
            lNodeWaiting.add(nodeWhoRequestId);
            res = ResponseType.FAIL;
        }
        else{
            memory.lockElement(index);
            res = ResponseType.OK;
        }

        localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
        return res;
    }

    @Override
    public void ReleaseMutexOnElement(int index, long requestTimestamp) throws RemoteException {
        memory.releaseElement(index);
        //localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
    }

    @Override
    public void PropagateModification(int index, int value, long requestTimestamp) {
        System.out.println("Received modification of element ("+ index +") = " + value);
        memory.setValue(index, value);
        //localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
    }

    public void AcquireMutexOnAllNodesLoop(int index) {
        localRequestTimestamp = localLogicalTimestamp;
        localEltRequestIndex = index;

        while(!AcquireMutexOnAllNodes(index)) {
            //On attend avant de retenter d'obtenir le verrou pour chaque node
            waiting = true;
            try {
                System.out.println("Node " + nodeId + " waiting");
                semaphore.acquire();
                waiting = false;
                System.out.println("Node "+ nodeId + " awaked");
                //semaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    boolean AcquireMutexOnAllNodes(int index) {

        System.out.println("Node " + nodeId + " trying to acquire mutex for element " + index);
        boolean returnValue = true;

        localLogicalTimestamp++;
        int failNode = -1;

        for (int i = 1; i <= nNode; i++) {
            if (i == nodeId) continue;

            Communication_itf node;
            ResponseType res;
            try {
                node = (Communication_itf) registry.lookup("Node" + i);
                res = node.AcquireMutexOnElement(nodeId, index, localRequestTimestamp);
            } catch (NotBoundException | RemoteException e) {
                continue; //Le noeud n'existe probablement plus
            }

            if (res == ResponseType.FAIL) {
                returnValue = false;
                failNode = i;
                break;
            }
        }

        if(returnValue) System.out.println("Node " + nodeId + " succeed to acquire mutex for element " + index);
        else {
            System.out.println("Node " + nodeId + " fail to acquire mutex on node " + failNode + " for element " + index + "(" + memory.isElementLocked(index)+")");
        }

        return returnValue;
    }

    public void ReleaseMutexOnAllNodes(int index) {

        System.out.println("Node " + nodeId + " start to release mutex for element " + index);

        memory.releaseElement(index);
        //localLogicalTimestamp++;

        for(int i = 1; i <= nNode; i++) {
            if (i == nodeId) continue;

            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + i);
                node.PropagateModification(index, memory.getValue(index), localLogicalTimestamp);
                node.ReleaseMutexOnElement(index, System.currentTimeMillis());
            }
            catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        localEltRequestIndex = -1;

        if(!lNodeWaiting.isEmpty()){
            //localLogicalTimestamp++;
            Integer nodeToWakeUpId = lNodeWaiting.get(0);
            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + nodeToWakeUpId);
                node.WakeUp(lNodeWaiting, localLogicalTimestamp);
                lNodeWaiting.clear();
            } catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        System.out.println("Node " + nodeId + " has released mutex for element " + index);

    }

    public void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long requestTimestamp) {
        System.out.println("WakeUp() node : " + nodeId);

        lNodeAlreadyWaiting.remove(0);
        lNodeWaiting.addAll(lNodeAlreadyWaiting);
        System.out.println("Other node who continue to wait : "+ lNodeWaiting);
        semaphore.release();

        //localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
    }
}
