import java.util.ArrayList;

public class Communication_impl implements Communication_itf {

    Memory memory;
    ArrayList<Integer> listOtherNodeIds;

    Communication_impl(Memory memory){
        this.memory = memory;
    }

    @Override
    public void AcquireMutexOnElement(int index) {

    }

    @Override
    public void ReleaseMutexOnElement(int index) {

    }

    @Override
    public void PropagateModification(int index) {

    }
}
