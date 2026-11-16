package android.graphics;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
public final class Paint {
 public static final int ANTI_ALIAS_FLAG=1,SUBPIXEL_TEXT_FLAG=2;
 public enum Style{FILL,STROKE}public enum Align{CENTER}public enum Cap{ROUND}
 int color=0xFF000000;float weight=1,size=16;Style style=Style.FILL;Typeface typeface=new Typeface(new java.awt.Font("SansSerif",0,16));Shader shader;boolean bold;
 private static final FontRenderContext METRICS=new FontRenderContext(new AffineTransform(),true,true);
 public Paint(int flags){}
 public void setStyle(Style value){style=value;}public void setColor(int value){color=value;}public void setAlpha(int alpha){color=color&0xFFFFFF|alpha<<24;}
 public void setStrokeWidth(float value){weight=value;}public void setTypeface(Typeface value){typeface=value;}
 public void setTextSize(float value){size=value;}public void setTextAlign(Align value){}public void setStrokeCap(Cap value){}
 public void setFakeBoldText(boolean value){bold=value;}
 public void setShader(Shader value){shader=value;}
 public void setShadowLayer(float radius,float x,float y,int color){}public void clearShadowLayer(){}
 java.awt.Font font(){return typeface.font.deriveFont(bold?java.awt.Font.BOLD:java.awt.Font.PLAIN,size);}
 public float measureText(String text){return (float)font().getStringBounds(text,METRICS).getWidth();}
}
