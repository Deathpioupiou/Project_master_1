/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 package com.googlecreativelab.drawar;

import javax.vecmath.Vector3f;

public class AppSettings {
    private static  Vector3f color = new Vector3f(0.274f, 0.706f, 0.922f);
    private static final float strokeDrawDistance = 0.06f;
    private static final float minDistance = 0.001f;
    private static final float nearClip = 0.001f;
    private static final float farClip = 80.0f;


    public static float getStrokeDrawDistance() {
        return strokeDrawDistance;
    }

    public static Vector3f getColor() {
        return color;
    }

    public static float getMinDistance() {
        return minDistance;
    }

    public static float getNearClip(){
        return nearClip;
    }
    public static float getFarClip(){
        return farClip;
    }
    public static void setColor(float r,float g,float b){
        color = new Vector3f(r/255,g/255,b/255);
    }

}
