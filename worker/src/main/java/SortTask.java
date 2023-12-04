import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;

public class SortTask implements Callable<String[]> {
        private String[] arr;

        public SortTask(String[] arr) {
            this.arr = arr;
        }

        @Override
        public String[] call() {
            Arrays.sort(arr);
            return arr;
        }

    public static String[] mergeSortedArrays(List<String[]> sortedSubArrs) {
        PriorityQueue<Element> pq = new PriorityQueue<>();
        int totalSize = 0;
        for (int i = 0; i < sortedSubArrs.size(); i++) {
            String[] subArr = sortedSubArrs.get(i);
            if (subArr.length > 0) {
                pq.add(new Element(i, 0, subArr[0]));
                totalSize += subArr.length;
            }
        }

        String[] sortedArr = new String[totalSize];
        for (int i = 0; !pq.isEmpty(); i++) {
            Element element = pq.poll();
            sortedArr[i] = element.getValue();
            if (element.getIndex() + 1 < sortedSubArrs.get(element.getArray()).length) {
                pq.add(new Element(element.getArray(), element.getIndex() + 1, sortedSubArrs.get(element.getArray())[element.getIndex() + 1]));
            }
        }
        return sortedArr;
    }
}
