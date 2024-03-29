/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.gui;

import geogebra.euclidian.EuclidianView;
import geogebra.gui.inputbar.AutoCompleteTextField;
import geogebra.gui.util.SpringUtilities;
import geogebra.gui.view.algebra.AlgebraView;
import geogebra.gui.view.algebra.InputPanel;
import geogebra.kernel.AbsoluteScreenLocateable;
import geogebra.kernel.AlgoSlope;
import geogebra.kernel.CircularDefinitionException;
import geogebra.kernel.GeoAngle;
import geogebra.kernel.GeoBoolean;
import geogebra.kernel.GeoConic;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoImage;
import geogebra.kernel.GeoLine;
import geogebra.kernel.GeoList;
import geogebra.kernel.GeoNumeric;
import geogebra.kernel.GeoPoint;
import geogebra.kernel.GeoSegment;
import geogebra.kernel.GeoText;
import geogebra.kernel.GeoVec3D;
import geogebra.kernel.GeoVector;
import geogebra.kernel.Kernel;
import geogebra.kernel.LimitedPath;
import geogebra.kernel.Locateable;
import geogebra.kernel.PointProperties;
import geogebra.kernel.TextProperties;
import geogebra.kernel.Traceable;
import geogebra.kernel.arithmetic.ExpressionNode;
import geogebra.kernel.arithmetic.NumberValue;
import geogebra.main.Application;
import geogebra.main.GeoElementSelectionListener;
import geogebra.main.View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/**
 * @author Markus Hohenwarter
 */
public class PropertiesDialogGeoElement
	extends JDialog
	implements
		WindowListener,
		WindowFocusListener,
		TreeSelectionListener,
		KeyListener,
		GeoElementSelectionListener {
			
	//private static final int MAX_OBJECTS_IN_TREE = 500;
	private static final int MAX_GEOS_FOR_EXPAND_ALL = 15;
	private static final int MAX_COMBOBOX_ENTRIES = 200;	
	
	private static final long serialVersionUID = 1L;
	private Application app;
	private Kernel kernel;
	private JTreeGeoElements geoTree;
	private JButton closeButton;
	private PropertiesPanel propPanel;
	private JColorChooser colChooser;

	final static int TEXT_FIELD_FRACTION_DIGITS = 3;
	final static int SLIDER_MAX_WIDTH = 170;
	
	final private static int MIN_WIDTH = 500;
	final private static int MIN_HEIGHT = 300;

	// double-click on slider -> open properties at slider tab
	public void showSliderTab() {
		if (propPanel != null)
			propPanel.showSliderTab();
	}

	/**
	 * Creates new PropertiesDialog.
	 * @param app: parent frame
	 */
	public PropertiesDialogGeoElement(Application app) {
		super(app.getFrame(), false);
		this.app = app;
		kernel = app.getKernel();	

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setResizable(true);

		addWindowListener(this);		
		geoTree = new JTreeGeoElements();	
		geoTree.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				// some textfields are updated when they lose focus
				// give them a chance to do that before we change the selection
				requestFocusInWindow();
			}
		});
		geoTree.addTreeSelectionListener(this);
		geoTree.addKeyListener(this);
				
		// build GUI
		initGUI();		
	}

	/**
	 * inits GUI with labels of current language	 
	 */
	public void initGUI() {
		setTitle(app.getPlain("Properties"));
		geoTree.root.setUserObject(app.getPlain("Objects"));
		geoTree.setFont(app.getPlainFont());			
		
		boolean wasShowing = isShowing();
		if (wasShowing) {
			setVisible(false);
		}
		
		
		//	LIST PANEL
		JPanel listPanel = new JPanel();
		//listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setLayout(new BorderLayout(5, 2));
		// JList with GeoElements		
		
		JScrollPane listScroller = new JScrollPane(geoTree);
		//geoTree.setMinimumSize(new Dimension(150, 200));		
		listPanel.add(listScroller, BorderLayout.CENTER);

		// rename, redefine and delete button
		//int pixelX = 20;
		//int pixelY = 10;
		/*
		MySmallJButton renameButton = new MySmallJButton(app.getImageIcon("rename.png"), pixelX, pixelY);
		renameButton.setToolTipText(app.getPlain("Rename"));
		renameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rename();
			}
		});
		*/
		
		/*
		MySmallJButton  redefineButton = new MySmallJButton (app.getImageIcon("redefine.gif"), pixelX, pixelY);
		redefineButton.setToolTipText(app.getPlain("Redefine"));
		redefineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				redefine();
			}
		});
		*/ 
		
		JButton delButton = new JButton(app.getImageIcon("delete_small.gif"));
		delButton.setText(app.getPlain("Delete"));
		delButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSelectedGeos();
			}
		});

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));			
		//if (app.letRedefine())
		//	buttonPanel.add(redefineButton);
		if (app.letDelete())
			buttonPanel.add(delButton);
		//if (app.letRename())
		//	buttonPanel.add(renameButton);

		listPanel.add(buttonPanel, BorderLayout.SOUTH);
//		Border compound =		
//			BorderFactory.createCompoundBorder(
//				//	BorderFactory.createTitledBorder(app.getPlain("Objects")),
//				BorderFactory.createEtchedBorder(),
//				BorderFactory.createEmptyBorder(2, 2, 2, 2));
		listPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));		

			
		// PROPERTIES PANEL
		if (colChooser == null) {
			// init color chooser
			colChooser = new JColorChooser();
			colChooser.setColor(new Color(1, 1,1, 100));
		}
			
		// check for null added otherwise you get two listeners for the colChooser
		// when a file is loaded
		if (propPanel == null) {
			propPanel = new PropertiesPanel(colChooser);
			propPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		}
		selectionChanged(); // init propPanel		

		closeButton = new JButton(app.getMenu("Close"));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();			
			}
		});

		// put it all together				 		 		 
		Container contentPane = getContentPane();
		contentPane.removeAll();
		//contentPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));		
		contentPane.setLayout(new BorderLayout());
				
		buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(closeButton);
		//buttonPanel.add(cancelButton);
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(propPanel, BorderLayout.CENTER);
		rightPanel.add(buttonPanel, BorderLayout.SOUTH);
		JPanel dialogPanel = new JPanel(new BorderLayout());
		dialogPanel.add(listPanel, BorderLayout.WEST);
		dialogPanel.add(rightPanel, BorderLayout.CENTER);

		contentPane.add(dialogPanel);						
							
		if (wasShowing) {
			setVisible(true);
		}		
	}
	
	/*
	public void cancel() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		kernel.detach(geoTree);
				
		// remember current construction step
		int consStep = kernel.getConstructionStep();
		
		// restore old construction state
		app.restoreCurrentUndoInfo();
		
		// go to current construction step
		ConstructionProtocol cp = app.getConstructionProtocol();
		if (cp != null) {
			cp.setConstructionStep(consStep);     			 
		}
		
		setCursor(Cursor.getDefaultCursor());
		setVisible(false);
	}
	
	public void apply() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));				
		app.storeUndoInfo();
		setCursor(Cursor.getDefaultCursor());
		setVisible(false);	
	}
	*/
	
	public void cancel() {
		setVisible(false);
	}
	
	public void closeDialog() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));				
		app.storeUndoInfo();
		setCursor(Cursor.getDefaultCursor());
		setVisible(false);	
	}
		
	/**
	 * shows this dialog and select GeoElement geo at screen position location
	 */
	public void setVisibleWithGeos(ArrayList geos) {
		setViewActive(true);					
	
		if (kernel.getConstruction().getGeoSetConstructionOrder().size() < 
				MAX_GEOS_FOR_EXPAND_ALL)		
			geoTree.expandAll();
		else 
			geoTree.collapseAll();		
		
		
		
		geoTree.setSelected(geos, false);
		if (!isShowing()) {		
			// pack and center on first showing
			if (firstTime) {
				pack();		
				setLocationRelativeTo(app.getMainComponent());	
				firstTime = false;
			}
			
			// ensure min size
			Dimension dim = getSize();
			if (dim.width < MIN_WIDTH) {
				dim.width = MIN_WIDTH;
				setSize(dim);
			}
			if (dim.height < MIN_HEIGHT) {
				dim.height = MIN_HEIGHT;
				setSize(dim);
			}			
			
			super.setVisible(true);	
		}					
	}
	private boolean firstTime = true;

	public void setVisible(boolean visible) {
		if (visible) {			
			setVisibleWithGeos(null);			
		} else {
			super.setVisible(false);
			setViewActive(false);
		}
		
		
	}
	
	private void setViewActive(boolean flag) {
		if (flag == viewActive) return; 
		viewActive = flag;
		
		if (flag) {			
			geoTree.clear();	
			kernel.attach(geoTree);
			
//			// only add objects if there are less than 200
//			int geoSize = kernel.getConstruction().getGeoSetConstructionOrder().size();
//			if (geoSize < MAX_OBJECTS_IN_TREE)
				kernel.notifyAddAll(geoTree);					
			
			app.setSelectionListenerMode(this);
			addWindowFocusListener(this);			
		} else {
			kernel.detach(geoTree);					
			
			removeWindowFocusListener(this);						
			app.setSelectionListenerMode(null);
		}		
	}
	private boolean viewActive = false;

	/**
	 * handles selection change	 
	 */
	private void selectionChanged() {	
		updateSelectedGeos(geoTree.getSelectionPaths());
				
		Object [] geos = selectionList.toArray();										
		propPanel.updateSelection(geos);
		//Util.addKeyListenerToAll(propPanel, this);
		
		// update selection of application too
		if (app.getMode() == EuclidianView.MODE_SELECTION_LISTENER)
			app.setSelectedGeos(selectionList);		
	}
	
	
	private ArrayList updateSelectedGeos(TreePath [] selPath ) {
		selectionList.clear();	
		
		if (selPath != null) {				
			// add all selected paths
			for (int i=0; i < selPath.length; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath[i].getLastPathComponent();						
				
				if (node == node.getRoot()) {	
					// root: add all objects
					selectionList.clear();
					selectionList.addAll(app.getKernel().getConstruction().getGeoSetLabelOrder());										
					i = selPath.length;		
				}				
				else if (node.getParent() == node.getRoot()) {
					// type node: select all children	
					for (int k=0; k < node.getChildCount(); k++) {
						DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(k);											
						selectionList.add(child.getUserObject());
					}
				} else {
					// GeoElement					
					selectionList.add(node.getUserObject());					
				}										
			}				
		}	
			
		return selectionList;
	}
	private ArrayList selectionList = new ArrayList();
	
	public void geoElementSelected(GeoElement geo, boolean addToSelection) {
		if (geo == null) return;
		tempArrayList.clear();
		tempArrayList.add(geo);
		geoTree.setSelected(tempArrayList, addToSelection);
		//requestFocus();
	}	
	private ArrayList tempArrayList = new ArrayList();

	/**
	 * deletes all selected GeoElements from Kernel	 
	 */
	private void deleteSelectedGeos() {
		ArrayList selGeos = selectionList;
		
		if (selGeos.size() > 0) {	
			Object [] geos = selGeos.toArray();			
			for (int i = 0; i < geos.length - 1; i++) {
				((GeoElement) geos[i]).removeOrSetUndefinedIfHasFixedDescendent();
			}
			
			// select element above last to delete
			GeoElement geo = (GeoElement) geos[geos.length - 1];
			TreePath tp = geoTree.getTreePath(geo);			
			if (tp != null) {
				int row = geoTree.getRowForPath(tp);
				tp = geoTree.getPathForRow(row - 1);
				geo.removeOrSetUndefinedIfHasFixedDescendent();								
				if (tp != null) geoTree.setSelectionPath(tp);
			}
		}
	}

	/**
	 * renames first selected GeoElement
	 *
	private void rename() {
		ArrayList selGeos = selectionList;	
		if (selGeos.size() > 0)	{
			GeoElement geo = (GeoElement) selGeos.get(0);
			app.showRenameDialog(geo, false, null);
			
			selectionList.clear();
			selectionList.add(geo);
			geoTree.setSelected(selectionList, false);	
		}								
	}*/
	
	/**
	 * redefines first selected GeoElement
	 *
	private void redefine() {
		ArrayList selGeos = selectionList;
		geoTree.clearSelection();
		if (selGeos.size() > 0)						
			app.showRedefineDialog((GeoElement) selGeos.get(0));		
	}*/

	

	/*
	 * Window Listener
	 */
	public void windowActivated(WindowEvent e) {
		/*
		if (!isModal()) {
			geoTree.setSelected(null, false);
			//selectionChanged();						
		}
		repaint();
		*/
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		//cancel();
		closeDialog();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}
	public void windowIconified(WindowEvent e) {
	}
	public void windowOpened(WindowEvent e) {
	}
	


	/**
	 * INNER CLASS
	 * PropertiesPanel for displaying all gui elements for changing properties
	 * of currently selected GeoElements. 
	 * @see update() in PropertiesPanel
	 * @author Markus Hohenwarter
	 */
	class PropertiesPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private NamePanel namePanel;
		private ShowObjectPanel showObjectPanel;		
		private ColorPanel colorPanel;
		private LabelPanel labelPanel;
		private LayerPanel layerPanel; // Michael Borcherds 2008-02-26
		private CoordPanel coordPanel;
		private LineEqnPanel lineEqnPanel;
		private ConicEqnPanel conicEqnPanel;
		private PointSizePanel pointSizePanel;
		private PointStylePanel pointStylePanel; // Florian Sonner 2008-07-17
		private TextOptionsPanel textOptionsPanel;
		private ArcSizePanel arcSizePanel;
		private LineStylePanel lineStylePanel;
		// added by Lo�c BEGIN
		private DecoSegmentPanel decoSegmentPanel;
		private DecoAnglePanel decoAnglePanel;
		private RightAnglePanel rightAnglePanel;
		//END
		
		private FillingPanel fillingPanel;
		private TracePanel tracePanel;
		private AnimatingPanel animatingPanel;
		private FixPanel fixPanel;
		private CheckBoxFixPanel checkBoxFixPanel;
 		private AllowReflexAnglePanel allowReflexAnglePanel;
 		private AllowOutlyingIntersectionsPanel allowOutlyingIntersectionsPanel;
		private AuxiliaryObjectPanel auxPanel;
		private AnimationStepPanel animStepPanel;
		private AnimationSpeedPanel animSpeedPanel;
		private SliderPanel sliderPanel;
		private SlopeTriangleSizePanel slopeTriangleSizePanel;
		private StartPointPanel startPointPanel;
		private CornerPointsPanel cornerPointsPanel;
		private TextEditPanel textEditPanel;
		private BackgroundImagePanel bgImagePanel;
		private AbsoluteScreenLocationPanel absScreenLocPanel;	
		private ShowConditionPanel showConditionPanel;
		private ColorFunctionPanel colorFunctionPanel;
		
		private JTabbedPane tabs;

		public PropertiesPanel(JColorChooser colChooser) {			
			//setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));	
			//setLayout(new FlowLayout());
			
			//setBorder(
					//BorderFactory.createTitledBorder(app.getPlain("Properties")));

			namePanel = new NamePanel(app);		
			showObjectPanel = new ShowObjectPanel();
			colorPanel = new ColorPanel(colChooser);
			labelPanel = new LabelPanel();
			layerPanel = new LayerPanel(); // Michael Borcherds 2008-02-26
			coordPanel = new CoordPanel();
			lineEqnPanel = new LineEqnPanel();
			conicEqnPanel = new ConicEqnPanel();
			pointSizePanel = new PointSizePanel();
			pointStylePanel = new PointStylePanel(); // Florian Sonner 2008-07-12
			textOptionsPanel = new TextOptionsPanel();
			arcSizePanel = new ArcSizePanel();
			slopeTriangleSizePanel = new SlopeTriangleSizePanel();
			lineStylePanel = new LineStylePanel();
			// added by Lo�c BEGIN
			decoSegmentPanel=new DecoSegmentPanel();
			decoAnglePanel=new DecoAnglePanel();
			rightAnglePanel=new RightAnglePanel();
			//END
			fillingPanel = new FillingPanel();
			tracePanel = new TracePanel();
			animatingPanel = new AnimatingPanel();
			fixPanel = new FixPanel();
			checkBoxFixPanel = new CheckBoxFixPanel();
			absScreenLocPanel = new AbsoluteScreenLocationPanel();
			auxPanel = new AuxiliaryObjectPanel();
			animStepPanel = new AnimationStepPanel(app);
			animSpeedPanel = new AnimationSpeedPanel(app);
			sliderPanel = new SliderPanel(app, this, false);
			startPointPanel = new StartPointPanel();
			cornerPointsPanel = new CornerPointsPanel();
			textEditPanel = new TextEditPanel();
			bgImagePanel = new BackgroundImagePanel();
			allowReflexAnglePanel = new AllowReflexAnglePanel();
			allowOutlyingIntersectionsPanel = new AllowOutlyingIntersectionsPanel();
			showConditionPanel = new ShowConditionPanel(app, this); 
			colorFunctionPanel = new ColorFunctionPanel(app, this);
			
 			//tabbed pane for properties
			tabs = new JTabbedPane();				
 			initTabs();
 			
 			
 			
 			setLayout(new BorderLayout());
 			add(tabs, BorderLayout.CENTER); 			
		}		
		
		// added by Lo�c BEGIN
		public void setSliderMinValue(){
			arcSizePanel.setMinValue();
		}
		//END		
		
		// double-click on slider -> open properties at slider tab
		public void showSliderTab() {
			tabs.setSelectedIndex(1);
		}
		
		// lists of TabPanel objects
		private ArrayList tabPanelList;
				
		private void initTabs() {				
			// basic tab
			ArrayList basicTabList = new ArrayList();
			basicTabList.add(namePanel);			
			basicTabList.add(showObjectPanel);														
			basicTabList.add(labelPanel);		
			basicTabList.add(tracePanel);			
			basicTabList.add(animatingPanel);			
			basicTabList.add(fixPanel);	
			basicTabList.add(auxPanel);
			basicTabList.add(checkBoxFixPanel);
			basicTabList.add(bgImagePanel);	
			basicTabList.add(absScreenLocPanel);
			basicTabList.add(allowReflexAnglePanel);	
			basicTabList.add(rightAnglePanel);
			basicTabList.add(allowOutlyingIntersectionsPanel);
			TabPanel basicTab = new TabPanel(app.getMenu("Properties.Basic"), basicTabList);
			basicTab.addToTabbedPane(tabs);	
			
			// name tab
//			ArrayList nameTabList = new ArrayList();
//			nameTabList.add(namePanel);
//			TabPanel nameTab = new TabPanel(app.getPlain("Name"), nameTabList);
//			nameTab.addToTabbedPane(tabs);	
				
			/*
			// change basic tab layout: create grid with two columns
			basicTab.removeAll();
			basicTab.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.weightx = 0.1;
			c.weighty = 0.1;				
			c.gridwidth = 2;
			basicTab.add(namePanel, c);
			c.gridwidth = 1;
			for (int i = 1; i < basicTabList.size(); i++) {
				JPanel p = (JPanel) basicTabList.get(i);
				c.gridx = (i-1) % 2;
				c.gridy = (i-1) / 2 + 1;													
				basicTab.add(p, c);
			}	
			*/
			
			// text tab
			ArrayList textTabList = new ArrayList();			
			textTabList.add(textOptionsPanel);	
			textTabList.add(textEditPanel);			
			TabPanel textTab = new TabPanel(app.getPlain("Text"), textTabList);
			textTab.addToTabbedPane(tabs);	
			
			// slider tab
			ArrayList sliderTabList = new ArrayList();	
			sliderTabList.add(sliderPanel);	
			TabPanel sliderTab = new TabPanel(app.getPlain("Slider"), sliderTabList);
			sliderTab.addToTabbedPane(tabs);	
	        
			// color tab
			ArrayList colorTabList = new ArrayList();
			colorTabList.add(colorPanel);		
			TabPanel colorTab = new TabPanel(app.getPlain("Color"), colorTabList);
			colorTab.addToTabbedPane(tabs);			
					

			// style tab
			ArrayList styleTabList = new ArrayList();
			styleTabList.add(slopeTriangleSizePanel);
			styleTabList.add(pointSizePanel);
			styleTabList.add(pointStylePanel); // Florian Sonner 2008-07-17
			styleTabList.add(lineStylePanel);	
			styleTabList.add(arcSizePanel);		
			styleTabList.add(fillingPanel);
			TabPanel styleTab = new TabPanel(app.getMenu("Properties.Style"), styleTabList);
			styleTab.addToTabbedPane(tabs);	
				
			// decoration
			ArrayList decorationTabList = new ArrayList();	
			decorationTabList.add(decoAnglePanel);
			decorationTabList.add(decoSegmentPanel);
			TabPanel lineStyleTab = new TabPanel(app.getPlain("Decoration"), decorationTabList);
			lineStyleTab.addToTabbedPane(tabs);	
			
			// filling style
//			ArrayList fillingTabList = new ArrayList();	
//			fillingTabList.add(fillingPanel);			
//			TabPanel fillingTab = new TabPanel(app.getPlain("Filling"), fillingTabList);
//			fillingTab.addToTabbedPane(tabs);										
			
			// position			
			ArrayList positionTabList = new ArrayList();	
			positionTabList.add(startPointPanel);	
			positionTabList.add(cornerPointsPanel);
			TabPanel positionTab = new TabPanel(app.getMenu("Properties.Position"), positionTabList);
			positionTab.addToTabbedPane(tabs);	
			
			// algebra tab
			ArrayList algebraTabList = new ArrayList();
			algebraTabList.add(coordPanel);
			algebraTabList.add(lineEqnPanel);
			algebraTabList.add(conicEqnPanel);	
			algebraTabList.add(animStepPanel);	
			TabPanel algebraTab = new TabPanel(app.getMenu("Properties.Algebra"), algebraTabList);
			algebraTab.addToTabbedPane(tabs);
			
			// advanced tab
			ArrayList advancedTabList = new ArrayList();
			advancedTabList.add(showConditionPanel);	
			advancedTabList.add(colorFunctionPanel);	
			advancedTabList.add(layerPanel); // Michael Borcherds 2008-02-26
			TabPanel advancedTab = new TabPanel(app.getMenu("Advanced"), advancedTabList);
			advancedTab.addToTabbedPane(tabs);			
					
			// fill tabPanelList
			tabPanelList = new ArrayList();
			for (int i=0; i < tabs.getTabCount(); i++) {
				tabPanelList.add( (TabPanel) tabs.getComponentAt(i));
			}
		}
		
		private void updateTabs(Object [] geos) {			
			if (geos.length == 0) {
				tabs.setVisible(false);
				return;
			}
			
			
			// remember selected tab
			Component selectedTab = tabs.getSelectedComponent();
			
			tabs.removeAll();				
			for (int i=0; i < tabPanelList.size(); i++) {
				TabPanel tp = (TabPanel) tabPanelList.get(i);
				tp.update(geos);
				tp.addToTabbedPane(tabs);
			}
														
			// switch back to previously selected tab
			if (tabs.getTabCount() > 0) {				
				int index = tabs.indexOfComponent(selectedTab);
				tabs.setSelectedIndex(Math.max(0, index));
				tabs.setVisible(true);
			} else
				tabs.setVisible(false);
		}
		
		private boolean updateTabPanel(TabPanel tabPanel, ArrayList tabList, Object [] geos) {
			// update all panels and their visibility			
			boolean oneVisible = false;
			int size = tabList.size();
			for (int i=0; i < size; i++) {
				UpdateablePanel up = (UpdateablePanel) tabList.get(i);
				boolean show = (up.update(geos) != null);						
				up.setVisible(show);
				if (show) oneVisible = true;
			}
			
			return oneVisible;				
		}				
		
		public void updateSelection(Object[] geos) {
			//if (geos == oldSelGeos) return;
			//oldSelGeos = geos;										
						
			updateTabs(geos);
		}				
						
		
		private class TabPanel extends JPanel {
		
			private String title;
			private ArrayList panelList;
			private boolean makeVisible = true;			
			
			public TabPanel(String title, ArrayList pVec) {
				this.title = title;
				panelList = pVec;
				
				setLayout(new BorderLayout());
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5,5,5));
				JScrollPane scrollPane = new JScrollPane(panel);
				//setPreferredSize(new Dimension(450, 110));
				add(scrollPane, BorderLayout.CENTER);
							
				// create grid with one column
				panel.setLayout(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.NORTHWEST;
				c.weightx = 1.0;
				c.weighty = 1E-12;
				
				for (int i = 0; i < pVec.size(); i++) {
					JPanel p = (JPanel) pVec.get(i);
					c.gridx = 0;
					c.gridy = i;
										
					panel.add(p, c);
				}			
				c.weighty = 1.0;
				panel.add(Box.createVerticalGlue(), c);
			}
			
			public void update(Object [] geos) {
				makeVisible = updateTabPanel(this, panelList, geos);
			}
			
			public void addToTabbedPane(JTabbedPane tabs) {
				if (makeVisible) {
					tabs.addTab(title, this);
				}
			}										
		}

	} // PropertiesPanel
		
	
	/**
	 * panel with show/hide object checkbox
	 */
	private class ShowObjectPanel extends JPanel implements ItemListener, UpdateablePanel {
	
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox showObjectCB;

		public ShowObjectPanel() {
			// check box for show object
			showObjectCB = new JCheckBox(app.getPlain("ShowObject"));
			showObjectCB.addItemListener(this);			
			add(showObjectCB);			
		}				

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			showObjectCB.removeItemListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalObjectVal = true;
			boolean showObjectCondition = geo0.getShowObjectCondition() != null;

			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same object visible value
				if (geo0.isSetEuclidianVisible()
					!= temp.isSetEuclidianVisible()) {
					equalObjectVal = false;
					break;
				}
				
				if (temp.getShowObjectCondition() != null) {
					showObjectCondition = true;
				}
			}

			// set object visible checkbox
			if (equalObjectVal)
				showObjectCB.setSelected(geo0.isSetEuclidianVisible());
			else
				showObjectCB.setSelected(false);

			showObjectCB.setEnabled(!showObjectCondition);
			
			showObjectCB.addItemListener(this);
			return this;
		}

		// show everything but numbers (note: drawable angles are shown)
		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!geo.isDrawable()
						// can't allow a free fixed number to become visible (as a slider)
						|| (geo.isGeoNumeric() && geo.isFixed())) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * listens to checkboxes and sets object and label visible state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoElement geo;
			Object source = e.getItemSelectable();

			// show object value changed
			if (source == showObjectCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setEuclidianVisible(showObjectCB.isSelected());
					geo.updateRepaint();
				}
			}
			propPanel.updateSelection(geos);
		}

	} // ShowObjectPanel

	/**
	 * panel to fix checkbox (boolean object)
	 */
	private class CheckBoxFixPanel extends JPanel implements ItemListener, UpdateablePanel {

		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox checkboxFixCB;

		public CheckBoxFixPanel() {
			checkboxFixCB = new JCheckBox(app.getPlain("FixCheckbox"));
			checkboxFixCB.addItemListener(this);			
			add(checkboxFixCB);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			checkboxFixCB.removeItemListener(this);

			// check if properties have same values
			GeoBoolean temp, geo0 = (GeoBoolean) geos[0];
			boolean equalObjectVal = true;
			
			for (int i = 1; i < geos.length; i++) {
				temp = (GeoBoolean) geos[i];
				// same object visible value
				if (geo0.isCheckboxFixed() != temp.isCheckboxFixed()) {
					equalObjectVal = false;
					break;
				}								
			}

			// set object visible checkbox
			if (equalObjectVal)
				checkboxFixCB.setSelected(geo0.isCheckboxFixed());
			else
				checkboxFixCB.setSelected(false);
			
			checkboxFixCB.addItemListener(this);
			return this;
		}

		// show everything but numbers (note: drawable angles are shown)
		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				if (geos[i] instanceof GeoBoolean) {
					GeoBoolean bool = (GeoBoolean) geos[i];
					if (!bool.isIndependent()) {
						return false;
					}
 				} else
 					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets object and label visible state
		 */
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();

			// show object value changed
			if (source == checkboxFixCB) {
				for (int i = 0; i < geos.length; i++) {
					GeoBoolean bool = (GeoBoolean) geos[i];
					bool.setCheckboxFixed(checkboxFixCB.isSelected());
					bool.updateRepaint();
				}
			}
			propPanel.updateSelection(geos);
		}

	} // CheckBoxFixPanel
	
	/**
	 * panel color chooser and preview panel
	 */
	private class ColorPanel extends JPanel implements UpdateablePanel, ChangeListener {

		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos		
		private JLabel previewLabel;
		private JPanel previewPanel;

		public ColorPanel(JColorChooser colChooser) {
			colChooser.setLocale(app.getLocale());
			previewPanel = new PreviewPanel();			
			previewLabel = new JLabel();
			AbstractColorChooserPanel [] tabs = colChooser.getChooserPanels();
			
			setLayout(new BorderLayout());		
			
			/*
			// Michael Borcherds 2008-03-14
			// added RGB in a new tab
			// and moved preview underneath
			JTabbedPane colorTabbedPane = new JTabbedPane();
			colorTabbedPane.addTab( app.getMenu("Swatches"), tabs[0] );
			//colorTabbedPane.addTab( app.getMenu("HSB"), tabs[1] );
			colorTabbedPane.addTab( app.getMenu("RGB"), tabs[2] );
			colorTabbedPane.setSelectedIndex(0);
			JPanel p = new JPanel();
			
			// create grid with one column
			p.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.weightx = 0.0;
			c.weighty = 0.0;
			
			c.gridx = 0;
			c.gridy = 0;			
			c.gridwidth = 4;
			p.add(colorTabbedPane, c);
			
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 1;
			c.insets = new Insets(10,0,0,0);  //top padding
			p.add(new JLabel(app.getMenu("Preview") + ": "), c);
								
			c.gridx = 1;
			c.gridy = 1;
			c.gridwidth = 1;
			p.add(previewPanel, c);
										
			c.weighty = 1.0;
			p.add(Box.createVerticalGlue(), c);
		*/
	
		
			setLayout(new BorderLayout());			
			add(tabs[0], BorderLayout.NORTH);		
			
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(new JLabel(app.getMenu("Preview") + ": "));
			p.add(previewPanel);
			p.add(previewLabel);
			add(p, BorderLayout.CENTER);
			
			// in order to get state changes we need to set color chooser to
			// a color that is different to the 	
			
			/*
			// remove possible old change listeners from color chooser						
			ChangeListener [] listeners = (ChangeListener[]) colChooser.getListeners(ChangeListener.class);
			if (listeners != null) {
				for (int i = 0; i< listeners.length; i++) {
					colChooser.getSelectionModel().removeChangeListener( listeners[i]);
				}
			}*/
			
						
			//colChooser.setColor(new Color(1, 1,1, 100));
			colChooser.getSelectionModel().addChangeListener(this);	
		}
		

		
		private class PreviewPanel extends JPanel {
		    public PreviewPanel() {
		        setPreferredSize(new Dimension(100,app.getFontSize() + 8));
		        setBorder(BorderFactory.createRaisedBevelBorder());
		      }
		      public void paintComponent(Graphics g) {
		        Dimension size = getSize();
	
		        g.setColor(getForeground());
		        g.fillRect(0,0,size.width,size.height);
		      }
	    }

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalObjColor = true;

			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same object color
				if (!geo0.getObjectColor().equals(temp.getObjectColor())) {
					equalObjColor = false;
					break;
				}
			}

			// set colorButton's color to object color
			Color col;
			if (equalObjColor) {
				col = geo0.getObjectColor();
				previewPanel.setToolTipText(col.getRed() + ", " + col.getGreen() + ", " + col.getBlue());
				previewLabel.setText("("+previewPanel.getToolTipText()+")");		
			} else {
				col = null;
				previewPanel.setToolTipText("");
				previewLabel.setText("");
			}
			
			previewPanel.setForeground(col);
			return this;
		}

		/**
		 * sets color of selected GeoElements
		 */
		private void updateColor(Color col) {						
			if (col == null || geos == null)
				return;
			
			// update preview panel
			previewPanel.setForeground(col);
			previewPanel.setToolTipText(col.getRed() + ", " + col.getGreen() + ", " + col.getBlue());
			previewLabel.setText("("+previewPanel.getToolTipText()+")");

			GeoElement geo;
			for (int i = 0; i < geos.length; i++) {
				geo = (GeoElement) geos[i];
				geo.setObjColor(col);
				geo.updateRepaint();
			}					
			
			Application.debug("Setting color RGB = "+col.getRed()+" "+col.getGreen()+" "+col.getBlue());
			
			// in order to get state changes we need to set color chooser to
			// a color that is not an available color
			colChooser.getSelectionModel().removeChangeListener(this);		
			Color differentColor = new Color(0,0,1);
			colChooser.setColor(differentColor);
			
			colChooser.getSelectionModel().addChangeListener(this);	
		}

	
		// show everything but images
		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				/* removed - we want to be able to change the color of everything in the spreadsheet
				if (geos[i] instanceof GeoNumeric) {
					GeoNumeric num = (GeoNumeric) geos[i];
					if (!num.isDrawable())
						return false;
				} else */
				if (geos[i] instanceof GeoImage)
					return false;
			}
			return true;
		}
		
		/**
		 * Listens for color chooser state changes
		 */
		public void stateChanged(ChangeEvent arg0) {
			updateColor(colChooser.getColor());	
		}	

	} // ColorPanel

	/**
	 * panel with label properties
	 */
	private class LabelPanel
		extends JPanel
		implements ItemListener, ActionListener , UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox showLabelCB;
		private JComboBox labelModeCB;
		private boolean showNameValueComboBox;

		public LabelPanel() {
			// check boxes for show object, show label
			showLabelCB = new JCheckBox(app.getPlain("ShowLabel") + ":");
			showLabelCB.addItemListener(this);

			// combo box for label mode: name or algebra
			labelModeCB = new JComboBox();
			labelModeCB.addItem(app.getPlain("Name")); // index 0
			labelModeCB.addItem(
				app.getPlain("NameAndValue"));
			// index 1
			labelModeCB.addItem(app.getPlain("Value")); // index 2
			labelModeCB.addItem(app.getPlain("Caption")); // index 3 Michael Borcherds
			labelModeCB.addActionListener(this);

			// labelPanel with show checkbox
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(showLabelCB);
			add(labelModeCB);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			showLabelCB.removeItemListener(this);
			labelModeCB.removeActionListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalLabelVal = true;
			boolean equalLabelMode = true;
			showNameValueComboBox =  geo0.isLabelValueShowable();

			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				//	same label visible value
				if (geo0.isLabelVisible() != temp.isLabelVisible())
					equalLabelVal = false;
				//	same label mode
				if (geo0.getLabelMode() != temp.getLabelMode())
					equalLabelMode = false;
				
				showNameValueComboBox =
					showNameValueComboBox && temp.isLabelValueShowable();
			}
			
			// change "Show Label:" to "Show Label" if there's no menu
			// Michael Borcherds 2008-02-18
			if (!showNameValueComboBox) showLabelCB.setText(app.getPlain("ShowLabel")); else showLabelCB.setText(app.getPlain("ShowLabel") + ":");


			//	set label visible checkbox
			if (equalLabelVal) {
				showLabelCB.setSelected(geo0.isLabelVisible());
				labelModeCB.setEnabled(geo0.isLabelVisible());
			} else {
				showLabelCB.setSelected(false);
				labelModeCB.setEnabled(false);
			}

			//	set label visible checkbox
			if (equalLabelMode)
				labelModeCB.setSelectedIndex(geo0.getLabelMode());
			else
				labelModeCB.setSelectedItem(null);

			// locus in selection
			labelModeCB.setVisible(showNameValueComboBox);
			showLabelCB.addItemListener(this);
			labelModeCB.addActionListener(this);
			return this;
		}

		// show everything but numbers (note: drawable angles are shown)
		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!geo.isLabelShowable()) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * listens to checkboxes and sets object and label visible state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoElement geo;
			Object source = e.getItemSelectable();

			// show label value changed
			if (source == showLabelCB) {
				boolean flag = showLabelCB.isSelected();				
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setLabelVisible(flag);
					geo.updateRepaint();
				}	
				update(geos);
			}
		}

		/**
		* action listener implementation for label mode combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == labelModeCB) {
				GeoElement geo;
				int mode = labelModeCB.getSelectedIndex();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setLabelMode(mode);
					geo.updateRepaint();
				}
			}
		}

	} // LabelPanel

	/*
	 * panel with layers properties
	 Michael Borcherds
	 */
	private class LayerPanel
		extends JPanel
		implements ItemListener, ActionListener , UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JComboBox layerModeCB;
		private JLabel layerLabel;

		public LayerPanel() {
			layerLabel = new JLabel(app.getPlain("Layer") + ":");	
			layerLabel.setLabelFor(layerModeCB);

			// combo box for label mode: name or algebra
			layerModeCB = new JComboBox();
			
			for (int layer=0 ; layer<=EuclidianView.MAX_LAYERS ; layer++) layerModeCB.addItem(" "+layer); 
			
			layerModeCB.addActionListener(this);

			// labelPanel with show checkbox
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(layerLabel);
			add(layerModeCB);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			layerModeCB.removeActionListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalLayer = true;

			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				//	same label visible value
				if (geo0.getLayer() != temp.getLayer())
					equalLayer = false;
			}
			

			if (equalLayer)
				layerModeCB.setSelectedIndex(geo0.getLayer());
			else
				layerModeCB.setSelectedItem(null);

			// locus in selection
			layerModeCB.addActionListener(this);
			return this;
		}

		// show everything that's drawable
		// don't want layers for dependent numbers as we want to 
		// minimise the XML for such objects to keep the spreadsheet fast
		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!((GeoElement)geos[i]).isDrawable()) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * listens to checkboxes and sets object and label visible state
		 */
		public void itemStateChanged(ItemEvent e) {
		}

		/**
		* action listener implementation for label mode combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == layerModeCB) {
				GeoElement geo;
				int layer = layerModeCB.getSelectedIndex();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setLayer(layer);
					geo.updateRepaint();
				}
			}
		}

	} // LayersPanel

	/**
	 * panel for trace
	 * @author Markus Hohenwarter
	 */
	private class TracePanel extends JPanel implements ItemListener,  UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox showTraceCB;

		public TracePanel() {
			// check boxes for show trace
			showTraceCB = new JCheckBox(app.getPlain("ShowTrace"));
			showTraceCB.addItemListener(this);
			add(showTraceCB);
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			showTraceCB.removeItemListener(this);

			// check if properties have same values
			Traceable temp, geo0 = (Traceable) geos[0];
			boolean equalTrace = true;

			for (int i = 1; i < geos.length; i++) {
				temp = (Traceable) geos[i];
				// same object visible value
				if (geo0.getTrace() != temp.getTrace())
					equalTrace = false;
			}

			// set trace visible checkbox
			if (equalTrace)
				showTraceCB.setSelected(geo0.getTrace());
			else
				showTraceCB.setSelected(false);

			showTraceCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof Traceable)) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			Traceable geo;
			Object source = e.getItemSelectable();

			// show trace value changed
			if (source == showTraceCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (Traceable) geos[i];
					geo.setTrace(showTraceCB.isSelected());
					geo.updateRepaint();
				}
			}
		}
	}
	/**
	 * panel for trace
	 * @author adapted from TracePanel
	 */
	private class AnimatingPanel extends JPanel implements ItemListener,  UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox showAnimatingCB;

		public AnimatingPanel() {
			// check boxes for animating
			showAnimatingCB = new JCheckBox(app.getPlain("Animating"));
			showAnimatingCB.addItemListener(this);
			add(showAnimatingCB);
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			showAnimatingCB.removeItemListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalAnimating = true;

			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same object visible value
				if (geo0.isAnimating() != temp.isAnimating())
					equalAnimating = false;
			}

			// set animating checkbox
			if (equalAnimating)
				showAnimatingCB.setSelected(geo0.isAnimating());
			else
				showAnimatingCB.setSelected(false);

			showAnimatingCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!((GeoElement)geos[i]).isAnimatable()) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoElement geo;
			Object source = e.getItemSelectable();

			// animating value changed
			if (source == showAnimatingCB) {
				boolean animate = showAnimatingCB.isSelected();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setAnimating(animate);
					geo.updateRepaint();
				}
				
				// make sure that we are animating 
				if (animate)
					kernel.getAnimatonManager().startAnimation();				
			}
		}
	}

	/**
	 * panel for fixing an object
	 * @author Markus Hohenwarter
	 */
	private class FixPanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox showFixCB;

		public FixPanel() {
			// check boxes for show trace
			showFixCB = new JCheckBox(app.getPlain("FixObject"));
			showFixCB.addItemListener(this);
			add(showFixCB);
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			showFixCB.removeItemListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalFix = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same object visible value
				if (geo0.isFixed() != temp.isFixed())
					equalFix = false;
			}

			// set trace visible checkbox
			if (equalFix)
				showFixCB.setSelected(geo0.isFixed());
			else
				showFixCB.setSelected(false);

			showFixCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!geo.isFixable())
					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoElement geo;
			Object source = e.getItemSelectable();

			// show trace value changed
			if (source == showFixCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setFixed(showFixCB.isSelected());
					geo.updateRepaint();
				}
			}		
			if (propPanel != null)		
				propPanel.updateSelection(geos);
			else
				update(geos);
		}
	}

	/**
	 * panel to set object's absoluteScreenLocation flag
	 * @author Markus Hohenwarter
	 */
	private class AbsoluteScreenLocationPanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox cbAbsScreenLoc;

		public AbsoluteScreenLocationPanel() {
			// check boxes for show trace
			setLayout(new FlowLayout(FlowLayout.LEFT));
			cbAbsScreenLoc = new JCheckBox(app.getPlain("AbsoluteScreenLocation"));
			cbAbsScreenLoc.addItemListener(this);

			// put it all together		
			add(cbAbsScreenLoc);						
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			cbAbsScreenLoc.removeItemListener(this);

			// check if properties have same values
			AbsoluteScreenLocateable temp, geo0 = (AbsoluteScreenLocateable) geos[0];
			boolean equalVal = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (AbsoluteScreenLocateable) geos[i];
				// same object visible value
				if (geo0.isAbsoluteScreenLocActive() != temp.isAbsoluteScreenLocActive())
					equalVal = false;
			}

			// set checkbox
			if (equalVal)
				cbAbsScreenLoc.setSelected(geo0.isAbsoluteScreenLocActive());
			else
				cbAbsScreenLoc.setSelected(false);

			cbAbsScreenLoc.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (geo instanceof AbsoluteScreenLocateable) {
					AbsoluteScreenLocateable absLoc = (AbsoluteScreenLocateable) geo;
					if (!absLoc.isAbsoluteScreenLocateable() || geo.isGeoBoolean())
						return false;
				}					
				else
					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			AbsoluteScreenLocateable geo;
			Object source = e.getItemSelectable();

			// absolute screen location flag changed
			if (source == cbAbsScreenLoc) {
				boolean flag = cbAbsScreenLoc.isSelected();
				EuclidianView ev = app.getEuclidianView();
				for (int i = 0; i < geos.length; i++) {
					geo = (AbsoluteScreenLocateable) geos[i];
					if (flag) {
						// convert real world to screen coords
						int x = ev.toScreenCoordX(geo.getRealWorldLocX());
						int y = ev.toScreenCoordY(geo.getRealWorldLocY());
						if (!geo.isAbsoluteScreenLocActive())
							geo.setAbsoluteScreenLoc(x, y);							
					} else {
						// convert screen coords to real world 
						double x = ev.toRealWorldCoordX(geo.getAbsoluteScreenLocX());
						double y = ev.toRealWorldCoordY(geo.getAbsoluteScreenLocY());
						if (geo.isAbsoluteScreenLocActive())
							geo.setRealWorldLoc(x, y);
					}
					geo.setAbsoluteScreenLocActive(flag);															
					geo.toGeoElement().updateRepaint();
				}
				
				if (propPanel != null)		
					propPanel.updateSelection(geos);
				else
					update(geos);
			}
		}
	}	
	
	/**
	 * panel for angles to set whether reflex angles are allowed 
	 * @author Markus Hohenwarter
	 */
	private class AllowReflexAnglePanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox reflexAngleCB;
		private JCheckBox forcereflexAngleCB;

		public AllowReflexAnglePanel() {
//			 Michael Borcherds 2007-11-19
			reflexAngleCB = new JCheckBox(app.getPlain("allowReflexAngle"));
			reflexAngleCB.addItemListener(this);
			forcereflexAngleCB = new JCheckBox(app.getPlain("forceReflexAngle"));
			forcereflexAngleCB.addItemListener(this);
			add(reflexAngleCB);	

			// TODO make sure this line is commented out for 3.0 release, then reinstated
			add(forcereflexAngleCB);			

			setLayout(new FlowLayout(FlowLayout.LEFT));
			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

//			 Michael Borcherds 2007-11-19
			reflexAngleCB.removeItemListener(this);
			forcereflexAngleCB.removeItemListener(this);

			// check if properties have same values
			GeoAngle temp, geo0 = (GeoAngle) geos[0];
			boolean equalangleStyle=true;
			boolean allreflex=true;
			
			for (int i = 0; i < geos.length; i++) {
				temp = (GeoAngle) geos[i];
				// same object visible value
				if (temp.getAngleStyle()!=3) allreflex=false;
				if (geo0.getAngleStyle() != temp.getAngleStyle())
					equalangleStyle = false;
			}
			
			if (allreflex==true) reflexAngleCB.setEnabled(false); else reflexAngleCB.setEnabled(true);

			
			if (equalangleStyle)
			{
				switch (geo0.getAngleStyle()) {
				case 2: // acute/obtuse
					reflexAngleCB.setSelected(false);
					forcereflexAngleCB.setSelected(false);
					break;
				case 3: // force reflex
					reflexAngleCB.setSelected(true);
					forcereflexAngleCB.setSelected(true);
					break;
				default: // should be 0: anticlockwise
					reflexAngleCB.setSelected(true);
					forcereflexAngleCB.setSelected(false);
					break;
					
				}
			}
			else
			{
				reflexAngleCB.setSelected(false);
				forcereflexAngleCB.setSelected(false);
			}

			reflexAngleCB.addItemListener(this);
			forcereflexAngleCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (geo.isIndependent() || !(geo instanceof GeoAngle))
					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoAngle geo;
			Object source = e.getItemSelectable();

//Michael Borcherds 2007-11-19
			if (source == reflexAngleCB || source==forcereflexAngleCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoAngle) geos[i];
					if (forcereflexAngleCB.isSelected()) {
						geo.setAngleStyle(3);
						reflexAngleCB.setEnabled(false);
					}
					else 
					{
						reflexAngleCB.setEnabled(true);
						if (reflexAngleCB.isSelected())
							geo.setAngleStyle(0);
						else
							geo.setAngleStyle(2);							
					}
//					Michael Borcherds 2007-11-19
					geo.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel for limted paths to set whether outlying intersection points are allowed 
	 * @author Markus Hohenwarter
	 */
	private class AllowOutlyingIntersectionsPanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox outlyingIntersectionsCB;

		public AllowOutlyingIntersectionsPanel() {
			// check boxes for show trace			
			outlyingIntersectionsCB = new JCheckBox(app.getPlain("allowOutlyingIntersections"));
			outlyingIntersectionsCB.addItemListener(this);
			
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(outlyingIntersectionsCB);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			outlyingIntersectionsCB.removeItemListener(this);

			// check if properties have same values
			LimitedPath temp, geo0 = (LimitedPath) geos[0];
			boolean equalVal = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (LimitedPath) geos[i];
				// same value?
				if (geo0.allowOutlyingIntersections() != temp.allowOutlyingIntersections())
					equalVal = false;
			}

			// set trace visible checkbox
			if (equalVal)
				outlyingIntersectionsCB.setSelected(geo0.allowOutlyingIntersections());
			else
				outlyingIntersectionsCB.setSelected(false);

			outlyingIntersectionsCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!(geo instanceof LimitedPath))
					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			LimitedPath geo;
			Object source = e.getItemSelectable();

			// show trace value changed
			if (source == outlyingIntersectionsCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (LimitedPath) geos[i];
					geo.setAllowOutlyingIntersections(outlyingIntersectionsCB.isSelected());
					geo.toGeoElement().updateRepaint();
				}
			}
		}
	}

	
 	/**
	 * panel to set a background image (only one checkbox)
	 * @author Markus Hohenwarter
	 */
	private class BackgroundImagePanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox isBGimage;

		public BackgroundImagePanel() {
			// check boxes for show trace
			isBGimage = new JCheckBox(app.getPlain("BackgroundImage"));
			isBGimage.addItemListener(this);
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(isBGimage);
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			isBGimage.removeItemListener(this);

			// check if properties have same values
			GeoImage temp, geo0 = (GeoImage) geos[0];
			boolean equalIsBGimage = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (GeoImage) geos[i];
				// same object visible value
				if (geo0.isInBackground() != temp.isInBackground())
					equalIsBGimage = false;
			}

			// set trace visible checkbox
			if (equalIsBGimage)
				isBGimage.setSelected(geo0.isInBackground());
			else
				isBGimage.setSelected(false);

			isBGimage.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoImage))
					return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoImage geo;
			Object source = e.getItemSelectable();

			// show trace value changed
			if (source == isBGimage) {
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoImage) geos[i];
					geo.setInBackground(isBGimage.isSelected());
					geo.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel for making an object auxiliary 
	 * @author Markus Hohenwarter
	 */
	private class AuxiliaryObjectPanel extends JPanel implements ItemListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JCheckBox auxCB;

		public AuxiliaryObjectPanel() {
			// check boxes for show trace
			setLayout(new FlowLayout(FlowLayout.LEFT));
			auxCB = new JCheckBox(app.getPlain("AuxiliaryObject"));
			auxCB.addItemListener(this);
			add(auxCB);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			auxCB.removeItemListener(this);

			// check if properties have same values
			GeoElement temp, geo0 = (GeoElement) geos[0];
			boolean equalAux = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same object visible value
				if (geo0.isAuxiliaryObject() != temp.isAuxiliaryObject())
					equalAux = false;
			}

			// set trace visible checkbox
			if (equalAux)
				auxCB.setSelected(geo0.isAuxiliaryObject());
			else
				auxCB.setSelected(false);

			auxCB.addItemListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			// geo should be visible in algebra view
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!geo.isAlgebraVisible()) return false;
			}
			return true;
		}

		/**
		 * listens to checkboxes and sets trace state
		 */
		public void itemStateChanged(ItemEvent e) {
			GeoElement geo;
			Object source = e.getItemSelectable();

			// show trace value changed
			if (source == auxCB) {
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setAuxiliaryObject(auxCB.isSelected());
				}
			}
		}
	}


	/**
	 * panel for location of vectors and text 
	 */
	private class StartPointPanel
		extends JPanel
		implements ActionListener, FocusListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JComboBox cbLocation;
		private DefaultComboBoxModel cbModel;

		public StartPointPanel() {
			// textfield for animation step
			JLabel label = new JLabel(app.getPlain("StartingPoint") + ": ");
			cbLocation = new JComboBox();
			cbLocation.setEditable(true);
			cbModel = new DefaultComboBoxModel();
			cbLocation.setModel(cbModel);
			label.setLabelFor(cbLocation);
			cbLocation.addActionListener(this);
			cbLocation.addFocusListener(this);

			// put it all together
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(label);
			add(cbLocation);			
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;

			cbLocation.removeActionListener(this);

			// repopulate model with names of points from the geoList's model
			// take all points from construction
			TreeSet points = kernel.getConstruction().getGeoSetLabelOrder(GeoElement.GEO_CLASS_POINT);
			if (points.size() != cbModel.getSize() - 1) {				
				cbModel.removeAllElements();
				cbModel.addElement(null);			
				Iterator it = points.iterator();
				int count = 0;
				while (it.hasNext() || ++count > MAX_COMBOBOX_ENTRIES) {
					GeoPoint p = (GeoPoint) it.next();
					cbModel.addElement(p.getLabel());				
				}
			}

			// check if properties have same values
			Locateable temp, geo0 = (Locateable) geos[0];
			boolean equalLocation = true;

			for (int i = 0; i < geos.length; i++) {
				temp = (Locateable) geos[i];
				// same object visible value
				if (geo0.getStartPoint() != temp.getStartPoint()) {
					equalLocation = false;
					break;
				}

			}

			// set location textfield
			GeoPoint p = geo0.getStartPoint();
			if (equalLocation && p != null) {
				cbLocation.setSelectedItem(p.getLabel());
			} else
				cbLocation.setSelectedItem(null);

			cbLocation.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true; 
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!(geo instanceof Locateable && !((Locateable)geo).isAlwaysFixed()) 
						||	geo.isGeoImage())					  
				{
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		 * handle textfield changes
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == cbLocation)
				doActionPerformed();
		}

		private void doActionPerformed() {
			String strLoc = (String) cbLocation.getSelectedItem();
			GeoPoint newLoc = null;

			if (strLoc == null || strLoc.trim().length() == 0) {
				newLoc = null;
			} else {
				newLoc = kernel.getAlgebraProcessor().evaluateToPoint(strLoc);
			}

			for (int i = 0; i < geos.length; i++) {
				Locateable l = (Locateable) geos[i];
				try {
					l.setStartPoint(newLoc);
					l.toGeoElement().updateRepaint();
				} catch (CircularDefinitionException e) {
					app.showError("CircularDefinition");
				}
			}

			propPanel.updateSelection(geos);
		}

		public void focusGained(FocusEvent arg0) {
		}

		public void focusLost(FocusEvent e) {
			doActionPerformed();
		}
	}

	/**
	 * panel for three corner points of an image (A, B and D)
	 */
	private class CornerPointsPanel
		extends JPanel
		implements ActionListener, FocusListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos; // currently selected geos
		private JComboBox [] cbLocation;
		private DefaultComboBoxModel [] cbModel;

		public CornerPointsPanel() {
			cbLocation = new JComboBox[3];
			cbModel = new DefaultComboBoxModel[3];
			
			// put it all together
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			// textfield for animation step
			String strLabelStart = app.getPlain("CornerPoint");
			String strLabel;
			for (int i = 0; i < 3; i++) {
				int pointNumber = i < 2 ? (i+1) : (i+2);
				strLabel = strLabelStart + " " + pointNumber + ":";
	
				JLabel label = new JLabel(strLabel);
				cbLocation[i] = new JComboBox();
				cbLocation[i].setEditable(true);
				cbModel[i] = new DefaultComboBoxModel();
				cbLocation[i].setModel(cbModel[i]);
				label.setLabelFor(cbLocation[i]);
				cbLocation[i].addActionListener(this);
				cbLocation[i].addFocusListener(this);
				
				JPanel locPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				locPanel.add(label);
				locPanel.add(cbLocation[i]);
				add(locPanel);
			}
		}

		public JPanel update(Object[] geos) {
			this.geos = geos;
			if (!checkGeos(geos))
				return null;
			
			for (int k=0; k<3; k++) {
				cbLocation[k].removeActionListener(this);					
			}
			
			// repopulate model with names of points from the geoList's model
			// take all points from construction
			TreeSet points = kernel.getConstruction().getGeoSetLabelOrder(GeoElement.GEO_CLASS_POINT);
			if (points.size() != cbModel[0].getSize() - 1) {			
				// clear models
				for (int k=0; k<3; k++) {					
					cbModel[k].removeAllElements();
					cbModel[k].addElement(null);
				}
								
				// insert points
				Iterator it = points.iterator();
				int count=0;
				while (it.hasNext() || ++count > MAX_COMBOBOX_ENTRIES) {
					GeoPoint p = (GeoPoint) it.next();						
					for (int k=0; k<3; k++) {
						cbModel[k].addElement(p.getLabel());
					}
				}
			}

			for (int k=0; k<3; k++) {				
				// check if properties have same values
				GeoImage temp, geo0 = (GeoImage) geos[0];
				boolean equalLocation = true;
	
				for (int i = 0; i < geos.length; i++) {
					temp = (GeoImage) geos[i];
					// same object visible value
					if (geo0.getCorner(k) != temp.getCorner(k)) {
						equalLocation = false;
						break;
					}	
				}
	
				// set location textfield
				GeoPoint p = geo0.getCorner(k);
				if (equalLocation && p != null) {
					cbLocation[k].setSelectedItem(p.getLabel());
				} else
					cbLocation[k].setSelectedItem(null);
	
				cbLocation[k].addActionListener(this);
			}
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (geo instanceof GeoImage) {
					GeoImage img = (GeoImage) geo;
					if (img.isAbsoluteScreenLocActive() ||
							!img.isIndependent())
						return false;					
				} else
					return false;
			}
			return true;
		}

		/**
		 * handle textfield changes
		 */
		public void actionPerformed(ActionEvent e) {	
			doActionPerformed(e.getSource());
		}

		private void doActionPerformed(Object source) {
			int number = 0;
			if (source == cbLocation[1])
				number = 1;
			else if (source == cbLocation[2])
				number = 2;
			
			String strLoc = (String) cbLocation[number].getSelectedItem();
			GeoPoint newLoc = null;

			if (strLoc == null || strLoc.trim().length() == 0) {
				newLoc = null;
			} else {
				newLoc = kernel.getAlgebraProcessor().evaluateToPoint(strLoc);
			}

			for (int i = 0; i < geos.length; i++) {
				GeoImage im = (GeoImage) geos[i];		
				im.setCorner(newLoc, number);
				im.updateRepaint();				
			}

			propPanel.updateSelection(geos);
		}

		public void focusGained(FocusEvent arg0) {
		}

		public void focusLost(FocusEvent e) {
			doActionPerformed(e.getSource());
		}
	}

	/**
	 * panel for text editing
	 */
	private class TextEditPanel
		extends JPanel
		implements ActionListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;	
		private TextInputDialog td;
		
		public TextEditPanel() {	
			setBorder(BorderFactory.createTitledBorder(app.getPlain("Edit")));
			td = new TextInputDialog(app, app.getPlain("Text"), null, null,
										30, 5);
			setLayout(new BorderLayout());
			add(td.getInputPanel(), BorderLayout.CENTER);
			JPanel btPanel = new JPanel(new BorderLayout(0,0));
			btPanel.add(td.getLaTeXPanel(), BorderLayout.WEST);
			btPanel.add(td.getButtonPanel(), BorderLayout.EAST);
			add(btPanel, BorderLayout.SOUTH);
		}

		public JPanel update(Object[] geos) {			
			if (geos.length != 1 || !checkGeos(geos))
				return null;			
			
			GeoText text = (GeoText) geos[0];			
			td.setGeoText(text);					
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			return geos.length == 1 && geos[0] instanceof GeoText
			&& !((GeoText)geos[0]).isTextCommand();			
		}

		/**
		 * handle textfield changes
		 */
		public void actionPerformed(ActionEvent e) {
			//if (e.getSource() == btEdit)
			//	app.showTextDialog((GeoText) geos[0]);
		}
	}

	/**
	 * panel to select the kind of coordinates (cartesian or polar)
	 *  for GeoPoint and GeoVector
	 * @author Markus Hohenwarter
	 */
	private class CoordPanel extends JPanel implements ActionListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JComboBox coordCB;		

		public CoordPanel() {
			JLabel coordLabel = new JLabel(app.getPlain("Coordinates")+":");
			coordCB = new JComboBox();
			coordCB.addItem(app.getPlain("CartesianCoords")); // index 0
			coordCB.addItem(app.getPlain("PolarCoords")); // index 1
			coordCB.addItem(app.getPlain("ComplexNumber")); // index 2
			coordCB.addActionListener(this);

			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(coordLabel);
			add(coordCB);
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			coordCB.removeActionListener(this);

			//	check if properties have same values
			GeoVec3D geo0 = (GeoVec3D) geos[0];
			boolean equalMode = true;

			int mode;
			if (equalMode)
				mode = geo0.getMode();
			else
				mode = -1;
			switch (mode) {
				case Kernel.COORD_CARTESIAN :
					coordCB.setSelectedIndex(0);
					break;
				case Kernel.COORD_POLAR :
					coordCB.setSelectedIndex(1);
					break;
				case Kernel.COORD_COMPLEX :
					coordCB.setSelectedIndex(2);
					break;
				default :
					coordCB.setSelectedItem(null);
			}

			coordCB.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			//boolean allPoints = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoPoint
					|| geos[i] instanceof GeoVector)) {
					geosOK = false;
				}
				
				// check if fixed
				if (((GeoElement)geos[i]).isFixed()) geosOK = false;
				
				//if (!(geos[i] instanceof GeoPoint)) allPoints = false;
			}
			
			// remove ComplexNumber option if any vectors are in list
			//if (!allPoints && coordCB.getItemCount() == 3) coordCB.removeItemAt(2);
			
			return geosOK;
		}

		/**
		* action listener implementation for coord combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == coordCB) {
				GeoVec3D geo;
				switch (coordCB.getSelectedIndex()) {
					case 0 : // Kernel.CARTESIAN					
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoVec3D) geos[i];
							geo.setMode(Kernel.COORD_CARTESIAN);
							geo.updateRepaint();
						}
						break;

					case 1 : // Kernel.POLAR					
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoVec3D) geos[i];
							geo.setMode(Kernel.COORD_POLAR);
							geo.updateRepaint();
						}
						break;
					case 2 : // Kernel.COMPLEX					
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoVec3D) geos[i];
								geo.setMode(Kernel.COORD_COMPLEX);
								geo.updateRepaint();
						}
						break;
				}
			}
		}
	}

	/**
	 * panel to select the kind of line equation 
	 *  for GeoLine 
	 * @author Markus Hohenwarter
	 */
	private class LineEqnPanel extends JPanel implements ActionListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private DefaultComboBoxModel eqnCBmodel;
		private JComboBox eqnCB;
		private JLabel eqnLabel;

		public LineEqnPanel() {
			eqnLabel = new JLabel(app.getPlain("Equation") + ":");
			eqnCB = new JComboBox();
			eqnCBmodel = new DefaultComboBoxModel();
			eqnCB.setModel(eqnCBmodel);
			eqnCBmodel.addElement(app.getPlain("ImplicitLineEquation"));
			// index 0
			eqnCBmodel.addElement(app.getPlain("ExplicitLineEquation"));
			// index 1		
			eqnCBmodel.addElement(app.getPlain("ParametricForm")); // index 2
			eqnCB.addActionListener(this);

			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(eqnLabel);
			add(eqnCB);
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			eqnCB.removeActionListener(this);

			//	check if properties have same values
			GeoLine temp, geo0 = (GeoLine) geos[0];
			boolean equalMode = true;
			for (int i = 1; i < geos.length; i++) {
				temp = (GeoLine) geos[i];
				// same mode?
				if (geo0.getMode() != temp.getMode())
					equalMode = false;
			}

			int mode;
			if (equalMode)
				mode = geo0.getMode();
			else
				mode = -1;
			switch (mode) {
				case GeoLine.EQUATION_IMPLICIT :
					eqnCB.setSelectedIndex(0);
					break;
				case GeoLine.EQUATION_EXPLICIT :
					eqnCB.setSelectedIndex(1);
					break;
				case GeoLine.PARAMETRIC :
					eqnCB.setSelectedIndex(2);
					break;
				default :
					eqnCB.setSelectedItem(null);
			}

			eqnCB.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoLine)
					|| geos[i] instanceof GeoSegment) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* action listener implementation for coord combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == eqnCB) {
				GeoLine geo;
				switch (eqnCB.getSelectedIndex()) {
					case 0 : // GeoLine.EQUATION_IMPLICIT				
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoLine) geos[i];
							geo.setMode(GeoLine.EQUATION_IMPLICIT);
							geo.updateRepaint();
						}
						break;

					case 1 : // GeoLine.EQUATION_EXPLICIT				
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoLine) geos[i];
							geo.setMode(GeoLine.EQUATION_EXPLICIT);
							geo.updateRepaint();
						}
						break;

					case 2 : // GeoLine.PARAMETRIC	
						for (int i = 0; i < geos.length; i++) {
							geo = (GeoLine) geos[i];
							geo.setMode(GeoLine.PARAMETRIC);
							geo.updateRepaint();
						}
						break;
				}
			}
		}
	}

	/**
	 * panel to select the kind of conic equation 
	 *  for GeoConic 
	 * @author Markus Hohenwarter
	 */
	private class ConicEqnPanel extends JPanel implements ActionListener, UpdateablePanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private DefaultComboBoxModel eqnCBmodel;
		private JComboBox eqnCB;
		private JLabel eqnLabel;
		int implicitIndex, explicitIndex, specificIndex;

		public ConicEqnPanel() {
			eqnLabel = new JLabel(app.getPlain("Equation") + ":");
			eqnCB = new JComboBox();
			eqnCBmodel = new DefaultComboBoxModel();
			eqnCB.setModel(eqnCBmodel);
			eqnCB.addActionListener(this);

			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(eqnLabel);
			add(eqnCB);
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			eqnCB.removeActionListener(this);

			// check if all conics have same type and mode
			// and if specific, explicit is possible		 
			GeoConic temp, geo0 = (GeoConic) geos[0];
			boolean equalType = true;
			boolean equalMode = true;
			boolean specificPossible = geo0.isSpecificPossible();
			boolean explicitPossible = geo0.isExplicitPossible();
			for (int i = 1; i < geos.length; i++) {
				temp = (GeoConic) geos[i];
				// same type?
				if (geo0.getType() != temp.getType())
					equalType = false;
				// same mode?
				if (geo0.getToStringMode() != temp.getToStringMode())
					equalMode = false;
				// specific equation possible?
				if (!temp.isSpecificPossible())
					specificPossible = false;
				// explicit equation possible?
				if (!temp.isExplicitPossible())
					explicitPossible = false;
			}

			// specific can't be shown because there are different types
			if (!equalType)
				specificPossible = false;

			specificIndex = -1;
			explicitIndex = -1;
			implicitIndex = -1;
			int counter = -1;
			eqnCBmodel.removeAllElements();
			if (specificPossible) {
				eqnCBmodel.addElement(geo0.getSpecificEquation());
				specificIndex = ++counter;
			}
			if (explicitPossible) {
				eqnCBmodel.addElement(app.getPlain("ExplicitConicEquation"));
				explicitIndex = ++counter;
			}
			implicitIndex = ++counter;
			eqnCBmodel.addElement(app.getPlain("ImplicitConicEquation"));

			int mode;
			if (equalMode)
				mode = geo0.getToStringMode();
			else
				mode = -1;
			switch (mode) {
				case GeoConic.EQUATION_SPECIFIC :
					if (specificIndex > -1)
						eqnCB.setSelectedIndex(specificIndex);
					break;

				case GeoConic.EQUATION_EXPLICIT :
					if (explicitIndex > -1)
						eqnCB.setSelectedIndex(explicitIndex);
					break;

				case GeoConic.EQUATION_IMPLICIT :
					eqnCB.setSelectedIndex(implicitIndex);
					break;

				default :
					eqnCB.setSelectedItem(null);
			}

			eqnCB.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (geos[i].getClass() != GeoConic.class) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* action listener implementation for coord combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == eqnCB) {
				GeoConic geo;
				int selIndex = eqnCB.getSelectedIndex();
				if (selIndex == specificIndex) {
					for (int i = 0; i < geos.length; i++) {
						geo = (GeoConic) geos[i];
						geo.setToSpecific();
						geo.updateRepaint();
					}
				} else if (selIndex == explicitIndex) {
					for (int i = 0; i < geos.length; i++) {
						geo = (GeoConic) geos[i];
						geo.setToExplicit();
						geo.updateRepaint();
					}
				} else if (selIndex == implicitIndex) {
					for (int i = 0; i < geos.length; i++) {
						geo = (GeoConic) geos[i];
						geo.setToImplicit();
						geo.updateRepaint();
					}
				}
			}
		}
	}

	/**
	 * panel to select the size of a GeoPoint
	 * @author Markus Hohenwarter
	 */
	private class PointSizePanel extends JPanel implements ChangeListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JSlider slider;

		public PointSizePanel() {			
			//setBorder(BorderFactory.createTitledBorder(app.getPlain("Size")));
			//JLabel sizeLabel = new JLabel(app.getPlain("Size") + ":");		
		
			slider = new JSlider(1, 9);
			slider.setMajorTickSpacing(2);
			slider.setMinorTickSpacing(1);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);	
			
			/*
			Dimension dim = slider.getPreferredSize();
			dim.width = SLIDER_MAX_WIDTH;
			slider.setMaximumSize(dim);		
			slider.setPreferredSize(dim);	
			*/

			// set label font
			Dictionary labelTable = slider.getLabelTable();
			Enumeration en = labelTable.elements();
			JLabel label;
			while (en.hasMoreElements()) {
				label = (JLabel) en.nextElement();
				label.setFont(app.getSmallFont());
			}

			slider.setFont(app.getSmallFont());	
			slider.addChangeListener(this);			
			
			setBorder(BorderFactory.createTitledBorder(app.getPlain("PointSize") ));		
			add(slider);			
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			slider.removeChangeListener(this);

			//	set value to first point's size 
			PointProperties geo0 = (PointProperties) geos[0];
			slider.setValue(geo0.getPointSize());

			slider.addChangeListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement)geos[i];
				if (!(geo.getGeoElementForPropertiesDialog().isGeoPoint())
						&& (!(geo.isGeoList() && ((GeoList)geo).showPointProperties()))) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void stateChanged(ChangeEvent e) {
			if (!slider.getValueIsAdjusting()) {
				int size = slider.getValue();
				PointProperties point;
				for (int i = 0; i < geos.length; i++) {
					point = (PointProperties) geos[i];
					point.setPointSize(size);
					point.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel to change the point style
	 * @author Florian Sonner
	 * @version 2008-07-17
	 */
	private class PointStylePanel extends JPanel implements UpdateablePanel, ActionListener {
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JRadioButton[] buttons;
		private JComboBox cbStyle;  //G.Sturr 20009-9-18
		
		public PointStylePanel() {
			
			//G.Sturr 2009-9-18
			// added comboBox for point styles
			PointStyleListRenderer renderer = new PointStyleListRenderer();
			renderer.setPreferredSize(new Dimension(18,18));		
			cbStyle = new JComboBox(EuclidianView.getPointStyles());
			cbStyle.setRenderer(renderer);
			cbStyle.setMaximumRowCount(EuclidianView.MAX_POINT_STYLE+1);
			cbStyle.setBackground(getBackground());
			cbStyle.addActionListener(this);
						
			buttons = new JRadioButton[2];
			buttons[0] = new JRadioButton(app.getPlain("Default"));			
			buttons[0].setActionCommand("default");
			buttons[0].addActionListener(this);
			buttons[1] = new JRadioButton();
			add(buttons[0]);
			add(buttons[1]);
			
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(buttons[0]);
			buttonGroup.add(buttons[1]);
			
			add(cbStyle);
			setBorder(BorderFactory.createTitledBorder(app.getMenu("PointStyle") ));
			
			/*
			setLayout(new GridLayout(2,0)); // two rows
			
			ButtonGroup buttonGroup = new ButtonGroup();
			
			String[] strPointStyle = { app.getPlain("Default"), "\u25cf", "\u25cb", "\u2716", "\u271a", "\u25c6", "\u25b2", "\u25bc" };
			String[] strPointStyleAC = { "-1", "0", "2", "1", "3", "4", "6", "7" };
			buttons = new JRadioButton[strPointStyle.length];

			for(int i = 0; i < strPointStyle.length; ++i) {
				buttons[i] = new JRadioButton(strPointStyle[i]);
				buttons[i].setActionCommand(strPointStyleAC[i]);
				buttons[i].addActionListener(this);
				
				if(!strPointStyleAC[i].equals("-1"))
					buttons[i].setFont(app.getSmallFont());
				
				buttonGroup.add(buttons[i]);
				buttons[0].setVerticalAlignment(SwingConstants.BOTTOM);
				add(buttons[i]);
			}	
		    
			setBorder(BorderFactory.createTitledBorder(app.getMenu("PointStyle") ));					
			*/		
		
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;

			//	set value to first point's style 
			PointProperties geo0 = (PointProperties) geos[0];

			//
			//G.Sturr 2009-9-18
			cbStyle.removeActionListener(this);
			if(geo0.getPointStyle() == -1){   // default style
				buttons[0].setSelected(true);
				cbStyle.setSelectedIndex(app.getEuclidianView().getPointStyle());
				
			} else {                         //custom style
				buttons[1].setSelected(true);
				cbStyle.setSelectedIndex(geo0.getPointStyle());
			}
			cbStyle.addActionListener(this);
			// end
			
			
		/*	
			for(int i = 0; i < buttons.length; ++i) {
				if(Integer.parseInt(buttons[i].getActionCommand()) == geo0.getPointStyle())
					buttons[i].setSelected(true);
				else
					buttons[i].setSelected(false);
			}
		*/
		 
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement)geos[i];
				if (!geo.getGeoElementForPropertiesDialog().isGeoPoint()
						&& (!(geo.isGeoList() && ((GeoList)geo).showPointProperties()))) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}
		
		public void actionPerformed(ActionEvent e) {
			//int style = Integer.parseInt(e.getActionCommand());
				
			//G.Sturr added 2009-9-18
			int style = -1;
			// comboBox click
			if (e.getSource() == cbStyle){
				style = cbStyle.getSelectedIndex();
				buttons[1].removeActionListener(this);
				buttons[1].setSelected(true);
				buttons[1].addActionListener(this);
			}	
			// default button click	
			if (e.getActionCommand()=="default"){	
				cbStyle.removeActionListener(this);
				cbStyle.setSelectedIndex(app.getEuclidianView().getPointStyle());
				cbStyle.addActionListener(this);
			}
			
			//G,Sturr end	
		
			PointProperties point;
			for (int i = 0; i < geos.length; i++) {
				point = (PointProperties) geos[i];
				point.setPointStyle(style);
				point.updateRepaint();
			}
		}
	}
	
	/**
	 * panel to select the size of a GeoText
	 * @author Markus Hohenwarter
	 */
	private class TextOptionsPanel extends JPanel implements ActionListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JComboBox cbFont, cbSize, cbDecimalPlaces;		
		private JToggleButton btBold, btItalic;
		
		private JPanel secondLine;
		private boolean secondLineVisible = false;

		
		public TextOptionsPanel() {	
			// font: serif, sans serif
			String [] fonts = { "Sans Serif", "Serif" };
			cbFont = new JComboBox(fonts);
			cbFont.addActionListener(this);	
			
			// font size					
			cbSize = new JComboBox();
			int fontSize = app.getFontSize();			
			for (int i=fontSize - 2; i <= fontSize + 6; i = i + 2) {
				cbSize.addItem(Integer.toString(i));
			}
			cbSize.addActionListener(this);						
			
			// toggle buttons for bold and italic
			btBold = new JToggleButton(app.getPlain("Bold").substring(0,1));
			btBold.setFont(app.getBoldFont());
			btBold.addActionListener(this);			
			btItalic = new JToggleButton(app.getPlain("Italic").substring(0,1));
			btItalic.setFont(app.getPlainFont().deriveFont(Font.ITALIC));
			btItalic.addActionListener(this);		
			
			// decimal places			
			ComboBoxRenderer renderer = new ComboBoxRenderer();
		    cbDecimalPlaces = new JComboBox(app.getRoundingMenu());
		    cbDecimalPlaces.setRenderer(renderer);
		    cbDecimalPlaces.addActionListener(this);

			// font, size
			JPanel firstLine = new JPanel();
			firstLine.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));			
			firstLine.add(cbFont);			
			firstLine.add(cbSize);	
			firstLine.add(btBold);
			firstLine.add(btItalic);
			
			// bold, italic
			secondLine = new JPanel();
			secondLine.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));		
			JLabel decimalLabel  = new JLabel(app.getMenu("Rounding") + ":");					
			secondLine.add(decimalLabel);
			secondLine.add(cbDecimalPlaces);									
			
			setLayout(new BorderLayout(5,5));
			add(firstLine, BorderLayout.NORTH);
			add(secondLine, BorderLayout.SOUTH);	
			secondLineVisible = true;
		}

		class ComboBoxRenderer extends JLabel implements ListCellRenderer {
		    JSeparator separator;

		    public ComboBoxRenderer() {
		      setOpaque(true);
		      setBorder(new EmptyBorder(1, 1, 1, 1));
		      separator = new JSeparator(JSeparator.HORIZONTAL);
		    }

		    public Component getListCellRendererComponent(JList list, Object value,
		        int index, boolean isSelected, boolean cellHasFocus) {
		      String str = (value == null) ? "" : value.toString();
		      if ("---".equals(str)) {
		        return separator;
		      }
		      if (isSelected) {
		        setBackground(list.getSelectionBackground());
		        setForeground(list.getSelectionForeground());
		      } else {
		        setBackground(list.getBackground());
		        setForeground(list.getForeground());
		      }
		      setFont(list.getFont());
		      setText(str);
		      return this;
		    }
		  }
		
		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;			
			
			cbSize.removeActionListener(this);
			cbFont.removeActionListener(this);
			cbDecimalPlaces.removeActionListener(this);

			//	set value to first text's size and style
			TextProperties geo0 = (TextProperties) geos[0];		
			
			cbSize.setSelectedItem(Integer.toString(geo0.getFontSize()+app.getFontSize()));
			cbFont.setSelectedIndex(geo0.isSerifFont() ? 1 : 0);
			int selItem = -1;
			
			int decimals = geo0.getPrintDecimals();
			if (decimals > 0 && decimals < Application.decimalsLookup.length && !geo0.useSignificantFigures())
				selItem = Application.decimalsLookup[decimals];

			int figures = geo0.getPrintFigures();
			if (figures > 0 && figures < Application.figuresLookup.length && geo0.useSignificantFigures())
				selItem = Application.figuresLookup[figures];
			
			cbDecimalPlaces.setSelectedIndex(selItem);
			
			if (((GeoElement)geo0).isIndependent()
					|| (geo0 instanceof GeoList)) { // don't want rounding option for lists of texts?
				if (secondLineVisible) {
					remove(secondLine);	
					secondLineVisible = false;
				}				
			} else {
				if (!secondLineVisible) {
					add(secondLine, BorderLayout.SOUTH);
					secondLineVisible = true;
				}	
			}
		
			int style = geo0.getFontStyle();
			btBold.setSelected(style == Font.BOLD || style == (Font.BOLD + Font.ITALIC));
			btItalic.setSelected(style == Font.ITALIC || style == (Font.BOLD + Font.ITALIC));				
			
			
			
			cbSize.addActionListener(this);
			cbFont.addActionListener(this);			
			cbDecimalPlaces.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(((GeoElement)geos[i]).getGeoElementForPropertiesDialog().isGeoText())) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			
			if (source == cbSize) {
				int size = Integer.parseInt(cbSize.getSelectedItem().toString()) - app.getFontSize();			
				TextProperties text;
				for (int i = 0; i < geos.length; i++) {
					text = (TextProperties) geos[i];
					text.setFontSize(size);
					((GeoElement)text).updateRepaint();
				}
			} 
			else if (source == cbFont) {
				boolean serif = cbFont.getSelectedIndex() == 1;		
				TextProperties text;
				for (int i = 0; i < geos.length; i++) {
					text = (TextProperties) geos[i];
					text.setSerifFont(serif);
					((GeoElement)text).updateRepaint();
				}
			}
			else if (source == cbDecimalPlaces) {
				int decimals = cbDecimalPlaces.getSelectedIndex();
				//Application.debug(decimals+"");
				//Application.debug(roundingMenuLookup[decimals]+"");
				TextProperties text;
				for (int i = 0; i < geos.length; i++) {
					text = (TextProperties) geos[i];
					if (decimals < 8) // decimal places
					{
						//Application.debug("decimals"+roundingMenuLookup[decimals]+"");
						text.setPrintDecimals(Application.roundingMenuLookup[decimals], true);
					}
					else // significant figures
					{
						//Application.debug("figures"+roundingMenuLookup[decimals]+"");
						text.setPrintFigures(Application.roundingMenuLookup[decimals], true);
					}
					((GeoElement)text).updateRepaint();
				}
			}
			else if (source == btBold || source == btItalic) {
				int style = 0;
				if (btBold.isSelected()) style += 1;
				if (btItalic.isSelected()) style += 2;
					
				TextProperties text;
				for (int i = 0; i < geos.length; i++) {
					text = (TextProperties) geos[i];
					text.setFontStyle(style);
					((GeoElement)text).updateRepaint();
				}
			}								
		}
	}
	
	/**
	 * panel to select the size of a GeoPoint
	 * @author Markus Hohenwarter
	 */
	private class SlopeTriangleSizePanel
		extends JPanel
		implements ChangeListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JSlider slider;

		public SlopeTriangleSizePanel() {
			setBorder(BorderFactory.createTitledBorder(app.getPlain("Size")));
			//JLabel sizeLabel = new JLabel(app.getPlain("Size") + ":");		
			slider = new JSlider(1, 10);
			slider.setMajorTickSpacing(2);
			slider.setMinorTickSpacing(1);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);

			/*
			Dimension dim = slider.getPreferredSize();
			dim.width = SLIDER_MAX_WIDTH;			
			slider.setMaximumSize(dim);
			slider.setPreferredSize(dim);
*/
			
			// set label font
			Dictionary labelTable = slider.getLabelTable();
			Enumeration en = labelTable.elements();
			JLabel label;
			while (en.hasMoreElements()) {
				label = (JLabel) en.nextElement();
				label.setFont(app.getSmallFont());
			}

			//slider.setFont(app.getSmallFont());	
			slider.addChangeListener(this);

			/*
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));	
			sizeLabel.setAlignmentY(Component.TOP_ALIGNMENT);
			slider.setAlignmentY(Component.TOP_ALIGNMENT);			
			//setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
			//		BorderFactory.createEmptyBorder(3,5,0,5)));	
			add(Box.createRigidArea(new Dimension(5,0)));
			add(sizeLabel);
			*/		
			add(slider);			
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			slider.removeChangeListener(this);

			//	set value to first point's size 
			GeoNumeric geo0 = (GeoNumeric) geos[0];
			slider.setValue(geo0.getSlopeTriangleSize());

			slider.addChangeListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				if (!(geo instanceof GeoNumeric
					&& geo.getParentAlgorithm() instanceof AlgoSlope)) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void stateChanged(ChangeEvent e) {
			if (!slider.getValueIsAdjusting()) {
				int size = slider.getValue();
				GeoNumeric num;
				for (int i = 0; i < geos.length; i++) {
					num = (GeoNumeric) geos[i];
					num.setSlopeTriangleSize(size);
					num.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel to select the size of a GeoAngle's arc
	 * @author Markus Hohenwarter
	 */
	private class ArcSizePanel extends JPanel implements ChangeListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JSlider slider;

		public ArcSizePanel() {
			setBorder(BorderFactory.createTitledBorder(app.getPlain("Size")));
			//JLabel sizeLabel = new JLabel(app.getPlain("Size") + ":");		
			slider = new JSlider(10, 100);
			slider.setMajorTickSpacing(10);
			slider.setMinorTickSpacing(5);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);

			/*
			Dimension dim = slider.getPreferredSize();
			dim.width = SLIDER_MAX_WIDTH;
			slider.setMaximumSize(dim);
			slider.setPreferredSize(dim);
*/
			
			// set label font
			Dictionary labelTable = slider.getLabelTable();
			Enumeration en = labelTable.elements();
			JLabel label;
			while (en.hasMoreElements()) {
				label = (JLabel) en.nextElement();
				label.setFont(app.getSmallFont());
			}

			/*
			//slider.setFont(app.getSmallFont());	
			slider.addChangeListener(this);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));	
			sizeLabel.setAlignmentY(Component.TOP_ALIGNMENT);
			slider.setAlignmentY(Component.TOP_ALIGNMENT);			
			//setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
			//		BorderFactory.createEmptyBorder(3,5,0,5)));
			add(Box.createRigidArea(new Dimension(5,0)));
			add(sizeLabel);
			*/		
			add(slider);			
		}
		//added by Lo�c BEGIN
		public void setMinValue(){
			slider.setValue(20);
		}
		// END
		
		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			slider.removeChangeListener(this);

			//	set value to first point's size 
			GeoAngle geo0 = (GeoAngle) geos[0];
			slider.setValue(geo0.getArcSize());

			slider.addChangeListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (geos[i] instanceof GeoAngle) {
					GeoAngle angle = (GeoAngle) geos[i];
					if (angle.isIndependent() || !angle.isDrawable()) {
						geosOK = false;
						break;
					}
				} else {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void stateChanged(ChangeEvent e) {
			if (!slider.getValueIsAdjusting()) {
				int size = slider.getValue();
				GeoAngle angle;
				for (int i = 0; i < geos.length; i++) {
					angle = (GeoAngle) geos[i];
					// addded by Lo�c BEGIN
					// check if decoration could be drawn
					if (size<20&&(angle.decorationType==GeoElement.DECORATION_ANGLE_THREE_ARCS
							|| angle.decorationType==GeoElement.DECORATION_ANGLE_TWO_ARCS)){
						angle.setArcSize(20);
						int selected=((GeoAngle)geos[0]).decorationType;
						if (selected==GeoElement.DECORATION_ANGLE_THREE_ARCS
							|| selected==GeoElement.DECORATION_ANGLE_TWO_ARCS){
							slider.setValue(20);							
							}
						}
					//END
					else angle.setArcSize(size);
					angle.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel to select the filling of a polygon or conic section
	 * @author Markus Hohenwarter
	 */
	private class FillingPanel extends JPanel implements ChangeListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JSlider slider;

		public FillingPanel() {
			setBorder(BorderFactory.createTitledBorder(app.getPlain("Filling")));
			//JLabel sizeLabel = new JLabel(app.getPlain("Filling") + ":");		
			slider = new JSlider(0, 100);
			slider.setMajorTickSpacing(25);
			slider.setMinorTickSpacing(5);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);

			/*
			Dimension dim = slider.getPreferredSize();
			dim.width = SLIDER_MAX_WIDTH;
			slider.setMaximumSize(dim);
			slider.setPreferredSize(dim);
*/
			
			// set label font
			Dictionary labelTable = slider.getLabelTable();
			Enumeration en = labelTable.elements();
			JLabel label;
			while (en.hasMoreElements()) {
				label = (JLabel) en.nextElement();
				label.setFont(app.getSmallFont());
			}

			/*
			//slider.setFont(app.getSmallFont());	
			slider.addChangeListener(this);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));	
			sizeLabel.setAlignmentY(TOP_ALIGNMENT);
			slider.setAlignmentY(TOP_ALIGNMENT);			
			//setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
			//		BorderFactory.createEmptyBorder(3,5,0,5)));
			add(Box.createRigidArea(new Dimension(5,0)));			
			add(sizeLabel);			
			*/
			add(slider);			
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			slider.removeChangeListener(this);

			//	set value to first geo's alpha value
			double alpha = ((GeoElement) geos[0]).getAlphaValue();
			slider.setValue((int) Math.round(alpha * 100));

			slider.addChangeListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!((GeoElement) geos[i]).isFillable()) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void stateChanged(ChangeEvent e) {
			if (!slider.getValueIsAdjusting()) {
				float alpha = slider.getValue() / 100.0f;
				GeoElement geo;
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setAlphaValue(alpha);
					geo.updateRepaint();
				}
			}
		}
	}

	/**
	 * panel to select thickness and style (dashing) of a GeoLine
	 * @author Markus Hohenwarter
	 */
	private class LineStylePanel
		extends JPanel
		implements ChangeListener, ActionListener, UpdateablePanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object[] geos;
		private JSlider slider;
		private JComboBox dashCB;

		public LineStylePanel() {
			// thickness slider		
			slider = new JSlider(1, 13);
			slider.setMajorTickSpacing(2);
			slider.setMinorTickSpacing(1);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);

			/*
			Dimension dim = slider.getPreferredSize();
			dim.width = SLIDER_MAX_WIDTH;
			slider.setMaximumSize(dim);
			slider.setPreferredSize(dim);
*/
			
			// set label font
			Dictionary labelTable = slider.getLabelTable();
			Enumeration en = labelTable.elements();
			JLabel label;
			while (en.hasMoreElements()) {
				label = (JLabel) en.nextElement();
				label.setFont(app.getSmallFont());
			}
			//slider.setFont(app.getSmallFont());	
			slider.addChangeListener(this);

			// line style combobox (dashing)		
			DashListRenderer renderer = new DashListRenderer();
			renderer.setPreferredSize(
				new Dimension(130, app.getFontSize() + 6));
			dashCB = new JComboBox(EuclidianView.getLineTypes());
			dashCB.setRenderer(renderer);
			dashCB.addActionListener(this);

			// line style panel
			JPanel dashPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JLabel dashLabel = new JLabel(app.getPlain("LineStyle") + ":");
			dashPanel.add(dashLabel);
			dashPanel.add(dashCB);

			// thickness panel
			JPanel thicknessPanel = new JPanel();
			thicknessPanel.setBorder(
				BorderFactory.createTitledBorder(app.getPlain("Thickness")));
			/*
			JLabel thicknessLabel = new JLabel(app.getPlain("Thickness") + ":");
			thicknessPanel.setLayout(new BoxLayout(thicknessPanel, BoxLayout.X_AXIS));	
			thicknessLabel.setAlignmentY(Component.TOP_ALIGNMENT);
			slider.setAlignmentY(Component.TOP_ALIGNMENT);			
			//thicknessPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
			//		BorderFactory.createEmptyBorder(3,5,0,5)));	
			thicknessPanel.add(Box.createRigidArea(new Dimension(5,0)));
			thicknessPanel.add(thicknessLabel);
			*/				
			thicknessPanel.add(slider);			

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			thicknessPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			dashPanel.setAlignmentX(Component.LEFT_ALIGNMENT);	
			add(thicknessPanel);
			add(dashPanel);			
		}
		
		private boolean allPolygons(Object[] geos) {
			
			if (geos == null || geos.length == 0) return false;
			
			for (int i = 0  ; i < geos.length ; i++) {
				GeoElement testGeo = ((GeoElement)geos[i]).getGeoElementForPropertiesDialog();
				if (!testGeo.isGeoPolygon() && !testGeo.isGeoList())
					return false;
			}
			
			return true;
			
		}

		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;

			this.geos = geos;
			slider.removeChangeListener(this);
			dashCB.removeActionListener(this);

			//	set slider value to first geo's thickness 
			GeoElement temp, geo0 = (GeoElement) geos[0];
			slider.setValue(geo0.getLineThickness());
			
			// allow polygons to have thickness 0
			slider.setMinimum(allPolygons(geos) ? 0 : 1);

			//	check if geos have same line style
			boolean equalStyle = true;
			for (int i = 1; i < geos.length; i++) {
				temp = (GeoElement) geos[i];
				// same style?
				if (geo0.getLineType() != temp.getLineType())
					equalStyle = false;
			}

			// select common line style
			if (equalStyle) {
				int type = geo0.getLineType();				
				for (int i = 0; i < dashCB.getItemCount(); i++) {
					if (type == ((Integer) dashCB.getItemAt(i)).intValue()) {
						dashCB.setSelectedIndex(i);
						break;
					}
				}
			} else
				dashCB.setSelectedItem(null);

			slider.addChangeListener(this);
			dashCB.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = ((GeoElement) geos[i]).getGeoElementForPropertiesDialog();
				if (!(geo.isPath()
					|| (geo.isGeoList() && ((GeoList)geo).showLineProperties() )
					|| (geo.isGeoNumeric()
						&& ((GeoNumeric) geo).isDrawable()))) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}

		/**
		* change listener implementation for slider
		*/
		public void stateChanged(ChangeEvent e) {
			if (!slider.getValueIsAdjusting()) {
				int size = slider.getValue();
				GeoElement geo;
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setLineThickness(size);
					geo.updateRepaint();
				}
			}
		}

		/**
		* action listener implementation for coord combobox
		*/
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == dashCB) {
				GeoElement geo;
				int type = ((Integer) dashCB.getSelectedItem()).intValue();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoElement) geos[i];
					geo.setLineType(type);
					geo.updateRepaint();
				}
			}
		}
	} 

	
	// added by Lo�c
	private class DecoSegmentPanel extends JPanel implements ActionListener , UpdateablePanel {
		private JComboBox decoCombo;
		private Object[] geos;
		DecoSegmentPanel(){
			super(new FlowLayout(FlowLayout.LEFT));
			// deco combobox 		
			DecorationListRenderer renderer = new DecorationListRenderer();
			renderer.setPreferredSize(new Dimension(130, app.getFontSize() + 6));
			decoCombo = new JComboBox(GeoSegment.getDecoTypes());
			decoCombo.setRenderer(renderer);
			decoCombo.addActionListener(this);

			JLabel decoLabel = new JLabel(app.getPlain("Decoration") + ":");
			add(decoLabel);
			add(decoCombo);
		}
		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;
			this.geos = geos;
			decoCombo.removeActionListener(this);

			//	set slider value to first geo's thickness 
			GeoSegment geo0 = (GeoSegment) geos[0];
			decoCombo.setSelectedIndex(geo0.decorationType);

			decoCombo.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoSegment)) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}
		
		public void actionPerformed(ActionEvent e){
			Object source = e.getSource();
			if (source == decoCombo) {
				GeoSegment geo;
				int type = ((Integer) decoCombo.getSelectedItem()).intValue();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoSegment) geos[i];
// Michael Borcherds 2007-11-20 BEGIN
//					geo.decorationType = type;
					geo.setDecorationType(type);
// Michael Borcherds 2007-11-20 END
					geo.updateRepaint();
				}
			}
		}
	}
	
	private class DecoAnglePanel extends JPanel implements ActionListener , UpdateablePanel{
		private JComboBox decoCombo;
		private Object[] geos;
		DecoAnglePanel(){
			super(new FlowLayout(FlowLayout.LEFT));
			// deco combobox 		
			DecorationAngleListRenderer renderer = new DecorationAngleListRenderer();
			renderer.setPreferredSize(new Dimension(80, 30));
			decoCombo = new JComboBox(GeoAngle.getDecoTypes());
			decoCombo.setRenderer(renderer);
			decoCombo.addActionListener(this);
			JLabel decoLabel = new JLabel(app.getPlain("Decoration") + ":");
			add(decoLabel);
			add(decoCombo);		
		}
		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;
			this.geos = geos;
			decoCombo.removeActionListener(this);

			//	set slider value to first geo's decoration 
			GeoAngle geo0 = (GeoAngle) geos[0];
			decoCombo.setSelectedIndex(geo0.decorationType);
			decoCombo.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoAngle)) {
					geosOK = false;
					break;
				}
			}
			return geosOK;
		}
		
		public void actionPerformed(ActionEvent e){
			Object source = e.getSource();
			if (source == decoCombo) {
				GeoAngle geo;
				int type = ((Integer) decoCombo.getSelectedItem()).intValue();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoAngle) geos[i];
// Michael Borcherds 2007-11-20 BEGIN
//					geo.decorationType = type;
					geo.setDecorationType(type);
// Michael Borcherds 2007-11-20 END
					// addded by Lo�c BEGIN
					// check if decoration could be drawn
					if (geo.getArcSize()<20&&(geo.decorationType==GeoElement.DECORATION_ANGLE_THREE_ARCS
							|| geo.decorationType==GeoElement.DECORATION_ANGLE_TWO_ARCS)){
						geo.setArcSize(20);
						propPanel.setSliderMinValue();
						}
					//END
					geo.updateRepaint();
				}
			}
		}
	}
	
	// added 3/11/06
	private class RightAnglePanel extends JPanel implements ActionListener , UpdateablePanel {
		private JCheckBox emphasizeRightAngle;
		private Object[] geos;
		RightAnglePanel(){
			super(new FlowLayout(FlowLayout.LEFT));
			emphasizeRightAngle=new JCheckBox(app.getPlain("EmphasizeRightAngle"));
			emphasizeRightAngle.addActionListener(this);
			add(emphasizeRightAngle);
		}
		public JPanel update(Object[] geos) {
			// check geos
			if (!checkGeos(geos))
				return null;
			this.geos = geos;
			emphasizeRightAngle.removeActionListener(this);

			//	set JcheckBox value to first geo's decoration 
			GeoAngle geo0 = (GeoAngle) geos[0];
			emphasizeRightAngle.setSelected(geo0.isEmphasizeRightAngle());
			emphasizeRightAngle.addActionListener(this);
			return this;
		}

		private boolean checkGeos(Object[] geos) {
			boolean geosOK = true;
			for (int i = 0; i < geos.length; i++) {
				if (!(geos[i] instanceof GeoAngle)) {
					geosOK = false;
					break;
				}
				/*
				// If it isn't a right angle
				else if (!kernel.isEqual(((GeoAngle)geos[i]).getValue(), Kernel.PI_HALF)){
					geosOK=false;
					break;
				}*/
			}
			return geosOK;
		}
		
		public void actionPerformed(ActionEvent e){
			Object source = e.getSource();
			if (source == emphasizeRightAngle) {
				GeoAngle geo;
				boolean b=emphasizeRightAngle.isSelected();
				for (int i = 0; i < geos.length; i++) {
					geo = (GeoAngle) geos[i];
					geo.setEmphasizeRightAngle(b);
					geo.updateRepaint();
				}
			}
		}
		
	}
	// END 

	
	
	
	/**
	 * INNER CLASS
	 * JList for displaying GeoElements
	 * @see GeoTreeCellRenderer
	 * @author Markus Hohenwarter
	 */
	private class JTreeGeoElements extends JTree implements View, MouseMotionListener, MouseListener {
	
		private static final long serialVersionUID = 1L;
		private DefaultTreeModel treeModel;
		private DefaultMutableTreeNode root;
		private HashMap typeNodesMap;		

		/*
		 * has to be registered as view for GeoElement 
		 */
		public JTreeGeoElements() {
			// build default tree structure
			root = new DefaultMutableTreeNode(app.getPlain("Objects"));					

			// create model from root node
			treeModel = new DefaultTreeModel(root);				
			setModel(treeModel);
			setLargeModel(true);
			typeNodesMap = new HashMap();
			
			getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);		
			GeoTreeCellRenderer renderer = new GeoTreeCellRenderer(app);			
			setCellRenderer(renderer);
			setRowHeight(-1); // to enable flexible height of cells

			// 	tree's options             
			setRootVisible(true);
			// show lines from parent to children
			//putClientProperty("JTree.lineStyle", "None");
			setInvokesStopCellEditing(true);
			setScrollsOnExpand(true);	
			
			addMouseMotionListener(this);
			addMouseListener(this);
		}				
		
		protected void setExpandedState(TreePath path, boolean state) {
            // Ignore all collapse requests of root        	
            if (path != getPathForRow(0)) {
                super.setExpandedState(path, state);
            }
        }
		
		public void expandAll() {
		    int row = 0;
		    while (row < getRowCount()) {
		      expandRow(row);
		      row++;
	       }
	    }
		
		public void collapseAll() {
		    int row = 1;
		    while (row < getRowCount()) {
		      collapseRow(row);
		      row++;
	       }
	    }
		
		
		

		/**
		 * selects object geo in the list of GeoElements	 
		 * @param addToSelection: false => clear old selection 
		 */
		public void setSelected(ArrayList geos, boolean addToSelection) {
			TreePath tp = null;	
					
			TreeSelectionModel lsm = getSelectionModel();					
			if (geos == null || geos.size() == 0) {
				lsm.clearSelection();
				selectFirstElement();
			}			
			else {
				// make sure geos are in list, this is needed when MAX_OBJECTS_IN_TREE was 
				// exceeded in setViewActive(true)
//				for (int i=0; i < geos.size(); i++) {
//					GeoElement geo = (GeoElement) geos.get(i);
//					add(geo);
//				}								
				
				if (!addToSelection) 
					lsm.clearSelection();		
							
				// get paths for all geos
				ArrayList paths = new ArrayList();
				for (int i=0; i<geos.size(); i++) {
					TreePath result = getGeoPath((GeoElement) geos.get(i));
					if (result != null) {	
						tp = result;
						expandPath(result);
						paths.add(result);
					}
				}				
							
				// select geo paths
				TreePath [] selPaths = new TreePath[paths.size()];
				for (int i=0; i < selPaths.length; i++) {
					selPaths[i] = (TreePath) paths.get(i);
				}
				lsm.addSelectionPaths(selPaths);
				
				if (tp != null && geos.size() == 1) {
					scrollPathToVisible(tp);
				}				
			}		
		}	
		
		private void selectFirstElement() {
			//  select all  if list is not empty
			if (root.getChildCount() > 0) {						
				DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) root.getFirstChild();
				TreePath tp = new TreePath(((DefaultMutableTreeNode)typeNode.getFirstChild()).getPath());																
				setSelectionPath(tp); // select																													
			}
		}
		
		/**		 
		 * returns geo's TreePath 
		 */
		private TreePath getGeoPath(GeoElement geo) {
			String typeString = geo.getObjectType();
			DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) typeNodesMap.get(typeString);
			if (typeNode == null)
				return null;
			
			int pos = AlgebraView.binarySearchGeo(typeNode, geo.getLabel());
			if (pos == -1)
				return null;
			else {
				// add to selection
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) typeNode.getChildAt(pos);
				
				//	expand typenode 
				TreePath tp = new TreePath(node.getPath());						

				return tp;
			}
		}
					
		public void clearSelection() {
			getSelectionModel().clearSelection();
		}

		/**
		 * Clears the list.
		 */
		private void clear() {			
			root.removeAllChildren();			
			treeModel.reload();
			typeNodesMap.clear();
		}

		/* **********************/
		/* VIEW IMPLEMENTATION */
		/* **********************/						
		
		/**
		   * adds a new element to the list
		   */
		final public void add(GeoElement geo) {	
			if (!geo.isLabelSet() || !geo.hasProperties()) 
				return;	
				
			// get type node
			String typeString = geo.getObjectType();
			DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) typeNodesMap.get(typeString);
			
			// init type node
			boolean initing = typeNode == null;
			if (initing) {
				String transTypeString = geo.translatedTypeString();
				typeNode = new DefaultMutableTreeNode(transTypeString);									
				typeNodesMap.put(typeString, typeNode);
				
				// find insert pos
				int pos = root.getChildCount();
				for (int i=0; i < pos; i++) {
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
					if (transTypeString.compareTo(child.toString()) < 0) {
						pos = i;
						break;
					}
				}
				
				treeModel.insertNodeInto(typeNode, root, pos);				
			}			
			
			// check if already present in type node
			int pos = AlgebraView.binarySearchGeo(typeNode, geo.getLabel());
			if (pos >= 0) return;
			
			// add geo to type node   
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(geo);
			pos = AlgebraView.getInsertPosition(typeNode, geo);
			treeModel.insertNodeInto(newNode, typeNode, pos);
			
			// make sure something is selected
			if (getSelectionModel().isSelectionEmpty()) {
				selectFirstElement();					
			}
		
			/*
			if (isShowing()) {
				TreePath geoPath = new TreePath(newNode.getPath());
				//addSelectionPath(geoPath);
				makeVisible(geoPath);
			} */		
		}		
		

		/**
		 * removes an element from the list
		 */
		public void remove(GeoElement geo) {		
			remove(geo, true);
			
			// close dialog if no elements left
			if (root.getChildCount() == 0) {	
				closeDialog();
				return;
			}
			
			// make sure something is selected
			if (getSelectionModel().isSelectionEmpty()) {
				selectFirstElement();					
			}						
		}
		
		/**
		 * 
		 * @param geo
		 * @param binarySearch: true for binary, false for linear search
		 */
		public void remove(GeoElement geo, boolean binarySearch) {
			// get type node
			DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) typeNodesMap.get(geo.getObjectType());
			if (typeNode == null) return;
									
			int pos = binarySearch ?
					AlgebraView.binarySearchGeo(typeNode, geo.getLabel()) :					
					AlgebraView.linearSearchGeo(typeNode, geo.getLabel());
			if (pos > -1) {				
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) typeNode.getChildAt(pos);					
				treeModel.removeNodeFromParent(child);
				
				if (typeNode.getChildCount() == 0) {
					// last child					
					typeNodesMap.remove(geo.getObjectType());	
					treeModel.removeNodeFromParent(typeNode);									
				} 						
			}
		}
		
		
		
		/**
		 * Returns the tree path of geo	
		 * @return returns null if geo is not in tree
		 */
		private TreePath getTreePath(GeoElement geo) {
			DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) typeNodesMap.get(geo.getObjectType());
			if (typeNode == null) return null;
			
			// find pos of geo 
			int pos = AlgebraView.binarySearchGeo(typeNode, geo.getLabel());					
			if (pos == -1) return null;
					
			return new TreePath(((DefaultMutableTreeNode)typeNode.getChildAt(pos)).getPath());			
		}

		/**
		 * renames an element and sorts list 
		 */
		public void rename(GeoElement geo) {		
			// the rename destroyed the alphabetical order,
			// so we have to use linear instead of binary search
			remove(geo, false);			
			add(geo);
			geoElementSelected(geo, false);
		}

		/**
		 * updates a list of elements
		 */
		public void update(GeoElement geo) {
			repaint();
		}

		public void updateAuxiliaryObject(GeoElement geo) {
			repaint();
		}

		public void reset() {
			repaint();
		}

		public void clearView() {
			clear();
		}
		
    	final public void repaintView() {
    		repaint();
    	}

		public void mouseDragged(MouseEvent arg0) {			
		}

		public void mouseMoved(MouseEvent e) {
			Point loc = e.getPoint();
			GeoElement geo = AlgebraView.getGeoElementForLocation(this, loc.x, loc.y);
			EuclidianView ev = app.getEuclidianView();

			// tell EuclidianView to handle mouse over
			ev.mouseMovedOver(geo);								
			if (geo != null)
				setToolTipText(geo.getLongDescriptionHTML(true, true));
			else
				setToolTipText(null);
		}

		/**
		 * Handles clicks on the show/hide icon to toggle the show-object status.
		 */
		public void mouseClicked(MouseEvent e) {			
			if (Application.isControlDown(e) || e.isShiftDown()) return;
			
			// get GeoElement at mouse location		
			TreePath tp = getPathForLocation(e.getX(), e.getY());
			GeoElement geo = AlgebraView.getGeoElementForPath(tp);

			// check if we clicked on the 16x16 show/hide icon
			Rectangle rect = getPathBounds(tp);
			boolean iconClicked = rect != null && e.getX() - rect.x < 13; // distance from left border				
			if (iconClicked && geo != null) {
				// icon clicked: toggle show/hide
				geo.setEuclidianVisible(!geo.isSetEuclidianVisible());
				geo.update();
				kernel.notifyRepaint();
				
				// update properties dialog by selecting this geo again
				geoElementSelected(geo, false);
			}			
		}

		public void mouseEntered(MouseEvent arg0) {
		
		}

		public void mouseExited(MouseEvent arg0) {
		}

		public void mousePressed(MouseEvent arg0) {
		}

		public void mouseReleased(MouseEvent arg0) {
		}

	} // JTreeGeoElements


	
	/*
	 * Keylistener implementation of PropertiesDialog
	 *

	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		switch (code) {
			case KeyEvent.VK_ESCAPE :
				//cancel();
				closeDialog();
				break;

			case KeyEvent.VK_ENTER :
				// needed for input fields
				//applyButton.doClick();				
				break;
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	} */

	public void windowGainedFocus(WindowEvent arg0) {
		// make sure this dialog is the current selection listener
		if (app.getMode() != EuclidianView.MODE_SELECTION_LISTENER ||
			app.getCurrentSelectionListener() != this) 
		{
			app.setSelectionListenerMode(this);
			selectionChanged();
		}		
	}
		

	public void windowLostFocus(WindowEvent arg0) {		
	}

	// Tree selection listener
	public void valueChanged(TreeSelectionEvent e) {			
		selectionChanged();		
	}

	/*
	 * KeyListener
	 */
	public void keyPressed(KeyEvent e) {
		Object src = e.getSource();
		
		if (src instanceof JTreeGeoElements) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE) {
				deleteSelectedGeos();
			}			
		}		
	}

	public void keyReleased(KeyEvent e) {	
	}

	public void keyTyped(KeyEvent e) {	
	}

} // PropertiesDialog

/**
 * panel for numeric slider
 * @author Markus Hohenwarter
 */
class SliderPanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object[] geos; // currently selected geos
	private AngleTextField tfMin, tfMax;
	private JTextField tfWidth;
	private JTextField [] tfields;
	private JCheckBox cbSliderFixed;
	private JComboBox coSliderHorizontal;
	
	private Application app;
	private PropertiesDialogGeoElement.PropertiesPanel propPanel;
	private AnimationStepPanel stepPanel;
	private AnimationSpeedPanel speedPanel;
	private Kernel kernel;

	public SliderPanel(Application app, PropertiesDialogGeoElement.PropertiesPanel propPanel, boolean useTabbedPane) {
		this.app = app;
		kernel = app.getKernel();
		this.propPanel = propPanel;
					
		JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,5));	
		JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 5));		
		JPanel animationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 5));
		
		cbSliderFixed = new JCheckBox(app.getPlain("fixed"));
		cbSliderFixed.addActionListener(this);
		sliderPanel.add(cbSliderFixed);		
		
		String [] comboStr = {app.getPlain("horizontal"), app.getPlain("vertical")};
		coSliderHorizontal = new JComboBox(comboStr);
		coSliderHorizontal.addActionListener(this);
		sliderPanel.add(coSliderHorizontal);				
					
		String[] labels = { app.getPlain("min")+":",
							app.getPlain("max")+":", app.getPlain("Width")+":"};
		tfMin = new AngleTextField(5);
		tfMax = new AngleTextField(5);
		tfWidth = new JTextField(4);
		tfields = new JTextField[3];
		tfields[0] = tfMin;
		tfields[1] = tfMax;
		tfields[2] = tfWidth;
		int numPairs = labels.length;

		//	add textfields
		for (int i = 0; i < numPairs; i++) {
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		    JLabel l = new JLabel(labels[i], SwingConstants.LEADING);
		    p.add(l);
		    JTextField textField = tfields[i];
		    l.setLabelFor(textField);
		    textField.addActionListener(this);
		    textField.addFocusListener(this);
		    p.add(textField);
		    p.setAlignmentX(Component.LEFT_ALIGNMENT);
		    
		    if (i < 2)
		    	intervalPanel.add(p);
		    else 
		    	sliderPanel.add(p);
		}
		
		// add increment to intervalPanel
		stepPanel = new AnimationStepPanel(app);
		stepPanel.setPartOfSliderPanel();
		intervalPanel.add(stepPanel);		
		
		speedPanel = new AnimationSpeedPanel(app);
		speedPanel.setPartOfSliderPanel();
		animationPanel.add(speedPanel);		
		
	
		// put together interval, slider options, animation panels
		if (useTabbedPane) {
			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			tabbedPane.addTab(app.getPlain("Interval"), intervalPanel);
			tabbedPane.addTab(app.getMenu("Slider"), sliderPanel);
			tabbedPane.addTab(app.getPlain("Animation"), animationPanel);
			add(tabbedPane);
		}
		else { // no tabs 
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));	
			intervalPanel.setBorder(BorderFactory.createTitledBorder(app.getPlain("Interval")));					
			sliderPanel.setBorder(BorderFactory.createTitledBorder(app.getPlain("Slider")));		
			animationPanel.setBorder(BorderFactory.createTitledBorder(app.getPlain("Animation")));
			add(intervalPanel);	
			add(Box.createVerticalStrut(5));
			add(sliderPanel);					
			add(Box.createVerticalStrut(5));
			add(animationPanel);
		}
	}

	public JPanel update(Object[] geos) {
		stepPanel.update(geos);
		speedPanel.update(geos);
		
		this.geos = geos;
		if (!checkGeos(geos))
			return null;
		
		for (int i=0; i<tfields.length; i++) 
			tfields[i].removeActionListener(this);
		coSliderHorizontal.removeActionListener(this);
		cbSliderFixed.removeActionListener(this);

		// check if properties have same values
		GeoNumeric temp, num0 = (GeoNumeric) geos[0];
		boolean equalMax = true;
		boolean equalMin = true;
		boolean equalWidth = true;
		boolean equalSliderFixed = true;
		boolean equalSliderHorizontal = true;
		boolean onlyAngles = true;

		for (int i = 0; i < geos.length; i++) {
			temp = (GeoNumeric) geos[i];

			if (!num0.isIntervalMinActive() || !temp.isIntervalMinActive() || !kernel.isEqual(num0.getIntervalMin(), temp.getIntervalMin()))
				equalMin = false;
			if (!num0.isIntervalMaxActive() || !temp.isIntervalMaxActive() || !kernel.isEqual(num0.getIntervalMax(), temp.getIntervalMax()))
				equalMax = false;
			if (!kernel.isEqual(num0.getSliderWidth(), temp.getSliderWidth()))
				equalWidth = false;
			if (num0.isSliderFixed() != temp.isSliderFixed())
				equalSliderFixed = false;
			if (num0.isSliderHorizontal() != temp.isSliderHorizontal())
				equalSliderHorizontal = false;
			
			if (!(temp instanceof GeoAngle))
				onlyAngles = false;
		}

		// set values
		//int oldDigits = kernel.getMaximumFractionDigits();
		//kernel.setMaximumFractionDigits(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);
        kernel.setTemporaryPrintDecimals(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);
		if (equalMin){
			if (onlyAngles)
				tfMin.setText(kernel.formatAngle(num0.getIntervalMin()).toString());
			else
				tfMin.setText(kernel.format(num0.getIntervalMin()));
		} else {
			tfMin.setText("");
		}
		
		if (equalMax){
			if (onlyAngles)
				tfMax.setText(kernel.formatAngle(num0.getIntervalMax()).toString());
			else
				tfMax.setText(kernel.format(num0.getIntervalMax()));
		} else {
			tfMax.setText("");
		}
		
		if (equalWidth){
			tfWidth.setText(kernel.format(num0.getSliderWidth()));
		} else {
			tfMax.setText("");
		}
		
		//kernel.setMaximumFractionDigits(oldDigits);
		kernel.restorePrintAccuracy();

		if (equalSliderFixed)
			cbSliderFixed.setSelected(num0.isSliderFixed());
		
		if (equalSliderHorizontal) {
			coSliderHorizontal.setSelectedIndex(num0.isSliderHorizontal() ? 0 : 1);
		}
			

		for (int i=0; i<tfields.length; i++) 
			tfields[i].addActionListener(this);
		coSliderHorizontal.addActionListener(this);
		cbSliderFixed.addActionListener(this);
	
		return this;
	}

	private boolean checkGeos(Object[] geos) {				
		boolean geosOK = true;
		for (int i = 0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];
 			if (!(geo.isIndependent() && geo.isGeoNumeric())) {
				geosOK = false;
				break;
			}
		}
		return geosOK;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == cbSliderFixed) 
			doCheckBoxActionPerformed((JCheckBox) source);
		else if (source == coSliderHorizontal)
			doComboBoxActionPerformed((JComboBox) source);
		else
			doTextFieldActionPerformed((JTextField) e.getSource());
	}
	
	private void doCheckBoxActionPerformed(JCheckBox source) {	
		boolean fixed = source.isSelected();			
		for (int i = 0; i < geos.length; i++) {
			GeoNumeric num = (GeoNumeric) geos[i];
			num.setSliderFixed(fixed);
			num.updateRepaint();
		}
		update(geos);
	}
	
	private void doComboBoxActionPerformed(JComboBox source) {	
		boolean horizontal = source.getSelectedIndex() == 0;			
		for (int i = 0; i < geos.length; i++) {
			GeoNumeric num = (GeoNumeric) geos[i];
			num.setSliderHorizontal(horizontal);
			num.updateRepaint();
		}
		update(geos);
	}

	private void doTextFieldActionPerformed(JTextField source) {			
		String inputText = source.getText().trim();
		boolean emptyString = inputText.equals("");
		double value = Double.NaN;
		if (!emptyString) {
			value = kernel.getAlgebraProcessor().evaluateToDouble(inputText);					
		}			
		
		if (source == tfMin) {
			for (int i = 0; i < geos.length; i++) {
				GeoNumeric num = (GeoNumeric) geos[i];
				if (emptyString) {
					num.setIntervalMinInactive();
				} else {
					num.setIntervalMin(value);
				}
				num.updateRepaint();				
			}
		}
		else if (source == tfMax) {
			for (int i = 0; i < geos.length; i++) {
				GeoNumeric num = (GeoNumeric) geos[i];
				if (emptyString) {
					num.setIntervalMaxInactive();
				} else {
					num.setIntervalMax(value);
				}
				num.updateRepaint();
			}
		}
		else if (source == tfWidth) {
			for (int i = 0; i < geos.length; i++) {
				GeoNumeric num = (GeoNumeric) geos[i];
				num.setSliderWidth(value);
				num.updateRepaint();
			}
		} 
		
		if (propPanel != null)		
			propPanel.updateSelection(geos);
		else
			update(geos);
	}

	public void focusGained(FocusEvent arg0) {
	}

	public void focusLost(FocusEvent e) {
		doTextFieldActionPerformed((JTextField) e.getSource());
	}
}	

/**
 * panel for animation step
 * @author Markus Hohenwarter
 */
class AnimationStepPanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	
	private static final long serialVersionUID = 1L;
	
	private Object[] geos; // currently selected geos
	private AngleTextField tfAnimStep;
	private boolean partOfSliderPanel = false;
	
	private Kernel kernel;

	public AnimationStepPanel(Application app) {
		kernel = app.getKernel();
		
		// text field for animation step
		JLabel label = new JLabel(app.getPlain("AnimationStep") + ": ");
		tfAnimStep = new AngleTextField(5);
		label.setLabelFor(tfAnimStep);
		tfAnimStep.addActionListener(this);
		tfAnimStep.addFocusListener(this);

		// put it all together
		JPanel animPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		animPanel.add(label);
		animPanel.add(tfAnimStep);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		animPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(animPanel);
	}
	
	public void setPartOfSliderPanel() {
		partOfSliderPanel = true;
	}

	public JPanel update(Object[] geos) {		
		this.geos = geos;
		if (!checkGeos(geos))
			return null;

		tfAnimStep.removeActionListener(this);

		// check if properties have same values
		GeoElement temp, geo0 = (GeoElement) geos[0];
		boolean equalStep = true;
		boolean onlyAngles = true;

		for (int i = 0; i < geos.length; i++) {
			temp = (GeoElement) geos[i];
			// same object visible value
			if (geo0.getAnimationStep() != temp.getAnimationStep())
				equalStep = false;
			if (!(temp.isGeoAngle()))
				onlyAngles = false;
		}

		// set trace visible checkbox
		//int oldDigits = kernel.getMaximumFractionDigits();
		//kernel.setMaximumFractionDigits(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);
		kernel.setTemporaryPrintDecimals(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);

        if (equalStep)
			if (onlyAngles)
				tfAnimStep.setText(
					kernel.formatAngle(geo0.getAnimationStep()).toString());
			else
				tfAnimStep.setText(kernel.format(geo0.getAnimationStep()));
		else
			tfAnimStep.setText("");
        
		//kernel.setMaximumFractionDigits(oldDigits);
        kernel.restorePrintAccuracy();

		tfAnimStep.addActionListener(this);
		return this;
	}

	private boolean checkGeos(Object[] geos) {
		boolean geosOK = true;
		for (int i = 0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];
			if (!geo.isChangeable() 
					|| geo.isGeoText() 
					|| geo.isGeoImage()
					|| geo.isGeoList()
					|| geo.isGeoBoolean()
					|| !partOfSliderPanel && geo.isGeoNumeric() && geo.isIndependent() // slider						
			)  
			{				
				geosOK = false;
				break;
			}
		}
		
		
		return geosOK;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tfAnimStep)
			doActionPerformed();
	}

	private void doActionPerformed() {
		double newVal =
			kernel.getAlgebraProcessor().evaluateToDouble(
				tfAnimStep.getText());
		if (!Double.isNaN(newVal)) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				geo.setAnimationStep(newVal);
				geo.updateRepaint();
			}
		}
		update(geos);
	}

	public void focusGained(FocusEvent arg0) {
	}

	public void focusLost(FocusEvent e) {
		doActionPerformed();
	}
}

/**
 * panel for animation speed
 * @author adapted from AnimationStepPanel
 */
class AnimationSpeedPanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	
	private static final long serialVersionUID = 1L;
	
	private Object[] geos; // currently selected geos
	private JTextField tfAnimSpeed;
	private boolean partOfSliderPanel = false;
	private JComboBox animationModeCB;	
	private Kernel kernel;

	public AnimationSpeedPanel(Application app) {

		kernel = app.getKernel();
			// combo box for 
		animationModeCB = new JComboBox();
		JLabel label2 = new JLabel(app.getPlain("Repeat") + ": ");
		animationModeCB.addItem("\u21d4 "+app.getPlain("Oscillating")); // index 0
		animationModeCB.addItem("\u21d2 "+app.getPlain("Increasing")); // index 1
		animationModeCB.addItem("\u21d0 "+app.getPlain("Decreasing")); // index 2
		animationModeCB.addActionListener(this);
		animationModeCB.setSelectedIndex(GeoElement.ANIMATION_OSCILLATING);
		
		// text field for animation step
		JLabel label = new JLabel(app.getPlain("AnimationSpeed") + ": ");
		tfAnimSpeed = new JTextField(5);
		label.setLabelFor(tfAnimSpeed);
		tfAnimSpeed.addActionListener(this);
		tfAnimSpeed.addFocusListener(this);

		// put it all together
		JPanel animPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		animPanel.add(label);
		animPanel.add(tfAnimSpeed);
		animPanel.add(label2);
		animPanel.add(animationModeCB);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		animPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(animPanel);
	}
	
	public void setPartOfSliderPanel() {
		partOfSliderPanel = true;
	}

	public JPanel update(Object[] geos) {		
		this.geos = geos;
		if (!checkGeos(geos))
			return null;

		tfAnimSpeed.removeActionListener(this);
		animationModeCB.removeActionListener(this);

		// check if properties have same values
		GeoElement temp, geo0 = (GeoElement) geos[0];
		boolean equalStep = true;
		boolean equalAnimationType = true;

		for (int i = 0; i < geos.length; i++) {
			temp = (GeoElement) geos[i];
			// same object visible value
			if (geo0.getAnimationSpeedObject() != temp.getAnimationSpeedObject())
				equalStep = false;
			if (geo0.getAnimationType() != temp.getAnimationType())
				equalAnimationType = false;
		}

		if (equalAnimationType)
			animationModeCB.setSelectedIndex(geo0.getAnimationType());
		else
			animationModeCB.setSelectedItem(null);

		// set trace visible checkbox
		//int oldDigits = kernel.getMaximumFractionDigits();
		//kernel.setMaximumFractionDigits(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);
        kernel.setTemporaryPrintDecimals(PropertiesDialogGeoElement.TEXT_FIELD_FRACTION_DIGITS);
        
        if (equalStep) {
        	GeoElement speedObj = geo0.getAnimationSpeedObject();
			tfAnimSpeed.setText(speedObj == null ? "1" : speedObj.getLabel());
        } else
			tfAnimSpeed.setText("");
        
		//kernel.setMaximumFractionDigits(oldDigits);
        kernel.restorePrintAccuracy();

		tfAnimSpeed.addActionListener(this);
		animationModeCB.addActionListener(this);
		return this;
	}

	private boolean checkGeos(Object[] geos) {
		boolean geosOK = true;
		for (int i = 0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];
			if (!geo.isChangeable() 
					|| geo.isGeoText() 
					|| geo.isGeoImage()
					|| geo.isGeoList()
					|| geo.isGeoBoolean()
					|| !partOfSliderPanel && geo.isGeoNumeric() && geo.isIndependent() // slider						
			)  
			{				
				geosOK = false;
				break;
			}
		}
		
		
		return geosOK;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tfAnimSpeed)
			doActionPerformed();
		else if (e.getSource() == animationModeCB) setType(animationModeCB.getSelectedIndex());
	}

	private void doActionPerformed() {
		NumberValue animSpeed = 
			kernel.getAlgebraProcessor().evaluateToNumeric(tfAnimSpeed.getText());
		if (animSpeed != null) {
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				geo.setAnimationSpeedObject(animSpeed);
				geo.updateCascade();
			}
			kernel.udpateNeedToShowAnimationButton();
			kernel.notifyRepaint();
		}
		update(geos);
	}

	private void setType(int type) {
		
		if (geos == null) return;
		
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				geo.setAnimationType(type);
				geo.updateRepaint();
			}
		
		update(geos);
	}

	public void focusGained(FocusEvent arg0) {
	}

	public void focusLost(FocusEvent e) {
		doActionPerformed();
	}
}


/**
 * panel for condition to show object
 * @author Markus Hohenwarter
 */
class ShowConditionPanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	
	private static final long serialVersionUID = 1L;
	
	private Object[] geos; // currently selected geos
	private JTextField tfCondition;
	
	private Kernel kernel;
	private PropertiesDialogGeoElement.PropertiesPanel propPanel;

	public ShowConditionPanel(Application app, PropertiesDialogGeoElement.PropertiesPanel propPanel) {
		kernel = app.getKernel();
		this.propPanel = propPanel;
		
		// textfield for animation step
		setBorder(
				BorderFactory.createTitledBorder(app.getMenu("Condition.ShowObject"))
				);
		
		// non auto complete input panel
		InputPanel inputPanel = new InputPanel(null, app, 20, false);
		tfCondition = (AutoCompleteTextField) inputPanel.getTextComponent();				
		
		tfCondition.addActionListener(this);
		tfCondition.addFocusListener(this);

		// put it all together
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(inputPanel);
	}

	public JPanel update(Object[] geos) {
		this.geos = geos;
		if (!checkGeos(geos))
			return null;

		tfCondition.removeActionListener(this);

		// take condition of first geo
		String strCond = "";
		GeoElement geo0 = (GeoElement) geos[0];	
		GeoBoolean cond = geo0.getShowObjectCondition();
		if (cond != null) {
			strCond = cond.getLabel();
		}	
		
		for (int i=0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];	
			cond = geo.getShowObjectCondition();
			if (cond != null) {
				String strCondGeo = cond.getLabel();
				if (!strCond.equals(strCondGeo))
					strCond = "";
			}	
		}		
		
		tfCondition.setText(strCond);
		tfCondition.addActionListener(this);
		return this;
	}

	private boolean checkGeos(Object[] geos) {
		for (int i=0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];	
			if (!geo.isEuclidianShowable())
				return false;
		}
		
		return true;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tfCondition)
			doActionPerformed();
	}

	private void doActionPerformed() {
		processed = true;
		GeoBoolean cond;
		String strCond = tfCondition.getText();
		if (strCond == null || strCond.trim().length() == 0) {
			cond = null;
		} else {
			
			// needed to make next replace easier
			strCond = strCond.replaceAll(">=", ExpressionNode.strGREATER_EQUAL);
			strCond = strCond.replaceAll("<=", ExpressionNode.strLESS_EQUAL);
			strCond = strCond.replaceAll("==", ExpressionNode.strEQUAL_BOOLEAN);
			strCond = strCond.replaceAll("!=", ExpressionNode.strNOT_EQUAL);
			
			// allow A=B as well as A==B
			// also stops A=B doing an assignment of B to A :)
			strCond = strCond.replaceAll("=", ExpressionNode.strEQUAL_BOOLEAN);
			
			cond = kernel.getAlgebraProcessor().evaluateToBoolean(strCond);
		}
				
		if (cond != null || strCond.trim().length() == 0) {
			// set condition
			try {
				for (int i = 0; i < geos.length; i++) {
					GeoElement geo = (GeoElement) geos[i];
					geo.setShowObjectCondition(cond);	
					
					// make sure object shown when condition removed
					if (cond == null) geo.updateRepaint(); 
				}	
				
			} catch (CircularDefinitionException e) {
				tfCondition.setText("");
				kernel.getApplication().showError("CircularDefinition");
			}	
			
			if (cond != null)
				cond.updateRepaint();		
			
			// to update "showObject" as well
			propPanel.updateSelection(geos);
		} else {
			// put back faulty condition (for editing)
			tfCondition.setText(strCond);
		}
		
	}

	public void focusGained(FocusEvent arg0) {
		processed = false;
	}
	
	boolean processed = false;

	public void focusLost(FocusEvent e) {
		if (!processed)
		doActionPerformed();
	}
}

/**
 * panel for condition to show object
 * @author Michael Borcherds 2008-04-01
 */
class ColorFunctionPanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	
	private static final long serialVersionUID = 1L;
	
	private Object[] geos; // currently selected geos
	private JTextField tfRed, tfGreen, tfBlue;
	private JButton btRemove;
	private JLabel nameLabelR,nameLabelG,nameLabelB;
	
	private Kernel kernel;
	private PropertiesDialogGeoElement.PropertiesPanel propPanel;

	public ColorFunctionPanel(Application app, PropertiesDialogGeoElement.PropertiesPanel propPanel) {
		kernel = app.getKernel();
		this.propPanel = propPanel;
		
		// textfield for animation step
		setBorder(
				BorderFactory.createTitledBorder(app.getMenu("DynamicColors"))
				);
		
		// non auto complete input panel
		InputPanel inputPanelR = new InputPanel(null, app, 1, 7, false, false, false);
		InputPanel inputPanelG = new InputPanel(null, app, 1, 7, false, false, false);
		InputPanel inputPanelB = new InputPanel(null, app, 1, 7, false, false, false);
		tfRed = (AutoCompleteTextField) inputPanelR.getTextComponent();				
		tfGreen = (AutoCompleteTextField) inputPanelG.getTextComponent();				
		tfBlue = (AutoCompleteTextField) inputPanelB.getTextComponent();				
		
		tfRed.addActionListener(this);
		tfRed.addFocusListener(this);
		tfGreen.addActionListener(this);
		tfGreen.addFocusListener(this);
		tfBlue.addActionListener(this);
		tfBlue.addFocusListener(this);
		
		nameLabelR = new JLabel(app.getMenu("Red") + ":");	
		nameLabelR.setLabelFor(inputPanelR);
		nameLabelG = new JLabel(app.getMenu("Green") + ":");	
		nameLabelG.setLabelFor(inputPanelR);
		nameLabelB = new JLabel(app.getMenu("Blue") + ":");	
		nameLabelB.setLabelFor(inputPanelR);
		
		btRemove = new JButton("\u2718");
		btRemove.setToolTipText(app.getPlain("Remove"));
		btRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i=0; i < geos.length; i++) {
					GeoElement geo = (GeoElement) geos[i];	
					geo.removeColorFunction();
					geo.setObjColor(geo.getObjectColor());
					geo.updateRepaint();
				}
				tfRed.setText("");
				tfGreen.setText("");
				tfBlue.setText("");
			}
		});

		// put it all together
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(nameLabelR);		
		add(inputPanelR);
		add(nameLabelG);		
		add(inputPanelG);
		add(nameLabelB);		
		add(inputPanelB);
		add(btRemove);
	}

	public JPanel update(Object[] geos) {
		this.geos = geos;
		if (!checkGeos(geos))
			return null;

		tfRed.removeActionListener(this);
		tfGreen.removeActionListener(this);
		tfBlue.removeActionListener(this);
		btRemove.removeActionListener(this);

		// take condition of first geo
		String strRed = "";
		String strGreen = "";
		String strBlue = "";
		GeoElement geo0 = (GeoElement) geos[0];	
		GeoList colorList = geo0.getColorFunction();
		if (colorList != null) {
			strRed = colorList.get(0).getLabel();
			strGreen = colorList.get(1).getLabel();
			strBlue = colorList.get(2).getLabel();
		}	
		
		for (int i=0; i < geos.length; i++) {
			GeoElement geo = (GeoElement) geos[i];	
			GeoList colorListTemp = geo.getColorFunction();
			if (colorListTemp != null) {
				String strRedTemp = colorListTemp.get(0).getLabel();
				String strGreenTemp = colorListTemp.get(1).getLabel();
				String strBlueTemp = colorListTemp.get(2).getLabel();
				if (!strRed.equals(strRedTemp)) strRed = "";
				if (!strGreen.equals(strGreenTemp)) strGreen = "";
				if (!strBlue.equals(strBlueTemp)) strBlue = "";
			}	
		}		

		tfRed.setText(strRed);
		tfRed.addActionListener(this);
		tfGreen.setText(strGreen);
		tfGreen.addActionListener(this);
		tfBlue.setText(strBlue);
		tfBlue.addActionListener(this);
		return this;
	}

	// return true: want to be able to color all spreadsheet objects
	private boolean checkGeos(Object[] geos) {
		return true;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tfRed || e.getSource() == tfGreen || e.getSource() == tfBlue)
			doActionPerformed();
	}

	private void doActionPerformed() {
		processed = true;
		
		GeoList list = null;
		String strRed = tfRed.getText();
		String strGreen = tfGreen.getText();
		String strBlue = tfBlue.getText();
		if ((strRed == null || strRed.trim().length() == 0) &&
			(strGreen == null || strGreen.trim().length() == 0) &&
			(strBlue == null || strBlue.trim().length() == 0)) {
			//num = null;
			list=null;
		} else {
			if (strRed == null || strRed.trim().length() == 0) strRed="0";
			if (strGreen == null || strGreen.trim().length() == 0) strGreen="0";
			if (strBlue == null || strBlue.trim().length() == 0) strBlue="0";
	
			list = kernel.getAlgebraProcessor().evaluateToList("{"+strRed + ","+strGreen+","+strBlue+"}");
		}
		
				
		// set condition
		//try {
		if (list != null) {							//
		if (((list.get(0) instanceof NumberValue)) && 	// bugfix, enter "x" for a color 
			((list.get(1) instanceof NumberValue)) &&	//
			((list.get(2) instanceof NumberValue)) )		//
			for (int i = 0; i < geos.length; i++) {
				GeoElement geo = (GeoElement) geos[i];
				geo.setColorFunction(list);				
			}	
			
	
		list.updateRepaint();		
		
		// to update "showObject" as well
		propPanel.updateSelection(geos);
		} else {
			// put back faulty text (for editing)
			tfRed.setText(strRed);
			tfGreen.setText(strGreen);
			tfBlue.setText(strBlue);
		}

	}

	public void focusGained(FocusEvent arg0) {
		processed = false;
	}
	
	boolean processed = false;

	public void focusLost(FocusEvent e) {
		if (!processed)
			doActionPerformed();
	}
}


/**
 * panel for name of object
 * @author Markus Hohenwarter
 */
class NamePanel
	extends JPanel
	implements ActionListener, FocusListener, UpdateablePanel {
	
	private static final long serialVersionUID = 1L;
		
	private AutoCompleteTextField tfName, tfDefinition, tfCaption;
	private JLabel nameLabel, defLabel, captionLabel;
	private InputPanel inputPanelName, inputPanelDef, inputPanelCap;
	private RenameInputHandler nameInputHandler;
	private RedefineInputHandler defInputHandler;
	private GeoElement currentGeo;	
	private Application app;

	public NamePanel(Application app) {	
		this.app = app;
		// NAME PANEL
		nameInputHandler = new RenameInputHandler(app, null, false);
		
		// non auto complete input panel
		inputPanelName = new InputPanel(null, app, 1, 10, false, true, false);
		tfName = (AutoCompleteTextField) inputPanelName.getTextComponent();				
		tfName.setAutoComplete(false);		
		tfName.addActionListener(this);
		tfName.addFocusListener(this);	
		
		// DEFINITON PANEL		
// Michael Borcherds 2007-12-31 BEGIN added third argument
		defInputHandler = new RedefineInputHandler(app, null, null);
//		 Michael Borcherds 2007-12-31 END
	
		// definition field: non auto complete input panel
		inputPanelDef = new InputPanel(null, app, 1, 20, true, true, false);
		tfDefinition = (AutoCompleteTextField) inputPanelDef.getTextComponent();
		tfDefinition.setAutoComplete(false);		
		tfDefinition.addActionListener(this);
		tfDefinition.addFocusListener(this);

		// caption field: non auto complete input panel
		inputPanelCap = new InputPanel(null, app, 1, 20, true, true, false);
		tfCaption = (AutoCompleteTextField) inputPanelCap.getTextComponent();
		tfCaption.setAutoComplete(false);		
		tfCaption.addActionListener(this);
		tfCaption.addFocusListener(this);
		
		// name panel			
		nameLabel = new JLabel(app.getPlain("Name") + ":");	
		nameLabel.setLabelFor(inputPanelName);
	
		
		// definition panel
		defLabel = new JLabel(app.getPlain("Definition") + ":");		
		defLabel.setLabelFor(inputPanelDef);
	
		
		// caption panel
		captionLabel = new JLabel(app.getMenu("Button.Caption") + ":");		
		captionLabel.setLabelFor(inputPanelCap);

		updateGUI(true, true);
	}
	
	private void updateGUI(boolean showDefinition, boolean showCaption) {
		int rows = 1;
		removeAll();
		
		add(nameLabel);		
		add(inputPanelName);
		
		if (showDefinition) {	
			rows++;
			add(defLabel);
			add(inputPanelDef);
		}
		
		if (showCaption) {
			rows++;
			add(captionLabel);
			add(inputPanelCap);
		}
		
		 //Lay out the panel
		setLayout(new SpringLayout());
        SpringUtilities.makeCompactGrid(this,
                                        rows, 2, 	// rows, cols
                                        5, 5,   //initX, initY
                                        5, 5);  //xPad, yPad	
	}

	public JPanel update(Object[] geos) {		
		if (!checkGeos(geos))
			return null;

		// NAME
		tfName.removeActionListener(this);

		// take name of first geo		
		GeoElement geo0 = (GeoElement) geos[0];	
		tfName.setText(geo0.getLabel());
	
		currentGeo = geo0;
		nameInputHandler.setGeoElement(geo0);
		
		tfName.addActionListener(this);		
		
		// DEFINITION
		//boolean showDefinition = !(currentGeo.isGeoText() || currentGeo.isGeoImage());
		boolean showDefinition = currentGeo.isGeoText() ? ((GeoText)currentGeo).isTextCommand() :
			! (currentGeo.isGeoImage() && currentGeo.isIndependent());
		if (showDefinition) {			
			tfDefinition.removeActionListener(this);
			defInputHandler.setGeoElement(currentGeo);
			tfDefinition.setText(getDefText(currentGeo));
			tfDefinition.addActionListener(this);
			
			if (currentGeo.isIndependent()) {
				defLabel.setText(app.getPlain("Value")+ ":");
			} else {
				defLabel.setText(app.getPlain("Definition")+ ":");
			}
		}
//		defLabel.setVisible(showDefinition);
//		inputPanelDef.setVisible(showDefinition);
		
		// CAPTION
		boolean showCaption = !currentGeo.isTextValue(); // borcherds was currentGeo.isGeoBoolean();
		if (showCaption) {			
			tfCaption.removeActionListener(this);
			tfCaption.setText(getCaptionText(currentGeo));
			tfCaption.addActionListener(this);			
		} 
//		captionLabel.setVisible(showCaption);
//		inputPanelCap.setVisible(showCaption);
		
		updateGUI(showDefinition, showCaption);
		
		return this;
	}

	private boolean checkGeos(Object[] geos) {				
		return geos.length == 1;
	}

	/**
	 * handle textfield changes
	 */
	public void actionPerformed(ActionEvent e) {		
			doActionPerformed(e.getSource());
	}

	private synchronized void doActionPerformed(Object source) {
		actionPerforming = true;
		
		if (source == tfName) {
			// rename
			String strName = tfName.getText();				
			nameInputHandler.processInput(strName);
			
			// reset label if not successful
			strName = currentGeo.getLabel();
			if (!strName.equals(tfName.getText())) {
				tfName.setText(strName);
				tfName.requestFocus();
			}
		} 
		else if (source == tfDefinition) {		
			String strDefinition = tfDefinition.getText();	
			if (!strDefinition.equals(getDefText(currentGeo))) {		
				defInputHandler.processInput(strDefinition);
	
				// reset definition string if not successful
				strDefinition = getDefText(currentGeo);
				if (!strDefinition.equals(tfDefinition.getText())) {
					tfDefinition.setText(strDefinition);
					tfDefinition.requestFocus();
				}
			}
		}		
		else if (source == tfCaption) {		
			String strCaption = tfCaption.getText();	
			currentGeo.setCaption(strCaption);			
			
			strCaption = getCaptionText(currentGeo);
			if (!strCaption.equals(tfCaption.getText().trim())) {
				tfCaption.setText(strCaption);	
				tfCaption.requestFocus();
			}
		}	
		currentGeo.updateRepaint();
		
		actionPerforming = false;
	}
	private boolean actionPerforming = false;

	public void focusGained(FocusEvent arg0) {
	}

	public void focusLost(FocusEvent e) {	
		if (!actionPerforming) 
			doActionPerformed(e.getSource());
	}
	
	private String getDefText(GeoElement geo) {
		/*
		return geo.isIndependent() ?
				geo.toOutputValueString() :
				geo.getCommandDescription();	*/
		return geo.getRedefineString(false, true);
	}
	
	private String getCaptionText(GeoElement geo) {
		String strCap = currentGeo.getRawCaption();
		if (strCap.equals(currentGeo.getLabel()))
			return "";
		else
			return strCap;
	}
	
}

interface UpdateablePanel {
	public JPanel update(Object[] geos);
	public void setVisible(boolean flag);
}