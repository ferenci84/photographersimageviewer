package com.dynamicrpogrammingsolutions.photographersimageviewer;

class ImageCalc {

    enum Sizing {
        FIT,ZOOM
    }

    Sizing sizing = Sizing.FIT;
    boolean initialized;
    double zoom;
    double posx;
    double posy;
    double vwwidth;
    double vwheight;
    double imgwidth;
    double imgheight;
    int orientation;

    double vpx;
    double vpy;
    double vpw;
    double vph;

    double drawx;
    double drawy;
    double draww;
    double drawh;

    void setVw(double vwwidth, double vwheight) {
        if (orientation == 0) {
            this.vwwidth = vwwidth;
            this.vwheight = vwheight;
        } else {
            this.vwwidth = vwheight;
            this.vwheight = vwwidth;
        }
    }

    void init(double imgwidth, double imgheight, double posx, double posy, int orientation, double vwwidth, double vwheight) {

        if (orientation == 0) {
            this.vwwidth = vwwidth;
            this.vwheight = vwheight;
        } else {
            this.vwwidth = vwheight;
            this.vwheight = vwwidth;
        }

        initialized = true;
        sizing = Sizing.FIT;
        this.posx = posx;
        this.posy = posy;
        this.imgwidth = imgwidth;
        this.imgheight = imgheight;
        this.orientation = orientation;
        this.zoom = minZoom();

        centerX();
        centerY();
        printVals();
    }

    private void printVals() {
        System.out.println(vwwidth+" "+vwheight+" "+imgwidth+" "+imgheight+" "+posx+" "+posy);
    }

    private void centerX() {
        if (vwwidth > imgwidth*zoom) posx = -((vwwidth-imgwidth*zoom)/2);
        else {
            if (posx < 0) posx = 0;
            if (posx > (imgwidth*zoom-vwwidth)) posx = imgwidth*zoom-vwwidth;
        }
    }
    private void centerY() {
        if (vwheight > imgheight*zoom) posy = -((vwheight-imgheight*zoom)/2);
        else {
            if (posy < 0) posy = 0;
            if (posy > (imgheight*zoom-vwheight)) posy = imgheight*zoom-vwheight;
        }
    }
    private double minZoom() {
        return Math.min(vwwidth/imgwidth, vwheight/imgheight);
    }

    void calcViewPort() {
        vpx = posx/zoom;
        vpy = posy/zoom;
        vpw = vwwidth/zoom;
        vph = vwheight/zoom;
        drawx = 0;
        drawy = 0;
        draww = vwwidth;
        drawh = vwheight;
        if (vpx < 0) {
            vpx = 0;
            vpw = imgwidth;
            drawx = -posx;
            draww = imgwidth*zoom;
        }
        if (vpy < 0) {
            vpy = 0;
            vph = imgheight;
            drawy = -posy;
            drawh = imgheight*zoom;
        }
    }

    void zoom(double deltafactor, double _refx, double _refy) {
        double refx = _refx;
        double refy = _refy;
        if (orientation == 3) {
            refx = this.vwwidth-_refy;
            refy = _refx;
        } else if (orientation == 1) {
            refx = _refy;
            refy = this.vwheight-_refx;
        }
        double refposx = (posx+refx)/this.zoom;
        double refposy = (posy+refy)/this.zoom;
        this.zoom*=deltafactor;
        this.zoom = Math.max(this.zoom,minZoom());
        this.posx = refposx*this.zoom-refx;
        this.posy = refposy*this.zoom-refy;
        centerX();
        centerY();
    }

    void move(double startPosX, double startPosY, double deltaX, double deltaY) {
        if (orientation == 0) {
            this.posx = startPosX + deltaX;
            this.posy = startPosY + deltaY;
        } else if (orientation == 3) {
            this.posx = startPosX - deltaY;
            this.posy = startPosY + deltaX;
        } else if (orientation == 1) {
            this.posx = startPosX + deltaY;
            this.posy = startPosY - deltaX;
        }
        centerX();
        centerY();
    }

    void resize(double vwwidth, double vwheight) {
        if (orientation == 0) {
            this.vwwidth = vwwidth;
            this.vwheight = vwheight;
        } else {
            this.vwwidth = vwheight;
            this.vwheight = vwwidth;
        }
        zoom = Math.max(zoom,minZoom());
        centerX();
        centerY();
    }

}
