package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.overlay.*;
import com.fromscratch.persistence.*;
import java.awt.Color;
import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real bank aggregation/quiet ticks -> live controller -> Filepath journal/checkpoint -> actual M3 renderer. */
public class BankOnlyPipelineTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private BankRepository repository;
	private final Source source = new Source();
	private Worker worker;
	private BankController controller;
	private final BankOverlayState overlay = new BankOverlayState();
	private final List<BankView> views = new ArrayList<>();
	@Before public void setup() throws Exception
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		repository = new BankRepository(new BankRunCodec(SnapshotFixtures.gson()), SnapshotFixtures.codec()); overlay.enable(); launch(); settle();
	}
	@After public void stop() { controller.stop(); }
	private void launch() { worker = new Worker(); controller = new BankController(source, repository, Runnable::run, v -> { views.add(v); overlay.updateBank(v); }); controller.start(() -> root, worker); worker.runAll(); }
	private void tick() { source.observer.tick(); controller.tick(); worker.runAll(); }
	private void settle() { tick(); tick(); tick(); tick(); }
	private BankView view() { return views.get(views.size() - 1); }
	private BankRun saved() throws Exception { return repository.load(root, source.character).get(); }
	private void start() { controller.startRun(true,null,true); worker.runAll(); settle(); assertTrue(view().getRun().isActive()); }
	private void change(long n) { source.quantity(n); settle(); }
	private void label(String expected) { assertEquals(expected, overlay.getProjection().label(SHIELD)); }
	@Test public void setupConfirmationAndBankCaptureSurviveStorageAndStartThereAtomically() throws Exception
	{
		int item=ItemID._4DOSEPRAYERRESTORE;com.fromscratch.potion.PotionType type=new com.fromscratch.potion.PotionType(1,"Prayer potion",false,Collections.singletonMap(item,4));source.storage=new com.fromscratch.potion.PotionObservation(false,Collections.singletonMap(1,type),Collections.emptyMap(),java.time.Instant.EPOCH);tick();UUID attempt=view().getSetup().getAttempt();BankObservation captured=view().getSetup().getBank();List<Overdrafts.Warning> alerts=new ArrayList<>();controller.overdrafts(alerts::addAll);
		BankPanel[] panel={null};javax.swing.SwingUtilities.invokeAndWait(()->{panel[0]=new BankPanel();panel[0].render(view(),controller);((javax.swing.JCheckBox)BankPolishTest.all(panel[0]).stream().filter(c->"setupBankingConfirmation".equals(c.getName())).findFirst().get()).doClick(0);});assertTrue(view().getSetup().isAttested());
		source.fullBank=false;source.storageOpen=true;source.items=null;source.observer.changed();tick();assertEquals(captured,view().getSetup().getBank());assertFalse(view().setupBankReady());assertTrue(view().getSetup().isAttested());
		source.items=new Item[]{new Item(SHIELD,1)};source.storage=new com.fromscratch.potion.PotionObservation(true,Collections.singletonMap(1,type),Collections.singletonMap(1,1045L),SnapshotFixtures.TIME);tick();assertEquals(attempt,view().getSetup().getAttempt());assertEquals(captured,view().getSetup().getBank());assertTrue(view().setupBankReady());assertNotNull(view().getCapture());
		javax.swing.SwingUtilities.invokeAndWait(()->{panel[0].render(view(),controller);assertTrue(((javax.swing.JCheckBox)BankPolishTest.all(panel[0]).stream().filter(c->"setupBankingConfirmation".equals(c.getName())).findFirst().get()).isSelected());assertTrue(BankPolishTest.all(panel[0]).stream().anyMatch(c->c instanceof javax.swing.JButton&&"Start From Scratch".equals(((javax.swing.JButton)c).getText())&&c.isEnabled()));});
		controller.startRun(true,view().getCapture(),false);assertFalse(repository.load(root,source.character).isPresent());worker.runAll();BankRun saved=saved();assertEquals(captured.getBank(),saved.getInitialBank());assertEquals(1045L,(long)saved.getPotions().getInitial().get(1));assertTrue(saved.getBalances().isEmpty());assertEquals(0,saved.getPotions().balance(1));assertTrue(alerts.isEmpty());
	}
	@Test public void changedBankInvalidatesPendingCaptureAndCannotStartFromStaleStorage() throws Exception
	{
		UUID attempt=view().getSetup().getAttempt();controller.setupAttested(attempt,true);source.fullBank=false;source.storageOpen=true;source.quantity(5);controller.startRun(true);worker.runAll();assertFalse(repository.load(root,source.character).isPresent());tick();assertNull(view().getSetup().getBank());assertTrue(view().getSetup().isAttested());controller.startRun(true);worker.runAll();assertFalse(repository.load(root,source.character).isPresent());source.storageOpen=false;source.fullBank=true;tick();assertEquals(5,view().getSetup().getBank().getBank().quantity(SHIELD));controller.startRun(true);worker.runAll();assertEquals(5,saved().getInitialBank().quantity(SHIELD));
	}
	@Test public void setupAnswersAndProofAreAttemptScopedAcrossLogoutAndCharacters() throws Exception
	{
		source.unlockKnown=false;source.storage=com.fromscratch.potion.PotionObservation.unknown();controller.stop();launch();settle();UUID attempt=view().getSetup().getAttempt();controller.setupAttested(attempt,true);controller.setupStorageAnswer(attempt,false);tick();assertEquals(Boolean.FALSE,view().getSetup().getStorageAnswer());source.open(false);tick();assertTrue(view().getSetup().isAttested());assertNotNull(view().getSetup().getBank());source.open(true);settle();controller.startRun(true);worker.runAll();assertTrue(saved().getRecent().get(0).getReason().contains("Player attested"));
		controller.reset(saved().getRevision());worker.runAll();assertNotEquals(attempt,view().getSetup().getAttempt());assertFalse(view().getSetup().isAttested());assertNull(view().getSetup().getBank());assertNull(view().getSetup().getStorageAnswer());controller.setupAttested(attempt,true);assertFalse(view().getSetup().isAttested());source.character=null;controller.tick();worker.runAll();assertNull(view().getSetup().getCharacter());source.character=SnapshotFixtures.CHARACTER_B;source.observer.reset();controller.tick();worker.runAll();settle();assertEquals(SnapshotFixtures.CHARACTER_B,view().getSetup().getCharacter());assertFalse(view().getSetup().isAttested());assertNull(view().getCapture());assertFalse(repository.load(root,source.character).isPresent());
	}
	@Test public void resetAcknowledgesImmediatelyRejectsDuplicatesAndPublishesOnlyAfterDisk() throws Exception
	{
		start();change(0);BankRun before=saved();List<Overdrafts.Warning> alerts=new ArrayList<>();controller.overdrafts(alerts::addAll);int shown=views.size();controller.reset(before.getRevision());assertTrue(view().isBusy());assertEquals("Resetting progress...",view().getMessage());assertEquals(before,saved());int queued=worker.tasks.size();controller.reset(before.getRevision());assertEquals(queued,worker.tasks.size());controller.startRun(true,null,true);assertEquals(queued,worker.tasks.size());worker.runAll();assertFalse(saved().isActive());assertTrue(Overdrafts.outstanding(view()).isEmpty());assertNull(overlay.getProjection());assertEquals(shown+2,views.size());assertTrue(view().getMessage().startsWith("Progress reset."));assertTrue(alerts.isEmpty());UUID previous=before.getRunId();settle();controller.startRun(true,null,true);worker.runAll();assertTrue(saved().isActive());assertNotEquals(previous,saved().getRunId());assertTrue(saved().getBalances().isEmpty());
	}
	@Test public void resetStorageFailureLeavesOldRunAndReportsFailure() throws Exception
	{
		start();BankRun before=saved();controller.stop();worker=new Worker();boolean[] fail={false};controller=new BankController(source,repository,Runnable::run,views::add);controller.start(()->{if(fail[0]){throw new java.io.IOException("Synthetic reset failure");}return root;},worker);worker.runAll();settle();fail[0]=true;controller.reset(before.getRevision());assertTrue(view().isBusy());worker.runAll();assertTrue(view().isError());assertFalse(view().isBusy());assertEquals(before,saved());assertFalse(view().getMessage().startsWith("Progress reset."));controller.startRun(true,null,true);assertEquals(before,saved());
	}
	@Test public void resetInFlightCannotPublishIntoAnotherCharacter() throws Exception
	{
		start(); change(0); BankRun before = saved();
		BankRun other = BankEngine.start(SnapshotFixtures.CHARACTER_B, null, bank(1), true, UUID.randomUUID(), SnapshotFixtures.TIME);
		repository.save(root, null, other);
		controller.reset(before.getRevision()); assertTrue(view().isBusy());
		source.open(false); source.character = SnapshotFixtures.CHARACTER_B; source.observer.reset(); controller.tick(); worker.runAll(); settle();
		assertEquals(other.getRunId(), view().getRun().getRunId()); assertEquals(other, saved());
		BankRun reset = repository.load(root, before.getCharacter()).get(); assertFalse(reset.isActive());
		source.character = before.getCharacter(); source.observer.reset(); controller.tick(); worker.runAll(); settle();
		assertFalse(view().getRun().isActive()); assertTrue(view().getRun().getBalances().isEmpty());
	}
	@Test public void resetDurableBeforeLogoutIgnoresItsLateCompletionAndReloadsTombstone() throws Exception
	{
		start(); BankRun before = saved(); controller.stop();
		Queue<Runnable> callbacks = new ArrayDeque<>(); worker = new Worker();
		controller = new BankController(source, repository, callbacks::add, views::add);
		controller.start(() -> root, worker);
		while (!callbacks.isEmpty() || !worker.tasks.isEmpty()) { while (!callbacks.isEmpty()) { callbacks.remove().run(); } worker.runAll(); }
		controller.reset(before.getRevision()); callbacks.remove().run(); assertTrue(view().isBusy());
		worker.runAll(); assertFalse(saved().isActive()); // Durable, but UI acknowledgment is still queued.
		controller.stop(); while (!callbacks.isEmpty()) { callbacks.remove().run(); }
		assertTrue(view().isBusy()); // A disabled lifecycle must not publish that callback.
		source.character = null; source.observer.reset(); launch();
		source.character = before.getCharacter(); controller.tick(); worker.runAll(); settle();
		assertFalse(view().getRun().isActive()); assertTrue(view().getRun().getBalances().isEmpty());
		assertTrue(view().getRun().getPotions().getTypes().isEmpty());
	}

	private void fullWithdrawal(boolean placeholders, boolean stackable) throws Exception
	{
		int id = stackable ? ItemID.COINS : SHIELD;
		int placeholder = stackable ? ItemID.Placeholder.COINS : ItemID.Placeholder.WOODEN_SHIELD;
		source.items = stackable ? new Item[]{new Item(id, 4)} : new Item[]{new Item(id, 1), new Item(id, 1), new Item(id, 1), new Item(id, 1)};
		source.observer.changed(); settle(); start(); assertEquals(0, saved().balance(id));
		source.items = new Item[]{new Item(id, 1)}; source.observer.changed(); settle(); assertEquals(-3, saved().balance(id));
		// Native placeholders may carry a widget/container quantity; their template, not that
		// number, establishes that no physical copies remain. Test both zero and nonzero forms.
		source.placeholders.add(placeholder);
		source.items = placeholders ? new Item[]{new Item(placeholder, stackable ? 0 : 1)} : new Item[0];
		source.observer.changed(); settle();
		assertEquals(0, saved().physical(id)); assertEquals(-4, saved().balance(id)); assertEquals(0, saved().balance(placeholder));
		assertEquals(Long.valueOf(-1), saved().getRecent().get(0).getPhysicalChanges().get(id));
		assertEquals(4, saved().getInitialBank().quantity(id));
		if (placeholders)
		{
			java.awt.image.BufferedImage expected = PipelineOverlayFixture.render(overlay, id, 0, true, id);
			java.awt.image.BufferedImage actual = PipelineOverlayFixture.render(overlay, placeholder, stackable ? 0 : 1, true, id);
			assertArrayEquals(expected.getRGB(0, 0, 100, 100, null, 0, 100), actual.getRGB(0, 0, 100, 100, null, 0, 100));
		}
		else
		{
			assertTrue(BankCorrections.search(view(), Integer.toString(id)).contains(id));
			assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, id, false).getRGB(15, 15));
		}
		BankRun before = saved(); source.observer.changed(); settle(); assertEquals(before, saved()); // tab/search refresh
		source.open(false); settle(); source.open(true); settle(); assertEquals(before, saved());
		controller.stop(); source.observer.reset(); launch(); settle(); assertEquals(-4, saved().balance(id));
		for (int quantity : new int[]{1, 2, 4})
		{
			source.items = new Item[]{new Item(id, quantity)}; source.observer.changed(); settle();
			assertEquals(quantity - 4, saved().balance(id)); assertEquals(0, saved().getFinancial().getGainedGp());
		}
		assertEquals(0, saved().balance(id)); assertEquals(0, saved().getFinancial().getEventCount());
	}
	@Test public void finalUnstackableWithdrawalWithoutPlaceholderRetainsDeficit() throws Exception { fullWithdrawal(false, false); }
	@Test public void finalUnstackableWithdrawalWithNativePlaceholderRendersOriginalDeficit() throws Exception { fullWithdrawal(true, false); }
	@Test public void finalStackableWithdrawalWithoutPlaceholderRetainsDeficit() throws Exception { fullWithdrawal(false, true); }
	@Test public void finalStackableWithdrawalWithNativePlaceholderRendersOriginalDeficit() throws Exception { fullWithdrawal(true, true); }
	@Test public void bankOnlySetupNeedsNoPersonalContainers() throws Exception { start(); assertEquals(0, saved().balance(SHIELD)); assertEquals(1, saved().getInitialBank().quantity(SHIELD)); }
	@Test public void attestationRequired() { controller.startRun(false,null,true); worker.runAll(); assertNull(view().getRun()); assertTrue(view().getMessage().contains("Confirm")); }
	@Test public void completeEmptyBankCanStart() throws Exception { change(0); start(); assertTrue(saved().getInitialBank().getQuantities().isEmpty()); }
	@Test public void legacyWithdrawalAndReturnAreMinusOneAndZero() throws Exception { start(); change(0); label("-1"); assertEquals(-1, saved().balance(SHIELD)); change(1); label("0"); }
	@Test public void successiveNewDepositsAndWithdrawal() throws Exception { change(0); start(); change(1); label("1"); change(2); label("2"); change(1); label("1"); assertEquals(1, saved().unlocked()); }
	@Test public void burnOutsideBankDoesNotChangeProgressOnReopen() throws Exception { change(0); start(); change(2); change(1); BankRun before = saved(); source.open(false); settle(); /* outside-bank actions have no source at all */ source.open(true); settle(); label("1"); assertEquals(before, saved()); }
	@Test public void repeatedWithdrawalsAndDepositsDoNotCreateLifetimeEarnings() throws Exception { start(); BankRun initial = saved(); for (int i = 0; i < 5; i++) { change(0); change(1); } label("0"); assertEquals(0, saved().unlocked()); assertEquals(initial.getInitialBank(), saved().getInitialBank()); }
	@Test public void mixedLegacyAndNewStackUsesSignedNetProgress() { source.quantity(100); settle(); start(); change(103); label("3"); change(99); label("-1"); change(103); label("3"); }
	@Test public void reorderingTabsAndSearchDoNotChangeAggregate() throws Exception { source.items = new Item[]{new Item(SHIELD, 1), new Item(OTHER, 2)}; source.observer.changed(); settle(); start(); BankRun before = saved(); source.items = new Item[]{new Item(OTHER, 2), new Item(SHIELD, 1)}; source.observer.changed(); settle(); assertEquals(before, saved()); }
	@Test public void unavailableBankIsNotDeletion() throws Exception { start(); BankRun before = saved(); source.items = null; source.observer.changed(); settle(); assertFalse(view().ready()); assertEquals(before, saved()); label("0"); }
	@Test public void nullSlotIsIncompleteNotEmpty() throws Exception { start(); BankRun before = saved(); source.items = new Item[]{null}; source.observer.changed(); settle(); assertFalse(view().ready()); assertEquals(before, saved()); }
	@Test public void pinDoesNotAcceptCachedBank() { source.pin = true; source.observer.changed(); settle(); controller.startRun(true,null,true); worker.runAll(); assertNull(view().getRun()); }
	@Test public void widgetOpenBeforeContainerWaitsThenRecovers() { source.items = null; source.open(true); settle(); assertFalse(view().ready()); source.quantity(1); settle(); start(); label("0"); }
	@Test public void alreadyOpenBankDoesNotNeedContainerChangedEvent() { start(); label("0"); }
	@Test public void sameTickIntermediateUpdatesAreCombined() throws Exception { start(); source.quantity(2); controller.tick(); source.quantity(3); controller.tick(); assertEquals(0, saved().balance(SHIELD)); settle(); label("2"); }
	@Test public void repeatedEventsAndNoChangeDoNotAppendJournal() throws Exception { start(); BankRun before = saved(); source.observer.changed(); controller.tick(); source.observer.changed(); settle(); assertEquals(before, saved()); }
	@Test public void absentNegativeItemIsSearchableWithoutInventingSlot() { start(); change(0); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(view(), "wooden")); assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, false).getRGB(15, 15)); label("-1"); }
	@Test public void correctionsSearchWorksWithNoPendingChanges() { start(); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(view(), "Wooden shield")); }
	@Test public void correctionUpdatesDurableOverlayAndSurvivesLaterBankChanges() throws Exception { start(); controller.correct(view().getRun().getRevision(), SHIELD, 1); worker.runAll(); label("1"); assertEquals(1, saved().balance(SHIELD)); change(2); label("2"); change(1); label("1"); }
	@Test public void correctionCannotExceedPhysicalBank() throws Exception { start(); BankRun before = saved(); controller.correct(before.getRevision(), SHIELD, 2); worker.runAll(); assertEquals(before, saved()); assertTrue(view().getMessage().contains("physical")); }
	@Test public void correctionCancelPreviewIsNonMutating() throws Exception { start(); BankRun before = saved(); assertEquals(-2, BankCorrections.desired(before, SHIELD, "-2", false)); BankCorrections.search(view(), "shield"); assertEquals(before, saved()); }
	@Test public void staleCorrectionCannotOverwriteAutomaticUpdate() throws Exception { start(); UUID stale = view().getRun().getRevision(); change(2); BankRun before = saved(); controller.correct(stale, SHIELD, 0); worker.runAll(); assertEquals(before, saved()); label("1"); }
	@Test public void uncommittedNewPhysicalDataRejectsCorrection() throws Exception { start(); BankRun before = saved(); source.quantity(2); controller.correct(before.getRevision(), SHIELD, 1); worker.runAll(); assertEquals(before, saved()); }
	@Test public void negativeBalanceSurvivesRestartAndNoChangeIsNotReplayed() throws Exception { start(); change(0); UUID runId = saved().getRunId(); controller.stop(); source.observer.reset(); launch(); settle(); label("-1"); assertEquals(runId, saved().getRunId()); BankRun before = saved(); source.open(false); settle(); source.open(true); settle(); assertEquals(before, saved()); }
	@Test public void offlineNetGainsApplyAutomaticallyAndSummaryRemainsAfterCommit() throws Exception { start(); controller.stop(); source.quantity(4); source.observer.reset(); launch(); settle(); label("3"); assertEquals(Long.valueOf(3), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); change(5); label("4"); assertEquals(Long.valueOf(3), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); }
	@Test public void offlineNetLossAutomaticallyAppliesWithoutReview() throws Exception { start(); controller.stop(); source.quantity(0); source.observer.reset(); launch(); settle(); label("-1"); assertEquals(Long.valueOf(-1), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); }
	@Test public void publishWaitsForDurableAcknowledgement() throws Exception { start(); source.quantity(2); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); assertFalse(worker.tasks.isEmpty()); label("0"); assertEquals(0, saved().balance(SHIELD)); worker.runAll(); label("1"); }
	@Test public void positiveItemsHaveAcceptedNormalAppearanceAndNegativeAreDim() { start(); assertNotEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); change(2); assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); change(0); assertNotEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); }
	@Test public void disableRemovesAllProjectionAndReenableRestoresSavedRun() { start(); controller.stop(); overlay.disable(); assertNull(overlay.getProjection()); overlay.enable(); source.observer.reset(); launch(); settle(); label("0"); }
	@Test public void verifiedIdentitySeparatesCharacters() throws Exception { start(); change(2); source.character = SnapshotFixtures.CHARACTER_B; source.observer.reset(); controller.tick(); worker.runAll(); settle(); assertNull(view().getRun()); assertEquals(1, repository.load(root, SnapshotFixtures.CHARACTER_A).get().balance(SHIELD)); }
	@Test public void placeholdersFillersAndVariantsUseExactIds() throws Exception { int variant = SHIELD + 1; source.items = new Item[]{new Item(SHIELD, 1), new Item(variant, 1), new Item(ItemID.BANK_FILLER, 1), new Item(ItemID.BLANKOBJECT, 1)}; source.placeholders.add(variant); source.observer.changed(); settle(); start(); assertEquals(Collections.singletonMap(SHIELD, 1L), saved().getPhysical()); source.placeholders.clear(); source.observer.changed(); settle(); assertEquals(1, saved().balance(variant)); assertEquals(0, saved().balance(SHIELD)); }
	@Test public void duplicateInitializationCannotReplaceOriginalRun() throws Exception { start(); BankRun before = saved(); controller.startRun(true,null,true); worker.runAll(); assertEquals(before, saved()); }
	@Test public void resetArchivesThenRequiresFreshAttestationAndBank() throws Exception { start(); controller.reset(saved().getRevision()); worker.runAll(); assertFalse(saved().isActive()); settle(); controller.startRun(false,null,true); worker.runAll(); assertFalse(saved().isActive()); controller.startRun(true,null,true); worker.runAll(); assertTrue(saved().isActive()); }
	@Test public void controllerStorageFailureDoesNotPublishOrAdvanceAnchor() throws Exception
	{
		start(); BankRun before = saved(); controller.stop(); worker = new Worker(); java.util.concurrent.atomic.AtomicBoolean fail = new java.util.concurrent.atomic.AtomicBoolean();
		controller = new BankController(source, repository, Runnable::run, v -> { views.add(v); overlay.updateBank(v); });
		controller.start(() -> { if (fail.get()) { throw new java.io.IOException("Synthetic unavailable storage"); } return root; }, worker); worker.runAll(); settle();
		before = saved(); fail.set(true); change(2); assertTrue(view().isError()); assertEquals(before, saved());assertEquals(before,controller.currentRun());assertNotNull(overlay.getProjection());assertEquals(before.balance(SHIELD),overlay.getProjection().balance(SHIELD,false));assertFalse(overlay.isPotionCurrent());
		fail.set(false); controller.retry(); worker.runAll(); settle(); label("1"); assertEquals(1, saved().balance(SHIELD));
	}
	@Test public void migrationRequiresOnlyOneBankAnchorThenNormalChangesAreAutomatic() throws Exception
	{
		com.fromscratch.model.RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 0); new RunRepository(SnapshotFixtures.codec()).activate(root, old);
		controller.retry(); worker.runAll(); settle(); assertFalse(view().getRun().isAnchored()); assertNull(overlay.getProjection());
		controller.anchor(view().getRun().getRevision(), source.readBank(), false); worker.runAll(); settle(); label("0");
		change(0); label("-1"); change(1); label("0"); controller.stop(); source.observer.reset(); launch(); settle(); label("0"); assertTrue(saved().isAnchored());
	}
	@Test public void migrationAnchorRejectsChangedBankPreview() throws Exception
	{
		new RunRepository(SnapshotFixtures.codec()).activate(root, SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 0)); controller.retry(); worker.runAll(); settle();
		BankView preview = view(); source.quantity(2); settle(); controller.anchor(preview.getRun().getRevision(), preview.getObservation().getBank(), false); worker.runAll(); assertFalse(saved().isAnchored());
	}
	@Test public void staleResetAfterCharacterSwitchCannotResetPreviousRun() throws Exception
	{
		start(); BankRun before = saved(); source.character = SnapshotFixtures.CHARACTER_B;
		controller.reset(before.getRevision()); worker.runAll(); assertEquals(before, repository.load(root, SnapshotFixtures.CHARACTER_A).get()); assertNull(view().getRun());
	}
	@Test public void pendingSaveCannotPublishIntoAnotherCharacter() throws Exception
	{
		start(); source.quantity(2); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick();
		source.character = SnapshotFixtures.CHARACTER_B; source.observer.reset(); controller.tick(); worker.runAll(); settle();
		assertNull(view().getRun()); assertNull(overlay.getProjection()); assertEquals(1, repository.load(root, SnapshotFixtures.CHARACTER_A).get().balance(SHIELD));
	}
}
