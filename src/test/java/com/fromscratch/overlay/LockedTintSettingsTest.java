package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class LockedTintSettingsTest
{
	@Test public void defaultTintIsDarkTranslucentAndTextIsBrightGreen() { BankOverlayState state = new BankOverlayState(); state.configure(new FromScratchConfig(){}); assertEquals(new Color(51,51,51,128), state.getSettings().getLockedTint()); assertEquals(new Color(0x1DFF00), state.getSettings().getColor()); }
	@Test public void liveColorAndOpacityChangeIndependently() { BankOverlayState state = new BankOverlayState(); state.configure(new FromScratchConfig(){ public Color lockedItemBackgroundColor(){return Color.RED;} public int lockedItemBackgroundOpacity(){return 25;} }); assertEquals(new Color(255,0,0,64), state.getSettings().getLockedTint()); assertEquals(BankOverlayState.DEFAULT_COLOR, state.getSettings().getColor()); }
	@Test public void opacityIsClampedAtPersistenceConfigurationBoundary() { assertEquals(0, new BankOverlayState.Settings(true,true,Color.MAGENTA,Color.BLUE,-10).getLockedTint().getAlpha()); assertEquals(255, new BankOverlayState.Settings(true,true,Color.MAGENTA,Color.BLUE,200).getLockedTint().getAlpha()); }
	@Test public void transparentAndPositiveItemsKeepOriginalPixels() { BufferedImage image = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); g.setColor(Color.GREEN); g.fillRect(0,0,40,40); BankItemPainter.paint(g,new Rectangle(0,0,32,32),0,"0",new BankOverlayState.Settings(false,true,Color.MAGENTA,Color.RED,0)); assertEquals(Color.GREEN.getRGB(),image.getRGB(10,10)); BankItemPainter.paint(g,new Rectangle(0,0,32,32),1,"1",new BankOverlayState.Settings(false,true,Color.MAGENTA,Color.RED,100)); assertEquals(Color.GREEN.getRGB(),image.getRGB(10,10)); g.dispose(); }
	@Test public void negativeTintUsesFullBoundsAndRespectsIncomingClip() { BufferedImage image = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); g.setClip(0,8,40,20); BankItemPainter.paint(g,new Rectangle(4,0,32,32),-1,"-1",new BankOverlayState.Settings(false,true,Color.MAGENTA,Color.RED,50)); assertEquals(0,image.getRGB(10,7)); assertEquals(image.getRGB(10,8),image.getRGB(10,27)); assertEquals(0,image.getRGB(10,28)); g.dispose(); }
	@Test public void negativeCounterHasUnfadedConfiguredPixelsAndBlackEdgeOverCoinColors()
	{
		for (Color coins : new Color[]{new Color(0xFFFF00), new Color(0xFFD54F)})
		{
			BufferedImage image=new BufferedImage(48,40,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); g.setColor(coins);g.fillRect(0,0,48,40);
			BankItemPainter.paint(g,new Rectangle(4,0,36,32),-1,"-1",new BankOverlayState.Settings(true,true,BankOverlayState.DEFAULT_COLOR));g.dispose();
			int green=0,black=0;for(int y=20;y<32;y++){for(int x=4;x<40;x++){if(image.getRGB(x,y)==BankOverlayState.DEFAULT_NEGATIVE_COLOR.getRGB()){green++;}if(image.getRGB(x,y)==Color.BLACK.getRGB()){black++;}}}
			assertTrue("Counter color must survive tint",green>0);assertTrue("Shadow separates the negative counter from bright coin colors",black>0);
		}
	}
}
