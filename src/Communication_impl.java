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
    Semaphore semaphore2;
    long localLogicalTimestamp;
    long localRequestTimestamp;
    boolean waiting;
    ReentrantLock lock;
    int elementOwned;
    int lockedByOtherElement;

    public Communication_impl(Memory memory, int nNode, int nodeId, Registry registry){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
        this.registry = registry;

        this.lNodeWaiting = new ArrayList<>();
        this.semaphore = new Semaphore(0);
        localLogicalTimestamp = 0; //A voir
        waiting = false;
        lock = new ReentrantLock();
        semaphore2 = new Semaphore(1);
        elementOwned = -1;
        lockedByOtherElement = -1;
        System.out.println("Fin constructeur node " + nodeId);
    }

    @Override
    public ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp) throws RemoteException {
        lock.lock();
        ResponseType res;

        //Si ce node veut un verrou sur le même élément (conflit) et qu'il a commencé avant -> Echec
        if(!waiting && localEltRequestIndex == index && ((requestTimestamp == localRequestTimestamp && nodeWhoRequestId < nodeId) || requestTimestamp < localRequestTimestamp)){
            lNodeWaiting.add(nodeWhoRequestId);
            res = ResponseType.FAIL;
        }
        else{
            System.out.println(waiting + " " + localEltRequestIndex + " " + index + " " + requestTimestamp + " " + localRequestTimestamp + " " + nodeWhoRequestId + " " + nodeId);
            lockedByOtherElement = index;
            memory.lockElement(index, nodeWhoRequestId);
            res = ResponseType.OK;
        }

        localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
        lock.unlock();
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
        if(memory.isElementLocked(index)){
            System.out.println("Node " + nodeId + " fail to acquire mutex for element " + index + " because node " + memory.whoLockedElement(index) + "already locked");
            Communication_itf node;
            try {
                node = (Communication_itf) registry.lookup("Node" + memory.whoLockedElement(index));
                node.WaitOn(nodeId);
            } catch (NotBoundException | RemoteException e) {

            }
            return false;
        }

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

        if(returnValue) {
            System.out.println("Node " + nodeId + " succeed to acquire mutex for element " + index);
        }
        else {
            System.out.println("Node " + nodeId + " fail to acquire mutex on node " + failNode + " for element " + index + "(" + memory.isElementLocked(index)+")");
        }

        return returnValue;
    }

    public void ReleaseMutexOnAllNodes(int index) {
        lock.lock();

        System.out.println("Node " + nodeId + " start to release mutex for element " + index);

        memory.releaseElement(index);

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
                System.out.println("Node " + nodeId + " wake up node " + nodeToWakeUpId);
                node.WakeUp(lNodeWaiting, localLogicalTimestamp);
                lNodeWaiting.clear();
            } catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        System.out.println("Node " + nodeId + " has released mutex for element " + index);
        lock.unlock();
    }

    public void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long requestTimestamp) {

        lNodeAlreadyWaiting.remove(0);
        lNodeWaiting.addAll(lNodeAlreadyWaiting);
        System.out.println("Other node who continue to wait on node " + nodeId + " : "+ lNodeWaiting);
        semaphore.release();
    }

    @Override
    public void WaitOn(int requestNodeId) throws RemoteException {
        lNodeWaiting.add(requestNodeId);
    }
}
