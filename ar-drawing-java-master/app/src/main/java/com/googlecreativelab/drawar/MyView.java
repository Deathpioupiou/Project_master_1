package com.googlecreativelab.drawar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.view.View;

import com.googlecreativelab.drawar.rendering.DataHolder;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

public class MyView extends View {

    Paint mPaint, otherPaint, outerPaint, mTextPaint;
    RectF mRectF;
    int mPadding;

    float arcLeft, arcTop, arcRight, arcBottom;

    Path mPath;


    public MyView(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(10);



        int screenWidth = 1000;
        int screenHeight = 1000;




    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        ArrayList<ArrayList<Vector3f>> strocke = DataHolder.getInstance().getData();
        for (int i =0;i<strocke.size();i+=1){
            for (int j=0 ;j<strocke.get(i).size()-1;j+=1){
                canvas.drawLine(strocke.get(i).get(j).x, strocke.get(i).get(j).y, strocke.get(i).get(j+1).x, strocke.get(i).get(j+1).y, mPaint);
            }
        }



    }


}