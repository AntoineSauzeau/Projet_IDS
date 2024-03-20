import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Memory {

    final static int MEMORY_SIZE = 1000;

    Integer[] memory;

    Lock[] lMutex;
    Condition[] lCond;

    public Memory(){
        memory = new Integer[MEMORY_SIZE];
        lMutex = new Lock[MEMORY_SIZE];
        lCond = new Condition[MEMORY_SIZE];
    }

    public int read(int index){
        //Verif si possible
        return memory[index];
    }

    public void write(int index, int value){
        //Verif si possible

        if(memory.length <= index) {
            memory[index] = value;
        }
    }

    public void lockElement(int index){

    }

    public void releaseElement(int index){

    }
}
