import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {
    private static final int THREAD_POOL_SIZE = 16;

    private String[] bigData;
    private String[] sortedArray;

    public ThreadPool(String[] bigData){
        this.bigData = bigData;
    } 

    public String[] getSorted() {
        return sortedArray;
    }

    public void execute() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<String[]>> futures = new ArrayList<>();

        int size = (int) Math.ceil((double) bigData.length / THREAD_POOL_SIZE);

        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            int start = i * size;
            int end = Math.min(start + size, bigData.length);
            
            if (start > end) {
                break;
            }

            String[] subArr = Arrays.copyOfRange(bigData, start, end);
            futures.add(executor.submit(new SortTask(subArr)));
        }

        List<String[]> sortedSubArrs = new ArrayList<>();
        
        for (Future<String[]> future : futures) {
            sortedSubArrs.add(future.get());
        }

        executor.shutdown();
        this.sortedArray = SortTask.mergeSortedArrays(sortedSubArrs);
        // notifique al main (cliente) que ya acabó
        Worker.notifyClient();
    }
}