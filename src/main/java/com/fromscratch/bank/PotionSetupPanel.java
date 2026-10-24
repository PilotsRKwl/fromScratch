package com.fromscratch.bank;

import com.fromscratch.potion.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Explicit setup consent bound to the exact native capture displayed for review. */
final class PotionSetupPanel extends JPanel
{
	private final JLabel status=copy("");
	private final JLabel step=copy("4. Open Potion Storage.");
	private final JCheckBox locked=new BankAttestation("<html>My character has not<br>unlocked Potion Storage.</html>","My character has not unlocked Potion Storage.");
	private final JButton review=new JButton("Review Captured Potions");
	private PotionCapture capture;
	private BankView view;
	private BankActions actions;
	private boolean ready,notUnlocked;
	PotionSetupPanel(Runnable changed)
	{
		setName("potionSetup");setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setAlignmentX(LEFT_ALIGNMENT);
		step.setName("potionSetupStep");step.setBorder(BorderFactory.createEmptyBorder(6,0,4,0));add(step);add(status);review.setAlignmentX(LEFT_ALIGNMENT);add(review);
		locked.setName("potionNotUnlocked");add(locked);
		locked.addActionListener(e->{if(actions!=null&&view!=null&&view.getSetup()!=null){actions.setupStorageAnswer(view.getSetup().getAttempt(),locked.isSelected()?Boolean.FALSE:null);}notUnlocked=locked.isSelected();ready=notUnlocked||capture!=null;changed.run();});
		review.addActionListener(e->{if(capture!=null){showReview(this,capture);}});
	}
	void render(BankView view){render(view,null);}
	void render(BankView view,BankActions actions)
	{
		this.view=view;this.actions=actions;capture=view.getCapture();
		boolean bank=view.getSetup()==null?view.getObservation()!=null&&view.getObservation().isReady():view.getSetup().getBank()!=null;
		boolean unlocked=capture!=null||view.isPotionUnlocked()||view.getSetup()!=null&&Boolean.TRUE.equals(view.getSetup().getStorageUnlocked())||!view.getPotions().getTypes().isEmpty();
		boolean unknown=!unlocked&&!view.isPotionUnlockKnown()&&(view.getSetup()==null||view.getSetup().getStorageUnlocked()==null);
		if(view.getSetup()!=null){locked.setSelected(Boolean.FALSE.equals(view.getSetup().getStorageAnswer()));}
		if(!unknown){locked.setSelected(false);}notUnlocked=!unlocked&&(!unknown||locked.isSelected());
		ready=bank&&(capture!=null||notUnlocked);setVisible(bank&&(unlocked||unknown));
		locked.setVisible(unknown);locked.setEnabled(unknown&&!view.isBusy()&&!view.isError());
		review.setVisible(capture!=null);review.setEnabled(capture!=null&&!view.isBusy()&&!view.isError());status.setVisible(capture!=null);
		step.setText(markup(capture==null?"4. Open Potion Storage.":"4. Potion Storage recorded."));
		if(capture!=null)
		{
			long doses=0,items=0;int stocked=0;for(PotionType t:capture.getObservation().getTypes().values()){long n=capture.getObservation().getQuantities().getOrDefault(t.getId(),0L);if(n>0){stocked++;}if(t.unit().equals("items")){items=Math.addExact(items,n);}else{doses=Math.addExact(doses,n);}}
			status.setText(markup(String.format(Locale.ENGLISH,"%,d stocked types<br>%,d doses and %,d items<br>All %,d types checked, including zero stock.",stocked,doses,items,capture.getObservation().getTypes().size())));
		}
		revalidate();
	}
	boolean ready(){return ready;}
	PotionCapture selectedCapture(){return capture;}
	boolean notUnlocked(){return notUnlocked;}
	@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
	private static String markup(String text){return "<html><div style='width:140pt'>"+text+"</div></html>";}
	private static JLabel copy(String text)
	{
		JLabel label=new JLabel(markup(text)){@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}@Override public Dimension getMinimumSize(){return getPreferredSize();}};
		label.setFont(new Font(Font.DIALOG,Font.PLAIN,11));label.setForeground(new Color(216,205,181));label.setAlignmentX(LEFT_ALIGNMENT);label.setBorder(BorderFactory.createEmptyBorder(3,0,5,0));return label;
	}
	static String localTime(java.time.Instant time){return DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a",Locale.ENGLISH).withZone(ZoneId.systemDefault()).format(time);}
	static DefaultTableModel capturedRows(PotionCapture capture)
	{
		DefaultTableModel rows=new DefaultTableModel(new String[]{"Potion","Stored","Units"},0){@Override public boolean isCellEditable(int row,int col){return false;}};
		capture.getObservation().getTypes().values().forEach(t->rows.addRow(new Object[]{t.getName(),String.format(Locale.ENGLISH,"%,d",capture.getObservation().getQuantities().getOrDefault(t.getId(),0L)),t.unit()}));return rows;
	}
	private static void showReview(Component owner,PotionCapture capture)
	{
		JPanel content=new JPanel(new BorderLayout(5,8));content.add(new JLabel(capture.getObservation().getTypes().size()+" types checked. Recorded "+localTime(capture.getObservation().getAt())),BorderLayout.NORTH);content.add(new JScrollPane(new JTable(capturedRows(capture))),BorderLayout.CENTER);
		JButton close=new JButton("Close");JPanel buttons=new JPanel();buttons.add(close);JDialog dialog=BankDialogs.dialog(owner,"Captured Potion Storage",content,buttons,new Dimension(580,390));close.addActionListener(e->dialog.dispose());dialog.setVisible(true);
	}
}
