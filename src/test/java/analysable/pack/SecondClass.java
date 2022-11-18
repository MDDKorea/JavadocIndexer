package analysable.pack;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * I exist! And I link to string: {@link
 *     java.lang.String}.
 */
public abstract class SecondClass<T extends Comparable<T>>
    extends AbstractList<List<T>> implements List<List<T>> {

  public static final List<? extends String> FIELD = null;

  public <R extends Comparable<R>> Collection<R> foo(R r, List<? super R> list) throws RuntimeException, Exception {
    return List.of();
  }

}
