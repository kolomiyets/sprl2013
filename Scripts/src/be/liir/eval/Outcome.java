package be.liir.eval;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Utility to evaluate supervised outcomes.
 *
 * @author Tiphaine Dalmas
 */
public class Outcome implements Cloneable{
    protected float[][] matrix;
    protected String[] values;

    public Outcome(Collection<String> values) {
	this.matrix = new float[values.size()][values.size()];
	this.values = new String[values.size()];
	int i = 0;
	for (Iterator<String> iter = values.iterator(); iter.hasNext(); i++) {
	    String value = iter.next();
	    this.values[i] = value;
	}
    }

    public Outcome(String[] values) {
	this.matrix = new float[values.length][values.length];
	this.values = values;
    }

    public synchronized void evaluate(String evaluation, String system) {
	update(evaluation, system);
    for (int i = 0; i < this.matrix.length; i++) {
	    if (this.values[i].compareTo(evaluation) == 0) {
			for (int j = 0; j < this.matrix[i].length; j++) {
			    if (this.values[j].compareTo(system) == 0) {
			    	this.matrix[i][j]++;
			    }
			}
	    }
	}
    }

    private void update(String evaluation, String system) {
    	update(evaluation);
    	update(system);
	}

	private void update(String label) {
    	List<String> vals = new ArrayList<String>(Arrays.asList(this.values));
    	if(!vals.contains(label)){
    		vals.add(label);
    		values = vals.toArray(values);
    		
    		float [][] localMatrix = new float[values.length][values.length];
    		for (int i = 0; i < matrix.length; i++)
    			for (int j = 0; j < matrix.length; j++)
    				localMatrix[i][j] = matrix[i][j];
    		
    		matrix = localMatrix;
    	}
	}

	public float n() {
	float n = 0;
	for (int i = 0; i < this.matrix.length; i++) {
	    for (int j = 0; j < this.matrix[i].length; j++) {
		n += this.matrix[i][j];
	    }
	}
	return n;
    }

    public float tpRate(int i) {
	return recall(i);
    }

    public float fpRate(int i) {
	float fp = fp(i);
	float n = fp + tn(i);
	if (n > 0) {
	    return fp / n;
	} else {
	    return 0;
	}
    }

    public float correctInstances() {
	float c = 0;
	for (int i = 0; i < this.matrix.length; i++) {
	    c += tp(i);
	}
	return c;
    }

    public float tp(int i) {
	return this.matrix[i][i];
    }

    public float fp(int i) {
	float fp = 0;
	for (int j = 0; j < this.matrix.length; j++) {
	    if (i != j) {
		fp += this.matrix[j][i];
	    }
	}
	return fp;
    }

    public float tn(int i) {
	float tn = 0;
	for (int j = 0; j < this.matrix.length; j++) {
	    if (i != j) {
		for (int k = 0; k < this.matrix.length; k++) {
		    if (i != k) {
			tn += this.matrix[j][k];
		    }
		}
	    }
	}
	return tn;
    }

    public float fn(int i) {
	float fn = 0;
	for (int j = 0; j < this.matrix.length; j++) {
	    if (i != j) {
		fn += this.matrix[i][j];
	    }
	}
	return fn;
    }

    /**
     * Defined as: TP / (TP + FN).
     */
    public float recall(int i) {
	float tp = tp(i);
	float n = tp + fn(i);
	if (n > 0) {
	    return tp / n;
	} else {
	    return 0;
	}
    }

    /**
     * Defined as: TP / (TP + FP).
     */
    public float precision(int i) {
	float tp = tp(i);
	float n = tp + fp(i);
	if (n > 0) {
	    return tp / n;
	} else {
	    return 0;
	}
    }

    /**
     * Defined as: (2 * P * R) / (P + R);
     */
    public float fMeasure(int i) {
	float r = recall(i);
	float p = precision(i);
	if (p + r > 0) {
	    return 2 * (p * r) / (p + r);
	} else {
	    return 0;
	}
    }
    public float fMeasure(float p, float r) {
	if (p + r > 0) {
	    return 2 * (p * r) / (p + r);
	} else {
	    return 0;
	}
    }

    public String toString() {
	DecimalFormat df = new DecimalFormat("0.0000");
	StringBuffer sb = new StringBuffer();
	float tp = correctInstances();
	float n = n();
	sb.append("=== Summary ===\n\n");
	sb.append("Correctly classified instances  \t");
	sb.append(tp);
	sb.append("\t");
	if (n > 0) {
	    sb.append(df.format(tp/n * 100));
	} else {
	    sb.append("0");
	}
	sb.append("%\n");
	sb.append("Incorrectly classified instances\t");
	sb.append(n-tp);
	sb.append("\t");
	if (n > 0) {
	    sb.append(df.format((n-tp)/n * 100));
	} else {
	    sb.append("0");
	}
	sb.append("%\n");
	sb.append("Total number of instances       \t");
	sb.append(n);
	sb.append("\t100%\n");
	
	df = new DecimalFormat("0.000");
	sb.append("\n\n=== Detailed Accuracy By Class ===\n\n");
	sb.append("TP Rate   FP Rate   Precision   Recall   F-Measure   Class\n");
	float pa = 0;
	float ra = 0;
	float fma = 0;
	for (int i = 0; i < values.length; i++) {
	    float pi = precision(i);
	    float ri = recall(i);
	    float fmi = fMeasure(pi, ri);
	    pa += pi;
	    ra += ri;
	    fma += fmi;
	    sb.append("  ");
	    sb.append(df.format(tpRate(i)));
	    sb.append("     ");
	    sb.append(df.format(fpRate(i)));
	    sb.append("       ");
	    sb.append(df.format(pi));
	    sb.append("    ");
	    sb.append(df.format(ri));
	    sb.append("       ");
	    sb.append(df.format(fmi));
	    sb.append("   ");
	    sb.append(values[i]);
	    sb.append("\n");
	}
	sb.append("\nMean Precision: ");
	sb.append(df.format(pa / values.length));
	sb.append("\nMean Recall: ");
	sb.append(df.format(ra / values.length));
	sb.append("\nMean F-Measure: ");
	sb.append(df.format(fma / values.length));

	sb.append("\n\n=== Confusion Matrix ===\n\n");
	for (int i = 0; i < this.values.length; i++) {
	    sb.append(values[i]);
	    sb.append("\t\t");
	}
	sb.append("<-- Classified as");
	sb.append("\n");
	for (int i = 0; i < this.matrix.length; i++) {
	    for (int j = 0; j < this.matrix[i].length; j++) {
		sb.append(this.matrix[i][j]);
		sb.append("\t\t");
	    }
	    sb.append("|  ");
	    sb.append(values[i]);
	    sb.append("\n");
	}
	return sb.toString();
    }
    
    private int getIndex(String outcome){
    	int index = -1;
    	for (int i = 0; i < values.length;i++) {
    		String value = values[i];
			if(value.equalsIgnoreCase(outcome)){
				index = i;
				break;
			}
		}
    	return index;
    }
    public float getPrecision(String outcome){
    	return precision(getIndex(outcome));
    }
    
    public float getRecall(String outcome){
    	return recall(getIndex(outcome));
    }
    
    public float getFmeasure(String outcome){
    	return fMeasure(getIndex(outcome));
    }

    public static void main(String[] args) {
	String[] values = {"a"};
	Outcome os = new Outcome(values);
	os.evaluate("a", "a");
	os.evaluate("a", "a");
	os.evaluate("a", "a");
	os.evaluate("a", "a");
	os.evaluate("a", "a");
	os.evaluate("a", "b");
	os.evaluate("b", "b");
	os.evaluate("b", "b");
	os.evaluate("b", "b");
	os.evaluate("b", "b");
	os.evaluate("b", "a");
	os.evaluate("c", "a");
	os.evaluate("c", "b");
	os.evaluate("c", "c");
	System.out.println(os);
    }

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
 
    
}