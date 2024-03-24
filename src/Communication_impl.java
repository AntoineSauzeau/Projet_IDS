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
    public ResponseType AcquireMutexOnElement(int index, long timestamp) {
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

    public void AcquireMutexOnAllNodesLoop(int index) throws NotBoundException, RemoteException, InterruptedException {
        while(AcquireMutexOnAllNodes(index) == false){
            //On attend avant de retenter d'obtenir le verrou pour chaque node
            Thread.sleep(100);
        }
    }

    public boolean AcquireMutexOnAllNodes(int index) throws NotBoundException, InterruptedException, RemoteException {

        if(memory.isElementLocked(index)) return false;

        localRequestTimestamp = System.currentTimeMillis();
        localEltRequestIndex = index;

        for (int i = 0; i < nNode; i++) {
            if (i == nodeId) continue;

            Communication_impl node = (Communication_impl) registry.lookup("Node" + i);
            ResponseType res = node.AcquireMutexOnElement(index, localRequestTimestamp);

            if(res == ResponseType.FAIL) return false;
        }

        return true;
    }

    public void ReleaseMutexOnAllNodes(int index) throws NotBoundException, RemoteException {
        for(int i = 0; i < nNode; i++) {
            if (i == nodeId) continue;
            Communication_impl node = (Communication_impl) registry.lookup("Node" + i);
            node.ReleaseMutexOnElement(index);
        }

        localEltRequestIndex = -1;
    }
}
