/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.kernel;

/**
 * GCD of a list.
 * adapted from AlgoListMax
 * @author Michael Borcherds
 * @version 03-01-2008
 */

public class AlgoListGCD extends AlgoElement {

	private static final long serialVersionUID = 1L;
	private GeoList geoList; //input
    private GeoNumeric num; //output	

    AlgoListGCD(Construction cons, String label, GeoList geoList) {
        super(cons);
        this.geoList = geoList;
               
        num = new GeoNumeric(cons);

        setInputOutput();
        compute();
        num.setLabel(label);
    }

    protected String getClassName() {
        return "AlgoListGCD";
    }

    protected void setInputOutput(){
        input = new GeoElement[1];
        input[0] = geoList;

        output = new GeoElement[1];
        output[0] = num;
        setDependencies(); // done by AlgoElement
    }

    GeoNumeric getGCD() {
        return num;
    }

    protected final void compute() {
    	int size = geoList.size();
    	if (!geoList.isDefined() ||  size == 0) {
    		num.setUndefined();
    		return;
    	}
    	
    	String MathPiperList=geoList.toValueString();
    	String MathPiperCommand="Gcd("+MathPiperList+")";    	
		String result=kernel.evaluateMathPiper(MathPiperCommand);
		try {
			double gcd = Double.valueOf(result).doubleValue();
			num.setValue(gcd);
			
		}
		catch (Exception e) {
			num.setUndefined();	
		}
    }
    
}
