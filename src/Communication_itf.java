import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Communication_itf extends Remote {

    enum ResponseType {OK, FAIL};

    ResponseType AcquireMutexOnElement(int nodeWhoRequestId, int index, long requestTimestamp) throws RemoteException;

    void ReleaseMutexOnElement(int index, long requestTimestamp) throws RemoteException;

    void PropagateModification(int index, int value, long requestTimestamp) throws RemoteException;

    void WakeUp(ArrayList<Integer> lNodeAlreadyWaiting, long requestTimestamp) throws RemoteException;

    void WaitOn(int requestNodeId) throws RemoteException;

}
