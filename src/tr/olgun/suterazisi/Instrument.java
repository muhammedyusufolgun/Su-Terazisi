package tr.olgun.suterazisi;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.TextView;
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
    private final TextView angles;
    private boolean available;
    private float x,y;
    private int headerBottom, lastX = Integer.MIN_VALUE, lastY;
    Instrument(Context context, TextView angles) {
        super(context);
        this.angles = angles;
    }
    void headerBottom(int bottom) {
        headerBottom = bottom;
        invalidate();
    }
    void status(String s) {
        available = false;
        lastX = Integer.MIN_VALUE;
        if (!s.contentEquals(angles.getText())) angles.setText(s);
        invalidate();
    }
    void measure(float x,float y) {
        available = true;
        int ax=Math.round(10*x), ay=Math.round(10*y);
        if(ax==lastX && ay==lastY) return;
        lastX=ax;
        lastY=ay;
        this.x=ax/10f;
        this.y=ay/10f;
        angles.setText(String.format(Locale.forLanguageTag("tr"),"X  %.1f°     Y  %.1f°",this.x,this.y));
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
        if(available) bubble(c,length/2+travel(value)*(length/2-32),32,23,18);
        fill(0xcc2b3907);
        p.setStrokeWidth(1.5f);
        for(int sign:new int[]{-1,1}) {
            float marker=length/2+sign*29;
            c.drawLine(marker,3,marker,61,p);
            c.drawLine(marker+3*sign,3,marker+3*sign,61,p);
        }
    }
    // More travel per degree near level; the scale continues through vertical.
    private float travel(float degrees) {
        double magnitude = Math.abs(degrees);
        return (float)Math.copySign(Math.log1p(magnitude/22) / Math.log1p(180.0/22), degrees);
    }
    private void edge(Canvas c,float left,float top,float length,float width,boolean vertical,float degrees) {
        c.save();
        c.translate(left,top);
        if(vertical) c.rotate(90);
        float scale=width/70;
        c.scale(scale,scale);
        c.translate(3,3);
        tube(c,length/scale-6,degrees);
        c.restore();
    }
    @Override protected void onDraw(Canvas c) {
        c.drawColor(0xff181a18);
        float w=getWidth(), h=getHeight();
        float density=getResources().getDisplayMetrics().density;
        float margin=4*density, gap=3*density;
        float width=Math.min(28*density, Math.min(w,h)*.075f);
        edge(c,margin,margin,w-2*margin,width,false,x);
        edge(c,margin,h-margin-width,w-2*margin,width,false,x);
        float top=margin+width+gap;
        edge(c,margin+width,top,h-2*top,width,true,-y);
        edge(c,w-margin,top,h-2*top,width,true,-y);
        float radius=Math.min(w/2-margin-width-8*density,
            h/2-Math.max(top,headerBottom)-8*density);
        if(radius<=0) return;
        c.save();
        c.translate(w/2,h/2);
        c.scale(radius/109,radius/109);
        fill(0xff50534a);
        c.drawCircle(0,0,109,p);
        fill(0xff0b0d0a);
        c.drawCircle(0,0,107,p);
        fill(-1);
        p.setShader(pool);
        c.drawCircle(0,0,99,p);
        if(available) {
            double magnitude=Math.hypot(x,y);
            float distance=79*travel((float)Math.min(180,magnitude));
            float factor=magnitude==0 ? 0 : (float)(distance/magnitude);
            bubble(c,x*factor,-y*factor,18,18);
        }
        fill(0xcc293902);
        p.setStrokeWidth(1.3f);
        c.drawLine(-99,0,99,0,p);
        c.drawLine(0,-99,0,99,p);
        p.setStyle(Paint.Style.STROKE);
        c.drawCircle(0,0,24,p);
        p.setStyle(Paint.Style.FILL);
        c.restore();
    }
}
