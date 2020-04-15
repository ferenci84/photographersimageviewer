package com.dynamicrpogrammingsolutions.photographersimageviewer;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private Deque<ImageFile> preloadList = new ConcurrentLinkedDeque<>();
    private Deque<LoadTask> loadList = new ConcurrentLinkedDeque<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExecutorService preloadExecutor = Executors.newSingleThreadExecutor();

    static class LoadTask {
        public LoadTask(ImageFile imageFile, Runnable whenDone) {
            this.imageFile = imageFile;
            this.whenDone = whenDone;
        }
        public ImageFile imageFile;
        public Runnable whenDone;
    };

    public void finish() {

    }

    public void loadImage(ImageFile imageFile, Runnable whenDone) {
        synchronized (preloadList) {
            preloadList.clear();
        }
        synchronized (loadList) {
            loadList.clear();
            loadList.offer(new LoadTask(imageFile,whenDone));
        }
        executor.submit(() -> {
            LoadTask task;
            ImageFile f;
            synchronized (loadList) {
                task = loadList.poll();
                f = task.imageFile;
            }
            if (f != null) {
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
        });
    }

    public void preLoadImage(ImageFile imageFile) {
        if (!imageFile.isLoadingOrLoaded()) {
            preloadList.offer(imageFile);
            preloadExecutor.submit(() -> {
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
        }
    }

}
