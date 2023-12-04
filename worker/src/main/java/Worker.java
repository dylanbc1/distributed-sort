import java.util.*;
import java.util.concurrent.*;

import TextSorter.WorkerInterfacePrx;
import com.zeroc.Ice.*;
import TextSorter.WorkerInterface;

public class Worker implements WorkerInterface {
    private static ThreadPool threadPool;
    private static Long startTime;
    private static TextSorter.MasterInterfacePrx masterInterfacePrx;
    private static WorkerInterfacePrx workerInterfacePrx;
    private static Scanner reader;
    private static com.zeroc.Ice.Communicator globalCommunicator;

    public static void main(String[] args) {
        reader = new Scanner(System.in);

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "worker.cfg")) {
            // communicator con propiedades de callback
            communicator.getProperties().setProperty("Ice.Default.Package", "com.zeroc.demos.Ice.worker");

            // PRX del master
            masterInterfacePrx = TextSorter.MasterInterfacePrx.uncheckedCast(communicator.propertyToProxy("MasterInterface.Proxy")).ice_oneway();

            // Crear el adaptador y agregar el objeto maestro
            ObjectAdapter adapter = communicator.createObjectAdapter("Worker");
            Worker worker = new Worker();

            adapter.add(worker, com.zeroc.Ice.Util.stringToIdentity("Worker"));
            adapter.activate();

            // PRX de este worker
            workerInterfacePrx =
                    WorkerInterfacePrx.uncheckedCast(adapter.createProxy(
                            com.zeroc.Ice.Util.stringToIdentity("Worker")));

            globalCommunicator = communicator;

            startTime = System.currentTimeMillis();

            // se suscribe
            System.out.println("va a hacer attach");
            masterInterfacePrx.attachWorker(workerInterfacePrx);

            // Esperar a que se cierre el servidor
            communicator.waitForShutdown();

            // Apagar el Communicator cuando se cierre el servidor
            communicator.destroy();
        } catch (com.zeroc.Ice.ObjectNotExistException ex) {
            ex.printStackTrace();
        }
    }

    // observer
    public static void notifyClient() {
        // Escribir el resultado ordenado en un nuevo archivo de texto
        // obtiene el array ordenado
        long start = System.currentTimeMillis();
        String[] sortedArr = threadPool.getSorted();
        System.out.println("Sort worker: " + (System.currentTimeMillis() - start));

        System.out.println("Sorted!");

        masterInterfacePrx.addPartialResult(sortedArr);
        unsubscribe();

        boolean flag = true;

        while (flag) {
            System.out.println("\na -> Conectarse nuevamente con Master" +
                    "\nb -> Apagar worker");
            String opt = reader.nextLine();

            switch (opt) {
                case "a":
                    flag = false;
                    subscribe();
                    break;
                case "b":
                    flag = false;
                    globalCommunicator.shutdown();
                    break;
                default:
                    System.out.println("Bad option");
                    break;
            }
        }
    }

    public static void unsubscribe() {
        masterInterfacePrx.detachWorker(workerInterfacePrx);
    }

    public static void subscribe() {
        masterInterfacePrx.attachWorker(workerInterfacePrx);
    }

    @Override
    public void subscribe(Current current) {
        masterInterfacePrx.attachWorker(workerInterfacePrx);
    }

    @Override
    public void processTask(String[] lines, Current current) {
        System.out.println("Sorting...");
        Worker.threadPool = new ThreadPool(lines);

        try {
            threadPool.execute();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
