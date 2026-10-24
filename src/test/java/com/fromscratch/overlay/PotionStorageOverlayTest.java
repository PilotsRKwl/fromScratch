package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.potion.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PotionStorageOverlayTest
{
	private static final int ITEM=net.runelite.api.gameval.ItemID._1DOSEPRAYERRESTORE;
	private static BankView view(long amount,boolean verified)
	{
		Map<Integer,Integer> variants=new TreeMap<>();variants.put(ITEM,1);variants.put(net.runelite.api.gameval.ItemID._4DOSEPRAYERRESTORE,4);PotionType type=new PotionType(1,"Prayer potion",false,variants);PotionObservation observed=new PotionObservation(true,Collections.singletonMap(1,type),Collections.singletonMap(1,1045L),SnapshotFixtures.TIME);BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BankFixtures.bank(0),true,UUID.randomUUID(),SnapshotFixtures.TIME,observed);run=BankEngine.correctStorage(run,1,amount,SnapshotFixtures.TIME);return new BankView(run,null,Collections.emptyMap(),false,false,"",null,null,verified?observed:PotionObservation.unknown());
	}
	private static final class NativeView implements PotionStorageOverlay.Context
	{
		boolean visible=true,current=true;String character=SnapshotFixtures.CHARACTER_A;Rectangle viewport=new Rectangle(0,0,520,100);java.util.List<PotionStorageOverlay.Entry> entries=new ArrayList<>();java.util.List<String> hints=new ArrayList<>();int visits;
		public String character(){return character;}public boolean visible(){return visible;}public boolean current(){return current;}public Rectangle viewport(){return viewport;}public void entries(java.util.function.Consumer<PotionStorageOverlay.Entry> consumer){visits++;entries.forEach(consumer);}public void hint(Rectangle bounds,String text){hints.add(text);}
	}
	private static PotionStorageOverlay.Entry entry(int item,int x,int y)
	{return new PotionStorageOverlay.Entry(item,new Rectangle(x,y,32,32),new Rectangle(x+40,y,175,15),new Rectangle(x+40,y+17,175,16),new Rectangle(x+225,y+12,18,18),"Doses: 1,045",-1,0,x==10?270:Integer.MAX_VALUE);}
	@Test public void actualOverlayPipelineUsesInterfaceHookAndDrawsBeyondSmallWithdrawalControl()
	{
		BankOverlayState state=new BankOverlayState();state.enable();BankView view=view(12,true);state.updateBank(view);NativeView nativeView=new NativeView();nativeView.entries.add(entry(ITEM,10,10));nativeView.entries.add(entry(net.runelite.api.gameval.ItemID._4DOSEPRAYERRESTORE,270,10));PotionStorageOverlay overlay=new PotionStorageOverlay(nativeView,state);assertEquals(net.runelite.client.ui.overlay.OverlayLayer.MANUAL,overlay.getLayer());assertTrue(overlay.getDrawHooks().contains((net.runelite.api.gameval.InterfaceID.BANKMAIN<<16)|0xffff));
		BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{Rectangle withdrawalControl=new Rectangle(10,10,32,32);assertNull(PotionStorageOverlay.paint(g,withdrawalControl,nativeView.entries.get(0).getPhysical(),null,80,"FS: 12",Color.GREEN));overlay.render(g);assertEquals(2,nativeView.hints.size());assertTrue(nativeView.hints.get(0).contains("FS: 12 doses"));assertTrue(hasColor(image,state.getSettings().getColor()));assertEquals(12,view.getRun().getPotions().balance(1));}finally{g.dispose();}
	}
	@Test public void wholeRenderPathHandlesUnknownZeroNegativeColorAndClosedDisabledStates()
	{
		BankOverlayState state=new BankOverlayState();state.enable();NativeView nativeView=new NativeView();nativeView.entries.add(entry(ITEM,10,10));PotionStorageOverlay overlay=new PotionStorageOverlay(nativeView,state);
		for(long n:new long[]{0,12,-3}){state.updateBank(view(n,true));state.configure(new FromScratchConfig(){public Color virtualQuantityColor(){return Color.CYAN;}});nativeView.hints.clear();BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{overlay.render(g);assertTrue(nativeView.hints.get(0).contains("FS: "+n+" doses"));assertTrue(hasColor(image,n<0?BankOverlayState.DEFAULT_NEGATIVE_COLOR:Color.CYAN));}finally{g.dispose();}}
		state.updateBank(view(12,false));nativeView.hints.clear();BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{overlay.render(g);assertTrue(nativeView.hints.get(0).contains("unverified"));int before=nativeView.visits;nativeView.visible=false;overlay.render(g);assertEquals(before,nativeView.visits);nativeView.visible=true;state.disable();overlay.render(g);assertEquals(before,nativeView.visits);state.enable();state.updateBank(new BankView(null,null,Collections.emptyMap(),false,false,""));overlay.render(g);assertEquals(before,nativeView.visits);}finally{g.dispose();}
	}
	@Test public void wholeRenderPathClipsScrollAndRebuildsWithoutDuplicateOrCrossColumnLabels()
	{
		BankOverlayState state=new BankOverlayState();state.enable();state.updateBank(view(12,true));NativeView nativeView=new NativeView();nativeView.viewport=new Rectangle(0,20,520,25);nativeView.entries.add(entry(ITEM,10,0));PotionStorageOverlay overlay=new PotionStorageOverlay(nativeView,state);BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{overlay.render(g);assertEquals(1,nativeView.hints.size());nativeView.entries.clear();nativeView.entries.add(entry(ITEM,270,12));nativeView.hints.clear();overlay.render(g);assertEquals(1,nativeView.hints.size());for(int x=0;x<520;x++){for(int y=0;y<100;y++){if(y<20||y>=45){assertEquals(0,image.getRGB(x,y));}}}assertEquals(243,PotionStorageOverlay.entryBounds(new Rectangle(0,0,520,100),entry(ITEM,10,0)).width);}finally{g.dispose();}
	}
	@Test public void committedLabelsUseExactSignedUnitsAndFollowFreshnessColorAndCleanup()
	{
		BankOverlayState state=new BankOverlayState();state.enable();for(long n:new long[]{24,0,-3}){state.updateBank(view(n,true));BankOverlayState.PotionLabel label=state.getProjection().getPotionLabels().get(ITEM);assertEquals("FS: "+n,label.getText());assertEquals(n<0,label.isNegative());assertTrue(state.isPotionCurrent());}
		state.updateBank(view(24,false));assertFalse(state.isPotionCurrent());assertEquals(new Color(0x1DFF00),state.getSettings().getColor());state.configure(new FromScratchConfig(){public Color virtualQuantityColor(){return Color.CYAN;}});assertEquals(Color.CYAN,state.getSettings().getColor());state.disable();assertNull(state.getProjection());state.enable();state.updateBank(new BankView(null,null,Collections.emptyMap(),false,false,""));assertNull(state.getProjection());
	}
	@Test public void labelUsesNativeTextWidthAndDoesNotCoverPhysicalNumberSpriteOrHeart()
	{
		BufferedImage image=new BufferedImage(300,70,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{g.setFont(new Font(Font.DIALOG,Font.PLAIN,11));Rectangle row=new Rectangle(10,10,270,42),nativeLabel=new Rectangle(50,29,150,16),heart=new Rectangle(257,22,20,20);Rectangle painted=PotionStorageOverlay.paint(g,row,nativeLabel,heart,80,"FS: 24",Color.CYAN);assertNotNull(painted);assertTrue(painted.x>=138);assertTrue(row.contains(painted));assertFalse(heart.intersects(painted));for(int x=10;x<130;x++){for(int y=10;y<52;y++){assertEquals(0,image.getRGB(x,y));}}assertTrue(hasColor(image,Color.CYAN));}finally{g.dispose();}
	}
	@Test public void narrowRowsUseCompactMarkerWithoutTouchingNativeTextOrOtherColumn()
	{
		BufferedImage image=new BufferedImage(240,60,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{g.setFont(new Font(Font.DIALOG,Font.PLAIN,11));Rectangle nativeLabel=new Rectangle(40,24,120,16);Rectangle painted=PotionStorageOverlay.paint(g,new Rectangle(0,0,155,48),nativeLabel,null,80,"FS: -9,999,999,999",BankOverlayState.DEFAULT_NEGATIVE_COLOR);assertNotNull(painted);assertEquals(g.getFontMetrics().stringWidth("FS"),painted.width);assertTrue(painted.x+painted.width<155);assertNull(PotionStorageOverlay.paint(g,new Rectangle(0,0,130,48),nativeLabel,null,80,"FS: 0",Color.GREEN));assertTrue(hasColor(image,BankOverlayState.DEFAULT_NEGATIVE_COLOR));}finally{g.dispose();}
	}
	@Test public void repeatedPaintAndViewportClippingLeaveNoBackgroundOrDuplicateArtifacts()
	{
		BufferedImage first=new BufferedImage(300,70,BufferedImage.TYPE_INT_ARGB),second=new BufferedImage(300,70,BufferedImage.TYPE_INT_ARGB);for(BufferedImage img:new BufferedImage[]{first,second}){Graphics2D g=img.createGraphics();try{g.setFont(new Font(Font.DIALOG,Font.PLAIN,11));g.clip(new Rectangle(0,25,300,12));for(int n=0;n<(img==first?1:2);n++){PotionStorageOverlay.paint(g,new Rectangle(0,0,290,60),new Rectangle(40,24,120,16),null,60,"FS: 0",Color.GREEN);}}finally{g.dispose();}}for(int x=0;x<300;x++){for(int y=0;y<70;y++){assertEquals(first.getRGB(x,y),second.getRGB(x,y));if(y<25||y>=37){assertEquals(0,first.getRGB(x,y));}}}
	}
	private static boolean hasColor(BufferedImage image,Color color){for(int x=0;x<image.getWidth();x++){for(int y=0;y<image.getHeight();y++){if(image.getRGB(x,y)==color.getRGB()){return true;}}}return false;}
	@Test public void topAlignedNativeDoseBaselineIgnoresTallWidgetAndFollowsScroll()
	{
		BufferedImage image=new BufferedImage(580,160,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setFont(new Font(Font.DIALOG,Font.PLAIN,11));
		try{for(int x:new int[]{10,290}){for(int y:new int[]{10,45,80}){for(String nativeText:new String[]{"Doses: 8","Doses: 1,049","Doses: 12,858"}){
			Rectangle label=new Rectangle(x+40,y+15,175,38);int baseline=PotionStorageOverlay.textBaseline(label,0,12,0,3);assertEquals(y+27,baseline);Rectangle painted=PotionStorageOverlay.paint(g,new Rectangle(x,y,270,54),label,new Rectangle(x+244,y+13,20,20),g.getFontMetrics().stringWidth(nativeText),"FS: -16","FS: -16",Color.RED,baseline);assertNotNull(painted);assertEquals(baseline,painted.y+g.getFontMetrics().getAscent());assertTrue(painted.y+painted.height<y+54);assertFalse(painted.intersects(new Rectangle(x+244,y+13,20,20)));
		}}}}finally{g.dispose();}
	}
	@Test public void liveNegativeColorAndNeutralUnknownDoNotChangeOwnership()
	{
		BankOverlayState state=new BankOverlayState();state.enable();BankView view=view(-16,true);state.updateBank(view);NativeView nativeView=new NativeView();nativeView.entries.add(entry(ITEM,10,10));PotionStorageOverlay overlay=new PotionStorageOverlay(nativeView,state);
		for(Color color:new Color[]{Color.RED,Color.MAGENTA}){state.configure(new FromScratchConfig(){public Color negativeVirtualQuantityColor(){return color;}});BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();overlay.render(g);g.dispose();assertTrue(hasColor(image,color));assertEquals(-16,view.getRun().getPotions().balance(1));}
		state.updateBank(view(-16,false));BufferedImage image=new BufferedImage(520,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();overlay.render(g);g.dispose();assertTrue(hasColor(image,PotionStorageOverlay.UNKNOWN));
	}
}
