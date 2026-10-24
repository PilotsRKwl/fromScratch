package com.fromscratch.overlay;

import com.fromscratch.bank.RuneLiteBankSource;
import java.awt.*;
import java.util.function.Consumer;
import javax.inject.*;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.*;
import net.runelite.client.util.Text;

/** Read-only decoration after the whole native bank interface, independent of child-zero hitbox size. */
@Singleton @lombok.extern.slf4j.Slf4j
public final class PotionStorageOverlay extends Overlay
{
	public static final Color UNKNOWN=new Color(0xB0B0B0);
	interface Context
	{
		String character();boolean visible();boolean current();Rectangle viewport();void entries(Consumer<Entry> consumer);void hint(Rectangle bounds,String text);
	}
	@lombok.Value static class Entry
	{
		int item;Rectangle sprite;Rectangle name;Rectangle physical;Rectangle heart;String nativeText;int nativeWidth;int alignment;int columnRight;int verticalAlignment;int fontBaseline;int lineHeight;
		Entry(int item,Rectangle sprite,Rectangle name,Rectangle physical,Rectangle heart,String nativeText,int nativeWidth,int alignment,int columnRight)
		{this(item,sprite,name,physical,heart,nativeText,nativeWidth,alignment,columnRight,0,-1,0);}
		Entry(int item,Rectangle sprite,Rectangle name,Rectangle physical,Rectangle heart,String nativeText,int nativeWidth,int alignment,int columnRight,int verticalAlignment,int fontBaseline,int lineHeight)
		{this.item=item;this.sprite=sprite;this.name=name;this.physical=physical;this.heart=heart;this.nativeText=nativeText;this.nativeWidth=nativeWidth;this.alignment=alignment;this.columnRight=columnRight;this.verticalAlignment=verticalAlignment;this.fontBaseline=fontBaseline;this.lineHeight=lineHeight;}
	}
	private final Context context;private final BankOverlayState state;private String lastDiagnostic;private int diagnostics;
	@Inject public PotionStorageOverlay(Client client,RuneLiteBankSource source,BankOverlayState state,TooltipManager tooltips)
	{
		this(new Context()
		{
			public String character(){return source.characterKey();}
			public boolean visible(){return source.bankOpen()&&source.potionStorageOpen();}
			public boolean current(){return source.potionReadCurrent();}
			public Rectangle viewport()
			{
				Widget w=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);if(w==null){return null;}Rectangle bounds=w.getBounds();
				for(Widget p=w.getParent();p!=null;p=p.getParent()){if(p.getType()==WidgetType.LAYER&&!p.getBounds().isEmpty()){bounds=bounds.intersection(p.getBounds());}}return bounds;
			}
			public void entries(Consumer<Entry> consumer)
			{
				Widget w=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);Widget[] children=w==null?null:w.getDynamicChildren();if(children==null){return;}
				// Core BankPlugin identifies the item at +1 and physical text at +3 in groups of five.
				// +0 is a native withdrawal control, NOT a guaranteed full-row rectangle.
				int left=Integer.MAX_VALUE,right=Integer.MAX_VALUE;
				for(int i=0;i+4<children.length;i+=5){Widget item=children[i+1];if(item==null||item.isHidden()||item.getItemId()<0){continue;}int x=item.getBounds().x;if(x<left){right=left;left=x;}else if(x>left&&x<right){right=x;}}
				for(int i=0;i+4<children.length;i+=5)
				{
					Widget item=children[i+1],name=children[i+2],label=children[i+3],heart=children[i+4];if(item==null||item.isHidden()||label==null||label.isHidden()||item.getItemId()<0||item.getItemId()==net.runelite.api.gameval.ItemID.VIAL_EMPTY||label.getText()==null||label.getText().isEmpty()){continue;}
					String text=Text.removeTags(label.getText());net.runelite.api.FontTypeFace font=label.getFont();Rectangle sprite=item.getBounds();
					consumer.accept(new Entry(item.getItemId(),sprite,name==null?null:name.getBounds(),label.getBounds(),heart==null?null:heart.getBounds(),text,font==null?-1:font.getTextWidth(text),label.getXTextAlignment(),sprite.x==left?right:Integer.MAX_VALUE,label.getYTextAlignment(),font==null?-1:font.getBaseline(),label.getLineHeight()));
				}
			}
			public void hint(Rectangle bounds,String text){net.runelite.api.Point mouse=client.getMouseCanvasPosition();if(mouse!=null&&bounds.contains(mouse.getX(),mouse.getY())){tooltips.add(new Tooltip(text));}}
		},state);
	}
	PotionStorageOverlay(Context context,BankOverlayState state)
	{
		this.context=context;this.state=state;setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);setPriority(PRIORITY_LOW);drawAfterInterface(InterfaceID.BANKMAIN);
	}
	@Override public Dimension render(Graphics2D graphics)
	{
		BankOverlayState.Projection frame=state.getProjection();
		if(!state.isEnabled()||frame==null){diagnostic("inactive projection");return null;}
		if(!state.getSettings().isQuantities()){diagnostic("quantity display disabled");return null;}
		if(!frame.getCharacterKey().equals(context.character())||!context.visible()){diagnostic("storage not visible or identity changed");return null;}
		Rectangle viewport=context.viewport();if(viewport==null||viewport.isEmpty()){diagnostic("empty viewport");return null;}
		Graphics2D g=(Graphics2D)graphics.create();int[] counts={0,0};
		try
		{
			g.clip(viewport);g.setFont(FontManager.getRunescapeSmallFont());
			context.entries(entry->
			{
				counts[0]++;if(!viewport.intersects(entry.physical)){return;}
				BankOverlayState.PotionLabel value=frame.getPotionLabels().get(entry.item);boolean verified=state.isPotionCurrent()&&context.current()&&value!=null;
				String exact=verified?value.getText():"FS: ?";Color color=!verified?UNKNOWN:value.isNegative()?state.getSettings().getNegativeColor():state.getSettings().getColor();
				Rectangle physical=new Rectangle(entry.physical);int nativeWidth=entry.nativeWidth<0?Math.max(g.getFontMetrics().stringWidth(entry.nativeText),g.getFontMetrics(FontManager.getRunescapeFont()).stringWidth(entry.nativeText)):entry.nativeWidth;
				if(entry.alignment==1){physical.x+=(physical.width-nativeWidth)/2;}else if(entry.alignment==2){physical.x+=physical.width-nativeWidth;}
				Rectangle row=entryBounds(viewport,entry);String compact=verified?value.getCompactText():exact;
				int baseline=textBaseline(physical,entry.verticalAlignment,entry.fontBaseline<0?g.getFontMetrics().getAscent():entry.fontBaseline,entry.lineHeight,g.getFontMetrics().getDescent());
				Rectangle painted=paint(g,row,physical,entry.heart,nativeWidth,exact,compact,color,baseline);
				if(painted!=null){counts[1]++;context.hint(painted,verified?exact+" "+value.getUnit()+" in From Scratch":"From Scratch Potion Storage unverified. Review unresolved balances in Manual Corrections.");}
			});
		}
		finally{g.dispose();}
		diagnostic("entries="+counts[0]+" labels="+counts[1]+" viewport="+viewport.width+"x"+viewport.height);return null;
	}
	static Rectangle entryBounds(Rectangle viewport,Entry entry)
	{
		Rectangle content=new Rectangle(entry.sprite);for(Rectangle r:new Rectangle[]{entry.name,entry.physical,entry.heart}){if(r!=null&&!r.isEmpty()){content=content.union(r);}}
		int end=Math.min(content.x+content.width,Math.min(viewport.x+viewport.width,entry.columnRight==Integer.MAX_VALUE?Integer.MAX_VALUE:entry.columnRight-4));
		return new Rectangle(content.x,content.y,Math.max(0,end-content.x),content.height);
	}
	private void diagnostic(String message)
	{
		// Bounded acceptance diagnostic: no identities, holdings, prices or private paths. No item callbacks logged.
		if(log.isDebugEnabled()&&diagnostics<6&&!message.equals(lastDiagnostic)){lastDiagnostic=message;diagnostics++;log.debug("From Scratch potion overlay: {}",message);}
	}
	static Rectangle paint(Graphics2D g,Rectangle row,Rectangle physical,Rectangle heart,int nativeWidth,String text,Color color)
	{return paint(g,row,physical,heart,nativeWidth,text,text,color);}
	static Rectangle paint(Graphics2D g,Rectangle row,Rectangle physical,Rectangle heart,int nativeWidth,String text,String compact,Color color)
	{return paint(g,row,physical,heart,nativeWidth,text,compact,color,physical.y+g.getFontMetrics().getAscent());}
	static int textBaseline(Rectangle bounds,int alignment,int ascent,int lineHeight,int descent)
	{
		// Native single-line quantity labels are top-aligned: widget top + font baseline.
		// Respect other native text alignments rather than centering every widget unconditionally.
		int height=lineHeight>0?lineHeight:ascent+descent;
		return bounds.y+(alignment==0?0:alignment==2?bounds.height-height:(bounds.height-height)/2)+ascent;
	}
	static Rectangle paint(Graphics2D g,Rectangle row,Rectangle physical,Rectangle heart,int nativeWidth,String text,String compact,Color color,int baseline)
	{
		FontMetrics fm=g.getFontMetrics();int x=physical.x+nativeWidth+8,right=row.x+row.width-4;
		if(heart!=null&&heart.intersects(new Rectangle(x,physical.y,Math.max(0,right-x),physical.height))){right=Math.min(right,heart.x-3);}
		String shown=text;if(x+fm.stringWidth(shown)>right){shown=compact;}
		if(x+fm.stringWidth(shown)>right){shown="FS";}
		if(x+fm.stringWidth(shown)>right){return null;}
		Rectangle bounds=new Rectangle(x,baseline-fm.getAscent(),fm.stringWidth(shown),fm.getHeight());if(!row.contains(bounds)){return null;}
		g.setColor(Color.BLACK);g.drawString(shown,x+1,baseline+1);g.setColor(color);g.drawString(shown,x,baseline);return bounds;
	}
}
