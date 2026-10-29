package com.fromscratch.buyback;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Fixed release prices, explicit variants, complete outfits and shared single-copy transformations. */
public class ExpandedBuybacksTest
{
 public static final Map<String,Long> PRICES=new LinkedHashMap<>();
 static
 {
  price("abyssal_lantern",3000000);price("fish_barrel",4000000);price("herb_sack",2500000);price("plank_sack",1500000);price("seed_box",2500000);price("tackle_box",4000000);
  price("saeldor_corrupted",110000000);price("faerdhinen_corrupted",130000000);price("emberlight",45000000);price("keris_breaching",12000000);price("keris_corruption",12000000);price("keris_sun",12000000);
  price("avernic_defender",31000000);price("crystal_helm",6000000);price("crystal_body",18000000);price("crystal_legs",12000000);price("quiver",15000000);price("blessed_quiver",60000000);price("elidinis_ward_f",105000000);
  price("archers_ring_i",3000000);price("berserker_ring_i",4000000);price("granite_ring_i",1000000);price("gods_ring_i",2000000);price("suffering_ring_i",15000000);price("seers_ring_i",1500000);price("treasonous_ring_i",1100000);price("tyrannical_ring_i",1200000);price("warrior_ring_i",1100000);price("wealth_ring_i",1000000);
  for(String key:new String[]{"carpenter","pyromancer","eye","smith","zealot"}){price(key,2000000);}
 }
 private static void price(String id,long gp){PRICES.put(id,gp);}
 public static int[] pieces(BuybackEntry entry){return entry.getRoles().entrySet().stream().mapToInt(role->BuybackCatalog.variants(role.getKey()).entrySet().stream().filter(v->v.getValue().equals(role.getValue())).findFirst().get().getKey()).toArray();}
 public static BankRun purchase(BankRun run,String id)
 {return EmptyStorageConfirmation.required(id)?BankEngine.buyEmptyStorage(run,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,new EmptyStorageConfirmation(id,run.getRevision(),SnapshotFixtures.TIME,true,true)):BuybackModelTest.buy(run,id);}
 private static BankRun observe(BankRun run,int...ids){Map<Integer,Long> q=new TreeMap<>(BuybackModelTest.bank(ids).getQuantities());q.put(ItemID.COINS,run.physical(ItemID.COINS));return BankEngine.observe(run,new ContainerSnapshot(q,q.size()),UUID.randomUUID(),SnapshotFixtures.TIME);}
 private static long visible(BankRun run,int id){return BuybackEngine.balances(run).getOrDefault(id,0L);}
 private static void rejected(Runnable action){try{action.run();fail("Expected safe rejection");}catch(IllegalArgumentException|ArithmeticException expected){}}
 @Test public void everyNewPurchaseHasItsExactFixedPriceAndAtomicOneCopyRoles()
 {
  PRICES.forEach((id,gp)->{BuybackEntry e=BuybackCatalog.entry(id);assertEquals(id,(long)gp,e.getPrice());int[] pieces=pieces(e);BankRun before=BuybackModelTest.funded(gp,pieces),after=purchase(before,id);assertEquals(id,0,BuybackEngine.available(after));assertEquals(id,(long)gp,after.getBuybacks().getCommittedGp());assertEquals(before.getPhysical(),after.getPhysical());assertEquals(before.getBalances(),after.getBalances());assertEquals(before.getInitialBank(),after.getInitialBank());assertEquals(1,after.getBuybacks().getPurchases().size());for(int piece:pieces){assertEquals(id,1,visible(after,piece));}});
 }
 @Test public void everyNewPurchaseRejectsInsufficientGpMissingLegacyAndRepeatedPayment()
 {
  PRICES.forEach((id,gp)->{BuybackEntry e=BuybackCatalog.entry(id);int[] pieces=pieces(e);BankRun poor=BuybackModelTest.funded(gp-1,pieces);rejected(()->purchase(poor,id));assertEquals(0,poor.getBuybacks().getCommittedGp());rejected(()->purchase(BuybackModelTest.funded(gp),id));BankRun paid=purchase(BuybackModelTest.funded(Math.multiplyExact(gp,2),pieces),id);rejected(()->purchase(paid,id));assertEquals((long)gp,paid.getBuybacks().getCommittedGp());});
 }
 @Test public void everyNewPurchaseRejectsANaturallyEarnedCopyAlone()
 {PRICES.forEach((id,gp)->{BankRun earned=observe(BuybackModelTest.funded(gp),pieces(BuybackCatalog.entry(id)));rejected(()->purchase(earned,id));});}
 @Test public void everyApprovedVariantQualifiesButNeverUnlocksMoreThanOnePhysicalCopy()
 {
  PRICES.forEach((id,gp)->{BuybackEntry e=BuybackCatalog.entry(id);if(e.getRoles().size()!=1){return;}String role=e.getRoles().keySet().iterator().next();for(Map.Entry<Integer,Integer> variant:BuybackCatalog.variants(role).entrySet()){if(!variant.getValue().equals(e.getRoles().get(role))){continue;}int item=variant.getKey();BankRun paid=purchase(BuybackModelTest.funded(gp,item,item),id);long count=BuybackCatalog.variants(role).keySet().stream().mapToLong(i->Math.max(0,visible(paid,i))).sum();assertEquals(id+":"+item,1,count);}});
 }
 @Test public void everyNewOutfitRequiresAllFourDistinctPieces()
 {
  for(String id:new String[]{"carpenter","pyromancer","eye","smith","zealot"}){BuybackEntry e=BuybackCatalog.entry(id);int[] all=pieces(e);assertEquals(4,all.length);for(int missing=0;missing<all.length;missing++){int[] partial=new int[3];for(int i=0,j=0;i<all.length;i++){if(i!=missing){partial[j++]=all[i];}}BankRun run=BuybackModelTest.funded(e.getPrice(),partial);rejected(()->purchase(run,id));assertEquals("Missing set pieces",BuybackEngine.status(run,e));}}
 }
 @Test public void outfitCosmeticsShareOneCopyPerPieceAndUnrelatedRewardsDoNotSubstitute()
 {
  int[] eye={ItemID.HAT_OF_THE_EYE_RED,ItemID.HAT_OF_THE_EYE_BLUE,ItemID.ROBE_TOP_OF_THE_EYE_GREEN,ItemID.ROBE_BOTTOM_OF_THE_EYE_BLUE,ItemID.BOOTS_OF_THE_EYE};BankRun paid=purchase(BuybackModelTest.funded(2000000,eye),"eye");assertEquals(1,visible(paid,ItemID.HAT_OF_THE_EYE_RED)+visible(paid,ItemID.HAT_OF_THE_EYE_BLUE));
  int[] smith={ItemID.SMITHING_UNIFORM_TORSO,ItemID.SMITHING_UNIFORM_LEGS,ItemID.SMITHING_UNIFORM_BOOTS,ItemID.SMITHING_UNIFORM_GLOVES_ICE};assertEquals(1,visible(purchase(BuybackModelTest.funded(2000000,smith),"smith"),ItemID.SMITHING_UNIFORM_GLOVES_ICE));
  rejected(()->purchase(BuybackModelTest.funded(2000000,ItemID.PYROMANCER_HOOD,ItemID.PYROMANCER_TOP,ItemID.PYROMANCER_BOTTOM,ItemID.PYROMANCER_GLOVES),"pyromancer"));
 }
 @Test public void quiverDirectBlessedAndUpgradeBothTotalSixtyMillionForOneEntitlement()
 {
  int regular=ItemID.DIZANAS_QUIVER_UNCHARGED,blessed=ItemID.DIZANAS_QUIVER_INFINITE;BankRun upgraded=purchase(BuybackModelTest.funded(60000000,regular,blessed),"quiver");assertEquals(45000000,BuybackCatalog.cost(BuybackCatalog.entry("blessed_quiver"),upgraded.getBuybacks().getEntitlements()));upgraded=purchase(upgraded,"blessed_quiver");assertEquals(60000000,upgraded.getBuybacks().getCommittedGp());assertEquals(0,visible(upgraded,regular));assertEquals(1,visible(upgraded,blessed));assertEquals(1,upgraded.getBuybacks().getEntitlements().size());BankRun direct=purchase(BuybackModelTest.funded(60000000,regular,blessed),"blessed_quiver");assertEquals(60000000,direct.getBuybacks().getCommittedGp());assertEquals(1,visible(direct,blessed));rejected(()->purchase(direct,"quiver"));
 }
 @Test public void ordinaryQuiverDoesNotUnlockBlessedUntilUpgradeAndCannotRefillWithdrawnCopy()
 {
  BankRun base=purchase(BuybackModelTest.funded(60000000,ItemID.DIZANAS_QUIVER_UNCHARGED),"quiver");BankRun changed=observe(base,ItemID.DIZANAS_QUIVER_INFINITE);assertEquals(0,visible(changed,ItemID.DIZANAS_QUIVER_INFINITE));assertEquals(1,visible(purchase(changed,"blessed_quiver"),ItemID.DIZANAS_QUIVER_INFINITE));BankRun withTwo=purchase(BuybackModelTest.funded(60000000,ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.DIZANAS_QUIVER_INFINITE),"quiver");BankRun outside=observe(withTwo,ItemID.DIZANAS_QUIVER_INFINITE);rejected(()->purchase(outside,"blessed_quiver"));
 }
 @Test public void paidRegularQuiverCannotMakeAnIndependentlyEarnedBlessedCopyBillable()
 {
  BankRun paid=purchase(BuybackModelTest.funded(60000000,ItemID.DIZANAS_QUIVER_UNCHARGED),"quiver");BankRun earned=observe(paid,ItemID.DIZANAS_QUIVER_UNCHARGED,ItemID.DIZANAS_QUIVER_INFINITE);
  assertEquals(1,visible(earned,ItemID.DIZANAS_QUIVER_UNCHARGED));assertEquals(1,visible(earned,ItemID.DIZANAS_QUIVER_INFINITE));assertEquals("Already unlocked",BuybackEngine.status(earned,BuybackCatalog.entry("blessed_quiver")));rejected(()->purchase(earned,"blessed_quiver"));assertEquals(15000000,earned.getBuybacks().getCommittedGp());
 }
 @Test public void componentSeedsHiltsAndUncorruptedWeaponsCannotQualify()
 {
  for(String key:new String[]{"saeldor_corrupted","faerdhinen_corrupted","emberlight","avernic_defender","elidinis_ward_f"}){BuybackEntry e=BuybackCatalog.entry(key);BankRun run=BuybackModelTest.funded(e.getPrice(),ItemID.BLADE_OF_SAELDOR_INACTIVE,ItemID.BOW_OF_FAERDHINEN_INACTIVE,ItemID.INFERNAL_DEFENDER_HILT,ItemID.ELIDINIS_WARD);rejected(()->purchase(run,key));}
  rejected(()->purchase(BuybackModelTest.funded(12000000,ItemID.KERIS_PARTISAN),"keris_sun"));
 }
 @Test public void imbuedRingFamiliesRetainOneCopyAcrossAllSourcesAndUnimbuedForms()
 {
  for(String role:BuybackCatalog.ringRoles()){BuybackEntry entry=BuybackCatalog.entry(role);int imbued=pieces(entry)[0],unimbued=BuybackCatalog.variants(role).entrySet().stream().filter(e->e.getValue()==0).findFirst().get().getKey();BankRun paid=purchase(BuybackModelTest.funded(entry.getPrice(),imbued,imbued),role);BankRun changed=observe(paid,unimbued,imbued);assertEquals(role,1,visible(changed,unimbued)+visible(changed,imbued));BankRun back=observe(changed,imbued,imbued);assertEquals(role,1,visible(back,imbued));rejected(()->purchase(back,role));rejected(()->purchase(BuybackModelTest.funded(entry.getPrice(),unimbued),role));}
 }
 @Test public void unimbuingLegacyRingCannotCreateFreeOwnershipWithoutPurchase()
 {
  BankRun run=BuybackModelTest.funded(4000000,ItemID.NZONE_BERZERKER_RING);run=observe(run,ItemID.BERZERKER_RING);assertEquals(0,visible(run,ItemID.BERZERKER_RING));run=observe(run,ItemID.SW_BERZERKER_RING);assertEquals(0,visible(run,ItemID.SW_BERZERKER_RING));assertEquals(1,visible(purchase(run,"berserker_ring_i"),ItemID.SW_BERZERKER_RING));
 }
 @Test public void legacyUnimbuedRingCannotMakeAnEarnedImbuedRingQualify()
 {BankRun run=observe(BuybackModelTest.funded(4000000,ItemID.BERZERKER_RING),ItemID.BERZERKER_RING,ItemID.NZONE_BERZERKER_RING);assertEquals(1,visible(run,ItemID.NZONE_BERZERKER_RING));rejected(()->purchase(run,"berserker_ring_i"));}
 @Test public void newEquivalentFamiliesCannotCreateOwnershipByChangingAnUnpaidLegacyForm()
 {
  Map<Integer,String> disjoint=new HashMap<>();for(String role:BuybackCatalog.naturalFamilies()){List<Integer> ids=new ArrayList<>(BuybackCatalog.variants(role).keySet());for(int id:ids){assertNull("Overlapping family for "+id,disjoint.put(id,role));}if(ids.size()<2){continue;}int original=ids.get(0),changed=ids.get(ids.size()-1);BankRun legacy=observe(BuybackModelTest.funded(200000000,original),changed);long unpaid=ids.stream().mapToLong(i->Math.max(0,visible(legacy,i))).sum();assertEquals(role,0,unpaid);BankRun earned=observe(legacy,changed,changed);long copies=ids.stream().mapToLong(i->Math.max(0,visible(earned,i))).sum();assertEquals(role,1,copies);}
 }
 @Test public void oldCatalogPricesAndSelectedSlayerAllowlistStayUnchanged()
 {
  String[] ids={"fire_cape","infernal_cape","saradomin_cape","guthix_cape","zamorak_cape","ancient_cape","assembler","defender","torso","salve_ei","ancient_sceptre","rune_pouch_family","looting_bag_family","void_melee","void_ranger","void_mage","void_top","void_robe","void_gloves","elite_void_top","elite_void_robe","graceful","prospector","angler","rogue","lumberjack","farmer","slayer_gear","elite_black_helm","elite_black_body","elite_black_legs"};
  long[] gp={1000000,15000000,750000,750000,750000,750000,1000000,250000,2000000,500000,500000,1000000,10000,300000,300000,300000,800000,800000,200000,1800000,1800000,1000000,1000000,750000,500000,500000,750000,1000000,30000,30000,30000};for(int i=0;i<ids.length;i++){assertEquals(ids[i],gp[i],BuybackCatalog.entry(ids[i]).getPrice());}assertEquals(104,SelectedBuybacks.items("slayer_gear").size());assertEquals(1,BuybackCatalog.VERSION);assertEquals(ids.length+PRICES.size(),BuybackCatalog.entries().size());
 }
 @Test public void categoryAndSearchOrdersAreDeterministicAndAlphabeticalWithNoLostEntries()
 {
  assertEquals(Arrays.asList("Utility & Storage","Weapons","Armour","Skilling Outfits"),BuybackCatalog.CATEGORIES);List<BuybackEntry> all=BuybackCatalog.search("");assertEquals("Abyssal Lantern",all.get(0).getName());Set<String> ids=new HashSet<>();BuybackEntry previous=null;for(BuybackEntry entry:all){assertTrue(ids.add(entry.getId()));assertTrue(BuybackCatalog.CATEGORIES.contains(entry.getCategory()));if(previous!=null){int a=BuybackCatalog.CATEGORIES.indexOf(previous.getCategory()),b=BuybackCatalog.CATEGORIES.indexOf(entry.getCategory());assertTrue(a<=b);if(a==b){int names=String.CASE_INSENSITIVE_ORDER.compare(previous.getName(),entry.getName());assertTrue(names<0||names==0&&previous.getId().compareTo(entry.getId())<0);}}previous=entry;}List<BuybackEntry> filtered=BuybackCatalog.search("RING");List<BuybackEntry> expected=new ArrayList<>();for(BuybackEntry entry:all){if(entry.getName().toLowerCase(Locale.ROOT).contains("ring")){expected.add(entry);}}assertEquals(expected,filtered);
 }
}
