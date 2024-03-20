import java.util.ArrayList;
import java.util.Arrays;

public class Communication_impl implements Communication_itf {

    Memory memory;
    int nNode;
    int nodeId;

    public Communication_impl(Memory memory, int nNode, int nodeId){
        this.memory = memory;
        this.nNode = nNode;
        this.nodeId = nodeId;
    }

    @Override
    public void AcquireMutexOnElement(int index, int timestamp) {
        memory.lockElement(index);
    }

    @Override
    public void ReleaseMutexOnElement(int index, int timestamp) {

    }

    @Override
    public void PropagateModification(int index, int value) {

    }
}
