package tr.olgun.suterazisi;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

final class Instrument extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader glass = new LinearGradient(0,0,0,64,
        new int[]{0xff334c00,0xff76a900,0xffd3ed0b,0xff9db700,0xff4f6600},
        new float[]{0,.22f,.67f,.88f,1},Shader.TileMode.CLAMP);
    private final Shader pool = new LinearGradient(0,-100,0,100,
        new int[]{0xff486f00,0xff85b000,0xffd0e819,0xff829800},null,Shader.TileMode.CLAMP);
    private final Shader air = new RadialGradient(-.23f,-.42f,1.43f,
        new int[]{0xffedffa6,0xffcadf3a,0xffa0bd19,0xff517000},new float[]{0,.43f,.72f,1},Shader.TileMode.CLAMP);
    private String message = "", reading = "";
    private float x,y;
    private int lastX = Integer.MIN_VALUE, lastY;
    Instrument(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    void status(String s) {
        message = s;
        lastX = Integer.MIN_VALUE;
        setContentDescription(s);
        invalidate();
    }
    void measure(float x,float y) {
        this.x=x;
        this.y=y;
        message="";
        int ax=Math.round(10*x), ay=Math.round(10*y);
        if(ax!=lastX || ay!=lastY) {
            lastX=ax;
            lastY=ay;
            reading=String.format(Locale.forLanguageTag("tr"),"X  %.1f°     Y  %.1f°",ax/10f,ay/10f);
            setContentDescription(reading);
        }
        postInvalidateOnAnimation();
    }
    private void fill(int color) { p.setShader(null); p.setColor(color); }
    private void rect(Canvas c,float l,float t,float r,float b,float radius,int color) {
        fill(color);
        c.drawRoundRect(l,t,r,b,radius,radius,p);
    }
    private void bubble(Canvas c,float cx,float cy,float rx,float ry) {
        c.save();
        c.translate(cx,cy);
        c.scale(rx,ry);
        fill(0xff516a09);
        c.drawCircle(0,0,1,p);
        fill(-1);
        p.setShader(air);
        c.drawCircle(0,0,.94f,p);
        c.restore();
    }
    private void tube(Canvas c,float length,float value) {
        rect(c,-3,-3,length+3,67,6,0xff4b4f46);
        rect(c,-2,-2,length+2,66,5,0xff10120f);
        fill(-1);
        p.setShader(glass);
        c.drawRoundRect(4,4,length-4,60,4,4,p);
        if(message.isEmpty()) bubble(c,length/2+travel(value)*(length/2-32),32,23,18);
        fill(0xcc2b3907);
        p.setStrokeWidth(1.5f);
        for(int sign:new int[]{-1,1}) {
            float marker=length/2+sign*29;
            c.drawLine(marker,3,marker,61,p);
            c.drawLine(marker+3*sign,3,marker+3*sign,61,p);
        }
    }
    private float travel(float v) { return Math.max(-1,Math.min(1,v/15)); }
    private void ring(Canvas c,float r,int color) {
        fill(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        c.drawCircle(0,0,r,p);
        p.setStyle(Paint.Style.FILL);
    }
    @Override protected void onDraw(Canvas c) {
        c.drawColor(0xff181a18);
        float scale=Math.min(getWidth()/330f,getHeight()/410f);
        c.save();
        c.translate((getWidth()-300*scale)/2,(getHeight()-380*scale)/2);
        c.scale(scale,scale);
        tube(c,300,x);
        c.save();
        c.translate(302,84);
        c.rotate(90);
        tube(c,294,-y);
        c.restore();
        c.save();
        c.translate(108,192);
        fill(0xff50534a);
        c.drawCircle(0,0,109,p);
        fill(0xff0b0d0a);
        c.drawCircle(0,0,107,p);
        fill(-1);
        p.setShader(pool);
        c.drawCircle(0,0,99,p);
        if(message.isEmpty()) {
            float norm=(float)Math.max(15,Math.hypot(x,y));
            bubble(c,79*x/norm,-79*y/norm,18,18);
        }
        fill(0xcc293902);
        p.setStrokeWidth(1.3f);
        c.drawLine(-99,0,99,0,p);
        c.drawLine(0,-99,0,99,p);
        ring(c,24,0xff354808);
        c.restore();
        rect(c,0,329,217,378,5,0xff464a41);
        rect(c,1,330,216,377,4,0xff10130f);
        fill(0xffe6ecda);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        p.setTextSize(message.isEmpty()?21:15);
        c.drawText(message.isEmpty()?reading:message,108,354-(p.ascent()+p.descent())/2,p);
        c.restore();
    }
}
