package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
/** Deliberately small Java2D adapter for executing the unmodified production painter. */
public final class Canvas {
 private Graphics2D graphics;
 private final ArrayList<Graphics2D> stack=new ArrayList<>();
 public int operations;
 public Canvas(BufferedImage bitmap){graphics=bitmap.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);}
 public int save(){stack.add(graphics);graphics=(Graphics2D)graphics.create();return stack.size();}
 public void restoreToCount(int value){while(stack.size()>=value){graphics.dispose();graphics=stack.remove(stack.size()-1);}}
 public void translate(float x,float y){graphics.translate(x,y);}public void scale(float x,float y){graphics.scale(x,y);}
 public void scale(float x,float y,float cx,float cy){graphics.translate(cx,cy);graphics.scale(x,y);graphics.translate(-cx,-cy);}
 public void rotate(float degrees){graphics.rotate(Math.toRadians(degrees));}public void rotate(float degrees,float x,float y){graphics.rotate(Math.toRadians(degrees),x,y);}
 public void clipPath(Path path){graphics.clip(path.value);}public void clipRect(float l,float t,float r,float b){graphics.clip(new Rectangle2D.Float(l,t,r-l,b-t));}
 private void prepare(Paint p){
  operations++;if(!Float.isFinite(p.weight)||p.weight<0||!Float.isFinite(p.size)||p.size<=0)throw new AssertionError("invalid drawing geometry");
  graphics.setComposite(AlphaComposite.SrcOver.derive(p.shader==null?1f:(p.color>>>24)/255f));
  graphics.setPaint(p.shader==null?new java.awt.Color(p.color,true):p.shader.awt());
  graphics.setStroke(new BasicStroke(p.weight,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
 }
 private void shape(Shape shape,Paint p){prepare(p);if(p.style==Paint.Style.STROKE)graphics.draw(shape);else graphics.fill(shape);}
 public void drawCircle(float x,float y,float r,Paint p){shape(new Ellipse2D.Float(x-r,y-r,r*2,r*2),p);}
 public void drawRoundRect(float l,float t,float r,float b,float x,float y,Paint p){shape(new RoundRectangle2D.Float(l,t,r-l,b-t,x*2,y*2),p);}
 public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}
 public void drawPath(Path path,Paint p){shape(path.value,p);}
 public void drawLine(float x,float y,float x2,float y2,Paint p){prepare(p);graphics.draw(new Line2D.Float(x,y,x2,y2));}
 public void drawArc(RectF r,float angle,float sweep,boolean center,Paint p){shape(new Arc2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,-angle,-sweep,Arc2D.OPEN),p);}
 public void drawText(String value,float x,float y,Paint p){prepare(p);graphics.setFont(p.font());graphics.drawString(value,x-p.measureText(value)/2,y);}
 public void close(){if(!stack.isEmpty())throw new AssertionError("unbalanced Canvas save/restore");graphics.dispose();}
}
