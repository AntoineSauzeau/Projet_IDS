import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Memory {

    final static int MEMORY_SIZE = 1000;

    int[] memory;
    int[] lockedBy;

    boolean lMutex[];

    public Memory(){
        memory = new int[MEMORY_SIZE];

        lockedBy = new int[MEMORY_SIZE];
        lMutex = new boolean[MEMORY_SIZE];
        for(int i = 0; i < lockedBy.length; i++){
            lockedBy[i] = -1;
            lMutex[i] = false;
        }
    }

    public int getValue(int index){
        //Verif si possible
        return memory[index];
    }

    public void setValue(int index, int value){
        //Verif si possible

        if(memory.length > index) {
            memory[index] = value;
        }
    }

    public void printMemory(){
        System.out.println("Memory :");
        for (int i = 0; i < MEMORY_SIZE; i++){
            System.out.print("[" + i + "," + memory[i] + "] ");
        }
        System.out.println("");
    }



    public void lockElement(int index, int ownerId){
        lMutex[index] = true;
        lockedBy[index] = ownerId;
    }

    public void releaseElement(int index){
        lMutex[index] = false;
    }

    public boolean isElementLocked(int index){
        return lMutex[index];
    }

    public int whoLockedElement(int index){
        return lockedBy[index];
    }
}
