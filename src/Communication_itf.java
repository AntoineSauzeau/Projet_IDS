import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Communication_itf extends Remote {

    enum ResponseType {OK, FAIL};

    ResponseType RequestMutexForElement(int index, long timestamp) throws RemoteException;

    ResponseType AcquireMutexOnElement(int index, long timestamp) throws RemoteException;

    void ReleaseMutexOnElement(int index) throws RemoteException;


    void PropagateModification(int index, int value) throws RemoteException;
}
