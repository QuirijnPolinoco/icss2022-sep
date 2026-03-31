package nl.han.ica.datastructures;

public class HANLinkedList<T> implements IHANLinkedList<T> {

    private ListNode<T> head;
    private int size;

    private static final class ListNode<T> {
        T value;
        ListNode<T> next;

        ListNode(T value, ListNode<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void addFirst(T value) {
        head = new ListNode<>(value, head);
        size++;
    }

    @Override
    public void clear() {
        head = null;
        size = 0;
    }

    @Override
    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }
        if (index == 0) {
            addFirst(value);
            return;
        }
        ListNode<T> before = nodeAt(index - 1);
        before.next = new ListNode<>(value, before.next);
        size++;
    }

    @Override
    public void delete(int pos) {
        if (pos < 0 || pos >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (pos == 0) {
            removeFirst();
            return;
        }
        ListNode<T> before = nodeAt(pos - 1);
        before.next = before.next.next;
        size--;
    }

    @Override
    public T get(int pos) {
        if (pos < 0 || pos >= size) {
            throw new IndexOutOfBoundsException();
        }
        return nodeAt(pos).value;
    }

    @Override
    public void removeFirst() {
        if (head == null) {
            return;
        }
        head = head.next;
        size--;
    }

    @Override
    public T getFirst() {
        if (head == null) {
            return null;
        }
        return head.value;
    }

    @Override
    public int getSize() {
        return size;
    }

    private ListNode<T> nodeAt(int index) {
        ListNode<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }
}
