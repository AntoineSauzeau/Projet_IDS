import java.rmi.Remote;

public interface Communication_itf implements Remote {

    void AcquireMutexOnElement(int index);

    void ReleaseMutexOnElement(int index);

    void PropagateModification(int index, int value);
}
