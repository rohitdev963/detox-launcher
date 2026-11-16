package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
/** Host preview uses the same angular stops; Android shader implementation is not emulated. */
public final class SweepGradient extends Shader {
 private final java.awt.Paint gradient;
 public SweepGradient(float cx,float cy,int[] colors,float[] positions){
  gradient=new java.awt.Paint(){
   public int getTransparency(){return Transparency.TRANSLUCENT;}
   public PaintContext createContext(ColorModel model,Rectangle device,Rectangle2D user,AffineTransform transform,RenderingHints hints){
    final AffineTransform inverse;try{inverse=transform.createInverse();}catch(NoninvertibleTransformException e){throw new IllegalStateException(e);}
    return new PaintContext(){
     public void dispose(){} public ColorModel getColorModel(){return ColorModel.getRGBdefault();}
     public Raster getRaster(int x,int y,int w,int h){
      WritableRaster raster=getColorModel().createCompatibleWritableRaster(w,h);int[] data=new int[w*h];
      Point2D.Double point=new Point2D.Double();
      for(int row=0;row<h;row++)for(int col=0;col<w;col++){
       point.setLocation(x+col+.5,y+row+.5);inverse.transform(point,point);
       double fraction=Math.atan2(point.y-cy,point.x-cx)/(2*Math.PI);if(fraction<0)fraction++;
       double scaled=fraction*(colors.length-1);int at=Math.min(colors.length-2,(int)scaled);float blend=(float)(scaled-at);int a=colors[at],b=colors[at+1],out=0;
       for(int shift=0;shift<=24;shift+=8){int first=a>>>shift&255,last=b>>>shift&255;out|=Math.round(first+(last-first)*blend)<<shift;}
       data[row*w+col]=out;
      }
      raster.setDataElements(0,0,w,h,data);return raster;
     }
    };
   }
  };
 }
 java.awt.Paint awt(){return gradient;}
}
