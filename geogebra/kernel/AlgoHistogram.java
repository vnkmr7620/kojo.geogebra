/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.kernel;

public class AlgoHistogram extends AlgoFunctionAreaSums {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AlgoHistogram(Construction cons, String label,
								   GeoList list1, GeoList list2) {
		super(cons, label, list1, list2);		
	}
	
	protected String getClassName() {
		return "AlgoHistogram";
	}
	
}
