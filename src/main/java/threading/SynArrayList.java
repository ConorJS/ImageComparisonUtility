package threading;

import java.util.ArrayList;

// ** Reusable **
public class SynArrayList<E> extends ArrayList<E> {

    private boolean lock = false;

    @Override
    public boolean add(E e) {
        synchronized (this) {
            return super.add(e);
        }
    }

    /*@Override
    public E get(int index) {
        synchronized (this) {
            return super.get(index);
        }
    }*/

}
