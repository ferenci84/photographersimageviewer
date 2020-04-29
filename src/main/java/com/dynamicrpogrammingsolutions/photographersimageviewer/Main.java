package com.dynamicrpogrammingsolutions.photographersimageviewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main extends Application {

    private static class Drag {
        boolean dragging;
        double startX;
        double startY;
        double startPosX;
        double startPosY;
    }

    private final static int MAX_IMG_IN_CACHE = 32;
    private final double ZOOM_FACTOR = 1.1;
    private final double ZOOM_FACTOR_KEY = 1.331;

    private final ImageCalc imageCalc = new ImageCalc();
    private final Drag drag = new Drag();
    private Image img;
    private Canvas canvas;
    private List<ImageFile> imageFiles = Collections.EMPTY_LIST;
    private Predicate<ImageFile> filter = (f) -> true;
    private List<ImageFile> filteredImageFiles;
    private Map<Integer,Integer> mapFilteredIdx;

    private int imageIdx = -1;
    private ImageFile currentImageFile;

    ImageLoader imageLoader = new ImageLoader();

    private void deleteImages() {
        System.out.println("delete images, cachesize: "+imageFiles.stream().filter(imageFile -> imageFile.getLastUsed() != null).count());
        imageFiles.stream()
                .filter(imageFile -> imageFile.getLastUsed() != null)
                .filter(imageFile -> imageFile.isLoaded())
                .sorted(Comparator.comparing(ImageFile::getLastUsed).reversed())
                .skip(MAX_IMG_IN_CACHE)
                .forEach(imageFile -> {
                    imageFile.deleteCached();
                });
    }

    private int getNumSelected() {
        return (int)imageFiles.stream().filter(f -> f.getSelect()).count();
    }

    private void updateTitle() {
        if (imageIdx == -1) primaryStage.setTitle("Photographers' Image Viewer");
        primaryStage.setTitle(imageFiles.get(imageIdx).getName()+" selected: "+getNumSelected());
    }
    private void updateTitle(ImageFile imgFile) {
        primaryStage.setTitle(imgFile.getName()+" selected: "+getNumSelected());
    }

    private void loadImage(ImageFile imageFile) {
        imageLoader.loadImage(imageFile,() -> {
            Platform.runLater(() -> {
                System.out.println("Showing "+imageFile.getName());
                img = imageFile.useImage();
                updateTitle(imageFile);
                if (img != null) {
                    double vw = canvas.getWidth();
                    double vh = canvas.getHeight();
                    currentImageFile = imageFile;
                    if (!imageCalc.initialized || imageCalc.imgwidth != img.getWidth() || imageCalc.imgheight != img.getHeight() || imageCalc.orientation != imageFile.orientation) {
                        imageCalc.init(img.getWidth(), img.getHeight(), 0, 0, imageFile.orientation, vw, vh);
                        refreshTransform();
                    }
                    refreshImage();
                } else {
                    System.out.println("couldn't show "+imageFile.getName());
                }
            });
            //System.out.println("Showing file: "+imageFile.getName());
            deleteImages();
        });
    }

    private void loadCurrentImage() throws IOException, InterruptedException {
        if (imageFiles.isEmpty()) return;
        if (imageIdx < 0) {
            imageIdx = 0;
        }
        ImageFile imageFile = imageFiles.get(imageIdx);
        /*Platform.runLater(() -> {
            updateTitle();
        });*/
        loadImage(imageFile);
    }

    private void preload(int idx) {
        if (idx > 0 && idx < imageFiles.size()) {
            imageLoader.preLoadImage(imageFiles.get(idx));
        }
    }

    private void setFilter(Predicate<ImageFile> filter) {
        this.filter = filter;
        this.filteredImageFiles = imageFiles.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    private int findFilteredImageIdx(int idx) {
        ImageFile imageFile = imageFiles.get(idx);
        for (int i = 0; i < this.filteredImageFiles.size(); i++) {
            if (filteredImageFiles.get(i) == imageFile) {
                return i;
            }
        }
        return -1;
    }
    private int findImageIdx(int filteredIdx) {
        if (filteredIdx > 0 && filteredIdx < filteredImageFiles.size()) {
            ImageFile imageFile = filteredImageFiles.get(filteredIdx);
            for (int i = 0; i < this.imageFiles.size(); i++) {
                if (imageFile == imageFiles.get(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void preload() {
        int filteredIdx = findFilteredImageIdx(imageIdx);
        if (filteredIdx == -1) return;
        preload(findImageIdx(filteredIdx + 1));
        preload(findImageIdx(filteredIdx + 2));
        preload(findImageIdx(filteredIdx + 3));
        preload(findImageIdx(filteredIdx + 4));
        preload(findImageIdx(filteredIdx + 5));
        preload(findImageIdx(filteredIdx + 6));
        preload(findImageIdx(filteredIdx + 7));
        preload(findImageIdx(filteredIdx + 8));
        preload(findImageIdx(filteredIdx - 1));
        preload(findImageIdx(filteredIdx - 2));
        preload(findImageIdx(filteredIdx - 3));
        preload(findImageIdx(filteredIdx - 4));
        preload(findImageIdx(filteredIdx - 5));
        preload(findImageIdx(filteredIdx - 6));
        preload(findImageIdx(filteredIdx - 7));
        preload(findImageIdx(filteredIdx - 8));
    }
    private int findNext(int curr) {
        int ret = curr;
        while(curr < imageFiles.size()-1) {
            curr++;
            if (filter.test(imageFiles.get(curr))) {
                ret = curr;
                break;
            }
        }
        if (ret == -1) ret = 0;
        return ret;
    }
    private int findPrev(int curr) {
        int ret = curr;
        while(curr > 0) {
            curr--;
            if (filter.test(imageFiles.get(curr))) {
                ret = curr;
                break;
            }
        }
        if (ret == -1) ret = 0;
        return ret;
    }
    private void stepForward() {
        if (imageFiles.isEmpty()) return;
        imageIdx = findNext(imageIdx);
        /*if (imageIdx < 0) {
            imageIdx = 0;
            return;
        }
        imageIdx++;
        imageIdx = Math.min(imageIdx,imageFiles.size()-1);*/
    }
    private void stepBackward() {
        if (imageFiles.isEmpty()) return;
        imageIdx = findPrev(imageIdx);
        /*if (imageIdx < 0) {
            imageIdx = 0;
        }
        imageIdx--;
        imageIdx = Math.max(imageIdx,0);*/
    }

    private void eventHandlerForward() {
        stepForward();
        try {
            loadCurrentImage();
            preload();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void eventHandlerBackward() {
        stepBackward();
        try {
            loadCurrentImage();
            preload();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void eventHandlerDelete() {
        if (currentImageFile!=null) {
            try {
                currentImageFile.setToDelete(!currentImageFile.getToDelete());
                refreshImage();
                updateTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void eventHandlerSelect() {
        if (currentImageFile!=null) {
            try {
                currentImageFile.setSelect(!currentImageFile.getSelect());
                refreshImage();
                updateTitle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ExecutorService executor = Executors.newCachedThreadPool();

    private void loadFolder(Path folder, ImageFile initialImage) throws IOException {
        if (Files.isDirectory(folder)) {
            List<ImageFile> files = new LinkedList<>();
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(folder)) {
                for (Path path : paths) {
                    if (ImageFile.filter(path.getFileName().toString())) {
                        if (path.equals(initialImage.getPath())) {
                            files.add(initialImage);
                        } else {
                            files.add(new ImageFile(path,executor));
                        }
                    }
                }
            }
            imageFiles = files.stream()
                    .sorted(Comparator.comparing(path -> path.getName()))
                    .collect(Collectors.toList());
            imageIdx = -1;
            for (int i = 0; i < imageFiles.size(); i++) {
                if (imageFiles.get(i).getPath().equals(initialImage.getPath())){
                    imageIdx = i;
                }
            }
            setFilter(f -> true);
        }
        System.out.println("finished loading folder imageIdx: "+imageIdx);
    }

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {

        List<String> parameters = this.getParameters().getUnnamed();

        if (parameters.size() < 1) {
            System.err.println("No Image to show");
            System.exit(1);
        }

        primaryStage.setTitle("Photographers Image Viewer");
        this.primaryStage = primaryStage;
        Group pane = new Group();
        Scene scene = new Scene(pane, 600, 330, Color.GRAY);


        canvas = new Canvas();

        canvas.getGraphicsContext2D().setImageSmoothing(false);
        pane.getChildren().add(canvas);
        canvas.setHeight(330);
        canvas.setWidth(600);


        String initialImage = parameters.get(0);
        Path initialImagePath = Paths.get(initialImage).toAbsolutePath();

        if (Files.isDirectory(initialImagePath)){
            System.err.println("Parameter is a directory");
            System.exit(1);
        }

        Path dir;
        if (!Files.isDirectory(initialImagePath)) {
            dir = initialImagePath.getParent();
        } else {
            dir = initialImagePath;
        }

        ImageFile initialImageFile = new ImageFile(initialImagePath,executor);
        loadImage(initialImageFile);
        new Thread(() -> {
            try {
                loadFolder(dir, initialImageFile);
                Platform.runLater(() -> {
                    updateTitle();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            preload();
        }).start();

        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            imageCalc.resize(scene.getWidth(),scene.getHeight());
            canvas.setWidth(scene.getWidth());
            refreshTransform();
            refreshImage();
        });

        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            imageCalc.resize(scene.getWidth(),scene.getHeight());
            canvas.setHeight(scene.getHeight());
            refreshTransform();
            refreshImage();
        });

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        AtomicLong lastMouseMoved = new AtomicLong();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis()-lastMouseMoved.get() >= 1000) {
                Platform.runLater(() -> {
                    scene.setCursor(Cursor.NONE);
                });
            }
        },700,100,TimeUnit.MILLISECONDS);

        scene.setOnScroll(event -> {
            double delta = 0;
            switch(event.getTextDeltaYUnits()) {
                case LINES:
                    delta = event.getTextDeltaY();
                    break;
                case PAGES:
                    delta = event.getTextDeltaY();
                    break;
                case NONE:
                    delta = event.getDeltaY();
                    break;
            }

            double zoomDelta = 0;
            if (delta > 0) {
                zoomDelta = ZOOM_FACTOR;
            } else if (delta < 0) {
                zoomDelta = 1/ZOOM_FACTOR;
            } else {
                return;
            }
            imageCalc.zoom(zoomDelta,event.getSceneX(),event.getSceneY());
            refreshImage();
        });

        scene.setOnKeyPressed(keyEvent -> {
            //System.out.println("event received: "+System.currentTimeMillis()+" code: "+keyEvent.getCode());
            if (keyEvent.getCode().equals(KeyCode.SPACE) ||
                    keyEvent.getCode().equals(KeyCode.RIGHT)
            ) {
                this.eventHandlerForward();
            }
            if (keyEvent.getCode().equals(KeyCode.BACK_SPACE) ||
                    keyEvent.getCode().equals(KeyCode.LEFT)
            ) {
                this.eventHandlerBackward();
            }
            if (keyEvent.getCode().equals(KeyCode.DELETE) ||
                    (!keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.D))
            ) {
                this.eventHandlerDelete();
            }
            if (keyEvent.getCode().equals(KeyCode.INSERT) ||
                    (!keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.S))
            ) {
                this.eventHandlerSelect();
            }
            if (keyEvent.getCode().equals(KeyCode.ADD)) {
                imageCalc.zoom(ZOOM_FACTOR_KEY,scene.getWidth()/2.0,scene.getHeight()/2.0);
                refreshImage();
            }
            if (keyEvent.getCode().equals(KeyCode.SUBTRACT)) {
                imageCalc.zoom(1/ZOOM_FACTOR_KEY,scene.getWidth()/2.0,scene.getHeight()/2.0);
                refreshImage();
            }
            if (keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.A)) {
                this.setFilter(f -> true);
            }
            if (keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.S)) {
                this.setFilter(f -> f.getSelect() && !f.getToDelete());
            }
            if (keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.D)) {
                this.setFilter(f -> !f.getToDelete());
            }
        });

        scene.setOnMousePressed(event -> {
            scene.setCursor(Cursor.OPEN_HAND);
            drag.dragging = true;
            drag.startX = event.getSceneX();
            drag.startY = event.getSceneY();
            drag.startPosX = imageCalc.posx;
            drag.startPosY = imageCalc.posy;

        });
        scene.setOnMouseDragged(event -> {
            lastMouseMoved.set(System.currentTimeMillis());
            if (drag.dragging) {
                scene.setCursor(Cursor.OPEN_HAND);
                double deltaX = -event.getSceneX() + drag.startX;
                double deltaY = -event.getSceneY() + drag.startY;
                imageCalc.move(drag.startPosX,drag.startPosY,deltaX,deltaY);
                refreshImage();
            }
        });
        scene.setOnMouseMoved(event -> {
            scene.setCursor(Cursor.DEFAULT);
            lastMouseMoved.set(System.currentTimeMillis());
        });
        scene.setOnMouseReleased(event -> {
            drag.dragging = false;
            scene.setCursor(Cursor.DEFAULT);
        });

        primaryStage.setOnCloseRequest(windowEvent -> {
            imageLoader.finish();
        });

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void setTimeout(Runnable run) {

    }
    private void removeTimeout() {

    }

    private Affine getRotateRight() {
        Affine affine = new Affine();
        affine.appendTranslation(-imageCalc.vwwidth / 2+imageCalc.vwheight / 2,-imageCalc.vwheight / 2+imageCalc.vwwidth / 2);
        affine.appendRotation(90,imageCalc.vwwidth / 2,imageCalc.vwheight / 2);
        return affine;
    }

    private Affine getRotateLeft() {
        Affine affine = new Affine();
        affine.appendTranslation(-imageCalc.vwwidth / 2+imageCalc.vwheight / 2,-imageCalc.vwheight / 2+imageCalc.vwwidth / 2);
        affine.appendRotation(270,imageCalc.vwwidth / 2,imageCalc.vwheight / 2);
        return affine;
    }

    private Affine getNoRotate() {
        return new Affine();
    }

    private void refreshImage() {
        if (currentImageFile != null && canvas != null) {
            refreshImageSizing(canvas.getGraphicsContext2D(),imageCalc);
            this.showImageInfo(canvas.getGraphicsContext2D(),imageCalc,currentImageFile);

        }
    }

    private void refreshTransform() {
        if (currentImageFile != null && canvas != null) {
            refreshTransform(canvas.getGraphicsContext2D(),imageCalc);
        }
    }

    private int graphicsContextRotate = 0;

    private void refreshTransform(GraphicsContext graphicsContext2D, ImageCalc imageCalc) {
        if (imageCalc.orientation == 1) {
            graphicsContextRotate = 1;
            graphicsContext2D.setTransform(getRotateRight());
        } else if (imageCalc.orientation == 3) {
            graphicsContextRotate = 3;
            graphicsContext2D.setTransform(getRotateLeft());
        } else {
            graphicsContextRotate = 0;
            graphicsContext2D.setTransform(getNoRotate());
        }
    }

    private void refreshImageSizing(GraphicsContext graphicsContext2D, ImageCalc imageCalc) {
        //System.out.println("drawing started: "+System.currentTimeMillis());
        imageCalc.calcViewPort();
        //System.out.println("draw: vpx:"+imageCalc.vpx+" vpy:"+imageCalc.vpy+" vpw:"+imageCalc.vpw+" vph:"+imageCalc.vph+" drawx:"+imageCalc.drawx+" drawy:"+imageCalc.drawy+" draww:"+imageCalc.draww+" drawh:"+imageCalc.drawh);

        /*if (imageCalc.orientation == 1) {
            graphicsContext2D.setTransform(getRotateRight());
        } else if (imageCalc.orientation == 3) {
            graphicsContext2D.setTransform(getRotateLeft());
        } else {
            graphicsContext2D.setTransform(getNoRotate());
        }*/
        graphicsContext2D.setImageSmoothing(imageCalc.zoom < 1);

        graphicsContext2D.drawImage(img,imageCalc.vpx,imageCalc.vpy,imageCalc.vpw,imageCalc.vph,imageCalc.drawx,imageCalc.drawy,imageCalc.draww,imageCalc.drawh);
        graphicsContext2D.setFill(Color.GRAY);
        if (imageCalc.draww < imageCalc.vwwidth) {
            graphicsContext2D.fillRect(0,0,imageCalc.drawx,imageCalc.vwheight);
            graphicsContext2D.fillRect(imageCalc.drawx+imageCalc.draww,0,imageCalc.vwwidth-imageCalc.draww-imageCalc.drawx,imageCalc.vwheight);
        }
        if (imageCalc.drawh < imageCalc.vwheight) {
            graphicsContext2D.fillRect(0,0,imageCalc.vwwidth,imageCalc.drawy);
            graphicsContext2D.fillRect(0,imageCalc.drawy+imageCalc.drawh,imageCalc.vwwidth,imageCalc.vwheight-imageCalc.drawh-imageCalc.drawy);
        }
        //System.out.println("drawing finished: "+System.currentTimeMillis());
    }

    private void showImageInfo(GraphicsContext graphicsContext2D, ImageCalc imageCalc, ImageFile imageFile) {
        int size = 20;
        int dist = 30;
        boolean drawrect = false;
        if (imageFile.getToDelete()) {
            graphicsContext2D.setFill(Color.ORANGERED);
            drawrect = true;
        } else if (imageFile.getSelect()) {
            graphicsContext2D.setFill(Color.LIME);
            drawrect = true;
        }
        if (drawrect) {
            if (graphicsContextRotate == 0) {
                graphicsContext2D.fillRect(imageCalc.vwwidth - dist - size, dist, size, size);
            } else if (graphicsContextRotate == 1) {
                graphicsContext2D.fillRect(10, 10, size, size);
            } else if (graphicsContextRotate == 3) {
                graphicsContext2D.fillRect(imageCalc.vwwidth - dist-size, imageCalc.vwheight - dist-size, size, size);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
