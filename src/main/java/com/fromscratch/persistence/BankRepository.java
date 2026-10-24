package com.fromscratch.persistence;

import com.fromscratch.bank.*;
import com.fromscratch.model.*;
import com.fromscratch.wealth.*;
import com.fromscratch.ge.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import javax.inject.Inject;
import net.runelite.client.util.Filepath;

/** Bank-only durable checkpoint/journal. All methods are background-worker-only. */
public final class BankRepository
{
	private final BankRunCodec codec;
	private final RunRecordCodec legacy;
	private final Publication publication;
	private final ProgressBackup backups;
	@Inject public BankRepository(BankRunCodec codec, RunRecordCodec legacy) { this(codec, legacy, BankRepository::publish); }
	BankRepository(BankRunCodec codec, RunRecordCodec legacy, Publication publication) { this.codec = codec; this.legacy = legacy; this.publication = publication;this.backups=new ProgressBackup(this,codec); }
	public ProgressBackup backups() { return backups; }
	public Optional<BankRun> load(Filepath root, String character) throws IOException
	{
		return locked(root, character, d ->
		{
			Filepath current = d.joinSegment("current.json");
			if (!current.exists())
			{
				try (Stream<Filepath> files = d.walk(1)) { if (files.anyMatch(f -> f.getFileName().endsWith(".json") || f.getFileName().startsWith("bank-migration-"))) { throw new SaveException("Current save is missing but recovery data exists. Restore its matching checkpoint; no new run was created."); } }
				return Optional.empty();
			}
			String text = BankStorageFiles.readText(current);
			int version;
			try { version = Math.toIntExact(BankRunCodec.number(new JsonParser().parse(text).getAsJsonObject().get("formatVersion"))); }
			catch (RuntimeException ex) { throw new SaveException("Save header is unreadable. Existing files were preserved."); }
			if (version == BankRunCodec.FORMAT || version == 9) { BankRun run = codec.decode(text, character); verify(d, run, true); return Optional.of(run); }
			if(version==8)
			{
				BankRun old=codec.decode(text,character);verify(d,old,true);String archived=archive(d);
				BankAudit audit=new BankAudit(UUID.randomUUID(),UUID.randomUUID(),Instant.now(),BankAudit.Kind.POUCH_BASELINE,Collections.emptyMap(),Collections.emptyMap(),"M7 files archived exactly. Existing pouch payment and adjustments retained; no original contents inferred.");
				List<BankAudit> recent=new ArrayList<>(old.getRecent());recent.add(0,audit);if(recent.size()>30){recent.remove(30);}
				BankRun next=new BankRun(old.getCharacter(),old.getRunId(),old.getPreviousRunId(),old.getStartedAt(),audit.getId(),Math.incrementExact(old.getSequence()),old.getInitialBank(),old.getPhysical(),old.getBalances(),old.isAnchored(),old.getLegacyDocument(),old.getMigrationArchive(),recent,old.getHostSummary(),old.getFinancial(),old.getExchange(),old.getBuybacks().withPouch(com.fromscratch.pouch.PouchState.unreviewed(archived)));
				commit(d,old,next);return Optional.of(next);
			}
			if(version==7)
			{
				BankRun old=codec.decode(text,character);verify(d,old,true);String archived=archive(d);
				BankAudit audit=new BankAudit(UUID.randomUUID(),UUID.randomUUID(),Instant.now(),BankAudit.Kind.BUYBACK_BASELINE,Collections.emptyMap(),Collections.emptyMap(),"M6 checkpoint and journals archived exactly. No previous buybacks were inferred.");
				List<BankAudit> recent=new ArrayList<>(old.getRecent());recent.add(0,audit);if(recent.size()>30){recent.remove(30);}
				BankRun upgraded=new BankRun(old.getCharacter(),old.getRunId(),old.getPreviousRunId(),old.getStartedAt(),audit.getId(),Math.incrementExact(old.getSequence()),old.getInitialBank(),old.getPhysical(),old.getBalances(),old.isAnchored(),old.getLegacyDocument(),old.getMigrationArchive(),recent,old.getHostSummary(),old.getFinancial(),old.getExchange(),com.fromscratch.buyback.BuybackState.empty(archived));
				commit(d,old,upgraded);return Optional.of(upgraded);
			}
			if(version==6)
			{
				BankRun old=codec.decode(text,character);verify(d,old,true);String archived=archive(d);
				BankAudit audit=new BankAudit(UUID.randomUUID(),UUID.randomUUID(),Instant.now(),BankAudit.Kind.EXCHANGE_BASELINE,Collections.emptyMap(),Collections.emptyMap(),"M5 files archived exactly. GE observation starts without replaying old trades or gains.");
				List<BankAudit> recent=new ArrayList<>(old.getRecent());recent.add(0,audit);if(recent.size()>30){recent.remove(30);}
				BankRun upgraded=new BankRun(old.getCharacter(),old.getRunId(),old.getPreviousRunId(),old.getStartedAt(),audit.getId(),Math.incrementExact(old.getSequence()),old.getInitialBank(),old.getPhysical(),old.getBalances(),old.isAnchored(),old.getLegacyDocument(),old.getMigrationArchive(),recent,old.getHostSummary(),old.getFinancial(),old.isActive()?GeState.empty(archived):GeState.empty());
				commit(d,old,upgraded);return Optional.of(upgraded);
			}
			if(version==5)
			{
				BankRun old=codec.decode(text,character);verify(d,old,true);String archived=archive(d);Instant at=Instant.now();
				BankAudit audit=new BankAudit(UUID.randomUUID(),UUID.randomUUID(),at,BankAudit.Kind.FINANCIAL_BASELINE,Collections.emptyMap(),Collections.emptyMap(),"M4 files archived exactly. Estimated gains begin at this imported bank baseline; earlier income is not reconstructed.");
				List<BankAudit> recent=new ArrayList<>(old.getRecent());recent.add(0,audit);if(recent.size()>30){recent.remove(30);}
				BankRun migrated=new BankRun(old.getCharacter(),old.getRunId(),old.getPreviousRunId(),old.getStartedAt(),audit.getId(),Math.incrementExact(old.getSequence()),old.getInitialBank(),old.getPhysical(),old.getBalances(),old.isAnchored(),old.getLegacyDocument(),old.getMigrationArchive(),recent,old.getHostSummary(),FinancialState.baseline(old.getBalances(),at,true).archived(archived));
				commit(d,old,migrated);return Optional.of(migrated);
			}
			// Decode and verify the old chain before making a byte-for-byte archive.
			RunRecordCodec.DecodedSave decoded = legacy.decodeSave(text, character);
			RunRecord old = decoded.record;
			if (version >= 3) { new ItemJournalStore(legacy).verifyChain(d, old, version); }
			String archive = archive(d);
			Map<Integer, Long> balances = new TreeMap<>();
			Set<Integer> ids = new TreeSet<>(old.getOwnership().getItems().keySet());
			if (old.getItemProgress() != null) { ids.addAll(old.getItemProgress().getLegacyDeficits().getItems().keySet()); }
			for (int id : ids)
			{
				long deficit = old.getItemProgress() == null ? 0 : old.getItemProgress().getLegacyDeficits().quantity(id);
				balances.put(id, Math.subtractExact(old.getOwnership().at(id, VirtualLocation.BANK), deficit));
			}
			boolean active = old.getState().getRunId() != null;
			BankAudit audit = new BankAudit(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), BankAudit.Kind.MIGRATION, Collections.emptyMap(), Collections.emptyMap(), "Previous checkpoint and journals archived. Confirm one complete bank observation before automatic comparisons.");
			BankRun migrated = new BankRun(character, old.getState().getRunId(), old.getPreviousRunId(), old.getState().getStartedAt(), audit.getId(), 1,
				active ? old.getSnapshot().at(SnapshotLocation.BANK) : null, Collections.emptyMap(), active ? balances : Collections.emptyMap(), false, text, archive, Collections.singletonList(audit), null,
				FinancialState.baseline(active?balances:Collections.emptyMap(),audit.getAt(),true).archived(archive));
			commit(d, null, migrated); return Optional.of(migrated);
		});
	}
	/** CAS prevents a stale editor or another client from overwriting more recent progress. */
	public BankRun save(Filepath root, BankRun expected, BankRun next) throws IOException
	{
		return locked(root, next.getCharacter(), d ->
		{
			Filepath current = d.joinSegment("current.json");
			BankRun actual = current.exists() ? codec.decode(BankStorageFiles.readText(current), next.getCharacter()) : null;
			if (next.equals(actual)) { verify(d, actual, false); return actual; }
			if (!Objects.equals(expected, actual)) { throw new SaveException("Bank progress changed. Reload before applying this change."); }
			if (actual != null) { verify(d, actual, false); }
			else
			{
				try (Stream<Filepath> files = d.walk(1)) { if (files.anyMatch(f -> f.getFileName().endsWith(".json"))) { throw new SaveException("Recovery files exist. Restore the prior checkpoint before starting a run."); } }
			}
			validate(expected, next);
			if (expected != null && !next.isActive()) { archive(d); }
			commit(d, expected, next); return next;
		});
	}
	private void validate(BankRun before, BankRun after) throws SaveException
	{
		BankAudit audit = after.getRecent().get(0);
		if (before == null) { if (after.getSequence() != 1 || audit.getKind() != BankAudit.Kind.START) { throw new SaveException("Invalid initial bank transaction."); } validateStart(after); return; }
		if (!before.getCharacter().equals(after.getCharacter()) || after.getSequence() != Math.incrementExact(before.getSequence())) { throw new SaveException("Bank transaction sequence changed."); }
		if (audit.getKind() == BankAudit.Kind.START && !before.isActive() && after.isActive()) { validateStart(after); if (!Objects.equals(before.getPreviousRunId(), after.getPreviousRunId())) { throw new SaveException("Previous run identity changed."); } return; }
		if (audit.getKind() == BankAudit.Kind.RESET) { if (!before.isActive() || after.isActive() || !before.getRunId().equals(after.getPreviousRunId())) { throw new SaveException("Invalid reset."); } return; }
		if (!Objects.equals(before.getRunId(), after.getRunId()) || !Objects.equals(before.getStartedAt(), after.getStartedAt())
			|| !Objects.equals(before.getInitialBank(), after.getInitialBank()) || !Objects.equals(before.getLegacyDocument(), after.getLegacyDocument())
			|| !Objects.equals(before.getMigrationArchive(), after.getMigrationArchive())) { throw new SaveException("Immutable run baseline changed."); }
		if (audit.getKind() == BankAudit.Kind.ANCHOR)
		{ if (before.isAnchored() || !after.isAnchored() || before.getLegacyDocument() == null) { throw new SaveException("Invalid migration anchor transition."); } }
		else if(audit.getKind()==BankAudit.Kind.EXCHANGE||audit.getKind()==BankAudit.Kind.BUYBACK)
		{if(!before.isActive()||before.isAnchored()!=after.isAnchored()||!audit.getPhysicalChanges().isEmpty()||audit.getKind()==BankAudit.Kind.EXCHANGE&&!audit.getBalanceChanges().isEmpty()){throw new SaveException("Invalid observation/purchase bank change.");}}
		else if (!before.isActive() || !before.isAnchored() || !after.isAnchored()
			|| (audit.getKind() != BankAudit.Kind.AUTOMATIC && audit.getKind() != BankAudit.Kind.SESSION_CHECK && audit.getKind() != BankAudit.Kind.CORRECTION && audit.getKind()!=BankAudit.Kind.EXPENSE && audit.getKind()!=BankAudit.Kind.POUCH && audit.getKind()!=BankAudit.Kind.STORAGE && audit.getKind()!=BankAudit.Kind.STORAGE_REVIEW))
		{ throw new SaveException("Invalid bank accounting transition."); }
		if (!BankEngine.differences(before.getBalances(), after.getBalances()).equals(audit.getBalanceChanges())) { throw new SaveException("Bank balance audit does not match checkpoint."); }
		if (audit.getKind() != BankAudit.Kind.ANCHOR && !BankEngine.differences(before.getPhysical(), after.getPhysical()).equals(audit.getPhysicalChanges())) { throw new SaveException("Bank observation audit does not match checkpoint."); }
		if(audit.getKind()==BankAudit.Kind.BUYBACK)
		{
			Map<Integer,Long> expected=new TreeMap<>();audit.getBuyback().getLegacyRunes().forEach((id,n)->expected.put(id,Math.negateExact(n)));
			if(!expected.equals(audit.getBalanceChanges())){throw new SaveException("Legacy pouch adjustments do not match their purchase.");}
			if(audit.getBuyback().getPouchItemId()>0)
			{
				long count=0;for(int id:com.fromscratch.buyback.BuybackCatalog.variants(audit.getBuyback().getCatalogId()).keySet()){count=Math.addExact(count,before.physical(id));}
				if(count!=1||before.physical(audit.getBuyback().getPouchItemId())!=1){throw new SaveException("Purchased pouch is ambiguous or absent.");}
			}
		}
		Map<Integer,Long> pouchTransfers=com.fromscratch.pouch.PouchEngine.transfers(before.getBuybacks().getPouch(),audit.getPhysicalChanges());
		Map<Integer,Long> bankProgress=new TreeMap<>(audit.getPhysicalChanges());pouchTransfers.forEach((id,n)->bankProgress.put(id,Math.subtractExact(bankProgress.get(id),n)));bankProgress.values().removeIf(n->n==0);
		Map<Integer,Long> ordinary=new TreeMap<>(before.getBalances());bankProgress.forEach((id,q)->ordinary.put(id,Math.addExact(before.balance(id),q)));
		com.fromscratch.potion.PotionEngine.Result storage=com.fromscratch.potion.PotionEngine.apply(before,after.getPhysical(),ordinary,audit.getPotion(),audit.getSession());
		com.fromscratch.potion.PotionState expectedStorage=audit.getKind()==BankAudit.Kind.CORRECTION?before.getPotions().corrected(audit.getBalanceChanges().keySet()):storage.getState();
		if(!expectedStorage.equals(after.getPotions())||audit.getPotion()!=null&&!storage.getExclusions().equals(audit.getPotion().getExclusions())){throw new SaveException("Potion Storage checkpoint does not match its observation evidence.");}
		if(before.getPotions().getObservedAt()!=null&&!before.getPotions().getInitial().equals(after.getPotions().getInitial())){throw new SaveException("Initial Potion Storage observation changed.");}
		if ((audit.getKind() == BankAudit.Kind.AUTOMATIC || audit.getKind() == BankAudit.Kind.SESSION_CHECK || audit.getKind()==BankAudit.Kind.STORAGE) && (!audit.getBalanceChanges().equals(BankEngine.differences(before.getBalances(),storage.getBank()))||!audit.getPouchTransfers().equals(pouchTransfers))) { throw new SaveException("Bank progress does not match its storage transfer evidence."); }
		if((audit.getKind()==BankAudit.Kind.STORAGE||audit.getKind()==BankAudit.Kind.STORAGE_REVIEW)&&(!audit.getPhysicalChanges().isEmpty()||audit.getPotion()==null)){throw new SaveException("Invalid Potion Storage operation.");}
		if(audit.getKind()==BankAudit.Kind.STORAGE_REVIEW&&(audit.getPotion().getReview()==null||!audit.getBalanceChanges().isEmpty())){throw new SaveException("Invalid storage review.");}
		if(audit.getKind()==BankAudit.Kind.POUCH)
		{
			boolean present=com.fromscratch.buyback.BuybackCatalog.variants("rune_pouch_family").keySet().stream().anyMatch(id->before.physical(id)>0);
			Map<Integer,Long> expected=audit.getPouch().getKind()==com.fromscratch.pouch.PouchAction.Kind.RESTORE_EARNED?audit.getPouch().getQuantities():Collections.emptyMap();
			if(!present||!audit.getPhysicalChanges().isEmpty()||!audit.getBalanceChanges().equals(expected)){throw new SaveException("Invalid pouch review bank adjustment.");}
		}
		if (audit.getKind() == BankAudit.Kind.CORRECTION && !audit.getPhysicalChanges().isEmpty()) { throw new SaveException("A correction cannot change the physical bank anchor."); }
		if(audit.getKind()==BankAudit.Kind.EXPENSE&&(!audit.getPhysicalChanges().isEmpty()||!audit.getBalanceChanges().isEmpty())){throw new SaveException("Expenses cannot change bank holdings.");}
		if(!FinancialEngine.advance(before.getFinancial(),after.getBalances(),audit).equals(after.getFinancial())){throw new SaveException("Financial checkpoint does not match durable event evidence.");}
		GeState exchange=before.getExchange();GeUpdate evidence=GeUpdate.empty();
		if(audit.getKind()==BankAudit.Kind.EXCHANGE)
		{
			if(audit.getExchange().getObservation()==null){throw new SaveException("Missing GE observation evidence.");}
			GeEngine.Result result=GeEngine.observe(exchange,audit.getExchange().getObservation(),before.getRunId());exchange=result.getState();evidence=result.getEvidence();
		}
		else if(audit.getKind()==BankAudit.Kind.AUTOMATIC){GeEngine.Result result=GeEngine.bank(exchange,audit.getId(),audit.getBalanceChanges(),audit.getAt());exchange=result.getState();evidence=result.getEvidence();}
		if(!exchange.equals(after.getExchange())||!evidence.equals(audit.getExchange())){throw new SaveException("GE checkpoint does not match its durable observations/attributions.");}
		com.fromscratch.buyback.BuybackState buybacks=audit.getBuyback()==null?com.fromscratch.buyback.BuybackEngine.progress(before.getBuybacks(),after.getBalances()):com.fromscratch.buyback.BuybackEngine.purchase(before,audit.getBuyback());
		com.fromscratch.pouch.PouchState pouch=before.getBuybacks().getPouch();
		if(audit.getKind()==BankAudit.Kind.AUTOMATIC){pouch=com.fromscratch.pouch.PouchEngine.bank(pouch,audit.getPhysicalChanges());}
		else if(audit.getKind()==BankAudit.Kind.POUCH){pouch=com.fromscratch.pouch.PouchEngine.apply(pouch,audit.getPouch(),audit.getId());}
		else if(audit.getBuyback()!=null&&audit.getBuyback().getCatalogId().equals("rune_pouch_family")&&audit.getBuyback().getPouchItemId()>0){pouch=com.fromscratch.pouch.PouchEngine.purchased(pouch,audit.getBuyback().getLegacyRunes());}
		buybacks=buybacks.withPouch(pouch);
		if(!buybacks.equals(after.getBuybacks())){throw new SaveException("Buyback payment and entitlement evidence do not match.");}
	}
	/** Read only enough immutable segments for one page; never run from the EDT/client thread. */
	public Long peak(Filepath root,BankRun run,Long value) throws IOException {return new PeakProgressRepository(codec).checkpoint(root,run,value);}
	public GeHistory.Page exchangeHistory(Filepath root,BankRun run,GeHistory.Cursor cursor,boolean buy) throws IOException
	{
		return locked(root,run.getCharacter(),d ->
		{
			UUID head=cursor==null?run.getRevision():cursor.getRevision();int examined=0;
			List<GeTrade> rows=new ArrayList<>(cursor==null?Collections.emptyList():cursor.getPending());
			Set<UUID> seen=new HashSet<>(cursor==null?Collections.emptySet():cursor.getSeen());
			// One row of lookahead makes pagination count finalized offers, not empty audit chunks.
			while(head!=null&&rows.size()<26&&examined++<64)
			{
				JsonObject batch=codec.payload(BankStorageFiles.readText(d.joinSegment("bank-ledger-"+head+".json")),1);
				if(!run.getCharacter().equals(BankRunCodec.str(batch,"character"))||!run.getRunId().toString().equals(BankRunCodec.nullable(batch,"run"))){head=null;break;}
				BankAudit audit=codec.readAudit(batch.getAsJsonObject("audit"));if(!head.equals(audit.getId())){throw new SaveException("GE history identity mismatch.");}
				GeTrade trade=GeHistory.finalized(audit.getExchange());if(trade!=null&&trade.isBuy()==buy&&seen.add(trade.getGeneration())){rows.add(trade);}
				String previous=BankRunCodec.nullable(batch,"previous");head=previous==null?null:UUID.fromString(previous);
			}
			if(rows.size()==26){return new GeHistory.Page(rows.subList(0,25),new GeHistory.Cursor(head,seen,rows.subList(25,26)),"");}
			if(head!=null){return new GeHistory.Page(Collections.emptyList(),new GeHistory.Cursor(head,seen,rows),"",true);}
			return new GeHistory.Page(rows,null,rows.isEmpty()?"No completed Grand Exchange trades recorded.":"");
		});
	}
	/** Read only enough immutable segments for one bank page; never run from the EDT/client thread. */
	public BankHistory.Page history(Filepath root, BankRun run, BankHistory.Cursor cursor, boolean manual) throws IOException
	{
		return locked(root, run.getCharacter(), d ->
		{
			UUID head = cursor == null ? run.getRevision() : cursor.getRevision(); int offset = cursor == null ? 0 : cursor.getOffset();
			if (offset < 0) { throw new SaveException("Invalid history page."); }
			List<BankHistory.Row> rows = new ArrayList<>(); int examined = 0;
			while (head != null && rows.size() < 25 && examined++ < 64)
			{
				JsonObject batch = codec.payload(BankStorageFiles.readText(d.joinSegment("bank-ledger-" + head + ".json")), 1);
				if (!run.getCharacter().equals(BankRunCodec.str(batch, "character")) || !Objects.equals(run.getRunId().toString(), BankRunCodec.nullable(batch, "run"))) { head = null; break; }
				BankAudit audit = codec.readAudit(batch.getAsJsonObject("audit"));
				if (!head.equals(audit.getId())) { throw new SaveException("Bank history page failed validation."); }
					boolean legacyPouch=audit.getKind()==BankAudit.Kind.BUYBACK&&!audit.getBuyback().getLegacyRunes().isEmpty();
					boolean include = manual ? audit.getKind() == BankAudit.Kind.CORRECTION || audit.getKind() == BankAudit.Kind.ANCHOR : audit.getKind() == BankAudit.Kind.AUTOMATIC||legacyPouch;
				if (include)
				{
					int index = 0;
					for (Map.Entry<Integer, Long> entry : audit.getBalanceChanges().entrySet())
					{
						if (index++ < offset) { continue; }
							rows.add(new BankHistory.Row(audit.getAt(), entry.getKey(), "", entry.getValue(),legacyPouch?(audit.getBuyback().getCatalogId().equals("looting_bag_family")?"Automatic — Legacy Looting Bag":"Automatic — Legacy Rune Pouch"):manual?"Manual":"Automatic",legacyPouch?audit.getId():null)); offset = index;
						if (rows.size() == 25) { return new BankHistory.Page(rows, new BankHistory.Cursor(head, offset), ""); }
					}
				}
				if(manual&&audit.getKind()==BankAudit.Kind.STORAGE_REVIEW)
				{
					String previous=BankRunCodec.nullable(batch,"previous");
					if(previous==null){throw new SaveException("Storage correction predecessor is missing.");}
					BankRun before=codec.decode(BankStorageFiles.readText(d.joinSegment("bank-checkpoint-"+previous+".json")),run.getCharacter());int index=0;
					for(com.fromscratch.potion.PotionType type:audit.getPotion().getObservation().getTypes().values())
					{
						long old=before.getPotions().balance(type.getId()),value=audit.getPotion().getReview().getOrDefault(type.getId(),0L);if(value==old&&before.getPotions().isReviewed()){continue;}
						if(index++<offset){continue;}
						rows.add(new BankHistory.Row(audit.getAt(),type.displayId(),type.getName()+" ("+type.unit()+")",Math.subtractExact(value,old),"Manual Potion Storage: "+old+" to "+value+" "+type.unit(),null));offset=index;
						if(rows.size()==25){return new BankHistory.Page(rows,new BankHistory.Cursor(head,offset),"");}
					}
				}
				String previous = BankRunCodec.nullable(batch, "previous"); head = previous == null ? null : UUID.fromString(previous); offset = 0;
			}
			return new BankHistory.Page(rows, head == null ? null : new BankHistory.Cursor(head, offset), rows.isEmpty() ? "No matching bank changes on this page." : "");
		});
	}
	/** Filter before pagination, so an item's older movements are not lost behind unrelated bank rows. */
	public ItemHistory.Page itemHistory(Filepath root,BankRun run,BankHistory.Cursor cursor,int itemId) throws IOException
	{
		if(itemId<0) { throw new SaveException("Choose an item first."); }
		return locked(root,run.getCharacter(),d ->
		{
			UUID head=cursor==null?run.getRevision():cursor.getRevision();int offset=cursor==null?0:cursor.getOffset();
			if(offset<0) { throw new SaveException("Invalid item history page."); }
			List<ItemHistory.Row> rows=new ArrayList<>();int examined=0;
			while(head!=null&&examined++<64)
			{
				JsonObject batch=codec.payload(BankStorageFiles.readText(d.joinSegment("bank-ledger-"+head+".json")),1);
				if(!run.getCharacter().equals(BankRunCodec.str(batch,"character"))||!Objects.equals(run.getRunId().toString(),BankRunCodec.nullable(batch,"run"))) { head=null;break; }
				BankAudit audit=codec.readAudit(batch.getAsJsonObject("audit"));
				if(!head.equals(audit.getId())) { throw new SaveException("Item history failed validation."); }
				List<ItemHistory.Row> matching=ItemHistory.rows(audit,itemId);
				for(int i=offset;i<matching.size();i++)
				{
					rows.add(matching.get(i));
					if(rows.size()==25) { return new ItemHistory.Page(rows,new BankHistory.Cursor(head,i+1),""); }
				}
				String previous=BankRunCodec.nullable(batch,"previous");head=previous==null?null:UUID.fromString(previous);offset=0;
			}
			return new ItemHistory.Page(rows,head==null?null:new BankHistory.Cursor(head,offset),rows.isEmpty()?"No recorded changes on this page.":"");
		});
	}
	private static void validateStart(BankRun run) throws SaveException
	{
		com.fromscratch.potion.PotionState storage=run.getPotions();
		if(storage.getObservedAt()!=null&&(!storage.isReviewed()||!storage.getBalances().isEmpty()||!storage.getInitial().equals(storage.getPhysical())||!storage.getRemainder().isEmpty()||!storage.getBankOut().isEmpty()||!storage.getStoreOut().isEmpty())){throw new SaveException("New Potion Storage must begin with a verified legacy baseline and zero virtual doses.");}
		if (!run.isActive() || !run.isAnchored() || !run.getBalances().isEmpty() || !run.getInitialBank().getQuantities().equals(run.getPhysical())
			|| !run.getRecent().get(0).getBalanceChanges().isEmpty() || !run.getRecent().get(0).getPhysicalChanges().isEmpty()
			|| !run.getRecent().get(0).getValuations().isEmpty() || !FinancialState.baseline(Collections.emptyMap(),run.getStartedAt(),false).equals(run.getFinancial()) || !run.getExchange().equals(GeState.empty()) || !run.getBuybacks().equals(com.fromscratch.buyback.BuybackState.empty()))
		{ throw new SaveException("A new run must start from its complete bank with zero progress."); }
	}
	private void commit(Filepath d, BankRun before, BankRun after) throws IOException
	{
		String encoded = codec.encode(after);
		JsonObject batch = new JsonObject(); batch.addProperty("character", after.getCharacter()); batch.addProperty("run", after.getRunId() == null ? null : after.getRunId().toString());
		batch.addProperty("previous", before == null ? null : before.getRevision().toString()); batch.addProperty("sequence", after.getSequence());
		batch.addProperty("result", SaveIntegrity.sha256(encoded)); batch.add("audit", codec.audit(after.getRecent().get(0)));
		writeVerified(d.joinSegment("bank-ledger-" + after.getRevision() + ".json"), codec.envelope(batch, 1));
		writeVerified(d.joinSegment("bank-checkpoint-" + after.getRevision() + ".json"), encoded);
		Filepath pending = d.joinSegment("bank-pending-" + after.getRevision() + ".json"); writeVerified(pending, encoded);
		if(before!=null)
		{
			Filepath backup=d.joinSegment("previous-pending-"+after.getRevision()+".json");writeVerified(backup,BankStorageFiles.readText(d.joinSegment("current.json")));
			publish(backup,d.joinSegment("previous.json"));
		}
		publication.replace(pending, d.joinSegment("current.json"));
		if (!codec.decode(BankStorageFiles.readText(d.joinSegment("current.json")), after.getCharacter()).equals(after)) { throw new SaveException("Bank save verification failed; recovery checkpoints were retained."); }
	}
	void verify(Filepath d, BankRun run, boolean fullChain) throws IOException
	{
		try
		{
			UUID head = run.getRevision(); long seq = run.getSequence(); Set<UUID> seen = new HashSet<>();
			while (head != null)
			{
				JsonObject batch = codec.payload(BankStorageFiles.readText(d.joinSegment("bank-ledger-" + head + ".json")), 1);
				BankRunCodec.keys(batch, "character", "run", "previous", "sequence", "result", "audit");
				BankAudit audit = codec.readAudit(batch.getAsJsonObject("audit"));
				if (!head.equals(audit.getId()) || !seen.add(head) || seq != BankRunCodec.number(batch.get("sequence")) || !run.getCharacter().equals(BankRunCodec.str(batch, "character"))) { throw new IllegalArgumentException("Broken bank journal chain"); }
				// Hash the original published bytes, not a future Gson version's re-encoding.
				if (head.equals(run.getRevision()) && (!audit.equals(run.getRecent().get(0)) || !SaveIntegrity.sha256(BankStorageFiles.readText(d.joinSegment("current.json"))).equals(BankRunCodec.str(batch, "result")))) { throw new IllegalArgumentException("Bank journal head mismatch"); }
				if (head.equals(run.getRevision()) && !Objects.equals(run.getRunId() == null ? null : run.getRunId().toString(), BankRunCodec.nullable(batch, "run"))) { throw new IllegalArgumentException("Bank journal run identity mismatch"); }
				if (!fullChain) { return; }
				String previous = BankRunCodec.nullable(batch, "previous"); head = previous == null ? null : UUID.fromString(previous); seq--;
			}
			if (seq != 0 || !seen.containsAll(run.getRecent().stream().map(BankAudit::getId).collect(Collectors.toSet())) || (run.getHostSummary() != null && !seen.contains(run.getHostSummary().getId()))) { throw new IllegalArgumentException("Missing bank journal history"); }
			if (run.getMigrationArchive() != null) { verifyArchive(d.joinSegment(run.getMigrationArchive())); }
			if(run.getFinancial().getImportArchive()!=null){verifyArchive(d.joinSegment(run.getFinancial().getImportArchive()));}
			if(run.getExchange().getArchive()!=null){verifyArchive(d.joinSegment(run.getExchange().getArchive()));}
			if(run.getBuybacks().getArchive()!=null){verifyArchive(d.joinSegment(run.getBuybacks().getArchive()));}
			if(run.getBuybacks().getPouch().getArchive()!=null){verifyArchive(d.joinSegment(run.getBuybacks().getPouch().getArchive()));}
		}
		catch (RuntimeException ex) { throw new SaveException("Bank history or migration archive is incomplete or corrupt. Files were preserved; restore the matching checkpoint and journals."); }
	}
	/** Every pre-existing JSON checkpoint/ledger is copied, forced and byte-hash verified before current changes. */
	private String archive(Filepath d) throws IOException
	{
		String name = "bank-migration-" + UUID.randomUUID(); Filepath archive = d.joinSegment(name); archive.createDirectories(); JsonObject files = new JsonObject();
		List<Filepath> originals;
		try (Stream<Filepath> walk = d.walk(1)) { originals = walk.filter(f -> f.getFileName().endsWith(".json")).collect(Collectors.toList()); }
		// Independent forced copies overlap slow per-file flushes. Only the storage worker
		// publishes the manifest/current; it waits for every copy, including on failure.
		String[] digests = new String[originals.size()]; Thread owner = Thread.currentThread();
		int workers = Math.min(4, originals.size());
		if (workers > 0)
		{
			ExecutorService copies = Executors.newFixedThreadPool(workers, task -> { Thread t = new Thread(task, "fromscratch-archive-copy"); t.setDaemon(true); return t; });
			try
			{
				List<CompletableFuture<Void>> pending = new ArrayList<>();
				for (int worker = 0; worker < workers; worker++)
				{
					int start = worker;
					pending.add(CompletableFuture.runAsync(() ->
					{
						for (int i = start; i < originals.size(); i += workers)
						{
							Filepath source = originals.get(i);
							try { digests[i] = copyVerified(source, archive.joinSegment(source.getFileName()), owner); }
							catch (IOException ex) { throw new CompletionException(ex); }
						}
					}, copies));
				}
				try { CompletableFuture.allOf(pending.toArray(new CompletableFuture<?>[0])).join(); }
				catch (CompletionException ex) { if (ex.getCause() instanceof IOException) { throw (IOException) ex.getCause(); } throw ex; }
			}
			finally { copies.shutdownNow(); }
		}
		for (int i = 0; i < originals.size(); i++) { files.addProperty(originals.get(i).getFileName(), digests[i]); }
		// Every copy has already been forced and byte-verified below. The forced manifest
		// publishes that evidence; reopening every newly-created copy again adds no proof.
		writeVerified(archive.joinSegment("manifest.json"), codec.envelope(files, 1));return name;
	}
	/** Stream/hash the source once, force the destination, then verify its exact-byte readback. */
	private static String copyVerified(Filepath source,Filepath copy,Thread owner) throws IOException
	{
		checkArchiveThread(owner);
		MessageDigest hash=sha256();ByteBuffer buffer=ByteBuffer.allocate(8192);
		try(FileChannel input=source.openFileChannel(StandardOpenOption.READ);FileChannel output=copy.openFileChannel(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,StandardOpenOption.READ))
		{
			int count;while((count=input.read(buffer))!=-1)
			{
				checkArchiveThread(owner);
				hash.update(buffer.array(),0,count);buffer.flip();while(buffer.hasRemaining()){output.write(buffer);}buffer.clear();
			}
			checkArchiveThread(owner);output.force(true);
			String expected=hex(hash.digest());output.position(0);buffer.clear();MessageDigest readback=sha256();
			while(output.read(buffer)!=-1){checkArchiveThread(owner);buffer.flip();readback.update(buffer);buffer.clear();}
			if(!expected.equals(hex(readback.digest()))){throw new SaveException("Archive verification failed. Original progression was not changed.");}return expected;
		}
	}
	private static void checkArchiveThread(Thread owner) throws SaveException
	{ if (owner.isInterrupted() || Thread.currentThread().isInterrupted()) { throw new SaveException("Reset archive interrupted. Current progression was preserved."); } }
	private void verifyArchive(Filepath archive) throws IOException
	{
		JsonObject files = codec.payload(BankStorageFiles.readText(archive.joinSegment("manifest.json")), 1);
		if (!files.has("current.json")) { throw new SaveException("Migration archive lacks the original checkpoint."); }
		for (Map.Entry<String, JsonElement> f : files.entrySet()) { if (!f.getKey().matches("[a-zA-Z0-9-]+\\.json") || !digest(archive.joinSegment(f.getKey())).equals(f.getValue().getAsString())) { throw new SaveException("Migration archive is incomplete or corrupt."); } }
	}
	private static String digest(Filepath file) throws IOException
	{
		MessageDigest hash=sha256();ByteBuffer buffer=ByteBuffer.allocate(8192);
		try(FileChannel channel=file.openFileChannel(StandardOpenOption.READ)){while(channel.read(buffer)!=-1){buffer.flip();hash.update(buffer);buffer.clear();}}
		return hex(hash.digest());
	}
	private static MessageDigest sha256()
	{try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
	private static String hex(byte[] bytes)
	{char[] result=new char[bytes.length*2];char[] digits="0123456789abcdef".toCharArray();for(int i=0;i<bytes.length;i++){result[i*2]=digits[(bytes[i]&255)>>>4];result[i*2+1]=digits[bytes[i]&15];}return new String(result);}
	private static void writeVerified(Filepath file, String text) throws IOException
	{
		if (!file.exists()) { BankStorageFiles.writeNew(file, text); }
		if (!BankStorageFiles.readText(file).equals(text)) { throw new SaveException("Recovery file conflicts with this transaction. Existing files were preserved."); }
	}
	private static void publish(Filepath pending, Filepath current) throws IOException
	{
		try { pending.moveTo(current, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
		catch (AtomicMoveNotSupportedException ex) { throw new SaveException("This filesystem cannot replace saves atomically. Current progress and recovery files were preserved."); }
	}
	private <T> T locked(Filepath root, String character, Task<T> task) throws IOException
	{
		if (character == null || !character.matches("[a-f0-9]{64}")) { throw new SaveException("Unverified character identity; storage is blocked."); }
		try
		{
			Filepath d = root.joinSegment("characters").joinSegment(character); d.createDirectories();
			try (FileChannel c = d.joinSegment("run.lock").openFileChannel(StandardOpenOption.CREATE, StandardOpenOption.WRITE); FileLock lock = c.tryLock())
			{ if (lock == null) { throw new SaveException("Another client is using this run. Close it and retry."); } return task.run(ProgressBackup.directory(d,codec)); }
		}
		catch (SaveException ex) { throw ex; }
		catch (OverlappingFileLockException ex) { throw new SaveException("Run storage is busy. Retry after the other operation finishes."); }
		catch (IOException | RuntimeException ex) { throw new SaveException("Bank data could not be verified or saved. Recovery files were preserved; check storage and retry."); }
	}
	@FunctionalInterface private interface Task<T> { T run(Filepath d) throws IOException; }
	@FunctionalInterface interface Publication { void replace(Filepath pending, Filepath current) throws IOException; }
}
