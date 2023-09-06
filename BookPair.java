import java.util.concurrent.locks.*;

public class BookPair {
    private int quantity;
    private Lock lock;

    public BookPair(int quantity)
    {
        this.quantity = quantity;
        this.lock = new ReentrantLock();
    }

    public int getQuantity()
    {
        return this.quantity;
    }

    public void beginLoan() {
        this.lock.lock();
        this.quantity--;
        this.lock.unlock();
    }

    public void endLoan() {
        this.lock.lock();
        this.quantity++;
        this.lock.unlock();
    }

}
