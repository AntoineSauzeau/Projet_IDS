import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Communication_itf extends Remote {

    void AcquireMutexOnElement(int index, int timestamp) throws RemoteException;

    void ReleaseMutexOnElement(int index, int timestamp) throws RemoteException;

    void PropagateModification(int index, int value) throws RemoteException;
}
