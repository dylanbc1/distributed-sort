module TextSorter {
    sequence<string> StringSeq;
    sequence<StringSeq> StringSeqSeq;

    interface WorkerInterface {
        void processTask(StringSeq lines);
    };

    sequence<string> result;
    interface MasterInterface{
        void attachWorker(WorkerInterface* subscriber);
        void addPartialResult(StringSeq res);
        void detachWorker(WorkerInterface* subscriber);
        void sort();
    }
}