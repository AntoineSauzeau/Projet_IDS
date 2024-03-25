import java.rmi.AccessException;
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

    public Communication_impl(Memory memory, int nNode, int nodeId, Registry registry){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
        this.registry = registry;
    }

    @Override
    public ResponseType AcquireMutexOnElement(int index, long timestamp) throws RemoteException {
        //Si ce node veut un verrou sur le même élément (conflit) et qu'il a commencé avant -> Echec
        if(localEltRequestIndex == index && timestamp > localRequestTimestamp){
            return ResponseType.FAIL;
        }
        memory.lockElement(index);
        return ResponseType.OK;
    }

    @Override
    public void ReleaseMutexOnElement(int index) throws RemoteException {
        memory.releaseElement(index);
    }

    @Override
    public void PropagateModification(int index, int value) {
        memory.setValue(index, value);
    }

    public void AcquireMutexOnAllNodesLoop(int index) {
        while (AcquireMutexOnAllNodes(index) == false) {
            System.out.println(localRequestTimestamp);
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
            localRequestTimestamp = System.currentTimeMillis();
            localEltRequestIndex = index;

            for (int i = 1; i <= nNode; i++) {
                if (i == nodeId) continue;

                Communication_itf node;
                ResponseType res;
                try {
                    node = (Communication_itf) registry.lookup("Node" + i);
                    res = node.AcquireMutexOnElement(index, localRequestTimestamp);
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
        else System.out.println("Node " + nodeId + " fail to acquire mutex for element " + index);

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
                e.printStackTrace();
            }
        }

        localEltRequestIndex = -1;
        System.out.println("Node " + nodeId + " have released mutex for element " + index);
    }
}
