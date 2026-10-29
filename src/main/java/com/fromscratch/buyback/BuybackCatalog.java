package com.fromscratch.buyback;

import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Explicit gameval allowlist; selected-copy additions are reviewed against RuneLite 1.13.4. Names never determine runtime eligibility. */
public final class BuybackCatalog
{
 public static final int VERSION=1;
 private static final Map<String,BuybackEntry> ENTRIES=new LinkedHashMap<>();
 private static final Map<String,Map<Integer,Integer>> ROLES=new LinkedHashMap<>();
 static
 {
  role("fire_cape",1,ItemID.TZHAAR_CAPE_FIRE,ItemID.TZHAAR_CAPE_FIRE_TROUVER);
  role("infernal_cape",1,ItemID.INFERNAL_CAPE,ItemID.INFERNAL_CAPE_TROUVER);
  role("saradomin_cape",1,ItemID.MA2_SARADOMIN_CAPE,ItemID.MA2_SARADOMIN_CAPE_TROUVER);
  role("guthix_cape",1,ItemID.MA2_GUTHIX_CAPE,ItemID.MA2_GUTHIX_CAPE_TROUVER);
  role("zamorak_cape",1,ItemID.MA2_ZAMORAK_CAPE,ItemID.MA2_ZAMORAK_CAPE_TROUVER);
  role("ancient_cape",1,ItemID.MA2_ZAROS_CAPE,ItemID.MA2_ZAROS_CAPE_TROUVER,ItemID.MA2_ZAROS_CAPE_DARKER);
  role("assembler",1,ItemID.AVAS_ASSEMBLER,ItemID.AVAS_ASSEMBLER_TROUVER);
  role("defender",1,ItemID.DRAGON_PARRYINGDAGGER,ItemID.DRAGON_PARRYINGDAGGER_T,ItemID.DRAGON_PARRYINGDAGGER_TROUVER,ItemID.DRAGON_PARRYINGDAGGER_T_TROUVER);
  role("torso",1,ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO,ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO_TROUVER,ItemID.BH_BARBASSAULT_PENANCE_FIGHTER_TORSO_CORRUPTED,ItemID.BH_BARBASSAULT_PENANCE_FIGHTER_TORSO_CORRUPTED_TROUVER);
  role("salve_ei",1,ItemID.NZONE_SALVE_AMULET_E,ItemID.SW_SALVE_AMULET_E);
  role("ancient_sceptre",1,ItemID.ANCIENT_SCEPTRE,ItemID.ANCIENT_SCEPTRE_TROUVER);
  role("rune_pouch_family",1,ItemID.BH_RUNE_POUCH,ItemID.BH_RUNE_POUCH_TROUVER,ItemID.DIVINE_RUNE_POUCH,ItemID.DIVINE_RUNE_POUCH_TROUVER);
	role("looting_bag_family",1,ItemID.LOOTING_BAG,ItemID.LOOTING_BAG_OPEN);
  role("void_melee",1,ItemID.GAME_PEST_MELEE_HELM,ItemID.GAME_PEST_MELEE_HELM_TROUVER,ItemID.LEAGUE_3_VOID_MELEE_HELM,ItemID.LEAGUE_3_VOID_MELEE_HELM_TROUVER);
  role("void_ranger",1,ItemID.GAME_PEST_ARCHER_HELM,ItemID.GAME_PEST_ARCHER_HELM_TROUVER,ItemID.LEAGUE_3_VOID_RANGE_HELM,ItemID.LEAGUE_3_VOID_RANGE_HELM_TROUVER);
  role("void_mage",1,ItemID.GAME_PEST_MAGE_HELM,ItemID.GAME_PEST_MAGE_HELM_TROUVER,ItemID.LEAGUE_3_VOID_MAGE_HELM,ItemID.LEAGUE_3_VOID_MAGE_HELM_TROUVER);
  role("void_gloves",1,ItemID.PEST_VOID_KNIGHT_GLOVES,ItemID.PEST_VOID_KNIGHT_GLOVES_TROUVER,ItemID.LEAGUE_3_VOID_KNIGHT_GLOVES,ItemID.LEAGUE_3_VOID_KNIGHT_GLOVES_TROUVER);
  role("void_top",1,ItemID.PEST_VOID_KNIGHT_TOP,ItemID.PEST_VOID_KNIGHT_TOP_TROUVER,ItemID.LEAGUE_3_VOID_KNIGHT_TOP,ItemID.LEAGUE_3_VOID_KNIGHT_TOP_TROUVER);
  role("void_robe",1,ItemID.PEST_VOID_KNIGHT_ROBES,ItemID.PEST_VOID_KNIGHT_ROBES_TROUVER,ItemID.LEAGUE_3_VOID_KNIGHT_ROBES,ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_TROUVER);
  role("void_top",2,ItemID.ELITE_VOID_KNIGHT_TOP,ItemID.ELITE_VOID_KNIGHT_TOP_TROUVER,ItemID.LEAGUE_3_VOID_KNIGHT_TOP_ELITE,ItemID.LEAGUE_3_VOID_KNIGHT_TOP_ELITE_TROUVER);
  role("void_robe",2,ItemID.ELITE_VOID_KNIGHT_ROBES,ItemID.ELITE_VOID_KNIGHT_ROBES_TROUVER,ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_ELITE,ItemID.LEAGUE_3_VOID_KNIGHT_ROBES_ELITE_TROUVER);
  role("graceful_hood",1,ItemID.GRACEFUL_HOOD,ItemID.ZEAH_GRACEFUL_HOOD_ARCEUUS,ItemID.ZEAH_GRACEFUL_HOOD_PISCARILIUS,ItemID.ZEAH_GRACEFUL_HOOD_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_HOOD_SHAYZIEN,ItemID.ZEAH_GRACEFUL_HOOD_HOSIDIUS,ItemID.ZEAH_GRACEFUL_HOOD_KOUREND,ItemID.GRACEFUL_HOOD_SKILLCAPECOLOUR,ItemID.GRACEFUL_HOOD_HALLOWED,ItemID.GRACEFUL_HOOD_TRAILBLAZER,ItemID.GRACEFUL_HOOD_ADVENTURER,ItemID.GRACEFUL_HOOD_WYRM);
  role("graceful_cape",1,ItemID.GRACEFUL_CAPE,ItemID.ZEAH_GRACEFUL_CAPE_ARCEUUS,ItemID.ZEAH_GRACEFUL_CAPE_PISCARILIUS,ItemID.ZEAH_GRACEFUL_CAPE_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_CAPE_SHAYZIEN,ItemID.ZEAH_GRACEFUL_CAPE_HOSIDIUS,ItemID.ZEAH_GRACEFUL_CAPE_KOUREND,ItemID.GRACEFUL_CAPE_SKILLCAPECOLOUR,ItemID.GRACEFUL_CAPE_HALLOWED,ItemID.GRACEFUL_CAPE_TRAILBLAZER,ItemID.GRACEFUL_CAPE_ADVENTURER,ItemID.GRACEFUL_CAPE_WYRM);
  role("graceful_top",1,ItemID.GRACEFUL_TOP,ItemID.ZEAH_GRACEFUL_TOP_ARCEUUS,ItemID.ZEAH_GRACEFUL_TOP_PISCARILIUS,ItemID.ZEAH_GRACEFUL_TOP_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_TOP_SHAYZIEN,ItemID.ZEAH_GRACEFUL_TOP_HOSIDIUS,ItemID.ZEAH_GRACEFUL_TOP_KOUREND,ItemID.GRACEFUL_TOP_SKILLCAPECOLOUR,ItemID.GRACEFUL_TOP_HALLOWED,ItemID.GRACEFUL_TOP_TRAILBLAZER,ItemID.GRACEFUL_TOP_ADVENTURER,ItemID.GRACEFUL_TOP_WYRM);
  role("graceful_legs",1,ItemID.GRACEFUL_LEGS,ItemID.ZEAH_GRACEFUL_LEGS_ARCEUUS,ItemID.ZEAH_GRACEFUL_LEGS_PISCARILIUS,ItemID.ZEAH_GRACEFUL_LEGS_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_LEGS_SHAYZIEN,ItemID.ZEAH_GRACEFUL_LEGS_HOSIDIUS,ItemID.ZEAH_GRACEFUL_LEGS_KOUREND,ItemID.GRACEFUL_LEGS_SKILLCAPECOLOUR,ItemID.GRACEFUL_LEGS_HALLOWED,ItemID.GRACEFUL_LEGS_TRAILBLAZER,ItemID.GRACEFUL_LEGS_ADVENTURER,ItemID.GRACEFUL_LEGS_WYRM);
  role("graceful_gloves",1,ItemID.GRACEFUL_GLOVES,ItemID.ZEAH_GRACEFUL_GLOVES_ARCEUUS,ItemID.ZEAH_GRACEFUL_GLOVES_PISCARILIUS,ItemID.ZEAH_GRACEFUL_GLOVES_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_GLOVES_SHAYZIEN,ItemID.ZEAH_GRACEFUL_GLOVES_HOSIDIUS,ItemID.ZEAH_GRACEFUL_GLOVES_KOUREND,ItemID.GRACEFUL_GLOVES_SKILLCAPECOLOUR,ItemID.GRACEFUL_GLOVES_HALLOWED,ItemID.GRACEFUL_GLOVES_TRAILBLAZER,ItemID.GRACEFUL_GLOVES_ADVENTURER,ItemID.GRACEFUL_GLOVES_WYRM);
  role("graceful_boots",1,ItemID.GRACEFUL_BOOTS,ItemID.ZEAH_GRACEFUL_BOOTS_ARCEUUS,ItemID.ZEAH_GRACEFUL_BOOTS_PISCARILIUS,ItemID.ZEAH_GRACEFUL_BOOTS_LOVAKENGJ,ItemID.ZEAH_GRACEFUL_BOOTS_SHAYZIEN,ItemID.ZEAH_GRACEFUL_BOOTS_HOSIDIUS,ItemID.ZEAH_GRACEFUL_BOOTS_KOUREND,ItemID.GRACEFUL_BOOTS_SKILLCAPECOLOUR,ItemID.GRACEFUL_BOOTS_HALLOWED,ItemID.GRACEFUL_BOOTS_TRAILBLAZER,ItemID.GRACEFUL_BOOTS_ADVENTURER,ItemID.GRACEFUL_BOOTS_WYRM);
  role("prospector_hat",1,ItemID.MOTHERLODE_REWARD_HAT,ItemID.FOSSIL_MOTHERLODE_REWARD_HAT);
  role("angler_hat",1,ItemID.TRAWLER_REWARD_HAT,ItemID.SPIRIT_ANGLER_HAT);
  role("lumberjack_hat",1,ItemID.RAMBLE_LUMBERJACK_HAT);
  role("prospector_top",1,ItemID.MOTHERLODE_REWARD_TOP,ItemID.FOSSIL_MOTHERLODE_REWARD_TOP);
  role("angler_top",1,ItemID.TRAWLER_REWARD_TOP,ItemID.SPIRIT_ANGLER_TOP);
  role("lumberjack_top",1,ItemID.RAMBLE_LUMBERJACK_TOP);
  role("prospector_legs",1,ItemID.MOTHERLODE_REWARD_LEGS,ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS);
  role("angler_legs",1,ItemID.TRAWLER_REWARD_LEGS,ItemID.SPIRIT_ANGLER_LEGS);
  role("lumberjack_legs",1,ItemID.RAMBLE_LUMBERJACK_LEGS);
  role("prospector_boots",1,ItemID.MOTHERLODE_REWARD_BOOTS,ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS);
  role("angler_boots",1,ItemID.TRAWLER_REWARD_BOOTS,ItemID.SPIRIT_ANGLER_BOOTS);
  role("lumberjack_boots",1,ItemID.RAMBLE_LUMBERJACK_BOOTS);
  role("rogue_mask",1,ItemID.ROGUESDEN_HELM);
  role("rogue_top",1,ItemID.ROGUESDEN_BODY);
  role("rogue_trousers",1,ItemID.ROGUESDEN_LEGS);
  role("rogue_gloves",1,ItemID.ROGUESDEN_GLOVES);
  role("rogue_boots",1,ItemID.ROGUESDEN_BOOTS);
  role("farmer_hat",1,ItemID.TITHE_REWARD_HAT_MALE,ItemID.TITHE_REWARD_HAT_FEMALE);
  role("farmer_top",1,ItemID.TITHE_REWARD_TORSO_MALE,ItemID.TITHE_REWARD_TORSO_FEMALE);
  role("farmer_legs",1,ItemID.TITHE_REWARD_LEGS_MALE,ItemID.TITHE_REWARD_LEGS_FEMALE);
  role("farmer_boots",1,ItemID.TITHE_REWARD_FEET_MALE,ItemID.TITHE_REWARD_FEET_FEMALE);
  ENTRIES.put("slayer_gear",new BuybackEntry("slayer_gear","Slayer Helmet / Black Mask","Combat & Utility",ItemID.SLAYER_HELM_I,1000000,Collections.emptyMap()));
  ENTRIES.put("elite_black_helm",new BuybackEntry("elite_black_helm","Elite Black Full Helm","Combat & Utility",ItemID.ELITE_BLACK_FULL_HELM,30000,Collections.emptyMap()));
  ENTRIES.put("elite_black_body",new BuybackEntry("elite_black_body","Elite Black Platebody","Combat & Utility",ItemID.ELITE_BLACK_PLATEBODY,30000,Collections.emptyMap()));
  ENTRIES.put("elite_black_legs",new BuybackEntry("elite_black_legs","Elite Black Platelegs","Combat & Utility",ItemID.ELITE_BLACK_PLATELEGS,30000,Collections.emptyMap()));
  single("fire_cape","Fire Cape",1000000,"fire_cape");
  single("infernal_cape","Infernal Cape",15000000,"infernal_cape");
  single("saradomin_cape","Imbued Saradomin Cape",750000,"saradomin_cape");
  single("guthix_cape","Imbued Guthix Cape",750000,"guthix_cape");
  single("zamorak_cape","Imbued Zamorak Cape",750000,"zamorak_cape");
  single("ancient_cape","Imbued Ancient Cape",750000,"ancient_cape");
  single("assembler","Ava's Assembler",1000000,"assembler");
  single("defender","Dragon Defender",250000,"defender");
  single("torso","Fighter Torso",2000000,"torso");
  single("salve_ei","Salve Amulet (ei)",500000,"salve_ei");
  single("ancient_sceptre","Ancient Sceptre",500000,"ancient_sceptre");
	item("rune_pouch_family","Rune Pouch / Divine Rune Pouch","Utility & Storage",1000000,1,"rune_pouch_family");
	item("looting_bag_family","Looting Bag","Utility & Storage",10000,1,"looting_bag_family");
  item("void_melee","Void Melee Helm","Void Knight",300000,1,"void_melee");
  item("void_ranger","Void Ranger Helm","Void Knight",300000,1,"void_ranger");
  item("void_mage","Void Mage Helm","Void Knight",300000,1,"void_mage");
  item("void_top","Void Knight Top","Void Knight",800000,1,"void_top");
  item("void_robe","Void Knight Robe","Void Knight",800000,1,"void_robe");
  item("void_gloves","Void Knight Gloves","Void Knight",200000,1,"void_gloves");
  item("elite_void_top","Elite Void Top","Void Knight",1800000,2,"void_top");
  item("elite_void_robe","Elite Void Robe","Void Knight",1800000,2,"void_robe");
  outfit("graceful","Graceful",1000000,"hood","cape","top","legs","gloves","boots");
  outfit("prospector","Prospector",1000000,"hat","top","legs","boots");
  outfit("angler","Angler",750000,"hat","top","legs","boots");
  outfit("rogue","Rogue",500000,"mask","top","trousers","gloves","boots");
  outfit("lumberjack","Lumberjack",500000,"hat","top","legs","boots");
  outfit("farmer","Farmer's Outfit",750000,"hat","top","legs","boots");
 }
 private BuybackCatalog(){}
 private static void role(String key,int level,int...ids){Map<Integer,Integer> values=ROLES.computeIfAbsent(key,k->new TreeMap<>());for(int id:ids){values.put(id,level);}}
 private static void single(String id,String name,long gp,String role){item(id,name,"Combat & Utility",gp,1,role);}
 private static void item(String id,String name,String category,long gp,int level,String role){Map<String,Integer> roles=new LinkedHashMap<>();roles.put(role,level);add(id,name,category,gp,roles);}
 private static void outfit(String id,String name,long gp,String...parts){Map<String,Integer> roles=new LinkedHashMap<>();for(String part:parts){roles.put(id+"_"+part,1);}add(id,name,"Skilling Outfits",gp,roles);}
 private static void add(String id,String name,String category,long gp,Map<String,Integer> roles){String first=roles.keySet().iterator().next();int level=roles.get(first);int icon=ROLES.get(first).entrySet().stream().filter(e->e.getValue()==level).findFirst().get().getKey();ENTRIES.put(id,new BuybackEntry(id,name,category,icon,gp,roles));}
 public static BuybackEntry entry(String id){BuybackEntry e=ENTRIES.get(id);if(e==null){throw new IllegalArgumentException("This item is not approved for buyback.");}return e;}
 public static Collection<BuybackEntry> entries(){return Collections.unmodifiableCollection(ENTRIES.values());}
 public static Map<Integer,Integer> variants(String role){if(SelectedBuybacks.copyRole(role)){return Collections.singletonMap(SelectedBuybacks.item(role),1);}Map<Integer,Integer> ids=ROLES.get(role);if(ids==null){throw new IllegalArgumentException("Unknown entitlement role");}return Collections.unmodifiableMap(ids);}
 public static long cost(BuybackEntry entry,Map<String,Integer> owned){if(entry.getId().startsWith("elite_void_")&&owned.getOrDefault(entry.getRoles().keySet().iterator().next(),0)>=1){return 1000000;}return entry.getPrice();}
 public static List<BuybackEntry> search(String text){String query=text.toLowerCase(Locale.ROOT).trim();List<BuybackEntry> result=new ArrayList<>();for(BuybackEntry entry:ENTRIES.values()){if(entry.getName().toLowerCase(Locale.ROOT).contains(query)){result.add(entry);}}return result;}
}
