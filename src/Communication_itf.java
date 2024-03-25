import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Communication_itf extends Remote {

    enum ResponseType {OK, FAIL};

    ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp, long logicalTimestamp) throws RemoteException;

    void ReleaseMutexOnElement(int index) throws RemoteException;

    void PropagateModification(int index, int value, long logicalTimestamp) throws RemoteException;

    void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long logicalTimestamp) throws RemoteException;

}
