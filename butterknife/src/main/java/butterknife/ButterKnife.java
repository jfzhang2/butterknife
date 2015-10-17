package butterknife;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Property;
import android.view.View;
import butterknife.internal.Finder;
import butterknife.internal.ViewBinder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Field and method binding for Android views. Use this class to simplify finding views and
 * attaching listeners by binding them with annotations.
 * 成员变量与方法的注解针对Android中的View  使用这个类简化寻找视图以及
 * 附加监听器  通过给监听器添加上相应的注解
 * <p>
 * Finding views from your activity is as easy as:
 * <pre><code>
 * public class ExampleActivity extends Activity {
 *   {@literal @}Bind(R.id.title) EditText titleView;
 *   {@literal @}Bind(R.id.subtitle) EditText subtitleView;
 *
 *   {@literal @}Override protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.example_activity);
 *     ButterKnife.bind(this);
 *   }
 * }
 * </code></pre>
 * Binding can be performed directly on an {@linkplain #bind(Activity) activity}, a
 * {@linkplain #bind(View) view}, or a {@linkplain #bind(Dialog) dialog}. Alternate objects to
 * bind can be specified along with an {@linkplain #bind(Object, Activity) activity},
 * {@linkplain #bind(Object, View) view}, or
 * {@linkplain #bind(Object, android.app.Dialog) dialog}.
 * <p>
 * Group multiple views together into a {@link List} or array.
 * <pre><code>
 * {@literal @}Bind({R.id.first_name, R.id.middle_name, R.id.last_name})
 * List<EditText> nameViews;
 * </code></pre>
 * There are three convenience methods for working with view collections:
 * <ul>
 * <li>{@link #apply(List, Action)} &ndash; Applies an action to each view.</li>
 * <li>{@link #apply(List, Setter, Object)} &ndash; Applies a setter value to each view.</li>
 * <li>{@link #apply(List, Property, Object)} &ndash; Applies a property value to each view.</li>
 * </ul>
 * <p>
 * To bind listeners to your views you can annotate your methods:
 * <pre><code>
 * {@literal @}OnClick(R.id.submit) void onSubmit() {
 *   // React to button click.
 * }
 * </code></pre>
 * Any number of parameters from the listener may be used on the method.
 * <pre><code>
 * {@literal @}OnItemClick(R.id.tweet_list) void onTweetClicked(int position) {
 *   // React to tweet click.
 * }
 * </code></pre>
 * <p>
 * Be default, views are required to be present in the layout for both field and method bindings.
 * If a view is optional add a {@code @Nullable} annotation for fields (such as the one in the
 * <a href="http://tools.android.com/tech-docs/support-annotations">support-annotations</a> library)
 * or the {@code @Optional} annotation for methods.
 * <pre><code>
 * {@literal @}Nullable @Bind(R.id.title) TextView subtitleView;
 * </code></pre>
 * Resources can also be bound to fields to simplify programmatically working with views:
 * <pre><code>
 * {@literal @}BindBool(R.bool.is_tablet) boolean isTablet;
 * {@literal @}BindInt(R.integer.columns) int columns;
 * {@literal @}BindColor(R.color.error_red) int errorRed;
 * </code></pre>
 */
public final class ButterKnife {
  //创建实体对象的方法不向外暴露
  private ButterKnife() {
    throw new AssertionError("No instances.");
  }

  /** An unbinder contract that can be bind with {@link butterknife.Unbinder}. */
  @SuppressWarnings("unused") // Used by generated code.
  //解绑定的接口
  public interface Unbinder {
    void unbind();
  }

  /** An action that can be applied to a list of views. */
  //内部支持的数据结构是继承View的数据类型
  public interface Action<T extends View> {
    /** Apply the action on the {@code view} which is at {@code index} in the list. */
    //将这个action应用到在指定索引的集合中的View的元素
    void apply(@NonNull T view, int index);
  }

  /** A setter that can apply a value to a list of views. */
  //这是一个setter方法 能够将一个数值设置给一个集合视图中的指定索引的元素
  public interface Setter<T extends View, V> {
    /** Set the {@code value} on the {@code view} which is at {@code index} in the list. */
    void set(@NonNull T view, V value, int index);
  }

  private static final String TAG = "ButterKnife";
  private static boolean debug = false;

  //HashMap 是 Class与 ViewBinder之间的匹配
  static final Map<Class<?>, ViewBinder<Object>> BINDERS = new LinkedHashMap<>();
  //实现一个ViewBinder的接口 但是方法体内是空得实现
  static final ViewBinder<Object> NOP_VIEW_BINDER = new ViewBinder<Object>() {
    @Override public void bind(Finder finder, Object target, Object source) { }
  };

  /** Control whether debug logging is enabled. */
  public static void setDebug(boolean debug) {
    ButterKnife.debug = debug;
  }

  /**
   * Bind annotated fields and methods in the specified {@link Activity}. The current content
   * 绑定注解的成员变量与方法在指定的Activity中  这个目前的View内容被当做view的根节点
   * view is used as the view root.
   *
   * @param target Target activity for view binding. 为了绑定view的目标的节点
   */
  public static void bind(@NonNull Activity target) {
    bind(target, target, Finder.ACTIVITY);
  }

  /**
   * Bind annotated fields and methods in the specified {@link View}. The view and its children
   * are used as the view root.
   * 绑定拥有注解的成员变量以及方法在指定的View中  这个View以及他得子节点被用来作为View的父节点
   * @param target Target view for view binding.
   */
  @NonNull
  public static View bind(@NonNull View target) {
    bind(target, target, Finder.VIEW);
    return target;
  }

  /**
   * Bind annotated fields and methods in the specified {@link Dialog}. The current content
   * view is used as the view root.
   *
   * @param target Target dialog for view binding.
   */
  @SuppressWarnings("unused") // Public api.
  public static void bind(@NonNull Dialog target) {
    bind(target, target, Finder.DIALOG);
  }

  /**
   * Bind annotated fields and methods in the specified {@code target} using the {@code source}
   * {@link Activity} as the view root.
   *
   * @param target Target class for view binding.
   * @param source Activity on which IDs will be looked up.
   */
  public static void bind(@NonNull Object target, @NonNull Activity source) {
    bind(target, source, Finder.ACTIVITY);
  }

  /**
   * Bind annotated fields and methods in the specified {@code target} using the {@code source}
   * {@link View} as the view root.
   *
   * @param target Target class for view binding.
   * @param source View root on which IDs will be looked up.
   */
  @NonNull
  public static View bind(@NonNull Object target, @NonNull View source) {
    bind(target, source, Finder.VIEW);
    return source;
  }

  /**
   * Bind annotated fields and methods in the specified {@code target} using the {@code source}
   * {@link Dialog} as the view root.
   *
   * @param target Target class for view binding.
   * @param source Dialog on which IDs will be looked up.
   */
  @SuppressWarnings("unused") // Public api.
  public static void bind(@NonNull Object target, @NonNull Dialog source) {
    bind(target, source, Finder.DIALOG);
  }

  static void bind(@NonNull Object target, @NonNull Object source, @NonNull Finder finder) {
    //首先通过反射机制 找到注解目标上面的类的
    Class<?> targetClass = target.getClass();
    try {
      if (debug) Log.d(TAG, "Looking up view binder for " + targetClass.getName());
      //为指定的Class找到对应的ViewBinder的对象
      ViewBinder<Object> viewBinder = findViewBinderForClass(targetClass);
      //然后调用viewBinder的bind方法去做绑定
      viewBinder.bind(finder, target, source);
    } catch (Exception e) {
      throw new RuntimeException("Unable to bind views for " + targetClass.getName(), e);
    }
  }

  @NonNull
  private static ViewBinder<Object> findViewBinderForClass(Class<?> cls)
      throws IllegalAccessException, InstantiationException {
    //从HashMap中找到对应的ViewBinder的对象
    ViewBinder<Object> viewBinder = BINDERS.get(cls);
    if (viewBinder != null) {
      if (debug) Log.d(TAG, "HIT: Cached in view binder map.");
      return viewBinder;
    }
    //如果缓存中没有
    String clsName = cls.getName();
    //如果包名是系统框架  禁止搜索  返回一个空实现的NOP_VIEW_BINDER
    if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
      if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
      return NOP_VIEW_BINDER;
    }
    try {
      //找到对应的类的内部类的ViewBinder的实现
      Class<?> viewBindingClass = Class.forName(clsName + "$$ViewBinder");
      //noinspection unchecked
      //创建对应的ViewBinder的对象实例
      viewBinder = (ViewBinder<Object>) viewBindingClass.newInstance();
      if (debug) Log.d(TAG, "HIT: Loaded view binder class.");
    } catch (ClassNotFoundException e) {
      if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
      viewBinder = findViewBinderForClass(cls.getSuperclass());
    }
    //同时将其加入到缓存中
    BINDERS.put(cls, viewBinder);
    return viewBinder;
  }

  /** Apply the specified {@code actions} across the {@code list} of views. */
  //将指定的action应用到View得集合
  @SafeVarargs public static <T extends View> void apply(@NonNull List<T> list,
      @NonNull Action<? super T>... actions) {
    for (int i = 0, count = list.size(); i < count; i++) {
      for (Action<? super T> action : actions) {
        action.apply(list.get(i), i);
      }
    }
  }

  /** Apply the specified {@code actions} across the {@code array} of views. */
  @SafeVarargs public static <T extends View> void apply(@NonNull T[] array,
      @NonNull Action<? super T>... actions) {
    for (int i = 0, count = array.length; i < count; i++) {
      for (Action<? super T> action : actions) {
        action.apply(array[i], i);
      }
    }
  }

  /** Apply the specified {@code action} across the {@code list} of views. */
  public static <T extends View> void apply(@NonNull List<T> list,
      @NonNull Action<? super T> action) {
    for (int i = 0, count = list.size(); i < count; i++) {
      action.apply(list.get(i), i);
    }
  }

  /** Apply the specified {@code action} across the {@code array} of views. */
  public static <T extends View> void apply(@NonNull T[] array, @NonNull Action<? super T> action) {
    for (int i = 0, count = array.length; i < count; i++) {
      action.apply(array[i], i);
    }
  }

  /** Apply {@code actions} to {@code view}. */
  @SafeVarargs public static <T extends View> void apply(@NonNull T view,
      @NonNull Action<? super T>... actions) {
    for (Action<? super T> action : actions) {
      action.apply(view, 0);
    }
  }

  /** Apply {@code action} to {@code view}. */
  public static <T extends View> void apply(@NonNull T view, @NonNull Action<? super T> action) {
    action.apply(view, 0);
  }

  /** Set the {@code value} using the specified {@code setter} across the {@code list} of views. */
  public static <T extends View, V> void apply(@NonNull List<T> list,
      @NonNull Setter<? super T, V> setter, V value) {
    for (int i = 0, count = list.size(); i < count; i++) {
      setter.set(list.get(i), value, i);
    }
  }

  /** Set the {@code value} using the specified {@code setter} across the {@code array} of views. */
  public static <T extends View, V> void apply(@NonNull T[] array,
      @NonNull Setter<? super T, V> setter, V value) {
    for (int i = 0, count = array.length; i < count; i++) {
      setter.set(array[i], value, i);
    }
  }

  /** Set {@code value} on {@code view} using {@code setter}. */
  public static <T extends View, V> void apply(@NonNull T view,
      @NonNull Setter<? super T, V> setter, V value) {
    setter.set(view, value, 0);
  }

  /**
   * Apply the specified {@code value} across the {@code list} of views using the {@code property}.
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static <T extends View, V> void apply(@NonNull List<T> list,
      @NonNull Property<? super T, V> setter, V value) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, count = list.size(); i < count; i++) {
      setter.set(list.get(i), value);
    }
  }

  /**
   * Apply the specified {@code value} across the {@code array} of views using the {@code property}.
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static <T extends View, V> void apply(@NonNull T[] array,
      @NonNull Property<? super T, V> setter, V value) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, count = array.length; i < count; i++) {
      setter.set(array[i], value);
    }
  }

  /** Apply {@code value} to {@code view} using {@code property}. */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static <T extends View, V> void apply(@NonNull T view,
      @NonNull Property<? super T, V> setter, V value) {
    setter.set(view, value);
  }

  /** Simpler version of {@link View#findViewById(int)} which infers the target type. */
  @SuppressWarnings({ "unchecked", "UnusedDeclaration" }) // Checked by runtime cast. Public API.
  @CheckResult
  public static <T extends View> T findById(@NonNull View view, @IdRes int id) {
    return (T) view.findViewById(id);
  }

  /** Simpler version of {@link Activity#findViewById(int)} which infers the target type. */
  @SuppressWarnings({ "unchecked", "UnusedDeclaration" }) // Checked by runtime cast. Public API.
  @CheckResult
  public static <T extends View> T findById(@NonNull Activity activity, @IdRes int id) {
    return (T) activity.findViewById(id);
  }

  /** Simpler version of {@link Dialog#findViewById(int)} which infers the target type. */
  @SuppressWarnings({ "unchecked", "UnusedDeclaration" }) // Checked by runtime cast. Public API.
  @CheckResult
  public static <T extends View> T findById(@NonNull Dialog dialog, @IdRes int id) {
    return (T) dialog.findViewById(id);
  }
}
