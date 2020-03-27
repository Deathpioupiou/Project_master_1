package com.googlecreativelab.drawar.rendering;

import android.graphics.Bitmap;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

public class DataHolder {
    private ArrayList<ArrayList<Vector3f>> data;
    private Bitmap b;
    private float[] projec;
    private static final DataHolder holder = new DataHolder();

    public static DataHolder getInstance() {
        return holder;
    }

    public ArrayList<ArrayList<Vector3f>> getData() {
        return data;
    }

    public float [] getProjec() {
        return projec;
    }
    public void setData(ArrayList<ArrayList<Vector3f>> data) {
        ArrayList<ArrayList<Vector3f>> tmp=new ArrayList<>();
        for (int i = 0 ; i<data.size();i++){
            tmp.add(new ArrayList<Vector3f>(data.get(i)));
        }
        this.data=tmp;
    }
    public void setData(float[] data) {
     projec = data;
    }
    public Bitmap getBitmap() {
        return b;
    }

    public void setBitmap(Bitmap data) {
        this.b = data;
    }

}