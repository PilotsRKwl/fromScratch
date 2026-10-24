package com.fromscratch.persistence;

import com.fromscratch.bank.BankRun;
import com.fromscratch.wealth.PriceQuote;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;

/** One tiny run-scoped maximum, never a second economic ledger. All calls run on the storage worker. */
public final class PeakProgressRepository
{
	private final BankRunCodec codec;
	public PeakProgressRepository(BankRunCodec codec){this.codec=codec;}
	/** Paid equipment allowances have no market value. Raw signed B already includes coins once;
	 * subtracting committed GP for net worth and adding it as permanent spending cancel exactly. */
	public static Long candidate(BankRun run,Map<Integer,PriceQuote> prices)
	{
		BigInteger sum=BigInteger.valueOf(run.getFinancial().getExpenseGp());
		Map<Integer,Long> quantities=new TreeMap<>(run.getBalances());
		Set<Integer> pooled=com.fromscratch.potion.PotionWealth.pooledItems(run);pooled.forEach(quantities::remove);
		// A bag opening/closing is one physical item, not two oppositely priced variants.
		long bag=0;for(int id:com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").keySet()){bag=Math.addExact(bag,quantities.getOrDefault(id,0L));quantities.remove(id);}if(bag!=0){quantities.put(ItemID.LOOTING_BAG,bag);}
		for(Map.Entry<Integer,Long> e:quantities.entrySet())
		{
			if(e.getValue()==0){continue;}
			PriceQuote p=e.getKey()==ItemID.COINS?PriceQuote.coins(Instant.now()):prices.get(e.getKey());
			if(p==null||!p.verifiedNow()){return null;}
			sum=sum.add(BigInteger.valueOf(e.getValue()).multiply(BigInteger.valueOf(p.getUnitGp())));
		}
		com.fromscratch.potion.PotionWealth.Value potions=com.fromscratch.potion.PotionWealth.calculate(run,prices);if(potions.getMissing()>0||potions.getStale()>0){return null;}sum=sum.add(potions.getGp());
		try{return Math.max(0,sum.longValueExact());}catch(ArithmeticException ex){return null;}
	}
	public Long checkpoint(Filepath root,BankRun run,Long candidate) throws IOException
	{
		Filepath base=root.join("characters",run.getCharacter());
		try(FileChannel channel=base.joinSegment("run.lock").openFileChannel(StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock())
		{
			if(lock==null){throw new SaveException("Progress is being saved.");}
			Filepath d=ProgressBackup.directory(base,codec),file=d.joinSegment(file(run));
			Long previous=read(d,run);
			// Never checkpoint an obsolete client/worker result after another client or restore changed the head.
			BankRun head=codec.decode(BankStorageFiles.readText(d.joinSegment("current.json")),run.getCharacter());
			if(!head.getRevision().equals(run.getRevision())||candidate==null||previous!=null&&candidate<=previous){return previous;}
			JsonObject p=new JsonObject();p.addProperty("character",run.getCharacter());p.addProperty("run",run.getRunId().toString());p.addProperty("gp",candidate);
			p.addProperty("from",file.exists()?BankRunCodec.str(codec.payload(BankStorageFiles.readText(file),1),"from"):Instant.now().toString());
			Filepath pending=d.joinSegment("peak-"+run.getRunId()+"-pending-"+UUID.randomUUID()+".json");BankStorageFiles.writeNew(pending,codec.envelope(p,1));
			if(file.exists())
			{
				Filepath backup=d.joinSegment("peak-"+run.getRunId()+"-backup-pending-"+UUID.randomUUID()+".json");BankStorageFiles.writeNew(backup,BankStorageFiles.readText(file));
				backup.moveTo(d.joinSegment("peak-"+run.getRunId()+"-backup.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
			}
			pending.moveTo(file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return read(d,run);
		}
		catch(OverlappingFileLockException ex){throw new SaveException("Progress is being saved.");}
	}
	Long read(Filepath directory,BankRun run) throws IOException
	{
		Filepath file=directory.joinSegment(file(run));
		if(!file.exists())
		{
			try(java.util.stream.Stream<Filepath> files=directory.walk(1)){if(files.anyMatch(f->f.getFileName().startsWith("peak-"+run.getRunId()+"-"))){throw new SaveException("Peak checkpoint is missing; recovery files were preserved.");}}
			return null;
		}
		try
		{
			JsonObject p=codec.payload(BankStorageFiles.readText(file),1);BankRunCodec.keys(p,"character","run","gp","from");
			long gp=BankRunCodec.number(p.get("gp"));Instant.parse(BankRunCodec.str(p,"from"));
			if(gp<0||!run.getCharacter().equals(BankRunCodec.str(p,"character"))||!run.getRunId().toString().equals(BankRunCodec.str(p,"run"))){throw new IllegalArgumentException();}return gp;
		}
		catch(RuntimeException ex){throw new SaveException("Peak Progress Value could not be verified; its files were preserved.");}
	}
	private static String file(BankRun run){return "peak-"+run.getRunId()+".json";}
}
