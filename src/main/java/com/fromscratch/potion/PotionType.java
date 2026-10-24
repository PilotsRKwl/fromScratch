package com.fromscratch.potion;

import java.util.*;
import lombok.Value;

/** Cache-derived storage enum identity; units are doses, or items for unfinished/single-unit entries. */
@Value
public class PotionType
{
	int id;
	String name;
	boolean unfinished;
	Map<Integer,Integer> items;
	public PotionType(int id,String name,boolean unfinished,Map<Integer,Integer> items)
	{
		if(id<0||name==null||name.length()>150||items.isEmpty()||items.size()>4){throw new IllegalArgumentException("Invalid potion mapping");}
		TreeMap<Integer,Integer> copy=new TreeMap<>();items.forEach((item,doses)->{if(item<0||doses<1||doses>4||unfinished&&doses!=1){throw new IllegalArgumentException("Invalid potion units");}copy.put(item,doses);});
		this.id=id;this.name=name;this.unfinished=unfinished;this.items=Collections.unmodifiableMap(copy);
	}
	public int displayId(){return items.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();}
	public String unit(){return unfinished||items.size()==1&&items.values().iterator().next()==1?"items":"doses";}
	public long doses(Map<Integer,Long> quantities){long total=0;for(Map.Entry<Integer,Integer> e:items.entrySet()){total=Math.addExact(total,Math.multiplyExact(quantities.getOrDefault(e.getKey(),0L),e.getValue()));}return total;}
}
