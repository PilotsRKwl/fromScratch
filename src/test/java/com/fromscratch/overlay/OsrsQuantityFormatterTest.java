package com.fromscratch.overlay;

import java.awt.*;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class OsrsQuantityFormatterTest
{
	@Test public void everySmallQuantityIsExactWithoutSeparators(){for(long i=0;i<=9999;i++){assertEquals(Long.toString(i),VirtualQuantityFormatter.signed(i));assertEquals(Long.toString(-i),VirtualQuantityFormatter.signed(-i));}}
	@Test public void requiredExamplesTruncateWithoutDecimals(){long[] quantities={0,1,999,1740,9999,10000,10999,99999,999999,1000000,1740000,12500000,-1,-1740,-10000};String[] labels={"0","1","999","1740","9999","10K","10K","99K","999K","1000K","1740K","12M","-1","-1740","-10K"};for(int i=0;i<quantities.length;i++){assertEquals(labels[i],VirtualQuantityFormatter.signed(quantities[i]));}}
	@Test public void suffixBoundariesDoNotRoundUp(){assertEquals("9999",VirtualQuantityFormatter.signed(9999));assertEquals("10K",VirtualQuantityFormatter.signed(10000));assertEquals("999K",VirtualQuantityFormatter.signed(999999));assertEquals("1000K",VirtualQuantityFormatter.signed(1000000));assertEquals("-999K",VirtualQuantityFormatter.signed(-999999));}
	@Test public void billionsUseIntegerMillionsAsOsrsStacks(){assertEquals("1000M",VirtualQuantityFormatter.signed(1_000_000_000));assertEquals("2147M",VirtualQuantityFormatter.signed(Integer.MAX_VALUE));assertEquals("-2147M",VirtualQuantityFormatter.signed(-((long)Integer.MAX_VALUE)));}
	@Test public void bothLongBoundsAvoidNegationOverflow(){assertEquals("9223372036854M",VirtualQuantityFormatter.signed(Long.MAX_VALUE));assertEquals("-9223372036854M",VirtualQuantityFormatter.signed(Long.MIN_VALUE));}
	@Test public void thousandsContinueUntilTenMillion(){long[] quantities={10000,99999,999999,1000000,1740000,9999999,10000000,10999999,25600000,100000000};String[] labels={"10K","99K","999K","1000K","1740K","9999K","10M","10M","25M","100M"};for(int i=0;i<quantities.length;i++){assertEquals(labels[i],VirtualQuantityFormatter.signed(quantities[i]));assertEquals("-"+labels[i],VirtualQuantityFormatter.signed(-quantities[i]));}}
	@Test public void noDecimalsSeparatorsOrUpwardRounding(){assertEquals("9999K",VirtualQuantityFormatter.signed(9999999));assertEquals("10M",VirtualQuantityFormatter.signed(10999999));for(long q:new long[]{1740,9999,10000,1000000,9999999,10000000,Long.MAX_VALUE,Long.MIN_VALUE}){assertFalse(VirtualQuantityFormatter.signed(q).contains("."));assertFalse(VirtualQuantityFormatter.signed(q).contains(","));}}
	@Test public void widerAndExtremeSignedLabelsFitWithoutMovingTheirBaseline()
	{
		Graphics2D graphics=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB).createGraphics();try{graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());Rectangle bounds=new Rectangle(20,20,36,32);FontMetrics metrics=graphics.getFontMetrics();for(long quantity:new long[]{1000000,-1000000,9999999,-9999999,Long.MAX_VALUE,Long.MIN_VALUE}){String label=VirtualQuantityFormatter.signed(quantity);double scale=BankItemPainter.textScale(bounds,metrics,label);assertTrue(scale>0&&scale<=1);assertTrue((metrics.stringWidth(label)+1)*scale<=bounds.width-4+0.001);assertEquals(bounds.y+bounds.height-2-metrics.getDescent(),BankItemPainter.textY(bounds,metrics));}}finally{graphics.dispose();}
	}
	@Test public void fittedPaintingStaysInsideItemAndViewportAndPreservesGraphicsState()
	{
		BufferedImage image=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=image.createGraphics();try{graphics.setClip(0,0,100,100);Rectangle bounds=new Rectangle(20,20,36,32);Font original=graphics.getFont();java.awt.geom.AffineTransform transform=graphics.getTransform();for(long quantity:new long[]{-9999999,Long.MIN_VALUE}){BankItemPainter.paint(graphics,bounds,quantity,VirtualQuantityFormatter.signed(quantity),new BankOverlayState.Settings(true,false,Color.GREEN));}int text=0;for(int y=0;y<100;y++){for(int x=0;x<100;x++){if(image.getRGB(x,y)!=0){assertTrue(bounds.contains(x,y));if(image.getRGB(x,y)==BankOverlayState.DEFAULT_NEGATIVE_COLOR.getRGB()){text++;}}}}assertTrue(text>0);assertEquals(original,graphics.getFont());assertEquals(transform,graphics.getTransform());assertEquals(new Rectangle(0,0,100,100),graphics.getClipBounds());}finally{graphics.dispose();}
	}
	@Test public void compactMoneySharesStackThresholdsButRetainsGpUnit(){assertEquals("1740K",VirtualQuantityFormatter.signed(1740000));assertEquals("1740K GP",com.fromscratch.wealth.GpFormatter.compact(1740000));}
	@Test public void rightAlignmentUsesActualSignedLabelWidth(){Graphics2D g=new BufferedImage(80,80,BufferedImage.TYPE_INT_ARGB).createGraphics();try{Rectangle bounds=new Rectangle(20,10,36,32);for(long n:new long[]{-10000,1740,999999,1000000}){String label=VirtualQuantityFormatter.signed(n);assertEquals(bounds.x+bounds.width-2,BankItemPainter.textX(bounds,g.getFontMetrics(),label)+g.getFontMetrics().stringWidth(label));}}finally{g.dispose();}}
}
