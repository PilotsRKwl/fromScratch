package com.fromscratch.bank;

import com.fromscratch.persistence.BankRepository;
import com.fromscratch.overlay.BankOverlayState;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import javax.inject.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import com.fromscratch.wealth.*;
import com.fromscratch.ge.*;

/** Client-thread coordinator with serialized background storage and lifecycle/identity guarded callbacks. */
@Singleton
public final class BankController implements BankActions
{
	private final BankSource source;
	private final BankRepository repository;
	private final Consumer<Runnable> dispatch;
	private final Consumer<BankView> display;
	private final AtomicLong epoch = new AtomicLong();
	private volatile boolean enabled;
	private ExecutorService worker;
	private DirectoryProvider directory;
	private String character;
	private long context;
	private BankRun run;
	private BankObservation observation;
	private com.fromscratch.potion.PotionObservation potionObservation=com.fromscratch.potion.PotionObservation.unknown();
	private com.fromscratch.potion.PotionCapture setupCapture;
	private UUID setupAttempt=UUID.randomUUID();
	private BankSetup setup=BankSetup.begin(null,setupAttempt);
	private boolean bankContinuous;
	private Consumer<List<Overdrafts.Warning>> overdraftListener=value->{};
	public void overdrafts(Consumer<List<Overdrafts.Warning>> listener){overdraftListener=listener;}
	private boolean busy;
	private boolean error;
	private String message = "Log in to load this character's local progress.";
	private String notice;
	private final Map<Integer, String> names = new TreeMap<>();
	private BankView last;
	private final Deque<Pending> pending = new ArrayDeque<>();
	private final Map<Integer,GeObservation> exchangeBaseline=new TreeMap<>();
	private final Map<Integer,GeObservation> latestOffers=new TreeMap<>();
	private static final class Pending
	{
		final BankObservation bank;final GeObservation offer;final com.fromscratch.potion.PotionObservation potions;final boolean continuous;
		Pending(BankObservation bank,GeObservation offer){this(bank,offer,null,false);}
		Pending(BankObservation bank,GeObservation offer,com.fromscratch.potion.PotionObservation potions,boolean continuous){this.bank=bank;this.offer=offer;this.potions=potions;this.continuous=continuous;}
	}
	private boolean automatic;
	private BankTimings.Trace committedTrace;
	private final PriceService prices;
	private BankValuation valuation;
	private boolean valuationQueued;
	private UUID valuedRevision;
	private long valuedPrices=-1;
	private Consumer<BankRun> projectionListener = value -> { };
	/** Client-thread read of acknowledged state; never exposes another character's cached run. */
	public BankRun currentRun()
	{ return enabled&&run!=null&&run.isActive()&&run.isAnchored()&&Objects.equals(character,source.characterKey())?run:null; }
	public void informationActions(Consumer<BankRun> listener) { projectionListener=listener; }
	private BooleanSupplier initialBankView=()->true;
	public void initialBankView(BooleanSupplier clear){initialBankView=clear;}
	private Consumer<Runnable> beforeRestore=Runnable::run;
	private Consumer<Runnable> beforeExport=Runnable::run;
	private Runnable afterRestore=()->{};
	public void restoreLifecycle(Consumer<Runnable> before,Runnable after){beforeRestore=before;afterRestore=after;}
	public void exportLifecycle(Consumer<Runnable> checkpoint){beforeExport=checkpoint;}
	@Inject public BankController(RuneLiteBankSource source, BankRepository repository, ClientThread clientThread, BankSidebar sidebar, BankOverlayState overlay,PriceService prices)
	{ this(source, repository, clientThread::invokeLater, v -> { overlay.updateBank(v); sidebar.update(v); },prices); }
	public BankController(BankSource source, BankRepository repository, Consumer<Runnable> dispatch, Consumer<BankView> display)
	{ this(source,repository,dispatch,display,PriceService.unavailable()); }
	public BankController(BankSource source, BankRepository repository, Consumer<Runnable> dispatch, Consumer<BankView> display,PriceService prices)
	{ this.source = source; this.repository = repository; this.dispatch = dispatch; this.display = display;this.prices=prices; }
	public void start(DirectoryProvider directory)
	{
		start(directory, new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8), task -> { Thread t = new Thread(task, "fromscratch-bank-storage"); t.setDaemon(true); return t; }));
	}
	public void start(DirectoryProvider directory, ExecutorService worker)
	{
		this.directory = directory; this.worker = worker; enabled = true; long token = epoch.incrementAndGet();
			dispatch.accept(() -> { if (!enabled || epoch.get() != token) { return; } character = null; setupCapture=null;setupAttempt=UUID.randomUUID();setup=BankSetup.begin(null,setupAttempt);run = null; busy = false; error = false; automatic = false; committedTrace = null; notice = null; pending.clear(); exchangeBaseline.clear(); latestOffers.clear(); observation = null; last = null; context++; valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1; names.clear(); publish(); tick(); });
	}
	public void stop() { enabled = false; epoch.incrementAndGet(); if (worker != null) { worker.shutdownNow(); } }
	public void tick()
	{
		if (!enabled) { return; }
		String key = source.characterKey();
		if (!Objects.equals(character, key))
		{
			character = key; setupCapture=null;setupAttempt=UUID.randomUUID();setup=BankSetup.begin(key,setupAttempt);context++; run = null; observation = null; busy = false; error = false; automatic = false; pending.clear(); exchangeBaseline.clear(); latestOffers.clear(); notice = null; committedTrace = null; names.clear();
			valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1;bankContinuous=false;potionObservation=com.fromscratch.potion.PotionObservation.unknown();
			if (key == null) { message = "Log in to load this character's local progress."; publish(); }
			else { load(); } return;
		}
		if (character == null || error || (busy && !automatic)) { return; }
		flushExchangeBaseline();
		observation = source.observe();
		potionObservation=source.potions();
		if(run==null||!run.isActive())
		{
			if(source.potionStorageUnlockKnown()){setup=setup.unlocked(source.potionStorageUnlocked());}
			boolean clear=initialBankView.getAsBoolean();
			if(source.fullBankView()&&clear&&observation.isReady()){setup=setup.capture(observation);}
			else if(setup.getBank()!=null)
			{
				boolean same=observation.isReady()&&setup.getBank().getSession().equals(observation.getSession())&&setup.getBank().getBank().getQuantities().equals(observation.getBank().getQuantities());
				// A mode change retains proof. A confirmed changed container invalidates it
				// until a new full-bank observation, rather than rebasing from storage widgets.
				setup=observation.isReady()&&!same?setup.capture(null):setup.current(clear&&same);
			}
			if(setupCapture!=null&&!source.validPotionCapture(setupCapture.getObservation())){setupCapture=null;}
			if(potionObservation.isVerified()&&!potionObservation.getTypes().isEmpty()&&source.validPotionCapture(potionObservation)){setupCapture=new com.fromscratch.potion.PotionCapture(character,setupAttempt,potionObservation);}
			else if(source.potionStorageOpen()){setupCapture=null;}
		}
		if(!observation.isReady()){bankContinuous=false;}
		try
		{
			if (run != null && run.isActive() && run.isAnchored() && observation.isReady())
			{
				enqueue(observation);bankContinuous=true;
				if (busy) { return; }
			}
			message = run != null && run.isActive() ? (run.isAnchored() ? "Bank progress updates automatically." : "Confirm a one-time bank anchor to finish upgrading this run.") : observation.getInstruction();
			publish();
		}
		catch (IllegalArgumentException | ArithmeticException ex) { error = true; message = "Bank quantities could not be safely updated: " + ex.getMessage(); publish(); }
	}
	/** Queue complete tick snapshots, not raw container events. A slow disk never blocks or reorders client events. */
	private void enqueue(BankObservation observed)
	{
		Pending tail = pending.peekLast();
		if (tail != null && tail.bank!=null && tail.bank.getSession().equals(observed.getSession()) && tail.bank.getBank().getQuantities().equals(observed.getBank().getQuantities())&&Objects.equals(tail.potions,potionObservation)) { return; }
		if (pending.size() >= 256) { error = true; message = "Bank storage cannot keep up. Progress was preserved; retry loading before continuing."; publish(); return; }
		pending.addLast(new Pending(observed,null,potionObservation,bankContinuous)); drain();
	}
	/** Passive copied GE packets share the same durable FIFO as bank snapshots, never a second quantity ledger. */
	public void exchange(GeObservation observed)
	{
		if(!enabled){return;}String key=source.characterKey();if(!Objects.equals(character,key)){tick();}
		if(key==null||!key.equals(character)||error){return;}
		latestOffers.put(observed.getSlot(),observed);
		if(run==null||!run.isActive()){exchangeBaseline.put(observed.getSlot(),observed);return;}
		// Keep room for ordinary bank snapshots during an extreme offer burst. The latest
		// cumulative value per slot survives overload; omitted intermediate fills remain uncertain.
		if(pending.size()>=128||exchangeBaseline.containsKey(observed.getSlot()))
		{
			exchangeBaseline.put(observed.getSlot(),new GeObservation(observed.getEventId(),observed.getSession(),observed.getAt(),observed.getSlot(),observed.getItemId(),observed.getName(),observed.getState(),observed.getTotal(),observed.getFilled(),observed.getLimitPrice(),observed.getReportedGp(),true,observed.isBeforeStart()));
			notice="GE updates arrived faster than storage. Their latest totals were retained; intermediate trade details may be incomplete.";publish();return;
		}
		pending.addLast(new Pending(null,observed));drain();
	}
	public void exchangeGap()
	{if(enabled){notice="Some GE history could not be observed. Bank progress continues automatically; trade details may be incomplete.";publish();}}
	private void flushExchangeBaseline()
	{
		if(run==null||!run.isActive()||exchangeBaseline.isEmpty()||pending.size()>=128){return;}
		List<GeObservation> baseline=new ArrayList<>(exchangeBaseline.values());exchangeBaseline.clear();baseline.sort(Comparator.comparing(GeObservation::getAt));
		for(GeObservation offer:baseline){exchange(offer);}
	}
	private void drain()
	{
		if (busy || error || run == null) { return; }
		while (!pending.isEmpty())
		{
			Pending update=pending.removeFirst();BankObservation next=update.bank;
			boolean sameStorage=update.potions==null||!update.potions.isVerified()&&run.getPotions().getTypes().entrySet().containsAll(update.potions.getTypes().entrySet())||Objects.equals(run.getPotions().getObservedAt(),update.potions.getAt())&&run.getPotions().getTypes().equals(update.potions.getTypes())&&run.getPotions().getPhysical().equals(update.potions.getQuantities())&&next!=null&&Objects.equals(run.getPotions().getSession(),next.getSession());
			if (next!=null && sameStorage && run.getPhysical().equals(next.getBank().getQuantities()) && run.getHostSummary() != null && run.getHostSummary().getSession().equals(next.getSession())) { continue; }
			BankRun expected = run; busy = true; automatic = true;
			publish();
			// No speculative balances are projected; accounting/JSON/disk work all happen on this worker.
			submit(root ->
			{
				BankRun candidate = next==null?BankEngine.exchange(expected,update.offer):BankEngine.observe(expected, next.getBank(), next.getSession(), next.getAt(), next.getTrace(),prices.snapshot(next.getBank().getQuantities().keySet()),update.potions);
				BankTimings.Trace trace=next==null?null:next.getTrace();BankTimings.mark(trace, BankTimings.Phase.WRITE_START);
				BankRun saved;
				try { saved = candidate == expected ? expected : repository.save(root, expected, candidate); }
				catch (IOException | RuntimeException ex) { if (trace != null) { trace.failed(); } throw ex; }
				BankTimings.mark(trace, BankTimings.Phase.WRITE_END); return saved;
			}, saved ->
			{
				run = saved; busy = false; automatic = false; committedTrace = next==null?null:next.getTrace(); notice = null; message = "Bank progress saved.";
				if(saved.getRecent().get(0).getExchange().getMatches().stream().anyMatch(m->m.isDeposit()&&m.getOrigin()==GeOffer.Origin.PREEXISTING))
				{notice="Possible return from a preexisting GE offer. Use Manual Bank Corrections if these bank counts should remain legacy.";}
				BankTimings.mark(committedTrace, BankTimings.Phase.PUBLISHED); publish();if(next!=null&&saved!=expected){overdraftListener.accept(Overdrafts.between(expected,saved,update.continuous,source::itemName));}drain();
			});
			return;
		}
		flushExchangeBaseline();
	}
	private void load()
	{
		bankContinuous=false;
		busy = true; automatic = false; pending.clear(); message = "Loading local bank progress…"; publish();
		String key = character;
		submit(root -> repository.load(root, key).orElse(null), loaded -> { run = loaded; busy = false; error = false; notice = null; tick(); });
	}
	public void startRun(boolean attested){startRun(attested,null,false);}
	public void setupAttested(UUID attempt,boolean value){act(()->{requireSetup(attempt);setup=setup.attest(value);publish();});}
	public void setupStorageAnswer(UUID attempt,Boolean unlocked){act(()->{requireSetup(attempt);setup=setup.answer(unlocked);publish();});}
	private void requireSetup(UUID attempt){if(run!=null&&run.isActive()||!setupAttempt.equals(attempt)||!Objects.equals(character,setup.getCharacter())){throw new IllegalArgumentException("Setup changed. Review this character's setup again.");}}
	public void startRun(boolean attested,com.fromscratch.potion.PotionCapture captured,boolean notUnlocked)
	{
		act(() ->
		{
			if (run != null && run.isActive()) { throw new IllegalArgumentException("A run is already active."); }
			if(!initialBankView.getAsBoolean()){throw new IllegalArgumentException("Full Bank could not be verified. Select View all items, then review your bank and try again.");}
			if(!source.fullBankView()&&!source.potionStorageOpen()){throw new IllegalArgumentException("Open your bank or Potion Storage to verify the recorded snapshots.");}
			BankObservation fresh = requireBank();
			if(!attested){throw new IllegalArgumentException("Confirm that inventory and equipment have been banked first.");}
			if(setup.getBank()==null||!setup.getBank().getSession().equals(fresh.getSession())||!setup.getBank().getBank().getQuantities().equals(fresh.getBank().getQuantities()))
			{setup=setup.capture(null);throw new IllegalArgumentException("Your bank changed or has not been recorded. Open your full bank again.");}
			setup=setup.attest(true);
			com.fromscratch.potion.PotionObservation storage;String proof;
			boolean known=source.potionStorageUnlockKnown()||setup.getStorageUnlocked()!=null,unlocked=source.potionStorageUnlockKnown()?source.potionStorageUnlocked():Boolean.TRUE.equals(setup.getStorageUnlocked());
			if(known&&!unlocked||!known&&(Boolean.FALSE.equals(setup.getStorageAnswer())||notUnlocked))
			{
				if(captured!=null||setupCapture!=null||source.potionStorageUnlocked()||source.potionStorageOpen()||!source.potions().getTypes().isEmpty()){throw new IllegalArgumentException("Potion Storage is available. Record and confirm its contents instead.");}
				storage=com.fromscratch.potion.PotionObservation.unavailableStore();proof=known?"RuneLite unlock state verified Potion Storage is not unlocked.":"Player attested Potion Storage is not unlocked.";
			}
			else
			{
				if(setupCapture==null||captured!=null&&!captured.equals(setupCapture)||!setupCapture.belongsTo(character,setupAttempt)||!source.validPotionCapture(setupCapture.getObservation())){throw new IllegalArgumentException("Open Potion Storage to record its contents for this setup attempt.");}
				storage=setupCapture.getObservation();proof="Complete current-setup Potion Storage capture at "+storage.getAt()+"; "+storage.getTypes().size()+" types including zero stock.";
			}
			BankRun candidate = BankEngine.start(character, run == null ? null : run.getPreviousRunId(), setup.getBank().getBank(), attested, fresh.getSession(), Instant.now(),storage,proof);
			latestOffers.forEach((slot,offer)->exchangeBaseline.put(slot,offer.beforeStart()));
			if (run != null)
			{
				candidate = new BankRun(candidate.getCharacter(), candidate.getRunId(), run.getPreviousRunId(), candidate.getStartedAt(), candidate.getRevision(), Math.incrementExact(run.getSequence()), candidate.getInitialBank(), candidate.getPhysical(), candidate.getBalances(), true, null, null, candidate.getRecent(), null,candidate.getFinancial(),candidate.getExchange(),candidate.getBuybacks(),candidate.getPotions());
			}
			save(candidate);
		});
	}
	public void anchor(UUID revision, com.fromscratch.model.ContainerSnapshot reviewedBank, boolean approveReductions)
	{ act(() -> { requireRevision(revision); BankObservation fresh = requireBank(); if (!fresh.getBank().getQuantities().equals(reviewedBank.getQuantities())) { throw new IllegalArgumentException("Your bank changed. Review the bank anchor again."); } save(BankEngine.anchor(run, fresh.getBank(), approveReductions, fresh.getSession(), Instant.now())); }); }
	public void correct(UUID revision, int id, long desired)
	{
		act(() ->
		{
			requireRevision(revision); BankObservation fresh = requireBank();
			if (!run.getPhysical().equals(fresh.getBank().getQuantities())) { throw new IllegalArgumentException("Your bank changed. Wait for the automatic update, then reopen Bank Corrections."); }
			save(BankEngine.correct(run, id, desired, fresh.getSession(), Instant.now()));
		});
	}
	public void reset(UUID revision)
	{ act(() -> { requireRevision(revision); save(BankEngine.reset(run, UUID.randomUUID(), Instant.now()),()->{setupCapture=null;setupAttempt=UUID.randomUUID();setup=BankSetup.begin(character,setupAttempt);potionObservation=com.fromscratch.potion.PotionObservation.unknown();source.newPotionSetup();}); }); }
	public void reviewStorage(UUID revision,Map<Integer,Long> reviewed)
	{
		act(()->
		{
			requireRevision(revision);BankObservation fresh=source.observe();
			if(fresh.isReady()&&!run.getPhysical().equals(fresh.getBank().getQuantities())){throw new IllegalArgumentException("Wait for bank changes to save before reviewing storage.");}
			com.fromscratch.potion.PotionState state=run.getPotions();com.fromscratch.potion.PotionObservation live=source.potions();
			if(state.getObservedAt()==null||live.isVerified()&&!live.getQuantities().equals(state.getPhysical())){throw new IllegalArgumentException("Record Potion Storage and wait for its changes to save before reviewing.");}
			com.fromscratch.potion.PotionObservation recorded=new com.fromscratch.potion.PotionObservation(true,state.getTypes(),state.getPhysical(),state.getObservedAt());
			save(BankEngine.reviewStorage(run,recorded,reviewed,state.getSession(),Instant.now()));
		});
	}
	public void correctStorage(UUID revision,int type,long desired)
	{
		act(()->
		{
			requireRevision(revision);BankObservation fresh=source.observe();
			if(fresh.isReady()&&!run.getPhysical().equals(fresh.getBank().getQuantities())){throw new IllegalArgumentException("Wait for bank changes to save, then reopen Manual Corrections.");}
			com.fromscratch.potion.PotionObservation live=source.potions();
			if(live.isVerified()&&!live.getQuantities().equals(run.getPotions().getPhysical())){throw new IllegalArgumentException("Wait for Potion Storage changes to save, then reopen Manual Corrections.");}
			save(BankEngine.correctStorage(run,type,desired,Instant.now()));
		});
	}
	public void buyback(UUID revision,String catalogId,UUID purchaseId)
	{buyback(revision,catalogId,purchaseId,null,p->{});}
	public void buyback(UUID revision,String catalogId,UUID purchaseId,com.fromscratch.pouch.PouchObservation preview,Consumer<com.fromscratch.buyback.BuybackPurchase> committed)
	{
		act(()->
		{
			requireRevision(revision);BankObservation fresh=requireBank();
			if(!run.getPhysical().equals(fresh.getBank().getQuantities())){throw new IllegalArgumentException("Your bank changed. Wait for the automatic update, then reopen Legacy Buybacks.");}
			com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalogId);
			for(Map.Entry<String,Integer> role:entry.getRoles().entrySet())
			{
				boolean verified=com.fromscratch.buyback.BuybackCatalog.variants(role.getKey()).entrySet().stream().anyMatch(v->v.getValue().equals(role.getValue())&&run.physical(v.getKey())>0&&source.buybackDefinition(v.getKey()));
				if(!verified){throw new IllegalArgumentException("A required item definition is unavailable or is not qualifying untradeable equipment. No GP was spent.");}
			}
			com.fromscratch.pouch.PouchObservation contents=entry.getId().equals("rune_pouch_family")?source.pouch():com.fromscratch.pouch.PouchObservation.unknown();
			if(preview!=null&&(!contents.verified()||!preview.getQuantities().equals(contents.getQuantities())||preview.getPouchId()!=contents.getPouchId())){throw new IllegalArgumentException(contents.verified()?"Rune Pouch contents changed. Review them again with the bank open.":contents.getProblem());}
			BankRun candidate=BankEngine.buyback(run,catalogId,purchaseId,fresh.getSession(),Instant.now(),contents);
			save(candidate,()->committed.accept(candidate.getBuybacks().getPurchases().get(candidate.getBuybacks().getPurchases().size()-1)));
		});
	}
	public void recordExpense(UUID revision,long gp,String reason)
	{ act(()->{requireRevision(revision);save(BankEngine.expense(run,gp,reason,UUID.randomUUID(),Instant.now()));}); }
	public void bagPreview(Consumer<com.fromscratch.buyback.BagObservation> callback)
	{dispatch.accept(()->callback.accept(enabled&&Objects.equals(character,source.characterKey())?source.bag():com.fromscratch.buyback.BagObservation.unknown()));}
	public void buyBag(UUID revision,UUID purchaseId,com.fromscratch.buyback.BagObservation preview,Consumer<com.fromscratch.buyback.BuybackPurchase> committed)
	{
		act(()->{
			requireRevision(revision);BankObservation fresh=requireBank();
			if(!run.getPhysical().equals(fresh.getBank().getQuantities())){throw new IllegalArgumentException("Your bank changed. Reopen Legacy Buybacks.");}
			com.fromscratch.buyback.BagObservation contents=source.bag();
			if(preview!=null&&preview.getState()==com.fromscratch.buyback.BagObservation.State.PLAYER_CONFIRMED_EMPTY)
			{
				if(!preview.fresh(Instant.now())){throw new IllegalArgumentException("Your empty-bag confirmation expired. Reopen Legacy Buybacks.");}
				if(contents.fresh(Instant.now())&&contents.getState()==com.fromscratch.buyback.BagObservation.State.VERIFIED_NONEMPTY){throw new IllegalArgumentException("Your Looting Bag has verified contents. Review them in Legacy Buybacks; an empty declaration cannot replace them.");}
				contents=preview;
			}
			else if(preview==null||!contents.fresh(Instant.now())||preview.getItemId()!=contents.getItemId()||!preview.getQuantities().equals(contents.getQuantities())){throw new IllegalArgumentException(com.fromscratch.buyback.BagObservation.INSTRUCTION);}
			if(!source.buybackDefinition(contents.getItemId())){throw new IllegalArgumentException("The Looting Bag definition could not be verified. No GP was spent.");}
			BankRun candidate=BankEngine.buyBag(run,purchaseId,fresh.getSession(),Instant.now(),contents);
			save(candidate,()->committed.accept(candidate.getBuybacks().getPurchases().get(candidate.getBuybacks().getPurchases().size()-1)));
		});
	}
	/** Read varbits only on a relevant user request, never a personal-container observation loop. */
	public void pouchPreview(Consumer<com.fromscratch.pouch.PouchObservation> callback)
	{
		dispatch.accept(()->{callback.accept(enabled&&Objects.equals(character,source.characterKey())?source.pouch():com.fromscratch.pouch.PouchObservation.unknown());});
	}
	public void retry() { dispatch.accept(() -> { if (enabled && !busy && character != null) { error = false; load(); } }); }
	public void history(boolean manual, BankHistory.Cursor cursor, Consumer<BankHistory.Page> callback)
	{
		dispatch.accept(() ->
		{
			if (!enabled || run == null || !run.isActive()) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "No active bank history.")); return; }
			BankRun captured = run; long token = epoch.get(); long capturedContext = context; DirectoryProvider provider = directory;
			try { worker.execute(() ->
			{
				BankHistory.Page page;
				try { page = repository.history(provider.get(), captured, cursor, manual); }
				catch (IOException | RuntimeException ex) { page = new BankHistory.Page(Collections.emptyList(), null, "History could not be read. Existing records were preserved."); }
				BankHistory.Page result = page;
				dispatch.accept(() ->
				{
					if (!enabled || epoch.get() != token || context != capturedContext || run == null || !Objects.equals(run.getRunId(), captured.getRunId()) || !captured.getCharacter().equals(source.characterKey())) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "Character or run changed. Reopen Bank History.")); return; }
					List<BankHistory.Row> rows = new ArrayList<>();
					for (BankHistory.Row row : result.getRows()) { String name = row.getName().isEmpty()?names.computeIfAbsent(row.getItemId(), source::itemName):row.getName(); rows.add(new BankHistory.Row(row.getAt(), row.getItemId(), name == null ? "Unknown item" : name, row.getChange(),row.getCategory(),row.getPurchase())); }
					callback.accept(new BankHistory.Page(rows, result.getNext(), result.getMessage()));
				});
			}); }
			catch (RejectedExecutionException ex) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "Storage is busy. Close this view and retry.")); }
		});
	}
	private void requireRevision(UUID revision)
	{
		if (run == null || !run.isActive() || !run.getRevision().equals(revision)) { throw new IllegalArgumentException("Progress changed. Reopen this tool before applying changes."); }
	}
	public void exchangeHistory(boolean buy,GeHistory.Cursor cursor,Consumer<GeHistory.Page> callback)
	{
		dispatch.accept(() ->
		{
			if(!enabled||run==null||!run.isActive()){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"No active GE history."));return;}
			BankRun captured=run;long token=epoch.get(),capturedContext=context;DirectoryProvider provider=directory;ExecutorService reader=worker;
			GeHistory.Cursor[] position={cursor};Runnable[] read=new Runnable[1];read[0]=() ->
			{
				GeHistory.Page page;try{page=repository.exchangeHistory(provider.get(),captured,position[0],buy);}catch(IOException|RuntimeException ex){page=new GeHistory.Page(Collections.emptyList(),null,"GE history could not be read; existing records were preserved.");}
				GeHistory.Page result=page;dispatch.accept(() ->
				{
					if(!enabled||epoch.get()!=token||context!=capturedContext||run==null||!Objects.equals(run.getRunId(),captured.getRunId())||!captured.getCharacter().equals(source.characterKey())){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"Character or run changed. Reopen GE history."));return;}
					if(result.isScanning())
					{
						// Yield the storage FIFO after 64 segments so normal bank commits can proceed.
						position[0]=result.getNext();try{reader.execute(read[0]);}catch(RejectedExecutionException ex){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"History is busy. Retry shortly."));}return;
					}
					callback.accept(result);
				});
			};try{reader.execute(read[0]);}catch(RejectedExecutionException ex){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"History is busy. Retry shortly."));}
		});
	}
	private BankObservation requireBank()
	{
		if (character == null || !character.equals(source.characterKey())) { throw new IllegalArgumentException("Character changed. Wait for local progress to reload."); }
		BankObservation fresh = source.observe(); if (!fresh.isReady()) { throw new IllegalArgumentException(fresh.getInstruction()); } return fresh;
	}
	private void save(BankRun next){save(next,()->{});}
	private void save(BankRun next,Runnable acknowledged)
	{
		if (next == run) { publish(); return; }
		boolean resetting=next.getRecent().get(0).getKind()==BankAudit.Kind.RESET;
		BankRun expected = run; busy = true; automatic = false; message = resetting?"Resetting progress...":"Saving bank progress…"; publish();
		submit(root -> repository.save(root, expected, next), saved ->
		{
			run = saved; busy = false; notice = null; message = "Bank progress saved.";
			if(resetting){bankContinuous=false;pending.clear();exchangeBaseline.clear();valuation=null;valuedRevision=null;valuedPrices=-1;valuationQueued=false;notice="Progress reset. Complete setup to start a new challenge.";acknowledged.run();}
			publish();if(!resetting){acknowledged.run();}flushExchangeBaseline();drain();
		});
	}
	private void act(Runnable action)
	{
		dispatch.accept(() ->
		{
			if (!enabled || busy || error || character == null) { return; }
			if (!character.equals(source.characterKey())) { tick(); return; }
			try { notice = null; action.run(); }
			catch (IllegalArgumentException | ArithmeticException ex) { notice = ex.getMessage(); publish(); }
		});
	}
	private void publish()
	{
		projectionListener.accept(currentRun());
		if(run!=null){prices.register(com.fromscratch.buyback.BuybackEngine.balances(run).keySet(),source::priceId);}if(observation!=null&&observation.isReady()){prices.register(observation.getBank().getQuantities().keySet(),source::priceId);}
		if(run!=null){Set<Integer> potionPrices=new HashSet<>();run.getPotions().getTypes().values().forEach(type->potionPrices.add(type.displayId()));prices.register(potionPrices,source::priceId);}
		refreshValuation();
		boolean setupBlocked=character!=null&&(run==null||!run.isActive())&&!initialBankView.getAsBoolean();
		String visibleMessage = notice!=null?notice:setupBlocked?"Full Bank is required before starting. Select View all items and try again.":message;
		BankObservation presented=setupBlocked?null:observation;
		boolean ready = !busy && !error && presented != null && presented.isReady();
		boolean previewChanged = (run == null || !run.isAnchored()) && last != null && presented != null && presented.isReady()
			&& (last.getObservation() == null || !last.getObservation().isReady() || !last.getObservation().getBank().getQuantities().equals(presented.getBank().getQuantities()));
		if (last != null && Objects.equals(last.getRun(), run) && last.isBusy() == busy && last.isError() == error && last.ready() == ready
			&& last.getMessage().equals(visibleMessage) && Objects.equals(last.getValuation(),valuation) && Objects.equals(last.getPotions(),potionObservation) && Objects.equals(last.getCapture(),setupCapture) && last.isPotionUnlocked()==source.potionStorageUnlocked() && last.isPotionUnlockKnown()==source.potionStorageUnlockKnown() && Objects.equals(last.getSetup(),setup) && !previewChanged) { return; }
		Set<Integer> ids = new TreeSet<>();
		if (run != null) { ids.addAll(run.getPhysical().keySet()); ids.addAll(run.getBalances().keySet()); if (run.getInitialBank() != null) { ids.addAll(run.getInitialBank().getQuantities().keySet()); } run.getRecent().forEach(a -> { ids.addAll(a.getBalanceChanges().keySet()); ids.addAll(a.getPhysicalChanges().keySet()); }); }
		if (observation != null && observation.isReady()) { ids.addAll(observation.getBank().getQuantities().keySet()); }
		for (int id : ids) { names.computeIfAbsent(id, key -> { String name = source.itemName(key); return name == null || name.isEmpty() ? "Item #" + key : name; }); }
		BankView view = new BankView(run, presented, names, busy, error, visibleMessage, committedTrace,valuation,potionObservation,setupCapture,source.potionStorageUnlocked(),source.potionStorageUnlockKnown(),setup);
		// Tick/time changes alone need not rebuild the entire Swing panel every game tick.
		if (last == null || !Objects.equals(last.getRun(), view.getRun()) || !last.getNames().equals(view.getNames()) || last.isBusy() != busy || last.isError() != error
			|| !last.getMessage().equals(visibleMessage) || !Objects.equals(last.getValuation(),valuation) || !Objects.equals(last.getPotions(),potionObservation) || !Objects.equals(last.getCapture(),setupCapture) || last.isPotionUnlocked()!=source.potionStorageUnlocked() || last.isPotionUnlockKnown()!=source.potionStorageUnlockKnown() || last.ready() != view.ready() || !Objects.equals(last.getSetup(),setup) || previewChanged) { last = view; display.accept(view); }
	}
	/** Derived values use the storage worker; only a new verified peak writes its small checkpoint. */
	private void refreshValuation()
	{
		if(busy||error||run==null||!run.isActive()||!run.isAnchored()||valuationQueued||(run.getRevision().equals(valuedRevision)&&prices.version()==valuedPrices)){return;}
		BankRun captured=run;long priceVersion=prices.version(),token=epoch.get(),capturedContext=context;valuationQueued=true;boolean storageFresh=captured.getPotions().getTypes().isEmpty()||potionObservation.isVerified()&&observation!=null&&observation.isReady()&&potionObservation.getQuantities().equals(captured.getPotions().getPhysical());
		try{worker.execute(()->{Set<Integer> ids=new HashSet<>(captured.getBalances().keySet());ids.addAll(com.fromscratch.buyback.BuybackEngine.balances(captured).keySet());captured.getPotions().getTypes().values().forEach(type->ids.add(type.displayId()));Map<Integer,PriceQuote> quotes=prices.snapshot(ids);BankValuation calculated=BankValuation.calculate(captured,quotes);Long peak=null;try{peak=repository.peak(directory.get(),captured,storageFresh?com.fromscratch.persistence.PeakProgressRepository.candidate(captured,quotes):null);}catch(IOException|RuntimeException ex){/* Peak unavailable; verified economic progression remains usable. */}BankValuation result=calculated.withPeak(peak);dispatch.accept(()->{
			if(!enabled||epoch.get()!=token||context!=capturedContext){return;}valuationQueued=false;
			if(run!=null&&run.getRevision().equals(captured.getRevision())&&captured.getCharacter().equals(source.characterKey())){valuation=result;valuedRevision=captured.getRevision();valuedPrices=priceVersion;publish();}
		});});}catch(RejectedExecutionException ex){valuationQueued=false;}
	}
	private <T> void submit(Storage<T> work, Consumer<T> success)
	{
		long token = epoch.get(); long capturedContext = context; String key = character;
		DirectoryProvider capturedDirectory = directory;
		try
		{
			worker.execute(() ->
			{
				if (!enabled || token != epoch.get() || Thread.currentThread().isInterrupted()) { return; }
				T result = null; String failure = null;
				try { result = work.run(capturedDirectory.get()); } catch (IOException | RuntimeException ex) { failure = ex instanceof IOException ? ex.getMessage() : "Bank storage failed validation. Existing files were preserved."; }
				T value = result; String errorMessage = failure;
				dispatch.accept(() ->
				{
					if (!enabled || token != epoch.get() || capturedContext != context || !Objects.equals(key, character) || !Objects.equals(key, source.characterKey())) { return; }
					if (errorMessage == null) { success.accept(value); }
					else { busy = false; automatic = false; error = true; message = errorMessage; publish(); }
				});
			});
		}
		catch (RejectedExecutionException ex) { busy = false; error = true; message = "Bank storage is busy. Retry loading progress."; publish(); }
	}
	@FunctionalInterface public interface DirectoryProvider { Filepath get() throws IOException; }
	public void exportProgress(Filepath file,Consumer<String> result)
	{
		dispatch.accept(()->
		{
			BankRun captured=currentRun();if(captured==null){result.accept("Load an active run before exporting.");return;}
			beforeExport.accept(()->backupTask(root->{repository.backups().exportProgress(root,captured,file);return true;},saved->result.accept("Progress exported. Keep this file private and safe."),result));
		});
	}
	public void previewImport(Filepath file,Consumer<com.fromscratch.persistence.ProgressBackup.Candidate> result,Consumer<String> failure)
	{
		dispatch.accept(()->{if(character==null||busy){failure.accept("Log in and wait for local saves to finish before importing.");return;}String key=character;backupTask(root->repository.backups().preview(root,key,file),result,failure);});
	}
	public void cancelImport(com.fromscratch.persistence.ProgressBackup.Candidate candidate)
	{ try{worker.execute(()->{try{repository.backups().discard(candidate);}catch(IOException ex){/* Unselected temporary files cannot affect progression. */}});}catch(RejectedExecutionException ex){/* Shutdown retains staging, never selects it. */} }
	public void previewRecovery(Consumer<com.fromscratch.persistence.ProgressBackup.Candidate> result,Consumer<String> failure)
	{dispatch.accept(()->{if(character==null||busy){failure.accept("Log in and wait for storage before recovering.");return;}String key=character;backupTask(root->repository.backups().previous(root,key),result,failure);});}
	public void importProgress(com.fromscratch.persistence.ProgressBackup.Candidate candidate,Consumer<String> result)
	{
		dispatch.accept(()->
		{
			if(!enabled||busy||character==null||!character.equals(candidate.getRun().getCharacter())||!character.equals(source.characterKey())){result.accept("Character or progress changed. Reopen Import Progress.");return;}
			busy=true;automatic=false;publish();long token=epoch.get(),capturedContext=context;
			beforeRestore.accept(()->
			{
				if(!enabled||epoch.get()!=token||context!=capturedContext){afterRestore.run();result.accept("Character changed. Nothing was imported.");return;}
				backupTask(root->repository.backups().restore(root,candidate),restored->
				{
					context++;run=restored;busy=false;error=false;notice=null;pending.clear();exchangeBaseline.clear();latestOffers.clear();observation=null;last=null;valuation=null;valuedRevision=null;valuedPrices=-1;
					bankContinuous=false;message="Progress imported. Open your bank to update it automatically.";afterRestore.run();publish();cancelImport(candidate);result.accept("Progress imported. Previous local files were retained in a backup.");
				},failure->{afterRestore.run();if(enabled&&epoch.get()==token&&context==capturedContext){busy=false;publish();}result.accept(failure);});
			});
		});
	}
	private <T>void backupTask(Storage<T> work,Consumer<T> success,Consumer<String> failure)
	{
		long token=epoch.get(),capturedContext=context;String key=character;DirectoryProvider provider=directory;
		try{worker.execute(()->
		{
			try{T value=work.run(provider.get());dispatch.accept(()->{if(enabled&&epoch.get()==token&&context==capturedContext&&Objects.equals(key,source.characterKey())){success.accept(value);}else{if(value instanceof com.fromscratch.persistence.ProgressBackup.Candidate){cancelImport((com.fromscratch.persistence.ProgressBackup.Candidate)value);}failure.accept("Character changed. Reopen the progress tools.");}});}
			catch(IOException|RuntimeException ex){dispatch.accept(()->failure.accept(ex instanceof IOException?ex.getMessage():"Backup could not be verified. Current progression was preserved."));}
		});}catch(RejectedExecutionException ex){failure.accept("Storage is busy. Try again shortly.");}
	}
	/** Names for item-history search only, including absent previously progressed types. No ledger scan. */
	public void historyItems(Consumer<Map<Integer,String>> callback)
	{
		dispatch.accept(()->
		{
			BankRun captured=currentRun();if(captured==null) { callback.accept(Collections.emptyMap());return; }
			Set<Integer> ids=new TreeSet<>(names.keySet());ids.addAll(captured.getFinancial().getHighWater().keySet());
			Map<Integer,String> choices=new TreeMap<>();for(int id:ids) { choices.put(id,names.computeIfAbsent(id,source::itemName)); }
			callback.accept(Collections.unmodifiableMap(choices));
		});
	}
	/** Paged item evidence shares the existing guarded history worker, without any accounting write. */
	public void itemHistory(int itemId,BankHistory.Cursor cursor,Consumer<ItemHistory.Page> callback)
	{
		dispatch.accept(()->
		{
			BankRun captured=currentRun();
			if(captured==null) { callback.accept(new ItemHistory.Page(Collections.emptyList(),null,"Log in to view this run's item history."));return; }
			long token=epoch.get(),capturedContext=context;DirectoryProvider provider=directory;
			try { worker.execute(()->
			{
				ItemHistory.Page page;
				try { page=repository.itemHistory(provider.get(),captured,cursor,itemId); }
				catch(IOException|RuntimeException ex) { page=new ItemHistory.Page(Collections.emptyList(),null,"Item history could not be read. Existing records were preserved."); }
				ItemHistory.Page result=page;
				dispatch.accept(()->callback.accept(enabled&&epoch.get()==token&&context==capturedContext&&currentRun()!=null&&Objects.equals(run.getRunId(),captured.getRunId())?result:new ItemHistory.Page(Collections.emptyList(),null,"Character or run changed. Reopen item history.")));
			}); }
			catch(RejectedExecutionException ex) { callback.accept(new ItemHistory.Page(Collections.emptyList(),null,"History is busy. Retry shortly.")); }
		});
	}
	@FunctionalInterface private interface Storage<T> { T run(Filepath root) throws IOException; }
}
