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
    public ResponseType RequestMutexForElement(int index, long timestamp) {

        if(localEltRequestIndex == index && timestamp > localRequestTimestamp){
            return ResponseType.FAIL;
        }
        return ResponseType.OK;
    }

    @Override
    public ResponseType AcquireMutexOnElement(int index, long timestamp) {
        memory.lockElement(index);
        return ResponseType.OK;
    }

    @Override
    public void ReleaseMutexOnElement(int index) throws RemoteException {
        memory.releaseElement(index);
        localEltRequestIndex = -1;
    }

    @Override
    public void PropagateModification(int index, int value) {

    }

    public void AcquireMutexOnAllNodes(int index) throws NotBoundException, RemoteException, InterruptedException {

        localRequestTimestamp = System.currentTimeMillis();
        localEltRequestIndex = index;

        boolean stage1Failed = false;

        //Stage 1
        for(int i = 0; i < nNode; i++){
            if(i == nodeId) continue;
            Communication_impl node = (Communication_impl) registry.lookup("Node"+i);
            ResponseType res = node.RequestMutexForElement(index, localRequestTimestamp);
            if(res == ResponseType.FAIL){
                stage1Failed = true;
            }
        }
        if(stage1Failed == false) {
            //Stage 2
            for (int i = 0; i < nNode; i++) {
                if (i == nodeId) continue;
                Communication_impl node = (Communication_impl) registry.lookup("Node" + i);
                node.AcquireMutexOnElement(index, localRequestTimestamp);
            }
        }
        else{
            //On retente au bout d'un certain temps si il y a eu un conflit
            Thread.sleep(100);
            AcquireMutexOnAllNodes(index);
        }
    }

    public void ReleaseMutexOnAllNodes(int index) throws NotBoundException, RemoteException {
        for(int i = 0; i < nNode; i++) {
            if (i == nodeId) continue;
            Communication_impl node = (Communication_impl) registry.lookup("Node" + i);
            node.ReleaseMutexOnElement(index);
        }
    }
}
