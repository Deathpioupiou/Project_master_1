package com.googlecreativelab.drawar;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.googlecreativelab.drawar.rendering.DataHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.vecmath.Vector3f;

public class Capture extends AppCompatActivity {
    private ArrayList<ArrayList<Vector3f>> strockes;
    private float[] projec;
    LinearLayout linearLayout;
    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        strockes = DataHolder.getInstance().getData();
        projec = DataHolder.getInstance().getProjec();
        float xmin =9999;
        float ymin = 9999;
        float xmax =-9999;
        float ymax = -9999;
        for (int i =0;i<strockes.size();i+=1){
            for (int j=0 ;j<strockes.get(i).size();j+=1){
                xmin=Math.min(xmin,strockes.get(i).get(j).x);
                ymin=Math.min(ymin,strockes.get(i).get(j).y*-1);
                xmax=Math.max(xmax,strockes.get(i).get(j).x);
                ymax=Math.max(ymax,strockes.get(i).get(j).y*-1);
            }
        }
        xmax = xmax-xmin;
        ymax=ymax-ymin;
        float echelle = Math.max(ymax,xmax);
        for (int i =0;i<strockes.size();i+=1){
            for (int j=0 ;j<strockes.get(i).size();j+=1){
                float x = (((strockes.get(i).get(j).x)-xmin)*1000)/echelle;
                float y = (((strockes.get(i).get(j).y)-xmin)*1000)/echelle;
                Vector3f vectore = new Vector3f(x,y,strockes.get(i).get(j).z);
                strockes.get(i).remove(j);
                strockes.get(i).add(j,vectore);
            }
        }
        DataHolder.getInstance().setData(strockes);
        Toast.makeText(this,String.valueOf(xmin)+" " +String.valueOf(ymin)+" "+String.valueOf(xmax)+" " +String.valueOf(ymax),Toast.LENGTH_LONG).show();
        linearLayout = findViewById(R.id.linearlayout);
        MyView myView = new MyView(this);
        linearLayout.addView(myView);

    }

}
