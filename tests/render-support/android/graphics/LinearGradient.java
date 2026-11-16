package android.graphics;
public final class LinearGradient extends Shader {
 private final java.awt.Paint gradient;
 public LinearGradient(float x,float y,float x2,float y2,int[] colors,float[] positions,TileMode mode){
  java.awt.Color[] stops=new java.awt.Color[colors.length];float[] fractions=new float[colors.length];
  for(int i=0;i<colors.length;i++){stops[i]=new java.awt.Color(colors[i],true);fractions[i]=positions==null?i/(float)(colors.length-1):positions[i];}
  gradient=new java.awt.LinearGradientPaint(x,y,x2,y2,fractions,stops);
 }
 java.awt.Paint awt(){return gradient;}
}
