package imaging;

import java.util.ArrayList;
import java.util.List;

public class NumberOrderedPairList<E> {

    private final List<SimplePair<E, Integer>> pairList = new ArrayList<>();

    public SimplePair<E, Integer> getLargest() {
        return pairList.get(0);
    }

    public SimplePair<E, Integer> getNthLargest(int n) {
        if (n > pairList.size()) {
            throw new ArrayIndexOutOfBoundsException(n + " exceeds bounds of ArrayList of size: " + pairList.size());
        }

        return pairList.get(n - 1);
    }

    public SimplePair<E, Integer> getSmallest() {
        return pairList.get(pairList.size() - 1);
    }

    public SimplePair<E, Integer> getNthSmallest(int n) {
        if (n > pairList.size()) {
            throw new ArrayIndexOutOfBoundsException(
                    (pairList.size() - n) + " exceeds bounds of ArrayList of size: " + pairList.size());
        }

        return pairList.get(pairList.size() - n);
    }

    /**
     * @param pair
     * @return
     *      return[0] = size order place of new pair (0 is largest, n is smallest)
     *      return[1] = index of smallest element (n-1)
     */
    public int[] add(SimplePair<E, Integer> pair) {
        int[] returnFlags = new int[2];

        if (pairList.size() == 0) {

            pairList.add(pair);

            returnFlags[0] = 0;
            returnFlags[1] = 0;
            return returnFlags;
        }

        for (int i = 0; i < pairList.size(); i++) {

            if (pairList.get(i).getValue() < pair.getValue()) {

                pairList.add(i, pair);

                returnFlags[0] = i;
                returnFlags[1] = pairList.size()-1;
                return returnFlags;
            }

        }

        pairList.add(pair);

        returnFlags[0] = pairList.size()-1;
        returnFlags[1] = pairList.size();
        return returnFlags;
    }

    public int[] add(E e, Integer numericValue) {
        return this.add(new SimplePair<E, Integer>(e, numericValue));
    }

    public String toString() { return pairList.toString(); }

    public List<SimplePair<E, Integer>> toList() {
        return this.pairList;
    }

}
