package com.fromscratch.buyback;

import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Reviewed RuneLite 1.13.4 allowlist. Each receipt buys ONE exact ID; no family transformations inferred. */
public final class SelectedBuybacks
{
	private static final Map<Integer,String> SLAYER=new LinkedHashMap<>();
	private static final Map<Integer,String> ARMOUR=new LinkedHashMap<>();
	private static final Set<Integer> TRADEABLE_MASKS=new HashSet<>();
	static
	{
		slayer(ItemID.HARMLESS_BLACK_MASK_10,"Black mask (10)");
		slayer(ItemID.HARMLESS_BLACK_MASK_9,"Black mask (9)");
		slayer(ItemID.HARMLESS_BLACK_MASK_8,"Black mask (8)");
		slayer(ItemID.HARMLESS_BLACK_MASK_7,"Black mask (7)");
		slayer(ItemID.HARMLESS_BLACK_MASK_6,"Black mask (6)");
		slayer(ItemID.HARMLESS_BLACK_MASK_5,"Black mask (5)");
		slayer(ItemID.HARMLESS_BLACK_MASK_4,"Black mask (4)");
		slayer(ItemID.HARMLESS_BLACK_MASK_3,"Black mask (3)");
		slayer(ItemID.HARMLESS_BLACK_MASK_2,"Black mask (2)");
		slayer(ItemID.HARMLESS_BLACK_MASK_1,"Black mask (1)");
		slayer(ItemID.HARMLESS_BLACK_MASK,"Black mask");
		slayer(ItemID.NZONE_BLACK_MASK_10,"Black mask (10) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_9,"Black mask (9) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_8,"Black mask (8) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_7,"Black mask (7) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_6,"Black mask (6) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_5,"Black mask (5) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_4,"Black mask (4) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_3,"Black mask (3) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_2,"Black mask (2) (i)");
		slayer(ItemID.NZONE_BLACK_MASK_1,"Black mask (1) (i)");
		slayer(ItemID.NZONE_BLACK_MASK,"Black mask (i)");
		slayer(ItemID.SLAYER_HELM,"Slayer helmet");
		slayer(ItemID.SLAYER_HELM_I,"Slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_BLACK,"Black slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_BLACK,"Black slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_GREEN,"Green slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_GREEN,"Green slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_RED,"Red slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_RED,"Red slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_PURPLE,"Purple slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_PURPLE,"Purple slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_TURQUOISE,"Turquoise slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_TURQUOISE,"Turquoise slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_HYDRA,"Hydra slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_HYDRA,"Hydra slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_TWISTED,"Twisted slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_TWISTED,"Twisted slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I,"Slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_BLACK,"Black slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_GREEN,"Green slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_RED,"Red slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_PURPLE,"Purple slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_TURQUOISE,"Turquoise slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_HYDRA,"Hydra slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_TWISTED,"Twisted slayer helmet (i)");
		slayer(ItemID.SW_BLACK_MASK_10,"Black mask (10) (i)");
		slayer(ItemID.SW_BLACK_MASK_9,"Black mask (9) (i)");
		slayer(ItemID.SW_BLACK_MASK_8,"Black mask (8) (i)");
		slayer(ItemID.SW_BLACK_MASK_7,"Black mask (7) (i)");
		slayer(ItemID.SW_BLACK_MASK_6,"Black mask (6) (i)");
		slayer(ItemID.SW_BLACK_MASK_5,"Black mask (5) (i)");
		slayer(ItemID.SW_BLACK_MASK_4,"Black mask (4) (i)");
		slayer(ItemID.SW_BLACK_MASK_3,"Black mask (3) (i)");
		slayer(ItemID.SW_BLACK_MASK_2,"Black mask (2) (i)");
		slayer(ItemID.SW_BLACK_MASK_1,"Black mask (1) (i)");
		slayer(ItemID.SW_BLACK_MASK,"Black mask (i)");
		slayer(ItemID.SLAYER_HELM_JAD,"Tztok slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_JAD,"Tztok slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_JAD,"Tztok slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_VERZIK,"Vampyric slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_VERZIK,"Vampyric slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_VERZIK,"Vampyric slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_ZUK,"Tzkal slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_ZUK,"Tzkal slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_ZUK,"Tzkal slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I,"Slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_BLACK,"Black slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_GREEN,"Green slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_RED,"Red slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_PURPLE,"Purple slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_TURQUOISE,"Turquoise slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_HYDRA,"Hydra slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_TWISTED,"Twisted slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_JAD,"Tztok slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_VERZIK,"Vampyric slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_ZUK,"Tzkal slayer helmet (i)");
		slayer(ItemID.PVPA_BLACK_MASK_10,"Black mask (10) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_9,"Black mask (9) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_8,"Black mask (8) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_7,"Black mask (7) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_6,"Black mask (6) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_5,"Black mask (5) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_4,"Black mask (4) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_3,"Black mask (3) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_2,"Black mask (2) (i)");
		slayer(ItemID.PVPA_BLACK_MASK_1,"Black mask (1) (i)");
		slayer(ItemID.PVPA_BLACK_MASK,"Black mask (i)");
		slayer(ItemID.SLAYER_HELM_ARAXYTE,"Araxyte slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_ARAXYTE,"Araxyte slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_ARAXYTE,"Araxyte slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_ARAXYTE,"Araxyte slayer helmet (i)");
		slayer(ItemID.SLAYER_HELM_HOODED,"Hooded slayer helmet");
		slayer(ItemID.SLAYER_HELM_I_HOODED,"Hooded slayer helmet (i)");
		slayer(ItemID.SW_SLAYER_HELM_I_HOODED,"Hooded slayer helmet (i)");
		slayer(ItemID.PVPA_SLAYER_HELM_I_HOODED,"Hooded slayer helmet (i)");
		slayer(ItemID.LEAGUE_6_SLAYER_HELM1,"Oathplate slayer helmet");
		slayer(ItemID.LEAGUE_6_SLAYER_HELM2,"Radiant slayer helmet");
		slayer(ItemID.LEAGUE_6_SLAYER_HELM1_I,"Oathplate slayer helmet (i)");
		slayer(ItemID.SW_LEAGUE_6_SLAYER_HELM1_I,"Oathplate slayer helmet (i)");
		slayer(ItemID.PVPA_LEAGUE_6_SLAYER_HELM1_I,"Oathplate slayer helmet (i)");
		slayer(ItemID.LEAGUE_6_SLAYER_HELM2_I,"Radiant slayer helmet (i)");
		slayer(ItemID.SW_LEAGUE_6_SLAYER_HELM2_I,"Radiant slayer helmet (i)");
		slayer(ItemID.PVPA_LEAGUE_6_SLAYER_HELM2_I,"Radiant slayer helmet (i)");
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_10);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_9);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_8);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_7);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_6);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_5);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_4);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_3);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_2);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK_1);
		TRADEABLE_MASKS.add(ItemID.HARMLESS_BLACK_MASK);
		ARMOUR.put(ItemID.ELITE_BLACK_FULL_HELM,"elite_black_helm");
		ARMOUR.put(ItemID.ELITE_BLACK_PLATEBODY,"elite_black_body");
		ARMOUR.put(ItemID.ELITE_BLACK_PLATELEGS,"elite_black_legs");
	}
	private SelectedBuybacks(){}
	private static void slayer(int id,String name){if(SLAYER.put(id,name)!=null){throw new IllegalStateException("Duplicate Slayer ID");}}
	public static boolean selected(String catalog){return catalog.equals("slayer_gear")||ARMOUR.containsValue(catalog);}
	public static String catalog(int id){return SLAYER.containsKey(id)?"slayer_gear":ARMOUR.get(id);}
	public static Set<Integer> items(String catalog)
	{Set<Integer> ids=new LinkedHashSet<>();if(catalog.equals("slayer_gear")){ids.addAll(SLAYER.keySet());}else{ARMOUR.forEach((id,key)->{if(key.equals(catalog)){ids.add(id);}});}return Collections.unmodifiableSet(ids);}
	public static String name(int id){return SLAYER.getOrDefault(id,id==ItemID.ELITE_BLACK_FULL_HELM?"Elite black full helm":id==ItemID.ELITE_BLACK_PLATEBODY?"Elite black platebody":id==ItemID.ELITE_BLACK_PLATELEGS?"Elite black platelegs":"Unknown item");}
	public static boolean tradeableException(int id){return TRADEABLE_MASKS.contains(id);}
	public static String role(int id){if(catalog(id)==null){throw new IllegalArgumentException("Unsupported selected item");}return "copy_"+id;}
	public static int item(String role){if(!role.matches("copy_[1-9][0-9]*")){throw new IllegalArgumentException("Invalid selected role");}int id=Integer.parseInt(role.substring(5));if(!role(id).equals(role)){throw new IllegalArgumentException("Invalid selected role");}return id;}
	public static boolean copyRole(String role){return role.startsWith("copy_");}
}
