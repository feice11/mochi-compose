# Generated from SharedTextureComparison and AndroidX Test class references.
# Used only with -PmochiTestBuildType=release. Regenerate when test dependencies change.
# Keep callable shared entry points; other release code remains optimizable.
-keep,allowoptimization @interface androidx.annotation.ChecksSdkIntAtLeast {
  public int api();
  public int extension();
}
-keep,allowoptimization @interface androidx.annotation.GuardedBy {
  public java.lang.String value();
}
-keep,allowoptimization @interface androidx.annotation.NonNull {
}
-keep,allowoptimization @interface androidx.annotation.Nullable {
}
-keep,allowoptimization @interface androidx.annotation.RequiresApi {
  public int value();
}
-keep,allowoptimization enum androidx.annotation.RestrictTo$Scope {
  androidx.annotation.RestrictTo$Scope LIBRARY;
  androidx.annotation.RestrictTo$Scope LIBRARY_GROUP;
}
-keep,allowoptimization @interface androidx.annotation.RestrictTo {
  public androidx.annotation.RestrictTo$Scope[] value();
}
-keep,allowoptimization @interface androidx.annotation.VisibleForTesting {
}
-keep,allowoptimization @interface androidx.compose.runtime.internal.StabilityInferred {
  public int parameters();
}
-keep,allowoptimization class androidx.concurrent.futures.AbstractResolvableFuture {
  public void addListener(java.lang.Runnable, java.util.concurrent.Executor);
  public boolean cancel(boolean);
  public java.lang.Object get();
  public java.lang.Object get(long, java.util.concurrent.TimeUnit);
  static java.lang.Object getUninterruptibly(java.util.concurrent.Future);
  public boolean isCancelled();
  public boolean isDone();
}
-keep,allowoptimization class androidx.concurrent.futures.CallbackToFutureAdapter$Completer {
  public boolean set(java.lang.Object);
  public boolean setException(java.lang.Throwable);
}
-keep,allowoptimization interface androidx.concurrent.futures.CallbackToFutureAdapter$Resolver {
  public java.lang.Object attachCompleter(androidx.concurrent.futures.CallbackToFutureAdapter$Completer);
}
-keep,allowoptimization class androidx.concurrent.futures.CallbackToFutureAdapter {
  public static com.google.common.util.concurrent.ListenableFuture getFuture(androidx.concurrent.futures.CallbackToFutureAdapter$Resolver);
}
-keep,allowoptimization enum androidx.concurrent.futures.DirectExecutor {
  androidx.concurrent.futures.DirectExecutor INSTANCE;
}
-keep,allowoptimization class androidx.concurrent.futures.ResolvableFuture {
  public static androidx.concurrent.futures.ResolvableFuture create();
  public boolean set(java.lang.Object);
  public boolean setException(java.lang.Throwable);
}
-keep,allowoptimization enum androidx.lifecycle.Lifecycle$State {
  public static androidx.lifecycle.Lifecycle$State[] values();
  androidx.lifecycle.Lifecycle$State CREATED;
  androidx.lifecycle.Lifecycle$State DESTROYED;
  androidx.lifecycle.Lifecycle$State RESUMED;
  androidx.lifecycle.Lifecycle$State STARTED;
}
-keep,allowoptimization class androidx.lifecycle.Lifecycle {
}
-keep,allowoptimization class androidx.tracing.Trace {
  public static void beginSection(java.lang.String);
  public static void endSection();
  public static void forceEnableAppTracing();
}
-keep,allowoptimization interface com.google.common.util.concurrent.ListenableFuture {
  public void addListener(java.lang.Runnable, java.util.concurrent.Executor);
}
-keep,allowoptimization @interface kotlin.Deprecated {
  public java.lang.String message();
  public kotlin.ReplaceWith replaceWith();
}
-keep,allowoptimization @interface kotlin.ExtensionFunctionType {
}
-keep,allowoptimization interface kotlin.Lazy {
  public java.lang.Object getValue();
}
-keep,allowoptimization class kotlin.LazyKt {
}
-keep,allowoptimization class kotlin.LazyKt__LazyJVMKt {
  public static kotlin.Lazy lazy(kotlin.jvm.functions.Function0);
}
-keep,allowoptimization @interface kotlin.Metadata {
  public int[] bv();
  public java.lang.String[] d1();
  public java.lang.String[] d2();
  public int k();
  public int[] mv();
  public int xi();
}
-keep,allowoptimization class kotlin.Result$Companion {
}
-keep,allowoptimization class kotlin.Result {
  public static java.lang.Object constructor-impl(java.lang.Object);
  public static java.lang.Throwable exceptionOrNull-impl(java.lang.Object);
  kotlin.Result$Companion Companion;
}
-keep,allowoptimization class kotlin.ResultKt {
  public static java.lang.Object createFailure(java.lang.Throwable);
  public static void throwOnFailure(java.lang.Object);
}
-keep,allowoptimization class kotlin.Unit {
  kotlin.Unit INSTANCE;
}
-keep,allowoptimization class kotlin.collections.CollectionsKt {
}
-keep,allowoptimization class kotlin.collections.CollectionsKt__CollectionsJVMKt {
  public static java.util.List build(java.util.List);
  public static java.util.List createListBuilder();
}
-keep,allowoptimization class kotlin.collections.CollectionsKt__CollectionsKt {
  public static java.util.List emptyList();
  public static java.util.List listOf(java.lang.Object[]);
  public static void throwIndexOverflow();
}
-keep,allowoptimization class kotlin.collections.CollectionsKt__IterablesKt {
  public static int collectionSizeOrDefault(java.lang.Iterable, int);
}
-keep,allowoptimization class kotlin.collections.CollectionsKt___CollectionsKt {
  public static java.lang.Object first(java.util.List);
  public static java.lang.Object last(java.util.List);
  public static java.util.List toList(java.lang.Iterable);
}
-keep,allowoptimization interface kotlin.coroutines.Continuation {
  public kotlin.coroutines.CoroutineContext getContext();
  public void resumeWith(java.lang.Object);
}
-keep,allowoptimization class kotlin.coroutines.ContinuationKt {
  public static kotlin.coroutines.Continuation createCoroutine(kotlin.jvm.functions.Function1, kotlin.coroutines.Continuation);
}
-keep,allowoptimization interface kotlin.coroutines.CoroutineContext {
}
-keep,allowoptimization class kotlin.coroutines.EmptyCoroutineContext {
  kotlin.coroutines.EmptyCoroutineContext INSTANCE;
}
-keep,allowoptimization class kotlin.coroutines.intrinsics.IntrinsicsKt {
}
-keep,allowoptimization class kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt {
  public static kotlin.coroutines.Continuation intercepted(kotlin.coroutines.Continuation);
}
-keep,allowoptimization class kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsKt {
  public static java.lang.Object getCOROUTINE_SUSPENDED();
}
-keep,allowoptimization class kotlin.coroutines.jvm.internal.ContinuationImpl {
  public <init>(kotlin.coroutines.Continuation);
  protected java.lang.Object invokeSuspend(java.lang.Object);
}
-keep,allowoptimization @interface kotlin.coroutines.jvm.internal.DebugMetadata {
  public java.lang.String c();
  public java.lang.String f();
  public int[] i();
  public int[] l();
  public java.lang.String m();
  public java.lang.String[] n();
  public java.lang.String[] s();
}
-keep,allowoptimization class kotlin.coroutines.jvm.internal.DebugProbesKt {
  public static void probeCoroutineSuspended(kotlin.coroutines.Continuation);
}
-keep,allowoptimization interface kotlin.coroutines.jvm.internal.SuspendFunction {
}
-keep,allowoptimization class kotlin.coroutines.jvm.internal.SuspendLambda {
  public <init>(int, kotlin.coroutines.Continuation);
  public kotlin.coroutines.Continuation create(java.lang.Object, kotlin.coroutines.Continuation);
  protected java.lang.Object invokeSuspend(java.lang.Object);
}
-keep,allowoptimization class kotlin.io.CloseableKt {
  public static void closeFinally(java.io.Closeable, java.lang.Throwable);
}
-keep,allowoptimization class kotlin.io.FilesKt {
}
-keep,allowoptimization class kotlin.io.FilesKt__FileReadWriteKt {
  public static void writeText$default(java.io.File, java.lang.String, java.nio.charset.Charset, int, java.lang.Object);
}
-keep,allowoptimization class kotlin.io.TextStreamsKt {
  public static java.lang.String readText(java.io.Reader);
}
-keep,allowoptimization @interface kotlin.jvm.JvmName {
  public java.lang.String name();
}
-keep,allowoptimization @interface kotlin.jvm.JvmStatic {
}
-keep,allowoptimization interface kotlin.jvm.functions.Function0 {
  public java.lang.Object invoke();
}
-keep,allowoptimization interface kotlin.jvm.functions.Function1 {
  public java.lang.Object invoke(java.lang.Object);
}
-keep,allowoptimization interface kotlin.jvm.functions.Function2 {
  public java.lang.Object invoke(java.lang.Object, java.lang.Object);
}
-keep,allowoptimization class kotlin.jvm.internal.CallableReference {
  java.lang.Object receiver;
}
-keep,allowoptimization class kotlin.jvm.internal.DefaultConstructorMarker {
}
-keep,allowoptimization class kotlin.jvm.internal.FunctionReferenceImpl {
  public <init>(int, java.lang.Object, java.lang.Class, java.lang.String, java.lang.String, int);
}
-keep,allowoptimization class kotlin.jvm.internal.Intrinsics {
  public static boolean areEqual(java.lang.Object, java.lang.Object);
  public static void checkNotNull(java.lang.Object);
  public static void checkNotNull(java.lang.Object, java.lang.String);
  public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
  public static void checkNotNullParameter(java.lang.Object, java.lang.String);
  public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
  public static void throwNpe();
  public static void throwUninitializedPropertyAccessException(java.lang.String);
}
-keep,allowoptimization class kotlin.jvm.internal.Lambda {
  public <init>(int);
}
-keep,allowoptimization class kotlin.jvm.internal.Ref$IntRef {
  public <init>();
  int element;
}
-keep,allowoptimization class kotlin.jvm.internal.Ref$ObjectRef {
  public <init>();
  java.lang.Object element;
}
-keep,allowoptimization class kotlin.jvm.internal.Ref {
}
-keep,allowoptimization @interface kotlin.jvm.internal.SourceDebugExtension {
  public java.lang.String[] value();
}
-keep,allowoptimization class kotlin.jvm.internal.StringCompanionObject {
  kotlin.jvm.internal.StringCompanionObject INSTANCE;
}
-keep,allowoptimization interface kotlin.sequences.Sequence {
  public java.util.Iterator iterator();
}
-keep,allowoptimization class kotlin.text.Charsets {
  java.nio.charset.Charset UTF_8;
}
-keep,allowoptimization class kotlin.text.StringsKt {
}
-keep,allowoptimization class kotlin.text.StringsKt__StringsKt {
  public static boolean contains$default(java.lang.CharSequence, java.lang.CharSequence, boolean, int, java.lang.Object);
  public static kotlin.sequences.Sequence lineSequence(java.lang.CharSequence);
  public static java.lang.CharSequence trim(java.lang.CharSequence);
}
-keep,allowoptimization class kotlin.time.Duration$Companion {
}
-keep,allowoptimization class kotlin.time.Duration {
  kotlin.time.Duration$Companion Companion;
}
-keep,allowoptimization class kotlin.time.DurationKt {
  public static long toDuration(int, kotlin.time.DurationUnit);
}
-keep,allowoptimization enum kotlin.time.DurationUnit {
  kotlin.time.DurationUnit SECONDS;
}
-keep,allowoptimization class kotlinx.coroutines.BuildersKt {
  public static kotlinx.coroutines.Deferred async(kotlinx.coroutines.CoroutineScope, kotlin.coroutines.CoroutineContext, kotlinx.coroutines.CoroutineStart, kotlin.jvm.functions.Function2);
  public static java.lang.Object runBlocking(kotlin.coroutines.CoroutineContext, kotlin.jvm.functions.Function2);
}
-keep,allowoptimization class kotlinx.coroutines.CancellableContinuation$DefaultImpls {
  public static boolean cancel$default(kotlinx.coroutines.CancellableContinuation, java.lang.Throwable, int, java.lang.Object);
}
-keep,allowoptimization interface kotlinx.coroutines.CancellableContinuation {
  public void invokeOnCancellation(kotlin.jvm.functions.Function1);
  public void resume(java.lang.Object, kotlin.jvm.functions.Function1);
}
-keep,allowoptimization class kotlinx.coroutines.CancellableContinuationImpl {
  public <init>(kotlin.coroutines.Continuation, int);
  public java.lang.Object getResult();
  public void initCancellability();
}
-keep,allowoptimization class kotlinx.coroutines.CoroutineDispatcher {
}
-keep,allowoptimization interface kotlinx.coroutines.CoroutineScope {
  public kotlin.coroutines.CoroutineContext getCoroutineContext();
}
-keep,allowoptimization enum kotlinx.coroutines.CoroutineStart {
  kotlinx.coroutines.CoroutineStart DEFAULT;
  kotlinx.coroutines.CoroutineStart UNDISPATCHED;
}
-keep,allowoptimization interface kotlinx.coroutines.Deferred {
  public java.lang.Object await(kotlin.coroutines.Continuation);
}
-keep,allowoptimization class kotlinx.coroutines.Dispatchers {
  public static kotlinx.coroutines.MainCoroutineDispatcher getMain();
  public static kotlinx.coroutines.CoroutineDispatcher getUnconfined();
}
-keep,allowoptimization class kotlinx.coroutines.ExecutorsKt {
  public static kotlinx.coroutines.CoroutineDispatcher from(java.util.concurrent.Executor);
}
-keep,allowoptimization class kotlinx.coroutines.Job$DefaultImpls {
  public static void cancel$default(kotlinx.coroutines.Job, java.util.concurrent.CancellationException, int, java.lang.Object);
}
-keep,allowoptimization interface kotlinx.coroutines.Job {
}
-keep,allowoptimization class kotlinx.coroutines.MainCoroutineDispatcher {
}
-keep,allowoptimization class kotlinx.coroutines.TimeoutKt {
  public static java.lang.Object withTimeout-KLykuaI(long, kotlin.jvm.functions.Function2, kotlin.coroutines.Continuation);
}
-keep,allowoptimization @interface org.jetbrains.annotations.NotNull {
}
-keep,allowoptimization @interface org.jetbrains.annotations.Nullable {
}
-keep,allowoptimization class ui.feice.mochi.demo.MainActivity {
}
-keeppackagenames androidx.concurrent.futures
