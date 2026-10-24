package com.fromscratch.bank;

/** Client-thread bank/storage boundary. Personal inventory/equipment are deliberately absent. */
public interface BankSource
{
	String characterKey();
	BankObservation observe();
	default boolean fullBankView(){return true;}
	default boolean potionStorageOpen(){return false;}
	default boolean potionStorageUnlocked(){return false;}
	default boolean potionStorageUnlockKnown(){return false;}
	default void newPotionSetup(){ }
	default boolean validPotionCapture(com.fromscratch.potion.PotionObservation observation){return true;}
	default com.fromscratch.potion.PotionObservation potions(){return com.fromscratch.potion.PotionObservation.unavailableStore();}
	String itemName(int id);
	/** Price identity only: accounting still keeps exact IDs. Implementations resolve this on the client thread. */
	default int priceId(int id) { return id; }
	/** Fail closed unless the live definition verifies genuine untradeable equipment, never a note/placeholder. */
	default boolean buybackDefinition(int id) { return false; }
	default com.fromscratch.pouch.PouchObservation pouch(){return com.fromscratch.pouch.PouchObservation.unknown();}
	default com.fromscratch.buyback.BagObservation bag(){return com.fromscratch.buyback.BagObservation.unknown();}
}
