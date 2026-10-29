package com.fromscratch.bank;

import java.util.UUID;

/** User intent only; no operation operates the game UI. */
public interface BankActions
{
	void startRun(boolean attested);
	default void setupAttested(UUID attempt,boolean value) { }
	default void setupStorageAnswer(UUID attempt,Boolean unlocked) { }
	default void startRun(boolean attested,com.fromscratch.potion.PotionCapture captured,boolean notUnlocked){startRun(attested);}
	void anchor(UUID revision, com.fromscratch.model.ContainerSnapshot reviewedBank, boolean approveReductions);
	void correct(UUID revision, int itemId, long desired);
	void reset(UUID revision);
	void retry();
	default void reviewStorage(UUID revision,java.util.Map<Integer,Long> quantities){throw new UnsupportedOperationException("Storage review unavailable");}
	default void correctStorage(UUID revision,int type,long desired){throw new UnsupportedOperationException("Storage correction unavailable");}
	default void exportProgress(net.runelite.client.util.Filepath file,java.util.function.Consumer<String> result){result.accept("Export is unavailable.");}
	default void previewImport(net.runelite.client.util.Filepath file,java.util.function.Consumer<com.fromscratch.persistence.ProgressBackup.Candidate> result,java.util.function.Consumer<String> failure){failure.accept("Import is unavailable.");}
	default void importProgress(com.fromscratch.persistence.ProgressBackup.Candidate candidate,java.util.function.Consumer<String> result){result.accept("Import is unavailable.");}
	default void cancelImport(com.fromscratch.persistence.ProgressBackup.Candidate candidate) { }
	default void previewRecovery(java.util.function.Consumer<com.fromscratch.persistence.ProgressBackup.Candidate> result,java.util.function.Consumer<String> failure){failure.accept("Recovery is unavailable.");}
	default void historyItems(java.util.function.Consumer<java.util.Map<Integer,String>> callback) { callback.accept(java.util.Collections.emptyMap()); }
	default void itemHistory(int itemId,BankHistory.Cursor cursor,java.util.function.Consumer<ItemHistory.Page> callback)
	{ callback.accept(new ItemHistory.Page(java.util.Collections.emptyList(),null,"No recorded item history.")); }
	default void buySelected(UUID revision,String catalogId,int itemId,UUID purchaseId){throw new UnsupportedOperationException("Selected buybacks unavailable");}
	default void buyback(UUID revision,String catalogId,UUID purchaseId){throw new UnsupportedOperationException("Buybacks unavailable");}
	default void pouchPreview(java.util.function.Consumer<com.fromscratch.pouch.PouchObservation> callback){callback.accept(com.fromscratch.pouch.PouchObservation.unknown());}
	default void bagPreview(java.util.function.Consumer<com.fromscratch.buyback.BagObservation> callback){callback.accept(com.fromscratch.buyback.BagObservation.unknown());}
	default void buyBag(UUID revision,UUID purchaseId,com.fromscratch.buyback.BagObservation contents,java.util.function.Consumer<com.fromscratch.buyback.BuybackPurchase> committed){throw new UnsupportedOperationException("Bag buybacks unavailable");}
	default void buyback(UUID revision,String catalogId,UUID purchaseId,com.fromscratch.pouch.PouchObservation contents,java.util.function.Consumer<com.fromscratch.buyback.BuybackPurchase> committed){buyback(revision,catalogId,purchaseId);}
	default void recordExpense(UUID revision,long gp,String reason) { throw new UnsupportedOperationException("Expense recording is unavailable"); }
	default void exchangeHistory(boolean buy,com.fromscratch.ge.GeHistory.Cursor cursor,java.util.function.Consumer<com.fromscratch.ge.GeHistory.Page> callback)
	{callback.accept(new com.fromscratch.ge.GeHistory.Page(java.util.Collections.emptyList(),null,"No completed Grand Exchange trades recorded."));}
	default void history(boolean manual, BankHistory.Cursor cursor, java.util.function.Consumer<BankHistory.Page> callback)
	{ callback.accept(new BankHistory.Page(java.util.Collections.emptyList(), null, "No bank changes recorded.")); }
}
