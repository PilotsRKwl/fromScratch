package com.fromscratch.potion;

import java.time.Instant;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/** Native enum/CS2 quantity queries also used by core Bank Tags.
 * Script execution changes interpreter stacks; no gameplay action is sent. */
@lombok.extern.slf4j.Slf4j
public final class RuneLitePotionSource
{
	/** Core Bank Tags' special native tab; no public gameval enumerator exists for its value. */
	public static final int NATIVE_TAB=15;
	public interface Access
	{
		boolean unlocked();Map<Integer,PotionType> types();long units(int enumId);int[] triggers();
		default boolean complete(Map<Integer,PotionType> types,Map<Integer,Long> quantities){return true;}
	}
	private final Access access;
	private PotionObservation cached=PotionObservation.unknown();
	private final Set<Integer> triggers=new HashSet<>();
	private String character;
	private boolean built,dirty,failed,nativeComplete;
	public RuneLitePotionSource(Client client)
	{
		this(new Access()
		{
			public boolean unlocked(){return client.getVarbitValue(VarbitID.MM_POTION_STORAGE_UNLOCKED)!=0;}
			public Map<Integer,PotionType> types()
			{
				Map<Integer,PotionType> result=new TreeMap<>();
				for(int category:new int[]{EnumID.POTIONSTORE_POTIONS,EnumID.POTIONSTORE_UNFINISHED_POTIONS})
				{
					EnumComposition list=client.getEnum(category);if(list==null||list.getIntVals()==null||list.size()==0){throw new IllegalArgumentException("Potion definitions unavailable");}
					boolean unfinished=category==EnumID.POTIONSTORE_UNFINISHED_POTIONS;
					for(int enumId:list.getIntVals())
					{
						EnumComposition e=client.getEnum(enumId);Map<Integer,Integer> items=new TreeMap<>();
						for(int dose=1;dose<=(unfinished?1:4);dose++){int item=e.getIntValue(dose);if(item>0){if(items.put(item,dose)!=null){throw new IllegalArgumentException("Ambiguous potion doses");}}}
						if(items.isEmpty()){throw new IllegalArgumentException("Unknown potion storage variant");}
						int display=items.keySet().iterator().next();String name=client.getItemDefinition(display).getMembersName().replaceFirst("\\(\\d\\)$","").trim();
						result.put(enumId,new PotionType(enumId,name,unfinished,items));
					}
				}
				return result;
			}
			public long units(int id){client.runScript(ScriptID.POTIONSTORE_DOSES,id);return client.getIntStack()[0];}
			public int[] triggers(){Widget w=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);return w==null?null:w.getVarTransmitTrigger();}
			public boolean complete(Map<Integer,PotionType> types,Map<Integer,Long> quantities)
			{
				Widget w=client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);int[] vars=triggers();
				if(client.getVarbitValue(VarbitID.BANK_CURRENTTAB)!=NATIVE_TAB||w==null||w.isHidden()||vars==null||vars.length==0){return false;}
				Widget[] children=w.getDynamicChildren();if(children==null||children.length<5||children.length%5!=0){return false;}
				Map<Integer,Integer> itemTypes=new HashMap<>();types.values().forEach(t->t.getItems().keySet().forEach(item->itemTypes.put(item,t.getId())));int matched=0;
				for(int i=0;i+4<children.length;i+=5)
				{
					Widget item=children[i+1],label=children[i+3];if(item==null||label==null){return false;}Integer type=itemTypes.get(item.getItemId());if(type==null){continue;}
					String text=net.runelite.client.util.Text.removeTags(label.getText());int colon=text.indexOf(':');if(colon<0||!text.startsWith(types.get(type).unit().equals("items")?"Quantity:":"Doses:")){return false;}
					long shown=Long.parseLong(text.substring(colon+1).replace(",","").trim());if(shown!=quantities.getOrDefault(type,0L)){return false;}matched++;
				}
				return matched>0; // Full enum queries above include zero stock and offscreen/search-hidden types.
			}
		});
	}
	public RuneLitePotionSource(Access access){this.access=access;}
	public void reset(){cached=PotionObservation.unknown();built=false;dirty=false;failed=false;nativeComplete=false;character=null;triggers.clear();}
	public boolean current(){return built&&!dirty&&!failed&&nativeComplete&&cached.isVerified();}
	public boolean validCapture(PotionObservation observation){return current()&&cached.equals(observation);}
	public void closed(){built=false;dirty=false;nativeComplete=false;cached=new PotionObservation(false,cached.getTypes(),Collections.emptyMap(),Instant.EPOCH);}
	public void script(int id){if(id==ScriptID.POTIONSTORE_BUILD||built&&id==ScriptID.POTIONSTORE_DOSE_CHANGE){built=true;dirty=true;failed=false;nativeComplete=false;int[] vars=access.triggers();if(vars!=null){for(int var:vars){triggers.add(var);}}}}
	public void varp(int id){if(built&&triggers.contains(id)){dirty=true;failed=false;nativeComplete=false;}}
	public PotionObservation observe(String key,boolean ready)
	{
		if(key==null||!ready){return PotionObservation.unknown();}
		if(character!=null&&!Objects.equals(character,key)){reset();}character=key;
		try
		{
			if(!access.unlocked()){return PotionObservation.unavailableStore();}
			if(failed){return PotionObservation.unknown();}
			if(!built){if(cached.getTypes().isEmpty()){cached=new PotionObservation(false,access.types(),Collections.emptyMap(),Instant.EPOCH);}return cached;}
			if(!dirty&&cached.isVerified()){return cached;}
			Map<Integer,PotionType> types=cached.getTypes().isEmpty()?access.types():cached.getTypes();Map<Integer,Long> quantities=new TreeMap<>();
			for(int id:types.keySet()){long q=access.units(id);if(q<0){throw new IllegalArgumentException("Invalid storage units");}if(q>0){quantities.put(id,q);}}
			// A native rebuild may lag its totals. Retry on the next observation without
			// replacing the last verified copy or presenting incomplete state as zero.
			try{nativeComplete=access.complete(types,quantities);}catch(RuntimeException ex){nativeComplete=false;log.debug("From Scratch native Potion Storage capture is not ready",ex);}
			if(!nativeComplete){return PotionObservation.unknown();}
			dirty=false;
			if(!cached.isVerified()||!cached.getTypes().equals(types)||!cached.getQuantities().equals(quantities)){cached=new PotionObservation(true,types,quantities,Instant.now());}
			return cached;
		}
		catch(RuntimeException ex){cached=PotionObservation.unknown();failed=true;log.debug("From Scratch Potion Storage read unavailable; waiting for a native refresh",ex);return cached;}
	}
}
