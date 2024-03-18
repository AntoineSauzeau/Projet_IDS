import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Memory {

    ArrayList<Integer> memory;

    ArrayList<Semaphore> mutexs;

    Memory(){
        memory = new ArrayList<>();
        mutexs = new ArrayList<>();
        //new Semaphore(1);
    }

    public int read(int index){
        //Verif si possible
        return memory.get(index);
    }

    public void write(int index, int value){
        //Verif si possible

        if(memory.size() <= index){

            memory.add(value);

        }else{

            memory.set(index, value);
        }


    }


}
