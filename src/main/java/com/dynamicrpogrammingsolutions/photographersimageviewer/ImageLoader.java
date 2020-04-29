package com.dynamicrpogrammingsolutions.photographersimageviewer;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ImageLoader {

    final private Deque<ImageFile> preloadList = new ConcurrentLinkedDeque<>();
    final private Deque<LoadTask> loadList = new ConcurrentLinkedDeque<>();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("load-thread");
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            });;
    private ThreadPoolExecutor preloadExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("preload-thread");
                thread.setPriority(Thread.NORM_PRIORITY-1);
                return thread;
            });

    ImageLoader() {
        System.out.println("available processors: "+Runtime.getRuntime().availableProcessors());
    }

    static class LoadTask {
        public LoadTask(ImageFile imageFile, Runnable whenDone) {
            this.imageFile = imageFile;
            this.whenDone = whenDone;
        }
        public ImageFile imageFile;
        public Runnable whenDone;
    };

    public void finish() {
        executor.shutdown();
        preloadExecutor.shutdown();
    }

    private AtomicReference<Future<?>> lastMainLoadFuture = new AtomicReference<>();

    public void loadImage(ImageFile imageFile, Runnable whenDone) {
        synchronized (preloadList) {
            preloadList.clear();
        }
        synchronized (loadList) {
            loadList.clear();
            loadList.offer(new LoadTask(imageFile,whenDone));
        }

        lastMainLoadFuture.set(executor.submit(() -> {
            LoadTask task;
            ImageFile f;
            synchronized (loadList) {
                task = loadList.poll();
            }
            if (task != null) {
                f = task.imageFile;
                try {
                    System.out.println("loading: " + f.getName());
                    //TODO: if the file is already loading, this thread should wait for it
                    if (!f.isLoaded()) {
                        f.load();
                    }
                    task.whenDone.run();
                    System.out.println("loaded: "+f.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    public void preLoadImage(ImageFile imageFile) {
        if (!imageFile.isLoadingOrLoaded()) {
            preloadList.offer(imageFile);

            // this will cause the action of submitting the preload thread be scheduled after the recent main loading had finished
            executor.submit(() -> {

                /*// Always allow the current image to load first before starting to preload
                Future<?> mainLoad = lastMainLoadFuture.get();
                if (mainLoad != null) {
                    try {
                        mainLoad.get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }*/

                preloadExecutor.submit(() -> {

                    // Always allow the current image to load first before starting to preload
                    Future<?> mainLoad = lastMainLoadFuture.get();
                    if (mainLoad != null) {
                        try {
                            mainLoad.get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }

                    ImageFile f;
                    synchronized (preloadList) {
                        f = preloadList.poll();
                    }
                    if (f != null) {
                        try {
                            System.out.println("preloading: "+f.getName());
                            if (!f.isLoaded()) {
                                f.load();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            });




        }
    }

}
