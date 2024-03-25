import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Memory {

    final static int MEMORY_SIZE = 1000;

    int[] memory;

    boolean lMutex[];

    public Memory(){
        memory = new int[MEMORY_SIZE];
        lMutex = new boolean[MEMORY_SIZE];
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



    public void lockElement(int index){
        lMutex[index] = true;
    }

    public void releaseElement(int index){
        lMutex[index] = false;
    }

    public boolean isElementLocked(int index){
        return lMutex[index];
    }
}
