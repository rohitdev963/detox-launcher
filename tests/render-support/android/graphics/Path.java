package android.graphics;
import java.awt.geom.*;
public final class Path {
 public enum Direction{CW}
 final Path2D.Float value=new Path2D.Float();
 public void reset(){value.reset();}public void moveTo(float x,float y){value.moveTo(x,y);}
 public void lineTo(float x,float y){value.lineTo(x,y);}
 public void cubicTo(float a,float b,float c,float d,float e,float f){value.curveTo(a,b,c,d,e,f);}
 public void close(){value.closePath();}
 public void addCircle(float x,float y,float r,Direction direction){value.append(new Ellipse2D.Float(x-r,y-r,r*2,r*2),false);}
 public void addRoundRect(RectF r,float x,float y,Direction direction){value.append(new RoundRectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,x*2,y*2),false);}
}
