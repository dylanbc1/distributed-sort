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
    private int counter = 0;
    private static ArrayList<String[]> subArrays;
    private static List<String> sorted;
    private static ThreadPool threadPool;
    private static String fileName;
    private static Scanner reader;
    private static String[] arr;
    private static long startConn;
    private static long startSort;
    private static com.zeroc.Ice.Communicator globalCommunicator;

    public Master(int numThreads) {
        this.numThreads = numThreads;
        this.threads = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.subArrays = new ArrayList<>();
        this.sorted = new ArrayList<>();
    }

    public static void main(String[] args) {
        reader = new Scanner(System.in);

        menu();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el nombre del archivo de datos: ");
        fileName = scanner.nextLine();
        Long pesoArchivo = obtenerPesoArchivo("doc/" + fileName);
        System.out.println("El tamaño del archivo es: " + pesoArchivo + " bytes");
        arr = readDataFromFile("doc/" + fileName);

        // si nodos = 0 no sortea nada, mal input
        if (nodes != 0) {

            // si nodos = 1 lo sorteo aqui mismo, monolítico
            if (nodes == 1) {
                threadPool = new ThreadPool(arr);

                try {
                    //Medición
                    long start = System.currentTimeMillis();
                    threadPool.execute();
                    System.out.println("Sort monolitico: " + (System.currentTimeMillis() - start) + "ms");

                } catch (InterruptedException | ExecutionException interruptedException) {
                    System.out.println(interruptedException.getMessage());
                }
            } else {
                startConn = System.currentTimeMillis();
                // inicializamos ICE para conectarnos con los workers distribuidos
                try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "master.cfg")) {
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
                    System.out.println("Servidor 'Master' listo para recibir conexiones...");
                    globalCommunicator = communicator;

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

    public static void menu() {
        boolean flag = true;

        while (flag) {
            System.out.println("Selecciona la cantidad de nodos que deseas utilizar para ordenar: " +
                    "\na -> 1 nodo" +
                    "\nb -> 4 nodos" +
                    "\nc -> 8 nodos" +
                    "\nd -> 12 nodos" +
                    "\ne -> exit");
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
                    nodes = 2;
                    break;

                case "e":
                    flag = false;
                    globalCommunicator.shutdown();
                    break;

                default:
                    System.out.println("Bad option");
                    break;
            }
        }
    }

    public static void doProcess(String[] arrayToSort) {
        createTasks(arrayToSort);
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
        System.out.println("Latencia sorted con conexión: " + (System.currentTimeMillis() - startConn) + "ms");
        System.out.println("Latencia sorted (no conexión): " + (System.currentTimeMillis() - startSort) + "ms");

        String outputFilePath = "doc/sorted_" + fileName;
        System.out.println("llama a escribir...");

        writeDataToFile(outputFilePath, threadPool.getSorted());
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
    }

    // enviamos a cada worker su respectivo subarray para que lo ordene
    public static void launchWorkers() {
        System.out.println("subarrays length: " + subArrays.size());

        ExecutorService executor = Executors.newFixedThreadPool(16); // 16 hilos en el pool

        for (int i = 0; i < subArrays.size(); i++) {
            System.out.println("envio a sortear al worker " + i);
            final int index = i;

            executor.submit(() -> {
                workers.get(index).processTask(subArrays.get(index));
            });
        }

        executor.shutdownNow(); // No aceptará nuevas tareas
    }

    @Override
    public void attachWorker(WorkerInterfacePrx subscriber, Current current) {
        workers.add(subscriber);
        System.out.println("\nWorker suscrito: " + subscriber.toString());

        if (workers.size() == nodes) {
            startSort = System.currentTimeMillis();
            doProcess(arr);
        }
    }

    @Override
    public void addPartialResult(String[] res, Current current) {
        counter++;
        System.out.println("le llego partial result");
        sorted.addAll(Arrays.asList(res));

        if (counter == nodes) {
            sort(current);
        }
    }

    @Override
    public void detachWorker(WorkerInterfacePrx subscriber, Current current) {
        // Destruir el objeto proxy del worker
        subscriber.ice_getConnection().close(ConnectionClose.Forcefully);
        workers.remove(subscriber);
        System.out.println("\nWorker desuscrito: " + subscriber.toString());
    }

    @Override
    public String getTask(Current current) {
        return null;  // Reemplazar con la tarea real
    }

    private static String[] readDataFromFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            List<String> words = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                words.addAll(Arrays.asList(line.trim().split("\\s+")));
            }
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

            System.out.println("terminó de escribir");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
