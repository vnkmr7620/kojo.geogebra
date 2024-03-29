/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.gui.inputbar;

import geogebra.gui.view.algebra.InputPanel;
import geogebra.main.Application;
import geogebra.util.LowerCaseDictionary;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Markus Hohenwarter
 */
public class AlgebraInput extends  JPanel implements ActionListener, KeyListener, MouseListener, FocusListener {
	private static final long serialVersionUID = 1L;
	
	private Application app;

	private JLabel inputLabel, helpIcon;
 	
	private MyComboBox cmdCB; // for command list
	
	// autocompletion text field
	private AutoCompleteTextField inputField;	

	/**
	 * creates new AlgebraInput
	 */
	public AlgebraInput(Application app) {		
		this.app = app;		
		 		
		initGUI();		
	}
	
	public void initGUI() {
		removeAll();
		helpIcon = new JLabel(app.getImageIcon("help.png")); 
		helpIcon.addMouseListener(this);
		inputLabel = new JLabel(); 
		inputLabel.addMouseListener(this);

		InputPanel inputPanel = new InputPanel(null, app, 30, true);
		inputField = (AutoCompleteTextField) inputPanel.getTextComponent();		
		
		// set up input field		
		inputField.setEditable(true);						
		inputField.addKeyListener(this);
		
		// set up command combo box
		cmdCB = new MyComboBox();
		if (app.showCmdList()) {
			//cmdCB.setMaximumSize(new Dimension(100, 200));
			// set to approx half screen height
			//cmdCB.setMaximumRowCount(app.getScreenSize().height/app.getFontSize()/3);
			cmdCB.addActionListener(this);
		}
		
		updateFonts();
				
		// add to panel				 		
		setLayout(new BorderLayout(2, 2));	
		JPanel iconLabelPanel = new JPanel();
		iconLabelPanel.add(helpIcon);
		iconLabelPanel.add(inputLabel);
		add(iconLabelPanel, BorderLayout.WEST);   
		add(inputPanel, BorderLayout.CENTER);
		if (app.showCmdList()) {
			JPanel p = new JPanel(new BorderLayout(5,5));
			p.add(cmdCB, BorderLayout.CENTER);		
			add(p, BorderLayout.EAST);
		}
		
//		setBorder(BorderFactory.createCompoundBorder(
//				   BorderFactory.createEtchedBorder(),  
//				   BorderFactory.createEmptyBorder(2,2,2,2) )
//			   );   

		setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		
		setLabels();
		inputField.addFocusListener(this);
	}
	
	public boolean requestFocusInWindow() { 
		return inputField.requestFocusInWindow();
	}
	
	public void requestFocus() {
		requestFocusInWindow();
	}
	
	public boolean hasFocus() {
		return inputField.hasFocus();
	}
	
	public void clear() {
		inputField.setText(null);
	}		
	
	public AutoCompleteTextField getTextField() {
		return inputField;
	}
	
	/**
	 * updates labesl according to current locale
	 */
	public void setLabels() {
		inputLabel.setText( app.getPlain("InputLabel") + ":");	
		inputLabel.setToolTipText(app.getMenu("Help"));
		helpIcon.setToolTipText(app.getMenu("Help"));
		setCommandNames();				
	}	
	
	public void updateFonts() {
		inputField.setFont(app.getBoldFont());		
		inputLabel.setFont(app.getPlainFont());
		if (app.showCmdList()) {	
			cmdCB.setFont(app.getPlainFont());
			// set to approx half screen height
			cmdCB.setMaximumRowCount(app.getScreenSize().height/app.getFontSize()/3);
		}
	}    
	
//	/**
//	 * Inserts string at current position of the input textfield and gives focus
//	 * to the input textfield.
//	 * @param str: inserted string
//	 */
//	public void insertString(String str) {
//		inputField.replaceSelection(str);
//	}
	
	/**
	 * Sets the content of the input textfield and gives focus
	 * to the input textfield.
	 */
	public void replaceString(String str) {
		inputField.setText(str);
	}

	
	// see actionPerformed
	private void insertCommand(String cmd) {
		if (cmd == null) return;
		
		int pos = inputField.getCaretPosition();
		String oldText = inputField.getText();
		String newText = 
			oldText.substring(0, pos) + 
			cmd + "[]" +
			oldText.substring(pos);			 			
		
		inputField.setText(newText);
		inputField.setCaretPosition(pos + cmd.length() + 1);		
		inputField.requestFocus();
	}
	

	
	
	/**
	 * fill command list with command names of current locale
	 */	
	public void setCommandNames() {	
		app.initTranslatedCommands();
		LowerCaseDictionary dict = app.getCommandDictionary();
		if (dict == null) return;
		
		ActionListener [] listeners = cmdCB.getActionListeners();
		for (int i=0; i < listeners.length; i++) 
			cmdCB.removeActionListener(listeners[i]);
		
		if (cmdCB.getItemCount() > 0) cmdCB.removeAllItems();	
		String commandString = app.getCommand("Command") + " ...";
		cmdCB.addItem(commandString);	
		
		Iterator it = dict.getLowerCaseIterator();
		while (it.hasNext()) {
			// combobox
			String cmdName = (String) dict.get(it.next());
			if (cmdName != null && cmdName.length() > 0)
				cmdCB.addItem(cmdName);
		}	 
		
		// set width of combo box to fit "Command ..."s
		cmdCB.setPrototypeDisplayValue(commandString+"Wa");
				
		for (int i=0; i < listeners.length; i++) 
			cmdCB.addActionListener(listeners[i]);
	}
	

	
   
	/**
	* action listener implementation for command combobox
	*/
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
	
		// command combobox
		if (source == cmdCB) { 			
			if (cmdCB.getSelectedIndex() != 0) { // not title
				insertCommand((String) cmdCB.getSelectedItem());				
				//cmdCB.setSelectedIndex(0);
			}					
		}			
	}
	    	
	public void keyPressed(KeyEvent e) {
		// the input field may have consumed this event
		// for auto completion
		if (e.isConsumed()) return;

		int keyCode = e.getKeyCode();    
		if (keyCode == KeyEvent.VK_ENTER) {	
			  String input = inputField.getText();					   
			  if (input == null || input.length() == 0)
			  {
				  app.getEuclidianView().requestFocus(); // Michael Borcherds 2008-05-12
				  return;
			  }
			  
			  app.getGuiManager().setScrollToShow(true);
			  
			  boolean success = null != 
				 app.getKernel().getAlgebraProcessor().processAlgebraCommand( input, true );				
			  
			  app.getGuiManager().setScrollToShow(false);

			  if (success) {						   
				 inputField.addToHistory(input);
				inputField.setText(null);  							  			   
			  }			  
		}
	}

	public void keyReleased(KeyEvent e) {

	}


	public void keyTyped(KeyEvent e) {	
	}
	
	public void mouseClicked(MouseEvent e) {
		
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent e) {	
		Object src = e.getSource();
				
		// click on help icon: open input bar help dialog
		if (src == helpIcon || src == inputLabel) {
			app.showHelp(app.getPlain("InputFieldHelp"));
		}
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void focusGained(FocusEvent arg0) {
		app.clearSelectedGeos();
	}

	public void focusLost(FocusEvent arg0) {
	
	}	 

}