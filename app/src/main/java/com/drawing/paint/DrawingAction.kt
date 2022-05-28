package com.drawing.paint

class DrawingAction {
    private var pts: MutableList<PathPoint?> = ArrayList()


//    fun getDrawer(): CorrectionView.Drawer? {
//        return drawer
//    }

    fun getPts(): List<PathPoint?>? {
        return pts
    }

    fun setPts(pts: MutableList<PathPoint?>) {
        this.pts = pts
    }

    fun addPts(point: PathPoint?) {
        pts.add(point)
        //Adds touch co-ordinates to this list from Drawing View( on Touch Listener)
    }
}