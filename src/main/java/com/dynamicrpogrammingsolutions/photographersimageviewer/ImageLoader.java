package com.dynamicrpogrammingsolutions.photographersimageviewer;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ImageLoader {

    private Deque<ImageFile> preloadList = new ConcurrentLinkedDeque<>();
    private Deque<ImageFile> loadList = new ConcurrentLinkedDeque<>();
    private boolean stopThreads = false;
    private Thread preLoadThread;
    private Thread loadThread;
    private Runnable afterLoad = null;

    public ImageLoader() {
        preLoadThread = new Thread(this::preLoadThread);
        preLoadThread.start();
        loadThread = new Thread(this::loadThread);
        loadThread.start();
    }

    public void finish() {
        stopThreads = true;
    }

    private void preLoadThread() {
        System.out.println("preload thread started");
        while(!stopThreads) {
            ImageFile f;
            synchronized (preloadList) {
                f = preloadList.poll();
            }
            if (f != null) {
                try {
                    System.out.println("preloading: "+f.getName());
                    f.load();
                    //deleteImages();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(1);
                    //preloadList.wait(1000L);
                } catch (InterruptedException e) {
                    System.out.println("interrupted");
                    break;
                }
            }
        }
        System.out.println("loadthread stopped");
    }

    private void loadThread() {
        System.out.println("load thread started");
        while(!stopThreads) {
            ImageFile f;
            synchronized (loadList) {
                f = loadList.poll();
            }
            if (f != null) {
                try {
                    System.out.println("loading: "+f.getName());
                    f.load();
                    //deleteImages();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(1);
                    //preloadList.wait(1000L);
                } catch (InterruptedException e) {
                    System.out.println("interrupted");
                    break;
                }
            }
        }
        System.out.println("loadthread stopped");
    }

    public void loadImage(ImageFile imageFile, Runnable whenDone) {
        if (!imageFile.isLoaded()) {
            //System.out.println("Set file for loading: "+imageFile.getName());
            imageFile.setLoading(whenDone);
            ImageFile imageFileToCancel;
            synchronized (loadList) {
                while ((imageFileToCancel = loadList.poll()) != null) {
                    if (imageFileToCancel != imageFile) imageFileToCancel.cancel();
                }
            }
            synchronized (preloadList) {
                while ((imageFileToCancel = preloadList.poll()) != null) {
                    if (imageFileToCancel != imageFile) imageFileToCancel.cancel();
                }
            }
            loadList.offer(imageFile);
        } else {
            //System.out.println("showing file now: "+imageFile.getName()+" "+System.currentTimeMillis());
            imageFile.setWhenDone(whenDone);
            //System.out.println("Showed: "+imageFile.getName()+" "+System.currentTimeMillis());
        }
    }

    public void preLoadImage(ImageFile imageFile) {
        if (!imageFile.isLoadingOrLoaded()) {
            imageFile.setLoading();
            preloadList.offer(imageFile);
        }
    }

}
