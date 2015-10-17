package butterknife.internal;
//定义绑定视图的ViewBinder 同时是支持泛型的
public interface ViewBinder<T> {
  //有三个参数
  void bind(Finder finder, T target, Object source);
}
