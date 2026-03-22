package nl.han.ica.datastructures;

public class HANQueue<T> implements IHANQueue<T> {
    private final IHANLinkedList<T> queue;

    public HANQueue() {
        this.queue = new HANLinkedList<>();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean isEmpty() {
        return queue.getSize() == 0;
    }

    @Override
    public void enqueue(T value) {
        queue.insert(queue.getSize(), value);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        T front = queue.getFirst();
        queue.removeFirst();
        return front;
    }

    @Override
    public T peek() {
        return queue.getFirst();
    }

    @Override
    public int getSize() {
        return queue.getSize();
    }
}
