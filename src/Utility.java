import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Utility {

    Communication_impl communicationImpl;
    Memory memory;

    public Utility(Memory memory, Communication_impl communicationImpl){
        this.memory = memory;
        this.communicationImpl = communicationImpl;
    }

    public int read(int index) {
        return memory.read(index);
    }

    public void write(int index, int value){
        write(index, value);
    }
}
