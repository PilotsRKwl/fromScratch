package com.fromscratch.bank;

import com.fromscratch.potion.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** One revision-bound correction entry point for bank items and normalized stored potion units. */
public final class BankCorrectionDialog
{
	public static final String PURPOSE="Correct an incorrectly recorded virtual balance.";
	private BankCorrectionDialog() { }
	public static void show(Component owner,BankView captured,BankActions actions){show(owner,captured,actions,new BankIcons(id->null));}
	public static void show(Component owner,BankView captured,BankActions actions,BankIcons icons)
	{
		Editor editor=new Editor(captured,icons);JButton save=new JButton("Save Correction"),cancel=new JButton("Cancel"),review=new JButton("Review Unresolved Storage");JPanel buttons=footer(review,cancel,save);
		JDialog dialog=BankDialogs.dialog(owner,"Manual Corrections",editor,buttons,new Dimension(940,660));
		editor.heading.setFont(editor.heading.getFont().deriveFont(Font.BOLD,16f));editor.selectedName.setFont(editor.selectedName.getFont().deriveFont(Font.BOLD));
		dialog.setMinimumSize(new Dimension(Math.min(640,dialog.getWidth()),Math.min(620,dialog.getHeight())));
		editor.changed=()->{save.setEnabled(editor.valid());review.setVisible(editor.storage()&&!captured.getRun().getPotions().isReviewed());review.setEnabled(captured.getRun().getPotions().getObservedAt()!=null&&!captured.isBusy()&&!captured.isError());};editor.update();
		save.addActionListener(e->{Integer id=editor.list.getSelectedValue();if(id==null||!editor.valid()){return;}long desired=editor.desired(id);PotionType type=captured.getRun().getPotions().getTypes().get(id);String name=editor.storage()?type.getName():captured.name(id);long before=editor.storage()?captured.getRun().getPotions().balance(id):captured.getRun().balance(id);
			if(BankDialogs.confirm(dialog,"Confirm Correction",name+": "+number(before)+" to "+number(desired)+(editor.storage()?" "+type.unit():" items")+". This adjusts virtual ownership only and is recorded in history. The physical game quantity stays unchanged.","Save Correction",false))
			{save.setEnabled(false);if(editor.storage()){actions.correctStorage(captured.getRun().getRevision(),id,desired);}else{actions.correct(captured.getRun().getRevision(),id,desired);}dialog.dispose();}});
		review.addActionListener(e->review(dialog,captured,actions));cancel.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(save);dialog.setVisible(true);
	}
	static String number(long n){return String.format(Locale.ENGLISH,"%,d",n);}
	static JPanel footer(JButton review,JButton cancel,JButton save)
	{JPanel buttons=new JPanel(new BorderLayout(8,0)),commands=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));buttons.add(review,BorderLayout.WEST);commands.add(cancel);commands.add(save);buttons.add(commands,BorderLayout.EAST);return buttons;}
	static java.util.List<Integer> storageSearch(BankView view,String query)
	{
		String find=query.trim().toLowerCase(Locale.ROOT);java.util.List<Integer> result=new ArrayList<>();view.getRun().getPotions().getTypes().values().stream().filter(t->t.getName().toLowerCase(Locale.ROOT).contains(find)).sorted(Comparator.comparing(PotionType::getName)).forEach(t->result.add(t.getId()));return result;
	}
	static long desiredStorage(BankRun run,int id,String input,boolean adjustment)
	{
		if(!run.getPotions().getTypes().containsKey(id)){throw new IllegalArgumentException("Select a known potion.");}
		long q=Long.parseLong(input.replace(",","").trim());if(adjustment){q=Math.addExact(run.getPotions().balance(id),q);}
		if(q==Long.MIN_VALUE||q>run.getPotions().getPhysical().getOrDefault(id,0L)){throw new IllegalArgumentException("Virtual units cannot exceed last verified physical stock.");}Math.subtractExact(q,run.getPotions().balance(id));return q;
	}
	static final class Editor extends JPanel
	{
		final JComboBox<String> category=new JComboBox<>(new String[]{"Bank Items","Potion Storage"});
		final JTextField search=new JTextField(20),input=new JTextField("0",14);
		final JCheckBox adjustment=new JCheckBox("Apply signed adjustment");
		final DefaultListModel<Integer> rows=new DefaultListModel<>();final JList<Integer> list=new JList<>(rows);
		final JLabel physical=new JLabel("Select an item"),balance=new JLabel(" "),preview=new JLabel(" "),status=new JLabel(" "),selectedName=new JLabel("Select an item"),selectedIcon=new JLabel();
		final JLabel heading=new JLabel("Manual Corrections");
		final JScrollPane selection=new JScrollPane(list);
		private final JPanel body=new JPanel(new GridBagLayout()),details=new JPanel(new BorderLayout())
		{
			@Override public Dimension getPreferredSize(){Dimension natural=super.getPreferredSize();return new Dimension(300,natural.height);}
			@Override public Dimension getMinimumSize(){return new Dimension(245,getPreferredSize().height);}
		};
		private final BankView view;private final BankIcons icons;private Boolean stacked;Runnable changed=()->{};
		Editor(BankView view,BankIcons icons)
		{
			super(new BorderLayout(0,12));this.view=view;this.icons=icons;setName("manualCorrections");category.setName("correctionCategory");input.setName("correctionAmount");selection.setName("correctionItemSelection");details.setName("correctionDetails");
			JPanel top=new JPanel(new BorderLayout(0,8)),copy=new JPanel(new GridLayout(0,1,0,3));heading.setFont(heading.getFont().deriveFont(Font.BOLD,16f));copy.add(heading);copy.add(new JLabel(PURPOSE));top.add(copy,BorderLayout.NORTH);
			JPanel toolbar=new JPanel(new BorderLayout(8,0)),searchBox=new JPanel(new BorderLayout(5,0));searchBox.add(new JLabel("Search"),BorderLayout.WEST);searchBox.add(search,BorderLayout.CENTER);search.setToolTipText("Search items by name");search.getAccessibleContext().setAccessibleName("Search items");toolbar.add(category,BorderLayout.WEST);toolbar.add(searchBox,BorderLayout.CENTER);top.add(toolbar,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
			list.setCellRenderer(new ItemRenderer());list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setVisibleRowCount(12);rowHeight();list.addPropertyChangeListener("font",e->rowHeight());selection.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);selection.setPreferredSize(new Dimension(500,440));selection.setMinimumSize(new Dimension(200,120));
			JPanel item=new JPanel(new BorderLayout(8,0));item.add(selectedIcon,BorderLayout.WEST);item.add(selectedName,BorderLayout.CENTER);selectedName.setFont(selectedName.getFont().deriveFont(Font.BOLD));
			JPanel fields=new JPanel(new GridBagLayout());int row=0;for(JComponent c:new JComponent[]{item,physical,balance,status,new JLabel("Correct virtual balance to:"),input,adjustment,new JLabel("<html>Checked: add this signed amount.<br>Unchecked: set the final balance.</html>"),preview}){GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=row++;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.NORTHWEST;g.insets=new Insets(0,0,c==status?14:6,0);fields.add(c,g);}
			details.add(fields,BorderLayout.NORTH);add(body,BorderLayout.CENTER);columns(false);
			category.addActionListener(e->{list.clearSelection();input.setText("0");adjustment.setSelected(false);filter();});search.getDocument().addDocumentListener(BankDialogs.changes(this::filter));input.getDocument().addDocumentListener(BankDialogs.changes(this::update));adjustment.addActionListener(e->update());list.addListSelectionListener(e->update());filter();
		}
		private void rowHeight(){list.setFixedCellHeight(Math.max(38,list.getFontMetrics(list.getFont()).getHeight()+12));}
		private void columns(boolean small)
		{
			if(stacked!=null&&stacked==small){return;}stacked=small;body.removeAll();GridBagConstraints left=new GridBagConstraints();left.gridx=0;left.gridy=0;left.weightx=small?1:0.6;left.weighty=1;left.fill=GridBagConstraints.BOTH;left.insets=new Insets(0,0,small?10:0,small?0:14);body.add(selection,left);GridBagConstraints right=new GridBagConstraints();right.gridx=small?0:1;right.gridy=small?1:0;right.weightx=small?1:0.4;right.weighty=small?0:1;right.fill=GridBagConstraints.BOTH;right.anchor=GridBagConstraints.NORTHWEST;body.add(details,right);
		}
		@Override public void doLayout(){columns(getWidth()<700);super.doLayout();}
		private final class ItemRenderer extends JPanel implements ListCellRenderer<Integer>
		{
			private final JLabel sprite=new JLabel(),name=new JLabel();ItemRenderer(){super(new BorderLayout(7,0));setBorder(BorderFactory.createEmptyBorder(2,5,2,5));add(sprite,BorderLayout.WEST);add(name,BorderLayout.CENTER);}
			@Override public void validate(){doLayout();}
			public Component getListCellRendererComponent(JList<? extends Integer> l,Integer id,int index,boolean selected,boolean focus){PotionType t=storage()?view.getRun().getPotions().getTypes().get(id):null;icons.apply(sprite,t==null?id:t.displayId());name.setText(t==null?view.name(id):t.getName());name.setFont(l.getFont());name.setForeground(selected?l.getSelectionForeground():l.getForeground());setBackground(selected?l.getSelectionBackground():l.getBackground());return this;}
		}
		boolean storage(){return category.getSelectedIndex()==1;}
		long desired(int id){return storage()?desiredStorage(view.getRun(),id,input.getText(),adjustment.isSelected()):BankCorrections.desired(view.getRun(),id,input.getText(),adjustment.isSelected());}
		boolean valid(){Integer id=list.getSelectedValue();if(id==null||view.isBusy()||view.isError()){return false;}try{desired(id);return storage()?view.getRun().getPotions().isReviewed()&&view.getRun().getPotions().getObservedAt()!=null:view.ready();}catch(RuntimeException e){return false;}}
		void filter(){Integer selected=list.getSelectedValue();rows.clear();(storage()?storageSearch(view,search.getText()):BankCorrections.search(view,search.getText())).forEach(rows::addElement);if(selected!=null){list.setSelectedValue(selected,true);}update();}
		void update()
		{
			PotionState s=view.getRun().getPotions();boolean current=view.getPotions().isVerified()&&s.getPhysical().equals(view.getPotions().getQuantities());
			status.setText(storage()?s.getTypes().isEmpty()?"<html>No recorded storage types.<br>Open Potion Storage if unlocked.</html>":s.getObservedAt()==null?"<html>Potion Storage unverified.<br>Open it before correcting.</html>":"<html>Potion Storage last verified:<br>"+PotionSetupPanel.localTime(s.getObservedAt())+(!current?"<br>Cached observation":"")+(!s.isReviewed()?"<br>Unresolved stock. Review first.":"")+"</html>":view.ready()?"Current bank observation.":"<html>Last recorded bank quantity.<br>Open your bank before saving.</html>");
			Integer id=list.getSelectedValue();selectedName.setText("Select an item");selectedIcon.setIcon(null);physical.setText("Physical quantity: ");balance.setText("Virtual quantity: ");preview.setText(" ");if(id!=null){PotionType t=storage()?s.getTypes().get(id):null;String unit=t==null?"items":t.unit(),name=t==null?view.name(id):t.getName();selectedName.setText(name);selectedName.setToolTipText(name);icons.apply(selectedIcon,t==null?id:t.displayId());long n=storage()?s.balance(id):view.getRun().balance(id);physical.setText(storage()&&s.getObservedAt()==null?"Physical quantity: Unverified":"Physical quantity: "+number(storage()?s.getPhysical().getOrDefault(id,0L):view.getRun().physical(id))+" "+unit);balance.setText((storage()&&!s.isReviewed()?"Unresolved virtual: ":"Virtual quantity: ")+number(n)+" "+unit+(n<0?" (outstanding)":""));balance.setForeground(n<0?new Color(255,179,104):list.getForeground());try{preview.setText("<html>Preview: "+number(n)+" to "+number(desired(id))+" "+unit+"</html>");}catch(RuntimeException e){preview.setText("<html>Enter a signed amount within<br>verified physical stock.</html>");}}changed.run();
		}
	}
	private static void review(Component owner,BankView view,BankActions actions)
	{
		PotionState state=view.getRun().getPotions();java.util.List<Integer> ids=new ArrayList<>(state.getTypes().keySet());DefaultTableModel model=new DefaultTableModel(new String[]{"Potion","Physical","Units","Virtual"},0){@Override public boolean isCellEditable(int r,int c){return c==3;}};
		for(int id:ids){PotionType t=state.getTypes().get(id);model.addRow(new Object[]{t.getName(),number(state.getPhysical().getOrDefault(id,0L)),t.unit(),Math.min(state.balance(id),state.getPhysical().getOrDefault(id,0L))});}
		JTable table=new JTable(model);JPanel content=new JPanel(new BorderLayout(5,8));content.add(BankDialogs.text("Confirm ownership earned during this challenge. Zero treats stock as legacy. Unknown past transfers cannot be inferred. The physical amounts are from the last verified observation, which may be cached. This audited review retains the original snapshot. Review uncertain bank bottles separately under Bank Items.",60),BorderLayout.NORTH);content.add(new JScrollPane(table),BorderLayout.CENTER);JLabel error=new JLabel();content.add(error,BorderLayout.SOUTH);
		JButton confirm=new JButton("Confirm Reviewed Balances"),cancel=new JButton("Cancel");JPanel buttons=new JPanel();buttons.add(cancel);buttons.add(confirm);JDialog dialog=BankDialogs.dialog(owner,"Review Unresolved Potion Storage",content,buttons,new Dimension(650,400));cancel.addActionListener(e->dialog.dispose());confirm.addActionListener(e->{try{if(table.isEditing()&&!table.getCellEditor().stopCellEditing()){return;}Map<Integer,Long> values=new TreeMap<>();for(int i=0;i<ids.size();i++){values.put(ids.get(i),desiredStorage(view.getRun(),ids.get(i),model.getValueAt(i,3).toString(),false));}if(BankDialogs.confirm(dialog,"Confirm Storage Review","Replace unresolved virtual storage amounts with these reviewed values? This is recorded in history.","Confirm Review",false)){actions.reviewStorage(view.getRun().getRevision(),values);dialog.dispose();if(owner instanceof JDialog){((JDialog)owner).dispose();}}}catch(RuntimeException invalid){error.setText("Enter signed units within the verified physical amounts.");}});dialog.setVisible(true);
	}
}
