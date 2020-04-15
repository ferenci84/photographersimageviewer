# Photographers' Image Viewer

The Photographers' Image Viewer (PgIV for short) is the simplest possible image viewer for quickly viewing through the photos you made and mark them "selected" or "remove".

## Reason for existence and distinction from available alternatives

Alternatives of this image viewer has one of these problems:

* The photos have to be imported before you can manage them - in PgIV you just open a file and can start to select the photos

* Sometimes the selection is written to the file itself - PgIV do not want to change the file. Instead it will use a technique called "marker file". You can use a simple script, that will check if the marker file exists and it will move/copy/link to an other directory accordingly.

* The deletion removes the file from disk - with PgIV you can mark the files for deletion, you will see a red dot, so you can change your mind, then for final decision you can use custom batch scripts (sample provided) to remove the files.

* The RAW files open too slowly - The full-featured image processors will process the RAW files before opening. On the contrary, PgIV will use the preview file that is somewhat lower resolution, but opens much quickly. This is helpful when you want to go through say 600 photos, quickly remove the bad ones, and select the best ones.

* Very big, too many features, and high cost - PgIV is a hobby-project, it's very simple, does the job and no more, and it's completely free and open-source.

## Supported file formats:

* JPG
* TIF
* PNG
* CR2

## Compile & Run & Build

You can use pure java or maven.

### Pure java

Download javafx-sdk-14 and javafx-jmods-14 from https://gluonhq.com/products/javafx/ and save them in the project directory.

Set these exports before compiling (linux):

    export PATH_TO_FX=$PWD/javafx-sdk-14/lib
    export PATH_TO_FX_MODS=$PWD/javafx-jmods-14

Compile:

    javac --module-path $PATH_TO_FX -d mods/photographersimageviewer $(find src/main/java/ -name "*.java")
    
Run:

    java --module-path $PATH_TO_FX:mods -m photographersimageviewer/com.dynamicrpogrammingsolutions.photographersimageviewer.Main
    
Package Module:

    $JAVA_HOME/bin/jlink --module-path $PATH_TO_FX_MODS:mods --add-modules photographersimageviewer --output photographersimageviewer
    
Run Module:

    photographersimageviewer/bin/java -m photographersimageviewer/com.dynamicrpogrammingsolutions.photographersimageviewer.Main
    
### Maven
    
Set `JAVA_HOME` to a java version `>=11`.

Run:

    mvn clean javafx:run
    
Package module:

    mvn clean compile javafx:jlink
    
Launch:
    
    ./target/photographersimageviewer/bin/launcher "image"
    
### Create command (linux):

    sudo sh -c 'echo "#!""/bin/sh" > /usr/local/bin/photographersimageviewer'
    sudo sh -c 'echo $PWD"/target/photographersimageviewer/bin/launcher ""$""@" >> /usr/local/bin/photographersimageviewer'
    sudo chmod +x /usr/local/bin/photographersimageviewer

