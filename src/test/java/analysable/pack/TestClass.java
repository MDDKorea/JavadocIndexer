package analysable.pack;

/**
 * Hello, I am Javadoc! I am referencing {@link SecondClass} and {@link String}.
 * <em>How nice!</em>
 *
 * <ul>
 *   <li>Hello, list item</li>
 *   <li>And another one</li>
 * </ul>
 * <br>
 * And some {@code inline code} and some real-deal code:
 * {@code
 *   class Foo {
 *   }
 * }
 *
 * Construct me using {@link TestClass#TestClass()}
 *
 * @author i_al_istannen
 */
public class TestClass {

  /**
   * And as always, I hope you {@index learned Cognitive process} something!
   */
  public static final String WELL = "HELLO";

  /**
   * Ayy, I am a constructor!
   */
  public TestClass() {
  }

  /**
   * A short description of this method.
   * <p>
   * And let's follow it up with a lot more.
   * <br>
   * Or maybe not? But let's reference it {@link #foo(long, String)}.
   *
   * @param a I am a parameter
   * @param foo Serendipitous! I am one as well!
   * @return That's cute, but I am the return value
   */
  public static long foo(int a, String foo) {
    return 42;
  }

  /**
   * Imposter method! <em>Proceed with <strong>caution!</strong></em> {@return Cute, huh?}
   *
   * @param a some param
   * @param foo another one
   * @return and a return value. Magic
   */
  public static long foo(long a, String foo) {
    return 42;
  }
}
