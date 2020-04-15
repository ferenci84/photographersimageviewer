package com.dynamicrpogrammingsolutions.photographersimageviewer;

import javafx.scene.image.Image;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class ImageFile {
    private Path path;

    private boolean toDelete;
    private boolean select;

    private Path metaFileDelete;
    private Path metaFileSelect;
    private LocalTime lastUsed;

    AtomicBoolean isLoading = new AtomicBoolean();
    AtomicReference<Image> img = new AtomicReference<>();
    int orientation = 0;
    AtomicReference<Runnable> whenDone = new AtomicReference<>();

    public ImageFile(Path path) throws IOException {
        this.path = path;
        String name = path.getName(path.getNameCount()-1).toString();
        String baseName = name.substring(0,name.lastIndexOf('.'));
        metaFileDelete = path.getParent().resolve(baseName+".delete");
        metaFileSelect = path.getParent().resolve(baseName+".select");
        checkMeta();
    }

    synchronized public void setWhenDone(Runnable whenDone) {
        if (isLoading.get()) {
            this.whenDone.set(whenDone);
        } else {
            whenDone.run();
        }
    }

    synchronized public void cancel() {
        if (isLoading.get()) {
            this.whenDone.set(null);
            isLoading.set(false);
            if (img.get() == null) lastUsed = null;
        }
    }

    public LocalTime getLastUsed() {
        return lastUsed;
    }

    public Image useImage() {
        lastUsed = LocalTime.now();
        return img.get();
    }

    synchronized public boolean isLoadingOrLoaded() {
        return isLoading.get() || img.get() != null;
    }
    synchronized public boolean isLoaded() {
        return img.get() != null;
    }

    synchronized public void setLoading() {
        if (img.get() == null) {
            isLoading.set(true);
            lastUsed = LocalTime.now();

        }
    }

    synchronized public void setLoading(Runnable whenDone) {
        if (img.get() == null) {
            isLoading.set(true);
            lastUsed = LocalTime.now();
        }
        setWhenDone(whenDone);
    }

    public void deleteCached() {
        if (!isLoading.get()) {
            img.set(null);
            lastUsed = null;
        }
    }

    public void load() throws IOException {
        boolean loading;
        synchronized (this) {
            loading = isLoading.get();
        }
        if (loading) {
            Image loadImage = loadImage(path);
            int orientation = getOrientation(path);
            synchronized (this) {
                if (isLoading.get()) {
                    img.set(loadImage);
                    this.orientation = 0;
                    if (orientation == 8) {
                        this.orientation = 3;
                    } else if (orientation == 5) {
                        this.orientation = 1;
                    }
                    Runnable runOnEnd = whenDone.getAndUpdate(runnable -> null);
                    if (runOnEnd != null) runOnEnd.run();
                    isLoading.set(false);
                }
            }
        }
    }

    public static boolean filter(String name) {
        return name.endsWith("CR2") || name.endsWith("cr2") || name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("png") || name.endsWith("PNG") || name.endsWith("tif") || name.endsWith("tiff") || name.endsWith("TIF") || name.endsWith("TIFF");
    }
    private static Image loadImage(Path path) throws IOException {
        String name = path.getFileName().toString();
        if (name.endsWith("CR2") || name.endsWith("cr2")) {
            return loadCR2(path);
        } else if (name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("png") || name.endsWith("PNG") || name.endsWith("tif") || name.endsWith("tiff") || name.endsWith("TIF") || name.endsWith("TIFF")) {
            return loadIMG(path);
        }
        return null;
    }
    private static Image loadIMG(Path path) throws IOException {
        return new Image(Files.newInputStream(path));
    }
    private static int getOrientation(Path path) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] command1 = new String[4];
        command1[0] = "exiftool";
        command1[1] = "-b";
        command1[2] = "-orientation";
        command1[3] = path.toString();
        Process process1 = rt.exec(command1);
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(process1.getInputStream()))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        String orientation = textBuilder.toString();
        return Integer.parseInt(orientation);
    }
    private static Image loadCR2(Path path) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] command = new String[4];
        command[0] = "exiftool";
        command[1] = "-b";
        command[2] = "-previewimage";
        command[3] = path.toString();
        Process process = rt.exec(command);
        InputStream inputStream = process.getInputStream();
        return new Image(inputStream);
    }
    public String getName() {
        return path.getFileName().toString();
    }
    public Path getPath() {
        return path;
    }
    private void checkMeta() {
        toDelete = Files.exists(metaFileDelete);
        select = Files.exists(metaFileSelect);
    }

    public boolean getToDelete() {
        return toDelete;
    }

    public void setToDelete(boolean toDelete) throws IOException {
        if (toDelete!=this.toDelete) {
            if (toDelete) {
                Files.createFile(metaFileDelete);
            } else {
                Files.delete(metaFileDelete);
            }
        }
        this.toDelete = toDelete;
    }

    public boolean getSelect() {
        return select;
    }

    public void setSelect(boolean select) throws IOException {
        if (select!=this.select) {
            if (select) {
                Files.createFile(metaFileSelect);
            } else {
                Files.delete(metaFileSelect);
            }
        }
        this.select = select;
    }
}
