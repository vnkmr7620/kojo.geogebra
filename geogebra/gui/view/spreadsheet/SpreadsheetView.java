
package geogebra.gui.view.spreadsheet;

import geogebra.kernel.Construction;
import geogebra.kernel.GeoAngle;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoNumeric;
import geogebra.kernel.GeoPoint;
import geogebra.kernel.GeoVector;
import geogebra.kernel.Kernel;
import geogebra.main.Application;
import geogebra.main.View;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class SpreadsheetView extends JScrollPane implements View
{

	public static final int ROW_HEADER_WIDTH = 35; // wide enough for "9999"
	
	private static final long serialVersionUID = 1L;

	protected MyTable table;
	protected DefaultTableModel tableModel;
	public JList rowHeader;
	private RowHeaderRenderer rowHeaderRenderer;
	protected Application app;
	private Kernel kernel;
	
	// if these are increased above 32000, you need to change traceRow to an int[]
	protected static int MAX_COLUMNS = 9999; // TODO make sure this is actually used
	protected static int MAX_ROWS = 9999; // TODO make sure this is actually used
	
	private int highestUsedColumn = -1; // for trace
	short[] traceRow = new short[MAX_COLUMNS + 1]; // for trace
	
	private static int DEFAULT_COLUMN_WIDTH = 70;
	
	public SpreadsheetView(Application app0, int columns, int rows) {
		/*
		JList table = new JList();
		setViewportView(table);
		table.setFocusable(true);
		table.addKeyListener(new KeyListener0());
		/**/
		
		app = app0;
		kernel = app.getKernel();
		// table
		tableModel = new DefaultTableModel(rows, columns);
		table = new MyTable(this, tableModel);
		
		table.columnHeader.setPreferredSize(new Dimension((int)(MyTable.TABLE_CELL_WIDTH)
				, (int)(MyTable.TABLE_CELL_HEIGHT)));

		
		// row header list
		MyListModel listModel = new MyListModel(tableModel);
		rowHeader = new JList(listModel);
		rowHeader.setFocusable(true);
		rowHeader.setAutoscrolls(false);
		rowHeader.addMouseListener(new MouseListener1());
		rowHeader.addMouseMotionListener(new MouseMotionListener1());
		rowHeader.addKeyListener(new KeyListener1());
		//rowHeader.setFixedCellWidth(MyTable.TABLE_CELL_WIDTH);
		rowHeader.setFixedCellWidth(ROW_HEADER_WIDTH);
		rowHeader.setFixedCellHeight(table.getRowHeight()); // + table.getRowMargin();
		rowHeaderRenderer = new RowHeaderRenderer(table, rowHeader);
		rowHeader.setCellRenderer(rowHeaderRenderer);
		// put the table and the row header list into a scroll plane
		setRowHeaderView(rowHeader);
		setViewportView(table);
		
		// create and set corners
		Corner upperLeftCorner = new Corner(); //use FlowLayout
		upperLeftCorner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, MyTable.TABLE_GRID_COLOR));		upperLeftCorner.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				table.selectAll();
			}
		});
		
		//Set the corners.
		setCorner(JScrollPane.UPPER_LEFT_CORNER, upperLeftCorner);
		setCorner(JScrollPane.LOWER_LEFT_CORNER, new Corner());
		setCorner(JScrollPane.UPPER_RIGHT_CORNER, new Corner());
		
		updateFonts();
	}
	
	private class Corner extends JComponent {
	    protected void paintComponent(Graphics g) {
	        // Fill me with dirty brown/orange.
	        g.setColor(MyTable.BACKGROUND_COLOR_HEADER);
	        g.fillRect(0, 0, getWidth(), getHeight());
	    }
	}

		/**/
	
	public void attachView() {
		clearView();
		kernel.notifyAddAll(this);
		kernel.attach(this);		
	}

	public void detachView() {
		kernel.detach(this);
		clearView();
		//kernel.notifyRemoveAll(this);		
	}
	
	public Application getApplication() {
		return app;
	}
	
	public int getHighestUsedColumn() {
		resetTraceRow(highestUsedColumn+1);
		resetTraceRow(highestUsedColumn+2);
		return highestUsedColumn;
	}
	
	private void resetTraceRow(int col) {
		if (col < MAX_COLUMNS) traceRow[col] = 1;
	}
	
	public int getTraceRow(int column) {
		if (column < 0 || column >= MAX_COLUMNS) return -1;
		if (traceRow[column] == 0) traceRow[column] = 1; //first call
		return (int)traceRow[column]++;
	}
	
	boolean collectingTraces = false;
	HashMap traces = null;
	
	public void startCollectingSpreadsheetTraces() {
		collectingTraces = true;
		if (traces == null)
			traces = new HashMap();
		traces.clear();
	}
	
	public void stopCollectingSpreadsheetTraces() {
		collectingTraces = false;
		
		Iterator it = traces.values().iterator();
		
		while (it.hasNext()) {
			traceToSpreadsheet((GeoElement)it.next());
		}
		
		traces.clear();

	}
	
	private double[] coords = new double[2];

	public void traceToSpreadsheet(GeoElement geo) {
		
		if (collectingTraces) {
			traces.put(geo.getTraceColumn1(), geo);
			return;
		}
		
		Construction cons = app.getKernel().getConstruction();
		
		
		switch (geo.getGeoClassType()) {
			
		case GeoElement.GEO_CLASS_POINT:
			
			GeoPoint P = (GeoPoint)geo;
			
    		boolean polar = P.getMode() == Kernel.COORD_POLAR;
    		
	    	if (polar)
	    		P.getPolarCoords(coords);
	    	else
	    		P.getInhomCoords(coords);

			
	    	String col = P.getTraceColumn1(); // call before getTraceRow()
	    	int row = P.getTraceRow();
	    	if (row > 0) {
    	    	//Application.debug(col+row);   		
		    	app.getGuiManager().setScrollToShow(true);
		    	
		    	GeoNumeric traceCell = new GeoNumeric(cons, col + row,coords[0]);
		    	traceCell.setAuxiliaryObject(true);
		    	
		    	col = P.getTraceColumn2(); // call before getTraceRow()
    	    	//Application.debug(col+row);   		
		    	
		    	GeoNumeric traceCell2;
		    	
		    	if (polar) traceCell2 = new GeoAngle(cons,col+row,coords[1]);
		    	else traceCell2 = new GeoNumeric(cons,col+row,coords[1]);
		    	
		    	traceCell2.setAuxiliaryObject(true);
		    	
		    	cons.getApplication().getGuiManager().setScrollToShow(false);	
		    	
		    	P.setLastTrace1(coords[0]);
		    	P.setLastTrace2(coords[1]);
	    	}
			break;
		case GeoElement.GEO_CLASS_VECTOR:
			
			GeoVector vector = (GeoVector)geo;
			
			vector.getInhomCoords(coords);
			
	    	col = vector.getTraceColumn1();
	    	row = vector.getTraceRow();
	    	if (row > 0) {
		    	cons.getApplication().getGuiManager().setScrollToShow(true);
	    		
	    		GeoNumeric traceCell = new GeoNumeric(cons,col+row,coords[0]);
		    	traceCell.setAuxiliaryObject(true);
		    	GeoNumeric traceCell2 = new GeoNumeric(cons,vector.getTraceColumn2()+row,coords[1]);
		    	traceCell2.setAuxiliaryObject(true);
		    	
		    	cons.getApplication().getGuiManager().setScrollToShow(false);
		    	
		    	vector.setLastTrace1(coords[0]);
		    	vector.setLastTrace2(coords[1]);
	    	}
			break;
		case GeoElement.GEO_CLASS_NUMERIC:
			
			GeoNumeric num = (GeoNumeric)geo;
			
	    	col = num.getTraceColumn1(); // must be called before getTraceRow()
	    	row = num.getTraceRow();
	    	
	    	cons.getApplication().getGuiManager().setScrollToShow(true);
	    	GeoNumeric traceCell = new GeoNumeric(cons, col+row, num.getValue());
	    	cons.getApplication().getGuiManager().setScrollToShow(false);
	    	
	    	traceCell.setAuxiliaryObject(true);
	    	
	    	num.setLastTrace1(num.getValue());
			break;
		
		}
	}
	
//	public void incrementTraceRow(int column) {
//		if (column < 0 || column >= MAX_COLUMNS) return;
//		traceRow[column]++;
//	}
	
//	public void resetTraceRow(int column) {
//		if (column < 0 || column >= MAX_COLUMNS) return;
//		traceRow[column] = 0;
//	}
	
	/* used to "reserve" a column
	 * 
	 */
//	public void incrementHighestUsedColumn() {
//		highestUsedColumn++;
//	}
	
	public void add(GeoElement geo) {	
		//Application.debug(new Date() + " ADD: " + geo);				

		update(geo);
		
		Point location = geo.getSpreadsheetCoords();
		
		// autoscroll to new cell's location
		if (scrollToShow && location != null )
			table.scrollRectToVisible(table.getCellRect(location.y, location.x, true));
		
		
		//Application.debug("highestUsedColumn="+highestUsedColumn);
	}
	
	private boolean scrollToShow = false;
	
	public void setScrollToShow(boolean scrollToShow) {
		this.scrollToShow = scrollToShow;
	}
	
	public void remove(GeoElement geo) {
		//Application.debug(new Date() + " REMOVE: " + geo);
				
		Point location = geo.getSpreadsheetCoords();
		if (location != null) {
			doRemove(geo, location.y, location.x);
		}
	}
	
	private void doRemove(GeoElement geo, int row, int col) {
		tableModel.setValueAt(null, row, col);
		if (col <= highestUsedColumn) checkColumnEmpty(highestUsedColumn);
		//Application.debug("highestUsedColumn="+highestUsedColumn);
	}
	
	private void checkColumnEmpty(int col) {
		
		if (col == -1) return; // end recursion
		
		// check if this was the last cell used in this column
		boolean columnNotEmpty = false;
		for (int r = 0 ; r < tableModel.getRowCount() ; r++) {
			if (tableModel.getValueAt(r, col) != null) {
				// column not empty
				columnNotEmpty = true;
				break;
			}
		}
		if (!columnNotEmpty) {
			highestUsedColumn--;
			checkColumnEmpty(highestUsedColumn);
		}
		
	}
	
	public void rename(GeoElement geo) {
		//Application.debug(new Date() + " RENAME");
		Point location = geo.getOldSpreadsheetCoords();
		if (location != null) {
			doRemove(geo, location.y, location.x);
		}

		add(geo);
	}
	
	public void updateAuxiliaryObject(GeoElement geo) {		
	}
	
	public static HashSet selectedElems = new HashSet();
	
	public void repaintView() {
		/*
		 * Markus Hohenwarter 2008-09-18
		 *   The following code is extremely slow and a very bad performance bottleneck.
		 *   If this needs to be done, then definitely NOT in repaintView()
		 * 
		ArrayList elems = app.getSelectedGeos();
		selectedElems.clear();
		for (int i = 0; i < elems.size(); ++ i) {
			GeoElement geo = (GeoElement)elems.get(i);
			selectedElems.add(geo.getLabel());
		}
		if (System.currentTimeMillis() - table.selectionTime > 100) {
			table.selectNone();
		}
		*/
		
		repaint();		
	}
	
	public void clearView() {
		//Application.debug(new Date() + " CLEAR VIEW");
		int rows = tableModel.getRowCount();
		int columns = tableModel.getColumnCount();
		for (int c = 0; c < columns; ++c) {
			for (int r = 0; r < rows; ++r) {
				tableModel.setValueAt(null, r, c);
			}
		}
	}
		
	public static class MyListModel extends AbstractListModel {
		
		private static final long serialVersionUID = 1L;
		
		protected DefaultTableModel model;

		public MyListModel(DefaultTableModel model0) {
			model = model0;
		}
		
		public int getSize() {
			return model.getRowCount();
		}
		
		public Object getElementAt(int index) {
			return "" + (index + 1);
		}
		
    }

	protected int minSelectionRow = -1;
	protected int maxSelectionRow = -1;

	public class RowHeaderRenderer extends JLabel implements ListCellRenderer, ListSelectionListener {
	
    	private static final long serialVersionUID = 1L;
    	
    	protected JTableHeader header;
    	protected JList rowHeader;
    	protected ListSelectionModel selectionModel;
    	private Color defaultBackground;
	
		public RowHeaderRenderer(JTable table, JList rowHeader) {
	 		super("", JLabel.CENTER);
    		setOpaque(true);
    		defaultBackground = MyTable.BACKGROUND_COLOR_HEADER;
			
			this.rowHeader = rowHeader;
			header = table.getTableHeader() ;
//			setOpaque(true);
			//setBorder(UIManager.getBorder("TableHeader.cellBorder" ));
			setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, MyTable.TABLE_GRID_COLOR));
//			setHorizontalAlignment(CENTER) ;
//			setForeground(header.getForeground()) ;
//			setBackground(header.getBackground());
			if (getFont().getSize() == 0) {
				Font font1 = app.getPlainFont();
				if (font1 == null || font1.getSize() == 0) {
					font1 = new Font("dialog", 0, 12);
				}
				setFont(font1);
			}
			table.getSelectionModel().addListSelectionListener(this);
		}
	
		public Component getListCellRendererComponent(JList list, Object value,	int index, boolean  isSelected, boolean cellHasFocus) {
			setText ((value == null) ? ""  : value.toString());
						
			if (minSelectionRow != -1 && maxSelectionRow != -1) {
				if (index >= minSelectionRow && index <= maxSelectionRow &&
						selectionModel.isSelectedIndex(index)) 
				{
					setBackground(MyTable.SELECTED_BACKGROUND_COLOR_HEADER);
				}
				else {
					setBackground(defaultBackground);					
				}
			}
			else {
				setBackground(defaultBackground);
			}
			return this;
		}
	
		public void valueChanged(ListSelectionEvent e) {
			selectionModel = (ListSelectionModel)e.getSource();
			minSelectionRow = selectionModel.getMinSelectionIndex();
			maxSelectionRow = selectionModel.getMaxSelectionIndex();
			rowHeader.repaint();
		}

    }

	protected int row0 = -1;

	protected class MouseListener1 implements MouseListener
	{
		
		public void mouseClicked(MouseEvent e) {
			
		}
		
		public void mouseEntered(MouseEvent e) {
		}
		
		public void mouseExited(MouseEvent e) {
		}
		
		public void mousePressed(MouseEvent e) {						
			boolean shiftPressed = e.isShiftDown();	
			boolean metaDown = Application.isControlDown(e);							
			boolean rightClick = Application.isRightClick(e);
					
			int x = e.getX();
			int y = e.getY();
			
			// left click
			if (!rightClick) {								
				Point point = table.getIndexFromPixel(x, y);
				if (point != null) {
					
					//GeoElement geo = (GeoElement)(table.getValueAt(point.x,point.y));
					//Application.debug(geo.getAlgebraDescription());
					
					
					if (table.getSelectionModel().getSelectionMode() != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION ||
							table.getColumnSelectionAllowed() == true) {
						table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
						table.setColumnSelectionAllowed(false);
						table.setRowSelectionAllowed(true);
					}
					if (shiftPressed) {
						if (row0 != -1) {
							int row = (int)point.getY();
							table.setRowSelectionInterval(row0, row);
						}
					}
					else if (metaDown) {					
						row0 = (int)point.getY();
						table.addRowSelectionInterval(row0, row0);
					}
					else {
						row0 = (int)point.getY();
						table.setRowSelectionInterval(row0, row0);
					}
					table.repaint();
				}
			}
			/* G.Sturr 2009-9-30: moved this to mouseReleased
			// RIGHT CLICK
			else {	
				if (!app.letShowPopupMenu()) return;    	
    		       
				if (minSelectionRow != -1 && maxSelectionRow != -1) {
					ContextMenuRow popupMenu = new ContextMenuRow(table, 0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow, new boolean[0]);
			        popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}	
						
			}
			*/
			
		}
		
		public void mouseReleased(MouseEvent e)	{
			//G.Sturr 2009-9-30: moved show contextMenu from mousePressed
			// and added right click selection
			
			boolean rightClick = Application.isRightClick(e);
			
			if (rightClick) { 			
				if (!app.letShowPopupMenu()) return; 
				
				
				Point p = table.getIndexFromPixel(e.getX(), e.getY());
				if (p == null) return;
				
				// if click is outside current selection then change selection
				if(p.getY() < minSelectionRow ||  p.getY() > maxSelectionRow 
						|| p.getX() < table.minSelectionColumn || p.getX() > table.maxSelectionColumn){
					// switch to row selection mode and select row
					if (table.getSelectionModel().getSelectionMode() != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION ||
							table.getColumnSelectionAllowed() == true) {
						table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
						table.setColumnSelectionAllowed(false);
						table.setRowSelectionAllowed(true);
					}
					table.selectNone();
					table.setRowSelectionInterval((int)p.getY(), (int)p.getY());
				}	
			
				//show contextMenu
				ContextMenuRow popupMenu = new ContextMenuRow(table, 0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow, new boolean[0]);
			        popupMenu.show(e.getComponent(), e.getX(), e.getY());
				} 
			
		}

	}
	
	protected class MouseMotionListener1 implements MouseMotionListener
	{
		
		public void mouseDragged(MouseEvent e) {
			
			if(Application.isRightClick(e))return; //G.Sturr 2009-9-30 
			
			int x = e.getX();
			int y = e.getY();
			Point point = table.getIndexFromPixel(x, y);
			if (point != null) {
				int row = (int)point.getY();
				table.setRowSelectionInterval(row0, row);
				table.repaint();
			}
		}
		
		public void mouseMoved(MouseEvent e) {
		}
		
	}
		
	protected class KeyListener1 implements KeyListener 
	{
		
		public void keyTyped(KeyEvent e) {
		}
		
		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			
			boolean metaDown = Application.isControlDown(e);				
			boolean altDown = e.isAltDown();				
			
			//Application.debug(keyCode);
			switch (keyCode) {				
			case KeyEvent.VK_C : // control + c
				if (metaDown && minSelectionRow != -1 && maxSelectionRow != -1) {
					table.copyPasteCut.copy(0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow, altDown);
					e.consume();
				}
				break;
			case KeyEvent.VK_V : // control + v
				if (metaDown && minSelectionRow != -1 && maxSelectionRow != -1) {
					boolean storeUndo = table.copyPasteCut.paste(0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow);
					if (storeUndo)
		 				app.storeUndoInfo();
					e.consume();
				}
				break;				
			case KeyEvent.VK_X : // control + x
				if (metaDown && minSelectionRow != -1 && maxSelectionRow != -1) {
					table.copyPasteCut.copy(0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow, altDown);
					e.consume();
				}
				boolean storeUndo = table.copyPasteCut.delete(0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow);
				if (storeUndo)
	 				app.storeUndoInfo();
				break;
				
			case KeyEvent.VK_DELETE : // delete
			case KeyEvent.VK_BACK_SPACE : // delete on MAC
				storeUndo = table.copyPasteCut.delete(0, minSelectionRow, table.getModel().getColumnCount() - 1, maxSelectionRow);
				if (storeUndo)
	 				app.storeUndoInfo();
				break;			
			}
		}
		
		public void keyReleased(KeyEvent e) {
			
		}
		
	}
		

	/**/
		
	public void restart() {
		highestUsedColumn = -1;
		updateColumnWidths();
	}	
	
	public void reset() {
	}	
	
	public void update(GeoElement geo) {
		Point location = geo.getSpreadsheetCoords();
		if (location != null && location.x < MAX_COLUMNS && location.y < MAX_ROWS) {
			
			if (location.x > highestUsedColumn) highestUsedColumn = location.x;
			
			if (location.y >= tableModel.getRowCount()) {
				tableModel.setRowCount(location.y + 1);		
				getRowHeader().revalidate();
			}
			if (location.x >= tableModel.getColumnCount()) {
				table.setMyColumnCount(location.x + 1);		
				JViewport cH = getColumnHeader();
				
				// bugfix: double-click to load ggb file gives cH = null
				if (cH != null) cH.revalidate();
			}
			tableModel.setValueAt(geo, location.y, location.x);
		}			
	}	
	
	/*
	public void add(GeoElement geo) {
	}
	public void remove(GeoElement geo) {
	}	
	public void rename(GeoElement geo) {
	}	
	public void updateAuxiliaryObject(GeoElement geo) {
	}	
	public void repaintView() {
	}
	public void clearView() {
	}
	
	/**/
	/**
	 * returns settings in XML format
	 */
	public String getXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<spreadsheetView>\n");
		
		int width = getWidth();//getPreferredSize().width;
		int height = getHeight();//getPreferredSize().height;
		
		//if (width > MIN_WIDTH && height > MIN_HEIGHT) 
		{
			sb.append("\t<size ");
			sb.append(" width=\"");
			sb.append(width);
			sb.append("\"");
			sb.append(" height=\"");
			sb.append(height);
			sb.append("\"");
			sb.append("/>\n");
		}
		
		// column widths 
		for (int col = 0 ; col < table.getColumnCount() ; col++) {
			TableColumn column = table.getColumnModel().getColumn(col); 
			int colWidth = column.getWidth();
			if (colWidth != DEFAULT_COLUMN_WIDTH)
				sb.append("\t<spreadsheetColumn id=\""+col+"\" width=\""+colWidth+"\"/>\n");
		}

		sb.append("</spreadsheetView>\n");
		return sb.toString();
	}
	
	public void setColumnWidth(int col, int width) {
		//Application.debug("col = "+col+" width = "+width);
		TableColumn column = table.getColumnModel().getColumn(col); 
		column.setPreferredWidth(width);
		//column.
	}
	
	public void updateFonts() {
		
		
		Font font = app.getPlainFont();
		
		int size = font.getSize();
		if (size < 12) size = 12; // minimum size
		double multiplier = (double)(size)/12.0;
		
		table.setRowHeight((int)(MyTable.TABLE_CELL_HEIGHT * multiplier));
		rowHeader.setFixedCellWidth((int)(ROW_HEADER_WIDTH * multiplier));
		rowHeader.setFixedCellHeight(table.getRowHeight()); 
		
		table.setFont(app.getPlainFont());
		rowHeader.setFont(font);
		table.columnHeader.setFont(font);
		rowHeaderRenderer.setFont(font);
		
		table.columnHeader.setPreferredSize(new Dimension((int)(MyTable.TABLE_CELL_WIDTH * multiplier)
						, (int)(MyTable.TABLE_CELL_HEIGHT * multiplier)));
		

	
		
	}
	
	public void updateColumnWidths() {
		Font font = app.getPlainFont();
		
		int size = font.getSize();
		if (size < 12) size = 12; // minimum size
		double multiplier = (double)(size)/12.0;
		for (int i = 0; i < table.getColumnCount(); ++ i) {
			table.getColumnModel().getColumn(i).setPreferredWidth((int)(MyTable.TABLE_CELL_WIDTH * multiplier));
		}
		

	}

	public MyTable getTable() {
		return table;
	}
	
}
		

