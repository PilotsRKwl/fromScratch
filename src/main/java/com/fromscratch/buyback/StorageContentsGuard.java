package com.fromscratch.buyback;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;

/** On-demand nonempty veto only. Zero/missing/stale data never establishes an empty store. */
public final class StorageContentsGuard
{
	public interface Access
	{
		boolean loggedIn();
		boolean varpsAvailable();
		int varbit(int id);
		Item[] items(int containerId);
	}
	private final Access access;
	private final Set<Integer> received=new HashSet<>();
	public StorageContentsGuard(Client client){this(new Access(){public boolean loggedIn(){return client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null;}public boolean varpsAvailable(){return client.getVarps()!=null;}public int varbit(int id){return client.getVarbitValue(id);}public Item[] items(int id){ItemContainer container=client.getItemContainer(id);return container==null?null:container.getItems();}});}
	public StorageContentsGuard(Access access){this.access=access;}
	public void changed(int containerId){if(containerId==InventoryID.SEED_BOX||containerId==InventoryID.TACKLE_BOX||containerId==InventoryID.DIZANAS_QUIVER_AMMO){received.add(containerId);}}
	public void clear(){received.clear();}
	public boolean nonempty(String catalogId)
	{
		if(!access.loggedIn()){return false;}
		if("plank_sack".equals(catalogId)&&access.varpsAvailable())
		{
			for(int bit:new int[]{VarbitID.PLANK_SACK_PLAIN,VarbitID.PLANK_SACK_OAK,VarbitID.PLANK_SACK_TEAK,VarbitID.PLANK_SACK_MAHOGANY,VarbitID.PLANK_SACK_CAMPHOR,VarbitID.PLANK_SACK_IRONWOOD,VarbitID.PLANK_SACK_ROSEWOOD}){if(access.varbit(bit)>0){return true;}}
		}
		int id="seed_box".equals(catalogId)?InventoryID.SEED_BOX:"tackle_box".equals(catalogId)?InventoryID.TACKLE_BOX:"quiver".equals(catalogId)||"blessed_quiver".equals(catalogId)?InventoryID.DIZANAS_QUIVER_AMMO:-1;
		if(!received.contains(id)){return false;}
		Item[] items=access.items(id);
		if(items!=null){for(Item item:items){if(item!=null&&item.getId()>=0&&item.getQuantity()>0){return true;}}}
		return false;
	}
}
