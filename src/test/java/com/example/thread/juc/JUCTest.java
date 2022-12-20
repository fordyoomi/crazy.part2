package com.example.thread.juc;

import com.example.crazymakercircle.util.ThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.function.*;

/**
 * @author YYF.
 * date 2022/12/19 10:18.
 */
public class JUCTest {

    /**
     * 1. Function : 有输入输出
     * 2. Runnable : 无输入输出
     * 3. Consumer : 有输入 无输出
     */
    @Test
    public void test1() {
        CompletionStage<Integer> oneStage = new CompletableFuture<>();
        oneStage.thenApply(this::square).thenAccept(System.out::println).thenRun(System.out::println);
    }

    public int square(Integer x) {
        return  x * x;
    }


    /**
     * 示例: 无输入 无返回值
     * @throws Exception
     */
    @Test
    public void test2() throws Exception {

        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            System.out.println("run end...");
        });
        future.get(2, TimeUnit.SECONDS);
    }

    /**
     * 示例: 无入参 有返回值
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void test3() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Long> longCompletableFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            sleep(2);
            System.out.println("run end...");
            return System.currentTimeMillis() - start;
        });

        Long aLong = longCompletableFuture.get(3, TimeUnit.SECONDS);
        System.out.println("异步执行耗时(秒): " + aLong / 1000);
    }

    /**
     * join | get | getNow
     * @see CompletableFuture#whenComplete(BiConsumer) 设置子任务完成时回调
     * @see CompletableFuture#whenCompleteAsync(BiConsumer) 设置子任务完成时回调,可能不在一个线程内完成
     * @see CompletableFuture#whenCompleteAsync(BiConsumer, Executor) 设置子任务完成时回调,提交给线程池完成
     * @see CompletableFuture#exceptionally(Function) 设置异常处理的回调
     */
    @Test
    public void test4() throws ExecutionException, InterruptedException {

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            System.out.println("抛出异常!");
            throw new RuntimeException("发生异常");
        });

        //调用cancel()方法取消CompletableFuture时，任务被视为异常完成，completeExceptionally()方法 所设置的异常回调钩子也会被执行
        //future.cancel(true);

        //设置异步任务执行完成后的回调钩子
        future.whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void unused, Throwable throwable) {
                // 未发生异常时 throwable 是null
                System.out.println("执行完成");
            }
        });

        //设置异步任务发生异常后的回调钩子
        future.exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable throwable) {
                System.out.println("执行失败");
                return null;
            }
        });
        sleep(2);
        // 获取结果
        // get 阻塞获取结果
        // getNow 实时获取线程结果,没有完成 返回默认值
        // join 通get

        future.get();  // 102  102  有异常抛出 ExecutionException
//        future.join(); // 76  76 有异常抛出 CompletionException
//        future.getNow(null); //76  76 有异常抛出 CompletionException
        sleep(10);
    }

    /**
     * handle
     * @see CompletableFuture#handle(BiFunction) 在执行任务的同一个线程中处理异常和结果
     * @see CompletableFuture#handleAsync(BiFunction) 可能在执行任务的同一个线程中处理异常和结果
     * @see CompletableFuture#handleAsync(BiFunction, Executor) 在指定线程池中处理异常和结果
     */
    @Test
    public void test5() throws ExecutionException, InterruptedException {
        // handle
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            //System.out.println("抛出异常");
            //throw new RuntimeException("发生异常");

            System.out.println("run end...");
        });

        future.handle(new BiFunction<Void, Throwable, Object>() {
            @Override
            public Object apply(Void unused, Throwable throwable) {
                if (throwable == null)
                    System.out.println("没有发生异常");
                else
                    System.out.println("error: 发生异常");
                return null;
            }
        });
        future.get();
    }

    /**
     * 线程池的使用
     * 默认线程池fork-join-pool(默认为CPU的核数) 可以通过JVM参数配置 option:-Djava.util.concurrent.ForkJoinPool.common.parallelism
     */
    @Test
    public void test6() throws ExecutionException, InterruptedException, TimeoutException {

        ThreadPoolExecutor mixedThreadPool = ThreadUtil.getMixedTargetThreadPool();
        CompletableFuture<Long> longCompletableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("run begin...");
            long start = System.currentTimeMillis();
            sleep(1);
            System.out.println("run end...");
            return System.currentTimeMillis() - start;
        }, mixedThreadPool);

        Long aLong = longCompletableFuture.get(2, TimeUnit.SECONDS);
        System.out.println("异步执行耗时(秒) : " + aLong / 1000);
    }

    /**
     * thenApply
     * 下一个任务依赖上一个任务结果
     * @see Function T 后一个任务所返回的结果类型 R 前一个任务的返回类型
     * @see CompletableFuture#thenApply(Function) 后一个任务与前一个任务在同一个线程中执行
     * @see CompletableFuture#thenApplyAsync(Function) 后一个任务与前一个任务可以不在同一个线程中执行
     * @see CompletableFuture#thenApplyAsync(Function, Executor) 后一个任务在指定线程池中执行
     */
    @Test
    public void test7() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(new Supplier<Long>() {
            @Override
            public Long get() {
                long l = 10L + 10L;
                String name = Thread.currentThread().getName();
                System.out.println("step.1 "+ name);
                return l;
            }
        }).thenApply(new Function<Long, Long>() {
            @Override
            public Long apply(Long aLong) {
                long l = aLong * 2;
                String name = Thread.currentThread().getName();
                System.out.println("step.2 "+ name);
                return l;
            }
        });
        Long aLong = future.get();
        System.out.println("result: " + aLong);
    }

    /**
     * thenRun
     * 只要前一个任务执行完成， 就开始执行后一个串行任务。
     * @see CompletableFuture#thenRun(Runnable) 后一个任务与前一个任务在同一个线程中执行
     * @see CompletableFuture#thenRunAsync(Runnable) 后一个任务与前一个任务可以不在同一个线程中执行
     * @see CompletableFuture#thenRunAsync(Runnable, Executor) 后一个任务在线程池中执行
     */
    @Test
    public void test8() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(3);
            String name = Thread.currentThread().getName();
            System.out.println("step.1 "+ name);
        }).thenRun(() -> {
            sleep(1);
            String name = Thread.currentThread().getName();
            System.out.println("step.2 "+ name);
        });
        future.get();
    }

    /**
     * thenAccept
     * 后一个任务消费前一个任务结果,但没有输出
     * @see CompletableFuture#thenAccept(Consumer) 后一个任务与前一个任务在同一个线程中执行
     * @see CompletableFuture#thenAcceptAsync(Consumer) 后一个任务与前一个任务可以不在同一个线程中执行
     * @see CompletableFuture#thenAcceptAsync(Consumer, Executor) 后一个任务在线程池中执行
     *
     */
    @Test
    public void test9() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            String name = Thread.currentThread().getName();
            System.out.println("step.1 "+ name);
            long l = 10L + 10L;
            return l;
        }).thenAccept(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) {
                String name = Thread.currentThread().getName();
                System.out.println("step.2 "+ name);
                System.out.println("消费结果" + aLong);
            }
        });

        future.get();
    }

    /**
     * thenCompose
     * thenCompose()方法在功能上与thenApply()、thenAccept()、thenRun()一样，可以对两个任务进 行串行的调度操作，第一个任务操作完成时，将其结果作为参数传递给第二个任务
     *
     * @see CompletableFuture#thenCompose(Function)
     * @see CompletableFuture#thenComposeAsync(Function)
     * @see CompletableFuture#thenComposeAsync(Function, Executor)
     */
    @Test
    public void test10() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            String name = Thread.currentThread().getName();
            System.out.println("step.1 " + name);
            long l = 10L + 10L;
            return l;
        }).thenCompose(new Function<Long, CompletionStage<Long>>() {
            String name2 = Thread.currentThread().getName();
            @Override
            public CompletionStage<Long> apply(Long aLong) {
                return CompletableFuture.supplyAsync(new Supplier<Long>() {
                    @Override
                    public Long get() {
                        //String name = Thread.currentThread().getName();
                        System.out.println("step.2 " + name2);
                        long l = aLong * 2;
                        return l;
                    }
                });
            }
        });

        Long aLong = future.get();
        System.out.println("aLong = " + aLong);
    }


    /*
    4 个任务串行方法的区别
    thenApply()、thenRun()、thenAccept()这三个方法的不同之处主要在于其核心参数fn、action、 consumer的类型不同，分别为Function<T, R>、Runnable、Consumer<? super T>类型。
        但是，thenCompose()方法与thenApply()方法有本质的不同:
    1)thenCompose()的返回值是一个新的CompletionStage实例，可以持续用来进行下一轮 CompletionStage任务的调度。
        具体来说，thenCompose()返回的是包装了普通异步方法的CompletionStage任务实例，通过该 实例还可以进行下一轮CompletionStage任务的调度和执行，比如可以持续进行CompletionStage链式 (或者流式)调用。
    2)thenApply()的返回值简单多了，直接就是第二个任务的普通异步方法的执行结果，其返回 类型与第二步执行的普通异步方法的返回类型相同，通过thenApply()所返回的值不能进行下一轮 CompletionStage链式(或者流式)调用。
     */


    /*       任务合并      */

    /**
     * thenCombine
     * 会在两个CompletionStage任务都执行完成后，一块来处理两个任务的执行结果
     * other : 待合并的第二步任务的CompletionStage实例   fn : 第一个任务和第二个任务执行完成后，第三步的需要执行的逻辑
     * T : 第一个任务所返回结果的类型  U : 第二个任务所返回结果的类型  V : 第三个任务所返回结果的类型
     * @see CompletableFuture#thenCombine(CompletionStage, BiFunction) 合并第二步CompletionStage,返回第三步的CompletionStage
     * @see CompletableFuture#thenCombineAsync(CompletionStage, BiFunction) 不一定在同一线程中执行
     * @see CompletableFuture#thenCombineAsync(CompletionStage, BiFunction, Executor) 第三步的CompletionStage在指定线程中执行
     */
    @Test
    public void test11() throws ExecutionException, InterruptedException {
        //thenCombine()方法的调用者为第一步的CompletionStage实例;
        // 该方法的第一个参数为第二步 的CompletionStage实例;
        // 该方法的返回值为第三步的CompletionStage实例。
        // 在逻辑上，thenCombine() 方法的功能是将第一步、第二步的结果合并到第三步上
        CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {

                return "烧水";
            }
        }).thenCombine(CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {

                return "清洗";
            }
        }), new BiFunction<String, String, String>() {
            @Override
            public String apply(String s, String s2) {
                System.out.println("已完成: " + s + " " + s2);
                return "泡茶";
            }
        });
        System.out.println("下一步: " + future.get());
    }

    /**
     * runAfterBoth
     * runAfterBoth()方法跟thenCombine()方法不一样的是:runAfterBoth()方法不关心每一步任务的 输入参数和处理结果
     *
     * @see CompletableFuture#runAfterBoth(CompletionStage, Runnable) 合并第二步任务的CompletionStage实例，返回第三步任务的CompletionStage
     * @see CompletableFuture#runAfterBothAsync(CompletionStage, Runnable) 不一定在同一个线程中执行第三步任务的CompletionStage实例
     * @see CompletableFuture#runAfterBothAsync(CompletionStage, Runnable, Executor) 第三步任务的CompletionStage实例在指定的executor线程池中执行
     */
    @Test
    public void test12() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("step.1");
        }).runAfterBoth(CompletableFuture.runAsync(() -> {
            System.out.println("step.2");
        }), () -> {
            System.out.println("step.3");
        });

        future.get();
    }

    /**
     * thenAcceptBoth
     * thenAcceptBoth()方法对runAfterBoth()方法和thenCombine()方法的特点进行了折中
     * 调用该方法，第三个任务可以接收其合并过来的第一个任务、第二个任务的处理结果
     * 但是第三个任务(合 并任务)却不能返回结果
     * @see CompletableFuture#thenAcceptBoth(CompletionStage, BiConsumer)
     * @see CompletableFuture#thenAcceptBothAsync(CompletionStage, BiConsumer)
     * @see CompletableFuture#thenAcceptBothAsync(CompletionStage, BiConsumer, Executor)
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void test13() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            //sleep(1);
            String name = Thread.currentThread().getName();
            System.out.println("step.1 "+ name);
            long l = 10L + 10L;
            return l;
        }).thenAcceptBoth(CompletableFuture.supplyAsync(() -> {
            //sleep(3);
            String name = Thread.currentThread().getName();
            System.out.println("step.2 " + name);
            long l = 20L + 20L;
            return l;
        }), new BiConsumer<Long, Long>() {
            @Override
            public void accept(Long aLong, Long aLong2) {
                //sleep(5);
                String name = Thread.currentThread().getName();
                long l = aLong + aLong2;
                System.out.println(name + " - 消费结果:" + l);
            }
        });

        future.get();
    }

    /**
     * allof
     * CompletionStage接口的allOf()会等待所有的任务结束，以合并所有的任务。thenCombine()只能 合并两个任务，如果需要合并多个异步任务，可以使用allOf()
     */
    @Test
    public void test14() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> f1 = CompletableFuture.supplyAsync(new Supplier<Long>() {
            @Override
            public Long get() {
                System.out.println("step.1");
                return 10L + 10L;
            }
        });
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
            sleep(6);
            System.out.println("step.3");
        });

        // f1.thenApply() 同一个线程的话,会阻塞allOf
        CompletableFuture<Long> f3 = f1.thenApplyAsync(new Function<Long, Long>() {
            @Override
            public Long apply(Long aLong) {
                sleep(5);
                System.out.println("step.2");
                return aLong * 2;
            }
        });
        CompletableFuture<Void> future = CompletableFuture.allOf(f1, f2); // 为何要等f3执行完
        future.thenRun(() -> {
            System.out.println("step.1/3 完成");
        });

        future.get();

        System.out.println("step.5");
    }

    /*   异步任务的选择执行    */


    /**
     * applyToEither | runAfterEither | acceptEither
     * 选择执行不是按照某种条件进行选择的，而是按照执行速度进 行选择的 : 前面两并行任务，谁的结果返回速度快，其结果将作为第三步任务的输入。
     * 1. applyToEither()方法功能为:  两个CompletionStage谁返回结果的速度快，applyToEither()就用这个最快的CompletionStage的 结果进行下一步(第三步)的回调操作
     * 2. runAfterEither()方法功能为: 前面两个CompletionStage实例，任何一个完成了都会执行第三步 回调操作。三个任务的回调函数都是Runnable类型的
     * 3. acceptEither()方法对applyToEither()方法和runAfterEither()方法的特点进行了折中，两个CompletionStage谁返回结果的速度快，acceptEither()就用这个最快的CompletionStage的结果作为下 一步(第三步)的输入，但是第三步没有输出
     * @see CompletableFuture#applyToEither(CompletionStage, Function)
     * @see CompletableFuture#runAfterEither(CompletionStage, Runnable)
     * @see CompletableFuture#acceptEither(CompletionStage, Consumer)
     *
     * @see CompletableFuture#applyToEitherAsync(CompletionStage, Function)
     * @see CompletableFuture#runAfterEitherAsync(CompletionStage, Runnable)
     * @see CompletableFuture#acceptEitherAsync(CompletionStage, Consumer)
     *
     * @see CompletableFuture#applyToEitherAsync(CompletionStage, Function, Executor)
     * @see CompletableFuture#runAfterEitherAsync(CompletionStage, Runnable, Executor)
     * @see CompletableFuture#acceptEitherAsync(CompletionStage, Consumer, Executor)
     *
     */
    @Test
    public void test15() throws ExecutionException, InterruptedException {

        CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                sleep(5);
                return "耗时5秒的任务";
            }
        });

        CompletableFuture<String> f2 = future.applyToEither(CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                sleep(6);
                return "耗时6秒的任务";
            }
        }), new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s;
            }
        });

        String s = f2.get();

        System.out.println(s);
    }


    @Test
    public void test141() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("start");
        }).thenAcceptBoth(CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "x";
            }
        }), new BiConsumer<Void, String>() {
            @Override
            public void accept(Void unused, String s) {
                System.out.println(s);
            }
        });

        future.get();
    }

    @Test
    public void test151() throws ExecutionException, InterruptedException {
        CompletableFuture<String> start = CompletableFuture.runAsync(() -> {
            System.out.println("start");
        }).thenCombine(CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "x";
            }
        }), new BiFunction<Void, String, String>() {
            @Override
            public String apply(Void unused, String s) {
                System.out.println("接收第二步返回值" + s);
                return s;
            }
        });

        String s = start.get();
        System.out.println(s);
    }

    @Test
    public void test161() throws ExecutionException, InterruptedException {

        // task.1
        CompletableFuture<Void> f1 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println("task.1 step 1: 洗水壶");
                return "干净的水壶";
            }
        }).thenAcceptAsync(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println("task.1 " + s);
                System.out.println("task.1 step 2: 烧开水");
                sleep(5);
                System.out.println("task.1 : 水开了");
            }
        });

        // task.2
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
            System.out.println("task.2 step 1: 洗茶壶");
            sleep(4);
            System.out.println("task.2 : 茶壶洗好了");
        }).thenRunAsync(() -> {
            System.out.println("task.2 step 2: 洗茶杯");
            sleep(3);
            System.out.println("task.2 : 茶杯子洗好了");
        }).thenRunAsync(() -> {
            System.out.println("task.2 step 3: 拿茶叶");
        });

        CompletableFuture<Void> f3 = CompletableFuture.allOf(f1, f2).thenRun(() -> {
            System.out.println("task.3 step 1: 泡茶");
        });

        f3.get();
    }

    public void sleep(int s) {
        try {
            TimeUnit.SECONDS.sleep(s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
