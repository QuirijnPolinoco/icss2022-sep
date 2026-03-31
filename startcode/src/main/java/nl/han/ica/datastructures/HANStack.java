package nl.han.ica.datastructures;

public class HANStack<T> implements IHANStack<T> {

    private IHANLinkedList<T> stack;

    public HANStack() {
        this.stack = new HANLinkedList<>();
    }

    public boolean isEmpty() {
        return stack.getSize() == 0;
    }

    @Override
    public void push(T value) {
        stack.addFirst(value);
    }

    @Override
    public T pop() {
        if (stack.getSize() == 0) {
            return null;
        }
        T top = stack.getFirst();
        stack.removeFirst();
        return top;
    }

    @Override
    public T peek() {
        if (stack.getSize() == 0) {
            return null;
        }
        return stack.getFirst();
    }
}
