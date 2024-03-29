/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.kernel.arithmetic;

import geogebra.main.Application;


/**
 * Class for polynomials with ExpressionValues as coefficients.
 * Needed for root finding. 
 */
public class SymbolicPolyFunction extends PolyFunction {
	
	//	symbolic coefficients, e.g. expression tree
	// used to recalc the coefficients of this polynomial
	private ExpressionNode [] symbCoeffs; 
	
	public SymbolicPolyFunction(int degree) {
		super(degree);
		symbCoeffs = new ExpressionNode[degree + 1];		
	}
	
	public ExpressionNode [] getSymbolicCoeffs() {
		return symbCoeffs;
	}
	
	/*
	public SymbolicPolyFunction getSymbolicDerivative(Kernel kernel) {
		int degree = getDegree();
		SymbolicPolyFunction deriv;
		if (degree < 1) {
			deriv = new SymbolicPolyFunction(0);
			deriv.symbCoeffs[0] = new ExpressionNode(kernel, new MyDouble(kernel, 0));
			return deriv;
		}
		
		// standard case
		deriv = new SymbolicPolyFunction(degree - 1);
		for (int i=1; i <= degree; i++) {
			deriv.symbCoeffs[i-1] = new ExpressionNode(kernel, new MyDouble(kernel, i), 
					ExpressionNode.MULTIPLY, symbCoeffs[i]);
		}
		return deriv;
	}
	*/
	
	/**
	 * Evaluates all symbolic coefficients and stores the results
	 * in this polynomial's double coefficients.
	 * @return true when successful 	 
	 */
	final public boolean updateCoeffValues() {			
		try {
			//	evaluate all the coefficients			
			 double coeff;
			 for (int i=0; i < symbCoeffs.length; i++) {				 
				ExpressionValue eval = symbCoeffs[i].evaluate();
				coeff = ((NumberValue) eval).getDouble();
				if (Double.isNaN(coeff) || Double.isInfinite(coeff))
					return false; // no valid values			 	
				coeffs[i] =  coeff; // set polynomial coefficient				 
			 }
			 return true;
		} catch (Exception e) {
			Application.debug("updateCoeffValues: ");
			e.printStackTrace();
			return false;
		}	catch (Error err) {
			Application.debug("updateCoeffValues: ");
			err.printStackTrace();
			return false;
		}
	}
	
}
