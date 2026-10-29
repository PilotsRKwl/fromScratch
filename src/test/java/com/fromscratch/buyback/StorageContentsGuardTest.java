package com.fromscratch.buyback;

import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class StorageContentsGuardTest
{
	private static final class Access implements StorageContentsGuard.Access
	{
		boolean loggedIn=true,varps=true;Map<Integer,Item[]> containers=new HashMap<>();Map<Integer,Integer> bits=new HashMap<>();
		public boolean loggedIn(){return loggedIn;}public boolean varpsAvailable(){return varps;}public int varbit(int id){return bits.getOrDefault(id,0);}public Item[] items(int id){return containers.get(id);}
	}
	@Test public void cachedContainersNeverBecomeFreshWithoutCurrentSessionPacket()
	{
		Access a=new Access();StorageContentsGuard guard=new StorageContentsGuard(a);a.containers.put(InventoryID.SEED_BOX,new Item[]{new Item(ItemID.POTATO_SEED,1)});
		assertFalse(guard.nonempty("seed_box"));guard.changed(InventoryID.SEED_BOX);assertTrue(guard.nonempty("seed_box"));guard.clear();assertFalse(guard.nonempty("seed_box"));
	}
	@Test public void emptyMissingPartialAndFullNativeContainersAreOnlyVetoes()
	{
		Access a=new Access();StorageContentsGuard guard=new StorageContentsGuard(a);guard.changed(InventoryID.TACKLE_BOX);assertFalse(guard.nonempty("tackle_box"));a.containers.put(InventoryID.TACKLE_BOX,new Item[0]);assertFalse(guard.nonempty("tackle_box"));
		a.containers.put(InventoryID.TACKLE_BOX,new Item[]{new Item(-1,0),new Item(ItemID.FEATHER,1)});assertTrue(guard.nonempty("tackle_box"));a.containers.put(InventoryID.TACKLE_BOX,new Item[]{new Item(ItemID.FEATHER,Integer.MAX_VALUE)});assertTrue(guard.nonempty("tackle_box"));a.loggedIn=false;assertFalse(guard.nonempty("tackle_box"));
	}
	@Test public void allSupportedPlankQuantitiesBlockEmptyDeclaration()
	{
		for(int bit:new int[]{VarbitID.PLANK_SACK_PLAIN,VarbitID.PLANK_SACK_OAK,VarbitID.PLANK_SACK_TEAK,VarbitID.PLANK_SACK_MAHOGANY,VarbitID.PLANK_SACK_CAMPHOR,VarbitID.PLANK_SACK_IRONWOOD,VarbitID.PLANK_SACK_ROSEWOOD})
		{Access a=new Access();StorageContentsGuard guard=new StorageContentsGuard(a);a.bits.put(bit,1);assertTrue(guard.nonempty("plank_sack"));a.bits.put(bit,28);assertTrue(guard.nonempty("plank_sack"));a.varps=false;assertFalse(guard.nonempty("plank_sack"));}
	}
	@Test public void unavailableFishAndHerbContentsCannotBeFabricated(){StorageContentsGuard guard=new StorageContentsGuard(new Access());assertFalse(guard.nonempty("fish_barrel"));assertFalse(guard.nonempty("herb_sack"));assertTrue(EmptyStorageConfirmation.required("fish_barrel"));assertTrue(EmptyStorageConfirmation.required("herb_sack"));}
	@Test public void quiverAmmoIsOneSharedStoreForRegularAndBlessedForms(){Access a=new Access();StorageContentsGuard guard=new StorageContentsGuard(a);a.containers.put(InventoryID.DIZANAS_QUIVER_AMMO,new Item[]{new Item(ItemID.RUNE_ARROW,1000)});assertFalse(guard.nonempty("quiver"));guard.changed(InventoryID.DIZANAS_QUIVER_AMMO);assertTrue(guard.nonempty("quiver"));assertTrue(guard.nonempty("blessed_quiver"));assertTrue(EmptyStorageConfirmation.required("quiver"));assertTrue(EmptyStorageConfirmation.required("blessed_quiver"));}
}
