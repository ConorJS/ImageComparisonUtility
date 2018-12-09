package imaging;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SimplePair<K, V> {

    private K key;
    private V value;
    /**
     * Creates a new pair
     *
     * @param key   The key for this pair
     * @param value The value to use for this pair
     */
    public SimplePair(K key, V value) {
        this.key = key;
        this.value =  value;
    }
}
