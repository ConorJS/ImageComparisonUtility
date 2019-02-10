package imaging.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SimpleTriple<A, B, C> {

    private A a;
    private B b;
    private C c;

    public SimpleTriple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
