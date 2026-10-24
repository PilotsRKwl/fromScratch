package com.fromscratch.bank;

import java.util.*;
import lombok.Value;

/** Immutable Swing/render projection; only committed runs cross the storage boundary. */
@Value
public class BankView
{
	BankRun run;
	BankObservation observation;
	Map<Integer, String> names;
	boolean busy;
	boolean error;
	String message;
	BankTimings.Trace trace;
	com.fromscratch.wealth.BankValuation valuation;
	com.fromscratch.potion.PotionObservation potions;
	com.fromscratch.potion.PotionCapture capture;
	boolean potionUnlocked;
	boolean potionUnlockKnown;
	BankSetup setup;
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message)
	{ this(run, observation, names, busy, error, message, null); }
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message, BankTimings.Trace trace)
	{ this(run,observation,names,busy,error,message,trace,null); }
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message, BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation)
	{this(run,observation,names,busy,error,message,trace,valuation,com.fromscratch.potion.PotionObservation.unavailableStore());}
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message, BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation,com.fromscratch.potion.PotionObservation potions)
	{this(run,observation,names,busy,error,message,trace,valuation,potions,null);}
	public BankView(BankRun run,BankObservation observation,Map<Integer,String> names,boolean busy,boolean error,String message,BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation,com.fromscratch.potion.PotionObservation potions,com.fromscratch.potion.PotionCapture capture)
	{this(run,observation,names,busy,error,message,trace,valuation,potions,capture,!potions.getTypes().isEmpty());}
	public BankView(BankRun run,BankObservation observation,Map<Integer,String> names,boolean busy,boolean error,String message,BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation,com.fromscratch.potion.PotionObservation potions,com.fromscratch.potion.PotionCapture capture,boolean potionUnlocked)
	{this(run,observation,names,busy,error,message,trace,valuation,potions,capture,potionUnlocked,potionUnlocked||potions.isVerified()||capture!=null);}
	public BankView(BankRun run,BankObservation observation,Map<Integer,String> names,boolean busy,boolean error,String message,BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation,com.fromscratch.potion.PotionObservation potions,com.fromscratch.potion.PotionCapture capture,boolean potionUnlocked,boolean potionUnlockKnown)
	{this(run,observation,names,busy,error,message,trace,valuation,potions,capture,potionUnlocked,potionUnlockKnown,null);}
	public BankView(BankRun run,BankObservation observation,Map<Integer,String> names,boolean busy,boolean error,String message,BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation,com.fromscratch.potion.PotionObservation potions,com.fromscratch.potion.PotionCapture capture,boolean potionUnlocked,boolean potionUnlockKnown,BankSetup setup)
	{ this.run = run; this.observation = observation; this.names = Collections.unmodifiableMap(new TreeMap<>(names)); this.busy = busy; this.error = error; this.message = message; this.trace = trace; this.valuation=valuation;this.potions=potions;this.capture=capture;this.potionUnlocked=potionUnlocked;this.potionUnlockKnown=potionUnlockKnown;this.setup=setup; }
	public boolean setupBankReady(){return !busy&&!error&&(setup==null?observation!=null&&observation.isReady():setup.getBank()!=null&&setup.isBankCurrent());}
	public boolean ready() { return !busy && !error && observation != null && observation.isReady(); }
	public String name(int id) { return names.getOrDefault(id, "Item #" + id); }
}
