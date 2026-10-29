package com.fromscratch.buyback;

import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Explicit gameval allowlist; selected-copy additions are reviewed against RuneLite 1.13.4. Names never determine runtime eligibility. */
public final class BuybackCatalog
{
 public static final int VERSION=1;
 public static final List<String> CATEGORIES=Collections.unmodifiableList(Arrays.asList("Utility & Storage","Weapons","Armour","Skilling Outfits"));
 private static final Comparator<BuybackEntry> ORDER=Comparator.comparingInt((BuybackEntry e)->CATEGORIES.indexOf(e.getCategory())).thenComparing(BuybackEntry::getName,String.CASE_INSENSITIVE_ORDER).thenComparing(BuybackEntry::getId);
 private static final Set<String> RINGS=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("archers_ring_i","berserker_ring_i","granite_ring_i","gods_ring_i","suffering_ring_i","seers_ring_i","treasonous_ring_i","tyrannical_ring_i","warrior_ring_i","wealth_ring_i")));
 private static final Map<String,BuybackEntry> ENTRIES=new LinkedHashMap<>();
 private static final Map<String,Map<Integer,Integer>> ROLES=new LinkedHashMap<>();
 private static final Set<String> NATURAL_FAMILIES=new LinkedHashSet<>();
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
  expandedRoles();
  ENTRIES.put("slayer_gear",new BuybackEntry("slayer_gear","Slayer Helmet / Black Mask","Armour",ItemID.SLAYER_HELM_I,1000000,Collections.emptyMap()));
  ENTRIES.put("elite_black_helm",new BuybackEntry("elite_black_helm","Elite Black Full Helm","Armour",ItemID.ELITE_BLACK_FULL_HELM,30000,Collections.emptyMap()));
  ENTRIES.put("elite_black_body",new BuybackEntry("elite_black_body","Elite Black Platebody","Armour",ItemID.ELITE_BLACK_PLATEBODY,30000,Collections.emptyMap()));
  ENTRIES.put("elite_black_legs",new BuybackEntry("elite_black_legs","Elite Black Platelegs","Armour",ItemID.ELITE_BLACK_PLATELEGS,30000,Collections.emptyMap()));
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
  item("void_melee","Void Melee Helm","Armour",300000,1,"void_melee");
  item("void_ranger","Void Ranger Helm","Armour",300000,1,"void_ranger");
  item("void_mage","Void Mage Helm","Armour",300000,1,"void_mage");
  item("void_top","Void Knight Top","Armour",800000,1,"void_top");
  item("void_robe","Void Knight Robe","Armour",800000,1,"void_robe");
  item("void_gloves","Void Knight Gloves","Armour",200000,1,"void_gloves");
  item("elite_void_top","Elite Void Top","Armour",1800000,2,"void_top");
  item("elite_void_robe","Elite Void Robe","Armour",1800000,2,"void_robe");
  outfit("graceful","Graceful",1000000,"hood","cape","top","legs","gloves","boots");
  outfit("prospector","Prospector",1000000,"hat","top","legs","boots");
  outfit("angler","Angler",750000,"hat","top","legs","boots");
  outfit("rogue","Rogue",500000,"mask","top","trousers","gloves","boots");
  outfit("lumberjack","Lumberjack",500000,"hat","top","legs","boots");
  outfit("farmer","Farmer's Outfit",750000,"hat","top","legs","boots");
  expandedEntries();
 }
 private BuybackCatalog(){}
 private static void expandedRoles()
 {
  Set<String> historicalRoles=new HashSet<>(ROLES.keySet());
  role("abyssal_lantern",1,ItemID.ABYSSAL_LANTERN,ItemID.ABYSSAL_LANTERN_NORMAL,ItemID.ABYSSAL_LANTERN_NORMAL_BLUE,ItemID.ABYSSAL_LANTERN_NORMAL_RED,ItemID.ABYSSAL_LANTERN_NORMAL_WHITE,ItemID.ABYSSAL_LANTERN_NORMAL_PURPLE,ItemID.ABYSSAL_LANTERN_NORMAL_GREEN,ItemID.ABYSSAL_LANTERN_OAK,ItemID.ABYSSAL_LANTERN_WILLOW,ItemID.ABYSSAL_LANTERN_MAPLE,ItemID.ABYSSAL_LANTERN_YEW,ItemID.ABYSSAL_LANTERN_BLISTERWOOD,ItemID.ABYSSAL_LANTERN_MAGIC,ItemID.ABYSSAL_LANTERN_REDWOOD);
  role("fish_barrel",1,ItemID.FISH_BARREL_CLOSED,ItemID.FISH_BARREL_OPEN,ItemID.FISH_SACK_BARREL_CLOSED,ItemID.FISH_SACK_BARREL_OPEN);
  role("herb_sack",1,ItemID.SLAYER_HERB_SACK,ItemID.SLAYER_HERB_SACK_OPEN,ItemID.SLAYER_HERB_SACK_SILK,ItemID.SLAYER_HERB_SACK_SILK_OPEN);
  role("plank_sack",1,ItemID.PLANK_SACK);
  role("seed_box",1,ItemID.SEED_BOX,ItemID.SEED_BOX_OPEN);
  role("tackle_box",1,ItemID.TACKLE_BOX);
  role("saeldor_corrupted",1,ItemID.BLADE_OF_SAELDOR_INFINITE,ItemID.BLADE_OF_SAELDOR_INFINITE_ITHELL,ItemID.BLADE_OF_SAELDOR_INFINITE_IORWERTH,ItemID.BLADE_OF_SAELDOR_INFINITE_TRAHAEARN,ItemID.BLADE_OF_SAELDOR_INFINITE_CADARN,ItemID.BLADE_OF_SAELDOR_INFINITE_CRWYS,ItemID.BLADE_OF_SAELDOR_INFINITE_MEILYR,ItemID.BLADE_OF_SAELDOR_INFINITE_AMLODD);
  role("faerdhinen_corrupted",1,ItemID.BOW_OF_FAERDHINEN_INFINITE,ItemID.BOW_OF_FAERDHINEN_INFINITE_ITHELL,ItemID.BOW_OF_FAERDHINEN_INFINITE_IORWERTH,ItemID.BOW_OF_FAERDHINEN_INFINITE_TRAHAEARN,ItemID.BOW_OF_FAERDHINEN_INFINITE_CADARN,ItemID.BOW_OF_FAERDHINEN_INFINITE_CRWYS,ItemID.BOW_OF_FAERDHINEN_INFINITE_MEILYR,ItemID.BOW_OF_FAERDHINEN_INFINITE_AMLODD,ItemID.BOW_OF_FAERDHINEN_INFINITE_DEADMAN);
  role("emberlight",1,ItemID.EMBERLIGHT);
  role("keris_breaching",1,ItemID.KERIS_PARTISAN_BREACH);
  role("keris_corruption",1,ItemID.KERIS_PARTISAN_CORRUPTION);
  role("keris_sun",1,ItemID.KERIS_PARTISAN_SUN);
  role("avernic_defender",1,ItemID.INFERNAL_DEFENDER,ItemID.INFERNAL_DEFENDER_TROUVER,ItemID.INFERNAL_DEFENDER_GHOMMAL_5,ItemID.INFERNAL_DEFENDER_GHOMMAL_5_TROUVER,ItemID.INFERNAL_DEFENDER_GHOMMAL_6,ItemID.INFERNAL_DEFENDER_GHOMMAL_6_TROUVER);
  role("crystal_helm",1,ItemID.CRYSTAL_HELMET,ItemID.CRYSTAL_HELMET_INACTIVE,ItemID.CRYSTAL_HELMET_HEFIN,ItemID.CRYSTAL_HELMET_INACTIVE_HEFIN,ItemID.CRYSTAL_HELMET_ITHELL,ItemID.CRYSTAL_HELMET_INACTIVE_ITHELL,ItemID.CRYSTAL_HELMET_IORWERTH,ItemID.CRYSTAL_HELMET_INACTIVE_IORWERTH,ItemID.CRYSTAL_HELMET_TRAHAEARN,ItemID.CRYSTAL_HELMET_INACTIVE_TRAHAEARN,ItemID.CRYSTAL_HELMET_CADARN,ItemID.CRYSTAL_HELMET_INACTIVE_CADARN,ItemID.CRYSTAL_HELMET_CRWYS,ItemID.CRYSTAL_HELMET_INACTIVE_CRWYS,ItemID.CRYSTAL_HELMET_AMLODD,ItemID.CRYSTAL_HELMET_INACTIVE_AMLODD,ItemID.CRYSTAL_HELMET_DEADMAN,ItemID.CRYSTAL_HELMET_INACTIVE_DEADMAN);
  role("crystal_body",1,ItemID.CRYSTAL_CHESTPLATE,ItemID.CRYSTAL_CHESTPLATE_INACTIVE,ItemID.CRYSTAL_CHESTPLATE_HEFIN,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_HEFIN,ItemID.CRYSTAL_CHESTPLATE_ITHELL,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_ITHELL,ItemID.CRYSTAL_CHESTPLATE_IORWERTH,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_IORWERTH,ItemID.CRYSTAL_CHESTPLATE_TRAHAEARN,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_TRAHAEARN,ItemID.CRYSTAL_CHESTPLATE_CADARN,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_CADARN,ItemID.CRYSTAL_CHESTPLATE_CRWYS,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_CRWYS,ItemID.CRYSTAL_CHESTPLATE_AMLODD,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_AMLODD,ItemID.CRYSTAL_CHESTPLATE_DEADMAN,ItemID.CRYSTAL_CHESTPLATE_INACTIVE_DEADMAN);
  role("crystal_legs",1,ItemID.CRYSTAL_PLATELEGS,ItemID.CRYSTAL_PLATELEGS_INACTIVE,ItemID.CRYSTAL_PLATELEGS_HEFIN,ItemID.CRYSTAL_PLATELEGS_INACTIVE_HEFIN,ItemID.CRYSTAL_PLATELEGS_ITHELL,ItemID.CRYSTAL_PLATELEGS_INACTIVE_ITHELL,ItemID.CRYSTAL_PLATELEGS_IORWERTH,ItemID.CRYSTAL_PLATELEGS_INACTIVE_IORWERTH,ItemID.CRYSTAL_PLATELEGS_TRAHAEARN,ItemID.CRYSTAL_PLATELEGS_INACTIVE_TRAHAEARN,ItemID.CRYSTAL_PLATELEGS_CADARN,ItemID.CRYSTAL_PLATELEGS_INACTIVE_CADARN,ItemID.CRYSTAL_PLATELEGS_CRWYS,ItemID.CRYSTAL_PLATELEGS_INACTIVE_CRWYS,ItemID.CRYSTAL_PLATELEGS_AMLODD,ItemID.CRYSTAL_PLATELEGS_INACTIVE_AMLODD,ItemID.CRYSTAL_PLATELEGS_DEADMAN,ItemID.CRYSTAL_PLATELEGS_INACTIVE_DEADMAN);
  role("quiver",1,ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.DIZANAS_QUIVER_UNCHARGED_TROUVER,ItemID.DIZANAS_QUIVER_CHARGED,ItemID.DIZANAS_QUIVER_CHARGED_TROUVER);
  role("quiver",2,ItemID.DIZANAS_QUIVER_INFINITE,ItemID.DIZANAS_QUIVER_INFINITE_TROUVER);
  role("elidinis_ward_f",1,ItemID.ELIDINIS_WARD_FORTIFIED,ItemID.ELIDINIS_WARD_ORNAMENT);
  ring("archers_ring_i",ItemID.RANGER_RING,ItemID.NZONE_RANGER_RING,ItemID.SW_RANGER_RING,ItemID.PVPA_RANGER_RING);
  ring("berserker_ring_i",ItemID.BERZERKER_RING,ItemID.NZONE_BERZERKER_RING,ItemID.SW_BERZERKER_RING,ItemID.PVPA_BERZERKER_RING);
  ring("granite_ring_i",ItemID.GRANITE_RING,ItemID.NZONE_GRANITE_RING,ItemID.SW_GRANITE_RING,ItemID.PVPA_GRANITE_RING);
  ring("gods_ring_i",ItemID.ROTG,ItemID.NZONE_ROTG,ItemID.SW_ROTG,ItemID.PVPA_ROTG);
  ring("seers_ring_i",ItemID.SEER_RING,ItemID.NZONE_SEER_RING,ItemID.SW_SEER_RING,ItemID.PVPA_SEER_RING);
  ring("treasonous_ring_i",ItemID.SHARP_RING,ItemID.NZONE_SHARP_RING,ItemID.SW_SHARP_RING,ItemID.PVPA_SHARP_RING);
  ring("tyrannical_ring_i",ItemID.HEAVY_RING,ItemID.NZONE_HEAVY_RING,ItemID.SW_HEAVY_RING,ItemID.PVPA_HEAVY_RING);
  ring("warrior_ring_i",ItemID.WARRIOR_RING,ItemID.NZONE_WARRIOR_RING,ItemID.SW_WARRIOR_RING,ItemID.PVPA_WARRIOR_RING);
  role("suffering_ring_i",0,ItemID.ZENYTE_RING_ENCHANTED,ItemID.ZENYTE_RING_ENCHANTED_RECOIL);
  role("suffering_ring_i",1,ItemID.NZONE_ZENYTE_RING_ENCHANTED,ItemID.NZONE_ZENYTE_RING_ENCHANTED_RECOIL,ItemID.SW_ZENYTE_RING_ENCHANTED,ItemID.SW_ZENYTE_RING_ENCHANTED_RECOIL,ItemID.PVPA_ZENYTE_RING_ENCHANTED,ItemID.PVPA_ZENYTE_RING_ENCHANTED_RECOIL);
  role("wealth_ring_i",0,ItemID.RING_OF_WEALTH,ItemID.RING_OF_WEALTH_5,ItemID.RING_OF_WEALTH_4,ItemID.RING_OF_WEALTH_3,ItemID.RING_OF_WEALTH_2,ItemID.RING_OF_WEALTH_1);
  role("wealth_ring_i",1,ItemID.RING_OF_WEALTH_I,ItemID.RING_OF_WEALTH_I5,ItemID.RING_OF_WEALTH_I4,ItemID.RING_OF_WEALTH_I3,ItemID.RING_OF_WEALTH_I2,ItemID.RING_OF_WEALTH_I1);
  role("carpenter_hat",1,ItemID.CONSTRUCTION_HAT);role("carpenter_top",1,ItemID.CONSTRUCTION_SHIRT);role("carpenter_legs",1,ItemID.CONSTRUCTION_TROUSERS);role("carpenter_boots",1,ItemID.CONSTRUCTION_BOOTS);
  // Warm gloves and the bruma torch are separate rewards, not Pyromancer outfit pieces.
  role("pyromancer_hat",1,ItemID.PYROMANCER_HOOD);role("pyromancer_top",1,ItemID.PYROMANCER_TOP);role("pyromancer_legs",1,ItemID.PYROMANCER_BOTTOM);role("pyromancer_boots",1,ItemID.PYROMANCER_BOOTS);
  role("eye_hat",1,ItemID.HAT_OF_THE_EYE,ItemID.HAT_OF_THE_EYE_RED,ItemID.HAT_OF_THE_EYE_GREEN,ItemID.HAT_OF_THE_EYE_BLUE);
  role("eye_top",1,ItemID.ROBE_TOP_OF_THE_EYE,ItemID.ROBE_TOP_OF_THE_EYE_RED,ItemID.ROBE_TOP_OF_THE_EYE_GREEN,ItemID.ROBE_TOP_OF_THE_EYE_BLUE);
  role("eye_legs",1,ItemID.ROBE_BOTTOM_OF_THE_EYE,ItemID.ROBE_BOTTOM_OF_THE_EYE_RED,ItemID.ROBE_BOTTOM_OF_THE_EYE_GREEN,ItemID.ROBE_BOTTOM_OF_THE_EYE_BLUE);
  role("eye_boots",1,ItemID.BOOTS_OF_THE_EYE);
  role("smith_top",1,ItemID.SMITHING_UNIFORM_TORSO);role("smith_legs",1,ItemID.SMITHING_UNIFORM_LEGS);role("smith_boots",1,ItemID.SMITHING_UNIFORM_BOOTS);role("smith_gloves",1,ItemID.SMITHING_UNIFORM_GLOVES,ItemID.SMITHING_UNIFORM_GLOVES_ICE);
  role("zealot_hat",1,ItemID.SHADES_PRAYER_HELM);role("zealot_top",1,ItemID.SHADES_PRAYER_TOP);role("zealot_legs",1,ItemID.SHADES_PRAYER_BOTTOM);role("zealot_boots",1,ItemID.SHADES_PRAYER_BOOTS);
  NATURAL_FAMILIES.addAll(ROLES.keySet());NATURAL_FAMILIES.removeAll(historicalRoles);
 }
 private static void ring(String role,int unimbued,int...imbued){role(role,0,unimbued);role(role,1,imbued);}
 private static void expandedEntries()
 {
  item("abyssal_lantern","Abyssal Lantern","Utility & Storage",3000000,1,"abyssal_lantern");
  item("fish_barrel","Fish Barrel","Utility & Storage",4000000,1,"fish_barrel");
  item("herb_sack","Herb Sack","Utility & Storage",2500000,1,"herb_sack");
  item("plank_sack","Plank Sack","Utility & Storage",1500000,1,"plank_sack");
  item("seed_box","Seed Box","Utility & Storage",2500000,1,"seed_box");
  item("tackle_box","Tackle Box","Utility & Storage",4000000,1,"tackle_box");
  item("saeldor_corrupted","Blade of Saeldor (corrupted)","Weapons",110000000,1,"saeldor_corrupted");
  item("faerdhinen_corrupted","Bow of Faerdhinen (corrupted)","Weapons",130000000,1,"faerdhinen_corrupted");
  item("emberlight","Emberlight","Weapons",45000000,1,"emberlight");
  item("keris_breaching","Keris Partisan of Breaching","Weapons",12000000,1,"keris_breaching");
  item("keris_corruption","Keris Partisan of Corruption","Weapons",12000000,1,"keris_corruption");
  item("keris_sun","Keris Partisan of the Sun","Weapons",12000000,1,"keris_sun");
  single("avernic_defender","Avernic Defender",31000000,"avernic_defender");
  single("crystal_helm","Crystal Helm",6000000,"crystal_helm");
  single("crystal_body","Crystal Body",18000000,"crystal_body");
  single("crystal_legs","Crystal Legs",12000000,"crystal_legs");
  single("quiver","Dizana's Quiver",15000000,"quiver");
  item("blessed_quiver","Dizana's Quiver (blessed)","Armour",60000000,2,"quiver");
  single("elidinis_ward_f","Elidinis' Ward (f)",105000000,"elidinis_ward_f");
  single("archers_ring_i","Archers Ring (i)",3000000,"archers_ring_i");
  single("berserker_ring_i","Berserker Ring (i)",4000000,"berserker_ring_i");
  single("granite_ring_i","Granite Ring (i)",1000000,"granite_ring_i");
  single("gods_ring_i","Ring of the Gods (i)",2000000,"gods_ring_i");
  single("suffering_ring_i","Ring of Suffering (i)",15000000,"suffering_ring_i");
  single("seers_ring_i","Seers Ring (i)",1500000,"seers_ring_i");
  single("treasonous_ring_i","Treasonous Ring (i)",1100000,"treasonous_ring_i");
  single("tyrannical_ring_i","Tyrannical Ring (i)",1200000,"tyrannical_ring_i");
  single("warrior_ring_i","Warrior Ring (i)",1100000,"warrior_ring_i");
  single("wealth_ring_i","Ring of Wealth (i)",1000000,"wealth_ring_i");
  outfit("carpenter","Carpenter's Outfit",2000000,"hat","top","legs","boots");
  outfit("pyromancer","Pyromancer Outfit",2000000,"hat","top","legs","boots");
  outfit("eye","Raiments of the Eye",2000000,"hat","top","legs","boots");
  outfit("smith","Smiths' Uniform",2000000,"top","legs","boots","gloves");
  outfit("zealot","Zealot's Robes",2000000,"hat","top","legs","boots");
 }
 private static void role(String key,int level,int...ids){Map<Integer,Integer> values=ROLES.computeIfAbsent(key,k->new TreeMap<>());for(int id:ids){values.put(id,level);}}
 private static void single(String id,String name,long gp,String role){item(id,name,id.equals("ancient_sceptre")?"Weapons":"Armour",gp,1,role);}
 private static void item(String id,String name,String category,long gp,int level,String role){Map<String,Integer> roles=new LinkedHashMap<>();roles.put(role,level);add(id,name,category,gp,roles);}
 private static void outfit(String id,String name,long gp,String...parts){Map<String,Integer> roles=new LinkedHashMap<>();for(String part:parts){roles.put(id+"_"+part,1);}add(id,name,"Skilling Outfits",gp,roles);}
 private static void add(String id,String name,String category,long gp,Map<String,Integer> roles){String first=roles.keySet().iterator().next();int level=roles.get(first);int icon=ROLES.get(first).entrySet().stream().filter(e->e.getValue()==level).findFirst().get().getKey();ENTRIES.put(id,new BuybackEntry(id,name,category,icon,gp,roles));}
 public static BuybackEntry entry(String id){BuybackEntry e=ENTRIES.get(id);if(e==null){throw new IllegalArgumentException("This item is not approved for buyback.");}return e;}
 public static Collection<BuybackEntry> entries(){List<BuybackEntry> entries=new ArrayList<>(ENTRIES.values());entries.sort(ORDER);return Collections.unmodifiableList(entries);}
 public static Map<Integer,Integer> variants(String role){if(SelectedBuybacks.copyRole(role)){return Collections.singletonMap(SelectedBuybacks.item(role),1);}Map<Integer,Integer> ids=ROLES.get(role);if(ids==null){throw new IllegalArgumentException("Unknown entitlement role");}return Collections.unmodifiableMap(ids);}
 public static long cost(BuybackEntry entry,Map<String,Integer> owned){if(entry.getId().startsWith("elite_void_")&&owned.getOrDefault(entry.getRoles().keySet().iterator().next(),0)>=1){return 1000000;}if(entry.getId().equals("blessed_quiver")&&owned.getOrDefault("quiver",0)>=1){return 45000000;}return entry.getPrice();}
 public static Set<String> ringRoles(){return RINGS;}
 public static Set<String> naturalFamilies(){return Collections.unmodifiableSet(NATURAL_FAMILIES);}
 public static List<BuybackEntry> search(String text){String query=text.toLowerCase(Locale.ROOT).trim();List<BuybackEntry> result=new ArrayList<>();for(BuybackEntry entry:entries()){if(entry.getName().toLowerCase(Locale.ROOT).contains(query)){result.add(entry);}}return result;}
}
