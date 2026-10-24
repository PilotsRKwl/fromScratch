package com.fromscratch.bank;

import java.util.UUID;
import lombok.Value;

/** Runtime-only, client-thread-owned setup evidence. Never a second progression save. */
@Value
public class BankSetup
{
	String character;
	UUID attempt;
	boolean attested;
	BankObservation bank;
	boolean bankCurrent;
	Boolean storageAnswer;
	Boolean storageUnlocked;
	public static BankSetup begin(String character,UUID attempt){return new BankSetup(character,attempt,false,null,false,null,null);}
	public BankSetup attest(boolean value){return new BankSetup(character,attempt,value,bank,bankCurrent,storageAnswer,storageUnlocked);}
	public BankSetup answer(Boolean value){return new BankSetup(character,attempt,attested,bank,bankCurrent,value,storageUnlocked);}
	public BankSetup capture(BankObservation value){return new BankSetup(character,attempt,attested,value,value!=null&&value.isReady(),storageAnswer,storageUnlocked);}
	public BankSetup current(boolean value){return new BankSetup(character,attempt,attested,bank,value,storageAnswer,storageUnlocked);}
	public BankSetup unlocked(boolean value){return new BankSetup(character,attempt,attested,bank,bankCurrent,storageAnswer,value);}
}
