import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

import com.zeroc.Ice.*;
import TextSorter.MasterInterface;
import TextSorter.WorkerInterfacePrx;

import java.io.*;
import java.util.*;

public class Master implements MasterInterface {
    private final int numThreads;  // Número de hilos a utilizar
    private final List<Thread> threads;  // Lista para almacenar los hilos
    private static List<WorkerInterfacePrx> workers;  // Lista para almacenar las referencias a los esclavos
    private static int nodes;
    private static int counter = 0;
    private static ArrayList<String[]> subArrays;
    private static List<String> sorted;
    private static ThreadPool threadPool;
    private static String fileName;
    private static Scanner reader;
    private static String[] arr;
    private static long startConn;
    private static long startSort;
    private static long startMon;
    private static String[] args;
    private static final double MS_PER_BYTE = 0.000018;
    private static boolean alreadyReady = false;
    private static com.zeroc.Ice.Communicator communicator;

    public Master(int numThreads) {
        this.numThreads = numThreads;
        this.threads = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.subArrays = new ArrayList<>();
        this.sorted = new ArrayList<>();
    }

    public static void main(String[] args) {
        reader = new Scanner(System.in);
        Master.args = args;

        menu();
    }

    public static long obtenerPesoArchivo(String rutaArchivo) {
        Path path = Paths.get(rutaArchivo);

        try {
            // Obtener el tamaño del archivo en bytes
            return Files.size(path);
        } catch (IOException e) {
            // Manejar la excepción en caso de error al obtener el tamaño del archivo
            e.printStackTrace();
            return -1; // Valor negativo indica un error
        }
    }

    public static void fileMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Type the data file name: ");
        fileName = scanner.nextLine();
        Long pesoArchivo = obtenerPesoArchivo("doc/" + fileName);
        System.out.println("File size: " + pesoArchivo + " bytes");
        arr = readDataFromFile("doc/" + fileName);
    }

    public static void menu() {
        boolean flag = true;

        while (flag) {
            System.out.println("\nSelect the amount of nodes you want: " +
                    "\na -> 1 node" +
                    "\nb -> 4 nodes" +
                    "\nc -> 8 nodes" +
                    "\nd -> 12 nodes" +
                    "\nt -> 2 nodes" +
                    "\ne -> shut down");
            String opt = reader.next();

            switch (opt) {
                case "a":
                    flag = false;
                    nodes = 1;
                    break;

                case "b":
                    flag = false;
                    nodes = 4;
                    break;

                case "c":
                    flag = false;
                    nodes = 8;
                    break;

                case "d":
                    flag = false;
                    nodes = 12;
                    break;

                case "t":
                    flag = false;
                    String nodesS = reader.next();
                    nodes = Integer.parseInt(nodesS);
                    break;

                case "e":
                    flag = false;
                    communicator.shutdown();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Bad option");
                    break;
            }
        }

        fileMenu();

        // si nodos = 0 no sortea nada, mal input
        if (nodes != 0) {

            // si nodos = 1 lo sorteo aqui mismo, monolítico
            if (nodes == 1) {
                threadPool = new ThreadPool(arr);

                try {
                    //Medición
                    startMon = System.currentTimeMillis();
                    threadPool.execute();
                    // System.currentInMillis - startMon (viejo)
                } catch (InterruptedException | ExecutionException interruptedException) {
                    System.out.println(interruptedException.getMessage());
                }
            } else {
                startConn = System.currentTimeMillis();

                if (!alreadyReady) {
                    // inicializamos ICE para conectarnos con los workers distribuidos
                    try {
                        communicator = com.zeroc.Ice.Util.initialize(args, "master.cfg");

                        // communicator con propiedades de callback
                        communicator.getProperties().setProperty("Ice.Default.Package",
                                "TextSorter");

                        // Crear el adaptador y agregar el objeto maestro - Los nombres son arbitrarios
                        // los colocamos por convención
                        ObjectAdapter adapter = communicator.createObjectAdapter("Master");
                        Master master = new Master(16);

                        adapter.add(master, com.zeroc.Ice.Util.stringToIdentity("Master"));
                        adapter.activate();

                        // Imprimir mensaje indicando que el servidor está listo
                        System.out.println("'Master' awaiting for connections...");

                        alreadyReady = true;
                        // Esperar a que se cierre el servidor
                        communicator.waitForShutdown();

                        // Apagar el Communicator cuando se cierre el servidor
                        communicator.destroy();
                    } catch (com.zeroc.Ice.ObjectNotExistException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void doProcess(String[] arrayToSort) {
        createTasks(arrayToSort);
        startSort = System.currentTimeMillis(); // no tener en cuenta particion de tareas
        launchWorkers();
    }

    @Override
    public void sort(Current current) {
        String[] array = new String[sorted.size()];

        threadPool = new ThreadPool(sorted.toArray(array));

        try {
            threadPool.execute();
        } catch (InterruptedException | ExecutionException interruptedException) {
            System.out.println(interruptedException.getMessage());
        }
    }

    public static void notifySorted() {
        System.out.println("\nFinal array sorted!");

        if (nodes != 1) {
            System.out.println("Process time (with connection) " + (System.currentTimeMillis() - startConn) + "ms");
            System.out.println("Process time (no connection): " + (System.currentTimeMillis() - startSort) + "ms");
        } else {
            System.out.println("Process time (mono) " + (System.currentTimeMillis() - startMon) + "ms");
        }

        subArrays = new ArrayList<>();
        sorted = new ArrayList<>();
        counter = 0;

        String outputFilePath = "doc/sorted_" + fileName;
        System.out.println("Writing...");

        writeDataToFile(outputFilePath, threadPool.getSorted());
        menu();
    }

    // creamos los subarrays para los workers
    public static void createTasks(String[] bigArray) {
        int size = (int) Math.ceil((double) bigArray.length / nodes);

        for (int i = 0; i < nodes; i++) {
            int start = i * size;
            int end = Math.min(start + size, bigArray.length);

            if (start > end) {
                break;
            }

            String[] subArr = Arrays.copyOfRange(bigArray, start, end);
            // añado subarrays para luego mandarselos a los workers
            subArrays.add(subArr);
        }

        // elimino el grande
        arr = null;
    }

    // enviamos a cada worker su respectivo subarray para que lo ordene
    public static void launchWorkers() {
        ExecutorService executor = Executors.newFixedThreadPool(nodes); // 16 hilos en el pool

        for (int i = 0; i < subArrays.size(); i++) {
            System.out.println("Send task to worker " + i);
            final int index = i;

            executor.submit(() -> {
                workers.get(index).processTask(subArrays.get(index));
            });
        }

        subArrays = new ArrayList<>();
        executor.shutdownNow(); // No aceptará nuevas tareas
    }

    @Override
    public void attachWorker(WorkerInterfacePrx subscriber, Current current) {
        workers.add(subscriber);
        System.out.println("\nWorker subscribed: " + subscriber.toString());

        if (workers.size() == nodes) {
            System.out.println("All necessary nodes connected");
            // startSort = System.currentTimeMillis(); -> tener en cuenta particion de tareas

            doProcess(arr);
        }
    }

    @Override
    public void addPartialResult(String[] res, Current current) {
        counter++;
        System.out.println("Partial result...");

        int chunkSize = res.length / 2;

        String[] chunk1 = Arrays.copyOfRange(res, 0, chunkSize);
        sorted.addAll(Arrays.asList(chunk1));

        String[] chunk2 = Arrays.copyOfRange(res, chunkSize, res.length);
        sorted.addAll(Arrays.asList(chunk2));

        if (counter == nodes) {
            sort(current);
        }
    }

    @Override
    public void detachWorker(WorkerInterfacePrx subscriber, Current current) {
        // Destruir el objeto proxy del worker
        subscriber.ice_getConnection().close(ConnectionClose.Forcefully);
        workers.remove(subscriber);
        System.out.println("\nWorker unsubscribed: " + subscriber.toString());
    }

    private static String[] readDataFromFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            List<String> words = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                words.addAll(Arrays.asList(line.trim().split("\\s+")));
            }
            br.close();
            return words.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private static void writeDataToFile(String filePath, String[] data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write each element of the sorted data on a new line
            for (String element : data) {
                writer.write(element + "\n");
            }

            writer.close();
            System.out.println("File wrote correctly");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
