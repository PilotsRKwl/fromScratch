package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import com.fromscratch.SnapshotFixtures;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Every reviewed ID is exercised through the actual bank engine, not a stand-in entitlement model. */
@RunWith(Parameterized.class)
public class SelectedVariantTest
{
	@Parameterized.Parameters(name="item={0}") public static Collection<Object[]> variants(){List<Object[]> rows=new ArrayList<>();for(String entry:new String[]{"slayer_gear","elite_black_helm","elite_black_body","elite_black_legs"}){for(int id:SelectedBuybacks.items(entry)){rows.add(new Object[]{id});}}return rows;}
	private final int id;
	public SelectedVariantTest(int id){this.id=id;}
	@Test public void onePaymentUnlocksOneExactCopyWithUnchangedPhysicalAndRawBalances()
	{
		String catalog=SelectedBuybacks.catalog(id);long price=BuybackCatalog.entry(catalog).getPrice();BankRun before=BuybackModelTest.funded(price*3,id,id,id);
		BankRun after=BankEngine.buySelected(before,catalog,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);
		assertEquals(1L,(long)BuybackEngine.balances(after).get(id));assertEquals(2,BuybackEngine.remaining(after,id));assertEquals(before.getPhysical(),after.getPhysical());assertEquals(before.getBalances(),after.getBalances());assertEquals(before.getInitialBank(),after.getInitialBank());assertEquals(before.getFinancial(),after.getFinancial());assertEquals(price*2,BuybackEngine.available(after));assertEquals(id,after.getBuybacks().getPurchases().get(0).getSelectedItemId());
		after=BankEngine.buySelected(after,catalog,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals(2L,(long)BuybackEngine.balances(after).get(id));assertEquals(1,BuybackEngine.remaining(after,id));
		after=BankEngine.buySelected(after,catalog,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals(3L,(long)BuybackEngine.balances(after).get(id));assertEquals(0,BuybackEngine.available(after));assertEquals(0,BuybackEngine.remaining(after,id));assertEquals(3,after.getBuybacks().getPurchases().size());
	}
	@Test public void insufficientFundsAndAlreadyEarnedCopiesCannotBePurchased()
	{
		String catalog=SelectedBuybacks.catalog(id);long price=BuybackCatalog.entry(catalog).getPrice();BankRun poor=BuybackModelTest.funded(price-1,id);
		try{BankEngine.buySelected(poor,catalog,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);fail();}catch(IllegalArgumentException expected){}
		BankRun natural=BuybackModelTest.funded(price);Map<Integer,Long> bank=new TreeMap<>(natural.getPhysical());bank.put(id,1L);natural=BankEngine.observe(natural,new com.fromscratch.model.ContainerSnapshot(bank,bank.size()),UUID.randomUUID(),SnapshotFixtures.TIME);
		assertEquals(1L,(long)BuybackEngine.balances(natural).get(id));assertEquals(0,BuybackEngine.remaining(natural,id));try{BankEngine.buySelected(natural,catalog,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);fail();}catch(IllegalArgumentException expected){}
	}
}
