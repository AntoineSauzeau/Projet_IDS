public class Utility {

    Communication_impl communicationImpl;
    Memory memory;

    Utility(Memory memory){
        this.memory;
    }

    public int read(int index){

        return memory.read(index);
    }

    public void write(int index, int value){
        write(index, value);
    }
}
