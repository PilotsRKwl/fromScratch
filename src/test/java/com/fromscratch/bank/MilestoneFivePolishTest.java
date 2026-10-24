package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.wealth.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic Swing layout, tooltip and confirmation tests only; never interacts with RuneScape. */
public class MilestoneFivePolishTest
{
	private static FinancialState financial(int unknown)
	{
		Map<Integer,Long> types=new TreeMap<>();for(int i=0;i<unknown;i++){types.put(100000+i,1L);}
		return new FinancialState(SnapshotFixtures.TIME,true,types,3_963_708,types,0,0,unknown);
	}
	private static BankView setup()
	{
		return new BankView(null,new BankObservation(UUID.randomUUID(),1,SnapshotFixtures.TIME,BankFixtures.bank(1),"Bank ready."),Collections.emptyMap(),false,false,"Bank ready.");
	}
	private static JButton button(BankPanel panel,String text)
	{
		return (JButton)BankPolishTest.all(panel).stream().filter(c->c instanceof JButton&&(text.equals(((JButton)c).getText())||text.equals(((JButton)c).getAccessibleContext().getAccessibleName()))).findFirst().get();
	}
	private static JCheckBox checkbox(BankPanel panel){return (JCheckBox)BankPolishTest.all(panel).stream().filter(c->c instanceof JCheckBox).findFirst().get();}
	private static JLabel named(BankPanel panel,String name){return (JLabel)BankPolishTest.all(panel).stream().filter(c->name.equals(c.getName())).findFirst().get();}
	private static void layout(Container root){root.doLayout();for(Component c:root.getComponents()){if(c instanceof Container){layout((Container)c);}}}
	private static List<String> labels(BankPanel panel){List<String> result=new ArrayList<>();for(Component c:BankPolishTest.all(panel)){if(c instanceof JLabel&&c.isVisible()){result.add(((JLabel)c).getText());}}return result;}
	@Test public void gainedTooltipHasExactGpAndNoTechnicalDates(){String tip=BankTooltips.gained(financial(1));assertTrue(tip.contains("Estimated Value Gained"));assertTrue(tip.contains("3,963,708 GP"));assertFalse(tip.contains("Measured from"));assertFalse(tip.contains("imported"));assertFalse(tip.contains("2026-"));assertFalse(tip.matches("(?s).*\\d{4}-\\d{2}-\\d{2}T.*"));assertTrue(tip.length()<420);}
	@Test public void tooltipGrammarHandlesOneUnknownItem(){assertTrue(BankTooltips.gained(financial(1)).contains("1 item type has an unknown historical price."));}
	@Test public void tooltipGrammarHandlesManyUnknownItems(){assertTrue(BankTooltips.gained(financial(2)).contains("2 item types have unknown historical prices."));}
	@Test public void noUnknownItemsOmitsUnnecessaryUnknownSentence(){assertFalse(BankTooltips.gained(financial(0)).contains("historical price"));}
	@Test public void missingFinancialDataUsesConciseUnavailableText(){assertTrue(BankTooltips.gained(null).contains("unavailable"));assertTrue(BankTooltips.netWorth(null).contains("unavailable"));}
	@Test public void tooltipHtmlEscapesAllDynamicContent(){String tip=BankTooltips.html("<Title>","A & B <img src='unsafe'>");assertTrue(tip.contains("&lt;Title&gt;"));assertTrue(tip.contains("A &amp; B"));assertFalse(tip.contains("<img"));}
	@Test public void realSwingTooltipWrapsWithinThreeHundredPixels()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{JToolTip tooltip=new JToolTip();tooltip.setTipText(BankTooltips.gained(financial(1)));Dimension size=tooltip.getPreferredSize();assertTrue("Tooltip width: "+size.width,size.width>=250&&size.width<=300);assertTrue(size.height>35&&size.height<160);});
	}
	@Test public void marketAndCashTooltipsAreShortAndDateFree(){BankValuation value=new BankValuation(UUID.randomUUID(),-12345L,2,1,SnapshotFixtures.TIME,1);String tip=BankTooltips.netWorth(value);assertTrue(tip.contains("-12,345 GP"));assertTrue(tip.contains("2 item types have no available price"));assertTrue(tip.contains("1 item type uses an older price"));assertFalse(tip.contains(SnapshotFixtures.TIME.toString()));assertTrue(BankTooltips.bankGp(-12345).contains("-12,345 GP"));}
	@Test public void allFinancialTileSurfacesShareTheWrappedTooltip()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);BankTile tile=(BankTile)BankPolishTest.all(panel).stream().filter(c->c instanceof BankTile&&"Peak Progress Value".equals(c.getAccessibleContext().getAccessibleName())).findFirst().get();assertTrue(tile.getToolTipText().startsWith("<html>"));for(Component c:BankPolishTest.all(tile)){if(c instanceof JLabel){assertEquals(tile.getToolTipText(),((JLabel)c).getToolTipText());}}});
	}
	@Test public void ordinarySetupOmitsNotStartedSubtitle()throws Exception{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);assertFalse(labels(panel).contains("Not Started"));assertTrue(labels(panel).contains("Before Starting"));});}
	@Test public void ordinaryDashboardOmitsActiveSubtitle()throws Exception{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);assertFalse(labels(panel).contains("Active"));});}
	@Test public void actionableErrorsRemainVisible()throws Exception{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(new BankView(BankFixtures.run(1),null,Collections.emptyMap(),false,true,"Storage unavailable. Retry loading."),null);assertTrue(labels(panel).stream().anyMatch(t->t!=null&&t.contains("Storage unavailable")));assertTrue(button(panel,"Retry Loading").isVisible());});}
	@Test public void elsewhereTextIsCenteredWithMatchingTileHeights()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);panel.setSize(225,960);layout(panel);JLabel help=named(panel,"elsewhereHelp");assertEquals(SwingConstants.CENTER,help.getHorizontalAlignment());assertEquals(SwingConstants.CENTER,help.getVerticalAlignment());assertTrue(help.getText().contains("text-align:center"));JPanel row=(JPanel)help.getParent().getParent();assertEquals(2,((GridLayout)row.getLayout()).getColumns());assertEquals(row.getComponent(0).getHeight(),row.getComponent(1).getHeight());assertTrue(help.getPreferredSize().width<=help.getWidth());assertTrue(help.getPreferredSize().height<=help.getHeight());});
	}
	@Test public void resetButtonIsCenteredInsideItsDangerArea()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);panel.setSize(225,960);layout(panel);JButton reset=button(panel,"Reset Progress");assertTrue(Math.abs(reset.getX()*2+reset.getWidth()-reset.getParent().getWidth())<=1);assertTrue(reset instanceof BankDangerButton);assertEquals(Color.WHITE,reset.getForeground());assertTrue(reset.getWidth()>=170);});
	}
	@Test public void setupCheckboxFollowsInstructionTwoWithoutLargeGap()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);panel.setSize(225,960);layout(panel);JLabel intro=named(panel,"setupInstructions");JCheckBox box=checkbox(panel);assertTrue(box.getY()-(intro.getY()+intro.getHeight())<=4);assertTrue(box.getY()>=intro.getY()+intro.getHeight());assertTrue(intro.getPreferredSize().width<=intro.getWidth());assertTrue(box.getPreferredSize().height<=box.getHeight());});
	}
	@Test public void startButtonIsCenteredAndFitsNarrowSidebar()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);panel.setSize(225,960);layout(panel);JButton start=button(panel,"Start From Scratch");assertTrue(start instanceof BankPrimaryButton);assertTrue(Math.abs(start.getX()*2+start.getWidth()-start.getParent().getWidth())<=1);assertTrue(start.getWidth()<=start.getParent().getWidth());assertTrue(start.getHeight()>=44);assertEquals(SwingConstants.CENTER,start.getHorizontalAlignment());assertEquals(SwingConstants.CENTER,start.getVerticalAlignment());});
	}
	@Test public void primaryButtonHasGreenWhiteHoverPressedAndDisabledStates()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPrimaryButton start=new BankPrimaryButton("Start From Scratch");start.setSize(190,44);assertEquals(BankPrimaryButton.DISABLED.getRGB(),pixel(start));start.setEnabled(true);assertEquals(Color.WHITE,start.getForeground());assertEquals(BankPrimaryButton.NORMAL.getRGB(),pixel(start));start.getModel().setRollover(true);assertEquals(BankPrimaryButton.HOVER.getRGB(),pixel(start));start.getModel().setArmed(true);start.getModel().setPressed(true);assertEquals(BankPrimaryButton.PRESSED.getRGB(),pixel(start));});
	}
	private static int pixel(JComponent component){BufferedImage image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();component.paint(g);g.dispose();return image.getRGB(5,5);}
	@Test public void startRequiresAttestationCompleteBankAndSafeState()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);JButton start=button(panel,"Start From Scratch");assertFalse(checkbox(panel).isSelected());assertFalse(start.isEnabled());checkbox(panel).doClick(0);assertTrue(start.isEnabled());BankView view=setup();panel.render(new BankView(null,view.getObservation(),Collections.emptyMap(),true,false,"Saving"),null);assertFalse(start.isEnabled());panel.render(new BankView(null,view.getObservation(),Collections.emptyMap(),false,true,"Failed"),null);assertFalse(start.isEnabled());panel.render(new BankView(null,null,Collections.emptyMap(),false,false,"Open bank"),null);assertFalse(start.isEnabled());});
	}
	private static JCheckBox notUnlocked(BankPanel panel){return (JCheckBox)BankPolishTest.all(panel).stream().filter(c->"potionNotUnlocked".equals(c.getName())).findFirst().get();}
	private static BankActions actions(AtomicInteger starts,AtomicInteger resets){return new BankActions(){public void startRun(boolean attested){assertTrue(attested);starts.incrementAndGet();}public void reset(UUID revision){resets.incrementAndGet();}public void retry(){}public void correct(UUID r,int id,long q){}public void anchor(UUID r,com.fromscratch.model.ContainerSnapshot b,boolean approve){}};}
	@Test public void startStillRequiresTheExistingConfirmationAndCancelIsNonMutating()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{AtomicInteger shown=new AtomicInteger(),starts=new AtomicInteger();BankPanel panel=new BankPanel(new BankIcons(id->null),(owner,title,text,action,typed)->{shown.incrementAndGet();assertEquals("Start From Scratch?",title);assertEquals(BankPanel.START_WARNING,text);assertEquals("Start Run",action);assertFalse(typed);return false;});panel.render(setup(),actions(starts,new AtomicInteger()));checkbox(panel).doClick(0);notUnlocked(panel).doClick(0);button(panel,"Start From Scratch").doClick(0);assertEquals(1,shown.get());assertEquals(0,starts.get());});
	}
	@Test public void resetStillRequestsTypedConfirmationAndCancelDoesNothing()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{AtomicInteger shown=new AtomicInteger(),resets=new AtomicInteger();BankPanel panel=new BankPanel(new BankIcons(id->null),(owner,title,text,action,typed)->{shown.incrementAndGet();assertTrue(typed);assertEquals("Reset Progress",title);return false;});panel.render(BankPolishTest.view(),actions(new AtomicInteger(),resets));button(panel,"Reset Progress").doClick(0);assertEquals(1,shown.get());assertEquals(0,resets.get());assertFalse(BankDialogs.resetConfirmed(""));assertTrue(BankDialogs.resetConfirmed("RESET"));});
	}
	@Test public void hiddenDashboardDoesNotMakeSetupNeedlesslyTall()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);int setupHeight=panel.getPreferredSize().height;panel.render(BankPolishTest.view(),null);assertTrue("Hidden dashboard must not determine setup height",panel.getPreferredSize().height>setupHeight);});
	}
	@Test public void offscreenPolishStatesRemainReadableAtSidebarWidth()throws Exception
	{
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(setup(),null);panel.setSize(225,960);layout(panel);save(panel,"m5-setup-unchecked.png");checkbox(panel).doClick(0);notUnlocked(panel).doClick(0);assertTrue(button(panel,"Start From Scratch").isEnabled());save(panel,"m5-setup-green.png");panel.render(BankPolishTest.view(),null);layout(panel);save(panel,"m5-dashboard-polish.png");JToolTip tooltip=new JToolTip();tooltip.setTipText(BankTooltips.gained(financial(1)));tooltip.setSize(tooltip.getPreferredSize());save(tooltip,"m5-gains-tooltip.png");});
	}
	private static void save(JComponent component,String name)
	{
		BufferedImage image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();component.paint(graphics);graphics.dispose();try{java.nio.file.Path file=java.nio.file.Paths.get("build/reports",name);java.nio.file.Files.createDirectories(file.getParent());javax.imageio.ImageIO.write(image,"png",file.toFile());}catch(java.io.IOException ex){throw new AssertionError(ex);}
	}
}
