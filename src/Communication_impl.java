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
    boolean debug;
    int lastWakeUpNode;

    public Communication_impl(Memory memory, int nNode, int nodeId, Registry registry, boolean debug){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
        this.registry = registry;
        this.debug = debug;

        this.lNodeWaiting = new ArrayList<>();
        this.semaphore = new Semaphore(0);
        localLogicalTimestamp = 0; //A voir
        waiting = false;
        lock = new ReentrantLock();
        semaphore2 = new Semaphore(1);
        elementOwned = -1;
        lockedByOtherElement = -1;
        lastWakeUpNode = -1;

        if(debug) System.out.println("Fin constructeur node " + nodeId);
    }

    @Override
    public ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp) throws RemoteException {
        lock.lock();
        ResponseType res;

        //Si ce node veut un verrou sur le même élément (conflit) et qu'il a commencé avant -> Echec
        if(lastWakeUpNode != nodeWhoRequestId && !waiting && localEltRequestIndex == index && ((requestTimestamp == localRequestTimestamp && nodeWhoRequestId < nodeId) || requestTimestamp < localRequestTimestamp) || index == elementOwned){
            lNodeWaiting.add(nodeWhoRequestId);
            res = ResponseType.FAIL;
        }
        else{
            lockedByOtherElement = index;
            memory.lockElement(index, nodeWhoRequestId);
            res = ResponseType.OK;
        }

        localLogicalTimestamp = Math.max(localLogicalTimestamp, requestTimestamp)+1;
        lock.unlock();
        return res;
    }

    @Override
    public void ReleaseMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp) throws RemoteException {
        if(lastWakeUpNode == nodeWhoRequestId){
            lastWakeUpNode = -1;
        }
        memory.releaseElement(index);
    }

    @Override
    public void PropagateModification(int index, int value, long requestTimestamp) {
        if(debug) System.out.println("Received modification of element ("+ index +") = " + value);
        memory.setValue(index, value);
    }

    public void AcquireMutexOnAllNodesLoop(int index) {
        localRequestTimestamp = localLogicalTimestamp;
        localEltRequestIndex = index;

        while(!AcquireMutexOnAllNodes(index)) {
            //On attend avant de retenter d'obtenir le verrou pour chaque node
            waiting = true;
            try {
                if(debug) System.out.println("Node " + nodeId + " waiting");
                semaphore.acquire();
                waiting = false;
                if(debug) System.out.println("Node " + nodeId + " awaked");
                //semaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    boolean AcquireMutexOnAllNodes(int index) {
        if(debug) System.out.println("Node " + nodeId + " trying to acquire mutex for element " + index);

        if(lastWakeUpNode != -1){
            if(debug) System.out.println("Node " + nodeId + " fail to acquire mutex for element " + index + " because node " + lastWakeUpNode + " not yet finished after wake up");

            Communication_itf node;
            try {
                node = (Communication_itf) registry.lookup("Node" + lastWakeUpNode);
                node.WaitOn(nodeId);
            } catch (NotBoundException | RemoteException e) {

            }
            return false;
        }

        if(memory.isElementLocked(index)){
            if(debug) System.out.println("Node " + nodeId + " fail to acquire mutex for element " + index + " because node " + memory.whoLockedElement(index) + " already locked");

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
            elementOwned = index;
            if(debug) System.out.println("Node " + nodeId + " succeed to acquire mutex for element " + index);
        }
        else {
            if(debug) System.out.println("Node " + nodeId + " fail to acquire mutex on node " + failNode + " for element " + index + "(" + memory.isElementLocked(index)+")");
        }

        return returnValue;
    }

    public void ReleaseMutexOnAllNodes(int index) {
        lock.lock();

        if(debug) System.out.println("Node " + nodeId + " start to release mutex for element " + index);

        for(int i = 1; i <= nNode; i++) {
            if (i == nodeId) continue;

            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + i);
                node.PropagateModification(index, memory.getValue(index), localLogicalTimestamp);
                node.ReleaseMutexOnElement(nodeId, index, System.currentTimeMillis());
            }
            catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        memory.releaseElement(index);
        localEltRequestIndex = -1;
        elementOwned = -1;

        if(!lNodeWaiting.isEmpty()){
            Integer nodeToWakeUpId = lNodeWaiting.get(0);
            Communication_itf node = null;
            try {
                node = (Communication_itf) registry.lookup("Node" + nodeToWakeUpId);
                if(debug) System.out.println("Node " + nodeId + " wake up node " + nodeToWakeUpId);
                lastWakeUpNode = nodeToWakeUpId;
                node.WakeUp(lNodeWaiting, localLogicalTimestamp);
                lNodeWaiting.clear();
            } catch (NotBoundException | RemoteException e) {
                //e.printStackTrace();
            }
        }

        if(debug) System.out.println("Node " + nodeId + " has released mutex for element " + index);
        lock.unlock();
    }

    public void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long requestTimestamp) {

        lNodeAlreadyWaiting.remove(0);
        lNodeWaiting.addAll(lNodeAlreadyWaiting);
        if(debug) System.out.println("Other node who continue to wait on node " + nodeId + " : "+ lNodeWaiting);
        semaphore.release();
    }

    @Override
    public void WaitOn(int requestNodeId) throws RemoteException {
        lNodeWaiting.add(requestNodeId);
    }
}
