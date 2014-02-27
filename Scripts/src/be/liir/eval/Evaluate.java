package be.liir.eval;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import be.liir.SpRL.corpus.SpRLReader;
import be.liir.SpRL.jaxb.DIRECTION;
import be.liir.SpRL.jaxb.DISTANCE;
import be.liir.SpRL.jaxb.LANDMARK;
import be.liir.SpRL.jaxb.MOTIONINDICATOR;
import be.liir.SpRL.jaxb.PATH;
import be.liir.SpRL.jaxb.RELATION;
import be.liir.SpRL.jaxb.SPATIALINDICATOR;
import be.liir.SpRL.jaxb.SpRL;
import be.liir.SpRL.jaxb.TRAJECTOR;
import be.liir.SpRL.jaxb.util.JAXBUtil;
import be.liir.SpRL.model.Markable;
import be.liir.SpRL.model.WithIdentifier;

public class Evaluate {
	private static final String NULL = "NULL";
	private static final int TRIPPLE = 3;
	private static final String TASK_A = "TASK A";
	private static final String TASK_B = "TASK B";
	private static final String TASK_C = "TASK C";
	private static final String TASK_D_SIMPLE = "TASK D_simple";
	private static final String TASK_D = "TASK D";
	private static final String RCC8_EQUAL = "EQ";
	private static final String TASK_E = "TASK E";
	private static final int MAX_LEN = 8;
	
	private static Map<String, List<Class<? extends Markable>>> EVAL_MAP = null;
	private static Map<String, String> PARAMS = null;
	
	private static Map<Class<? extends Markable>, Class<? extends Markable>> SYMMETRIC_CLASSES = null;
	
	static {
		PARAMS = new HashMap<String, String>();
		PARAMS.put("goldData","");
		PARAMS.put("systemData","");
		
		// map that contains evaluation setting (key) and the classes to be evaluated for relation participants
		EVAL_MAP = new HashMap<String, List<Class<? extends Markable>>>();
		
		List<Class<? extends Markable>> classes = new ArrayList<Class<? extends Markable>>();
		classes.add(TRAJECTOR.class);
		classes.add(LANDMARK.class);
		classes.add(SPATIALINDICATOR.class);
		EVAL_MAP.put(TASK_B, classes);
		
		classes = new ArrayList<Class<? extends Markable>>();
		classes.add(TRAJECTOR.class);
		classes.add(MOTIONINDICATOR.class);
		classes.add(PATH.class);
		EVAL_MAP.put(TASK_D_SIMPLE, classes);

		classes = new ArrayList<Class<? extends Markable>>();
		classes.add(TRAJECTOR.class);
		classes.add(MOTIONINDICATOR.class);
		classes.add(PATH.class);
		classes.add(LANDMARK.class);
		classes.add(SPATIALINDICATOR.class);
		classes.add(DISTANCE.class);
		classes.add(DIRECTION.class);
		
		EVAL_MAP.put(TASK_D, classes);

		classes = new ArrayList<Class<? extends Markable>>();
		classes.add(TRAJECTOR.class);
		classes.add(MOTIONINDICATOR.class);
		classes.add(PATH.class);
		classes.add(LANDMARK.class);
		classes.add(SPATIALINDICATOR.class);
		classes.add(DISTANCE.class);
		classes.add(DIRECTION.class);
		
		EVAL_MAP.put(TASK_E, classes);

		SYMMETRIC_CLASSES = new HashMap<Class<? extends Markable>, Class<? extends Markable>>();
		SYMMETRIC_CLASSES.put(TRAJECTOR.class, LANDMARK.class);
		SYMMETRIC_CLASSES.put(LANDMARK.class, TRAJECTOR.class);
		
	}

	private static void usage(){
		System.out.println("Usage: java -jar SpRL.jar goldData systemData [strictEval]\n");
		System.out.println("goldData \t manually annotated gold standard corpus or file");
		System.out.println("systemData \t automatically annotated corpus or file");
		System.out.println("strictEval \t true or false: for strict or relaxed (default) evaluation");
	}
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, JAXBException {
		
		if(!validParams(args)){
			System.err.println("Wrong parameters. Please check the usage:");
			usage();
		}else
			validate(args);
		
	}

	private static void validate(String[] args) throws SAXException, IOException, ParserConfigurationException, JAXBException {
		File goldData = new File(args[0]);
		File systemData = new File(args[1]);
		boolean strictSpan = false;
		if(args.length > 2)
			strictSpan = Boolean.parseBoolean(args[2]);
		
		Map<String, Outcome> outcomes = new HashMap<String, Outcome>();// evaluation results. Each label evaluation is saved in a separate outcome object.
		Outcome outcomeA = new Outcome(new String[]{NULL}); 
		Outcome outcomeB = new Outcome(new String[]{NULL});
		Outcome outcomeC = new Outcome(new String[]{NULL});
		Outcome outcomeD = new Outcome(new String[]{NULL});
		Outcome outcomeE = new Outcome(new String[]{NULL});
		outcomes.put(TASK_A, outcomeA);
		outcomes.put(TASK_B, outcomeB);
		outcomes.put(TASK_C, outcomeC);
		outcomes.put(TASK_D, outcomeD);
		outcomes.put(TASK_E, outcomeE);
		
		if(goldData.isFile()){
			evaluateFile(goldData, systemData, outcomes, strictSpan);
			print(outcomes);
			
		}else if(goldData.isDirectory()){
			File[] goldFiles = goldData.listFiles();
			File[] systemFiles = systemData.listFiles();
			for (File file : goldFiles) {
				if(file.getName().indexOf(".xml")==-1)
					continue;
				File sFile = getPairedFileExist(file, systemFiles);
				if(sFile == null){
					System.err.println(String.format("System standard file does not exists for %s", file.getName()));
				}else{
					evaluateFile(file, sFile, outcomes, strictSpan);
				}
			}
			print(outcomes);
		}
	}

	private static File getPairedFileExist(File file, File[] goldFiles) {
		File result = null;
		for (File gFile : goldFiles) {
			if(file.getName().equalsIgnoreCase(gFile.getName())){
				result = gFile;
				break;
			}
		}
		return result;
	}

	private static void print(Map<String, Outcome> outcomes) {
		List<String> keys = new ArrayList<String>(outcomes.keySet());
		Collections.sort(keys);
		for (String string : keys) {
			Outcome out = outcomes.get(string);
			System.out.println("EVALUATION RESULTS FOR "+string);
			System.out.println(out);
			System.out.println();
			System.out.println();
		}
	}

	/**
	 * Evaluates annotations for one file and
	 * saves the evaluation results for all the labels into the list of outcomes.
	 * 
	 * @param goldData - gold annotation file
	 * @param systemData - system annotated file
	 * @param outcomes - list of outcomes
	 * @param strictSpan TODO
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws JAXBException
	 */
	public static void evaluateFile(File goldData, File systemData, Map<String, Outcome> outcomes, boolean strictSpan)
			throws SAXException, IOException, ParserConfigurationException,
			JAXBException {
		SpRL gold = SpRLReader.getDocument(goldData.getAbsolutePath());
		SpRL system = SpRLReader.getDocument(systemData.getAbsolutePath());
		
		List list = EVAL_MAP.get(TASK_B);
		Outcome outcome = outcomes.get(TASK_A);
		evaluateMarkables(gold, system, list, outcome);
		
		
		Class<RELATION> relation = RELATION.class;
		boolean matchLabel = false; // when matching spans, should the label be considered too?  
		outcome = outcomes.get(TASK_B);
		evaluateRelations(gold, system, relation, TASK_B, outcome, matchLabel, strictSpan);

		
		outcome = outcomes.get(TASK_C);
		evaluateMarkables(gold, system, null, outcome);
		
		outcome = outcomes.get(TASK_D);
		evaluateRelations(gold, system, relation, TASK_D, outcome, matchLabel, strictSpan);

		
		outcome = outcomes.get(TASK_E);
		evaluateRelations(gold, system, relation, TASK_E, outcome, matchLabel, strictSpan);
		

	}

	/**
	 * Evaluates relations. 
	 * 
	 * @param gold
	 * @param system
	 * @param labelClass
	 * @param evalOption
	 * @param matchLabel TODO
	 * @param strictSpan TODO
	 * @param outcomes
	 */
	private static <T extends WithIdentifier> void evaluateRelations(SpRL gold, SpRL system, Class<T> labelClass,
			String evalOption, Outcome outcome, boolean matchLabel, boolean strictSpan) {
		
		if(!gold.getTEXT().getContent().equalsIgnoreCase(system.getTEXT().getContent())){
			if(!gold.getTEXT().getContent().contains(system.getTEXT().getContent()))
				//throw new IllegalArgumentException("The content of the data is not equal.");
				System.err.println(system.getFilename()+": The content of the data is not equal. Gold text length: "+gold.getTEXT().getContent().length()+" System text length:"+system.getTEXT().getContent().length()+ " EVALUATING ANYWAY!");

		}
		//else{

		List<T> goldAnnotations = null;
		List<T> systemAnnotations = null;

		if(labelClass == null){
			throw new NullPointerException("Annotation class is not defined.");				
		}else{
			goldAnnotations = JAXBUtil.getAnnotations(gold, labelClass);
			systemAnnotations = JAXBUtil.getAnnotations(system, labelClass);
		}

		List<T> goldRelations = JAXBUtil.getAnnotations(gold, labelClass); // all gold relations

		String goldLabel = "";
		String systemLabel = "";
		if(systemAnnotations !=null){
			for (Iterator<T> iterator = systemAnnotations.iterator(); iterator
					.hasNext();) {
				T t = iterator.next();
				RELATION systemRelation = (RELATION) t;
				if(systemRelation.getGeneralType() != null && systemRelation.getGeneralType().trim().length() == 0)
					System.err.println("ERROR in "+system.getFilename());
				if(systemRelation.getRCC8Value() != null && systemRelation.getRCC8Value().equalsIgnoreCase(RCC8_EQUAL))
					continue;

				Map<Class<? extends Markable>, List<? extends Markable>> args = JAXBUtil.getArgs(systemRelation, system);
				RELATION goldMatch = (RELATION) evaluateRelation(args, goldRelations, gold, labelClass, evalOption, outcome, strictSpan);


				if(evalOption.equalsIgnoreCase(TASK_E)){
					goldLabel = goldMatch!=null?goldMatch.getGeneralType():NULL;
					systemLabel = systemRelation.getGeneralType();
				}else{
					goldLabel = labelClass.getSimpleName();
					systemLabel = labelClass.getSimpleName();
				}
				
				if(systemLabel == null)
					systemLabel = NULL; // if no rel found
				
				if(goldMatch != null){
					outcome.evaluate(truncateTo(goldLabel, MAX_LEN), truncateTo(systemLabel, MAX_LEN)); //exact match for markables
					goldRelations.remove(goldMatch);
				}else{
					outcome.evaluate(NULL, truncateTo(systemLabel, MAX_LEN));
					goldMatch = null;
				}

				/*
					System.out.println("SYSTEM OUTPUT:\t"+r.toString(args));
					if(goldMatch != null){
						System.out.println("MATCH FOUND:\t"+goldMatch.toString(JAXBUtil.getArgs(goldMatch, gold)));
					}else
						System.out.println("NO MATCH FOUND");
					System.out.println();
				 */


				iterator.remove();
			}
		}

		if(goldRelations != null){
			// for the rest of gold annotation which were not matched by markables
			for (Iterator<T> iterator = goldRelations.iterator(); iterator
					.hasNext();) {
				T t = iterator.next();
				RELATION r = (RELATION) t;

				// ignore coreferential relations 
				if(r.getRCC8Value() != null && r.getRCC8Value().equalsIgnoreCase(RCC8_EQUAL))
					continue;

				if(evalOption.equalsIgnoreCase(TASK_E)){
					goldLabel = r.getGeneralType();
					systemLabel = NULL;
				}else{
					goldLabel = labelClass.getSimpleName();
					systemLabel = NULL;
				}

				outcome.evaluate(truncateTo(goldLabel, MAX_LEN), NULL);

				/*
					System.out.println("SYSTEM OUTPUT:\t"+null);
					System.out.println("GOLD ANN:\t"+r.toString(JAXBUtil.getArgs((RELATION) r, gold)));
					System.out.println();
				 */
			}
		}
		//}
	}

	/**
	 * Evaluates one relation and returns a gold relation which corresponds to the specified system relation arguments.
	 * 
	 * @param args - system relation agruments.
	 * @param goldRelations - list of all gold relations
	 * @param gold - JAXB-representation of the entire document.
	 * @param label - label class used for the evaluation (depricated)
	 * @param evalOption - evaluation task
	 * @param outcome - outcome to store results.
	 * @param strict TODO
	 */
	private static <T extends WithIdentifier> T evaluateRelation(Map<Class<? extends Markable>, List<? extends Markable>> args, 
			List<T> goldRelations, SpRL gold, 
			Class<T> label, String evalOption, Outcome outcome, boolean strict){
		
		//List<T> goldRelations = JAXBUtil.getAnnotations(gold, label);
		
		int maxMatch = Integer.MIN_VALUE;
		
		T found = null;
		
		if(goldRelations != null){
			for (Iterator<T> iterator = goldRelations.iterator(); iterator.hasNext();) {
				T r = iterator.next();
				RELATION goldRelation = (RELATION) r;
				
				//skip if it is an EQ = coreference
				if(goldRelation.getRCC8Value() != null && goldRelation.getRCC8Value().equalsIgnoreCase(RCC8_EQUAL))
					continue;

				Map<Class<? extends Markable>, List<? extends Markable>> goldArgs = JAXBUtil.getArgs(goldRelation, gold);
				
				// matches is a map of labels and matches (true, false) between system and gold annotations. The span and the label have to be the same.
				Map<Class<Markable>, Boolean> matches = 
						JAXBUtil.getMarkableMatches(goldArgs, args, strict);
				
				int matchesFound = JAXBUtil.getCntNonEmptyArgs(matches, 
						EVAL_MAP.get(evalOption).toArray(new Class[EVAL_MAP.get(evalOption).size()]));
				
				/*
				// span-based matches
				Map<Class<Markable>, Boolean> spanMatches =
						JAXBUtil.getSpanMatches(goldArgs, args);

				int spanMatchesFound = JAXBUtil.getCntNonEmptyArgs(spanMatches, 
						EVAL_MAP.get(evalOption).toArray(new Class[EVAL_MAP.get(evalOption).size()]));
				*/
				
				// swap swappable arguments
				Map<Class<? extends Markable>, List<? extends Markable>>  swappedArgs = swapSymmetricalArgs(args);
				// find matches
				Map<Class<Markable>, Boolean> swappedMatches = 
						JAXBUtil.getMarkableMatches(goldArgs, swappedArgs, strict);
				// count matches
				int swappedMatchesFound = JAXBUtil.getCntNonEmptyArgs(swappedMatches, 
						EVAL_MAP.get(evalOption).toArray(new Class[EVAL_MAP.get(evalOption).size()]));
				
				// take the max
				matchesFound = Math.max(matchesFound, swappedMatchesFound);
				
				int toMatch = EVAL_MAP.get(evalOption).size();
				
				
				
				if(matchesFound-toMatch > maxMatch){
					found = r;
					maxMatch = matchesFound-toMatch;
				}
				
				// if the max match number is found
				if(maxMatch>=0)
					break;
			}
		}
		
		if(maxMatch>=0 && found != null){
			//outcome.evaluate(label.getSimpleName(), label.getSimpleName());
			//goldRelations.remove(found);
		}else{
			//outcome.evaluate(NULL, label.getSimpleName());
			found = null;
		}
		
		return found;	
	}

	/**
	 * Swaps eligible arguments in relations to address symmetry.
	 * @param newArgs
	 * @return
	 */
	private static Map<Class<? extends Markable>, List<? extends Markable>> 
			swapSymmetricalArgs(Map<Class<? extends Markable>, List<? extends Markable>> newArgs) {
		
		Map<Class<? extends Markable>, List<? extends Markable>> result = 
				new HashMap<Class<? extends Markable>, List<? extends Markable>>(newArgs);
		
		
		Set<Class<? extends Markable>> labels = SYMMETRIC_CLASSES.keySet();
		for (Class<? extends Markable> label : labels) {
			Class<? extends Markable> swapWithLabel = SYMMETRIC_CLASSES.get(label); 

			List<? extends Markable> origMarkables = newArgs.get(label);
			Markable firstMarkable = JAXBUtil.unwrapFromList(origMarkables); // first element to swap

			if(firstMarkable == null)
				firstMarkable = createMarkable(label);
			Markable firstClone = JAXBUtil.copyObj(firstMarkable);

			
			origMarkables = newArgs.get(swapWithLabel);
			Markable secondMarkable = JAXBUtil.unwrapFromList(origMarkables); // second element to swap
			if(secondMarkable == null)
				secondMarkable = createMarkable(swapWithLabel);
			Markable secondClone = JAXBUtil.copyObj(secondMarkable);
			
			
			swap(firstClone, secondClone);
			
			// set those which received value from nulls to nulls.
			if(firstClone.getStart() == null)
				firstClone = null;
			if(secondClone.getStart() == null)
				secondClone = null;
			
			result.put(label, JAXBUtil.wrapToList(firstClone));
			result.put(swapWithLabel, JAXBUtil.wrapToList(secondClone));
		}
		return result;
	}

	/**
	 * @param label
	 * @return
	 */
	private static Markable createMarkable(Class<? extends Markable> label) {
		Markable result = null;
		try {
			result = label.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @param arg1
	 * @param arg2
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private static void swap(Markable arg1, Markable arg2){
		BigInteger start = null, end = null;
		String text = null;
		
		start = arg1.getStart();
		end = arg1.getEnd();
		text = arg1.getText();
				 

		arg1.setStart(arg2.getStart());
		arg1.setEnd(arg2.getEnd());
		arg1.setText(arg2.getText());

		arg2.setStart(start);
		arg2.setEnd(end);
		arg2.setText(text);

	}

	/**
	 * Evaluates markable annotations for one specified label (provided as a JAXB annotation class) and
	 * saves the results into the list of outcomes.
	 * 
	 * @param gold - JAXB-representation of a gold standard file
	 * @param system - JAXB-representation of a system annotated file
	 * @param labels - JAXB-class for which the evaluation is performed. If <code>null</code> is provided the evaluation is performed 
	 * for all markables.
	 * @param outcome - list of outcomes. It will be updated with the evaluation results for this class.
	 */
	private static <T extends Comparable<? super T>> void evaluateMarkables(SpRL gold, SpRL system,
			List<Class<T>> labels, Outcome outcome) {

		
		if(!gold.getTEXT().getContent().equalsIgnoreCase(system.getTEXT().getContent())){
			if(!gold.getTEXT().getContent().contains(system.getTEXT().getContent()))
				//throw new IllegalArgumentException("The content of the data is not equal.");
				System.err.println("The content of the data is not equal. Gold text length: "+gold.getTEXT().getContent().length()+" System text length:"+system.getTEXT().getContent().length()+ " EVALUATING ANYWAY!");
		}
		//else{
		List<T> goldAnnotations = null;
		List<T> systemAnnotations = null;

		if(labels == null){
			goldAnnotations = (List<T>) JAXBUtil.getAllMarkables(gold);
			systemAnnotations = (List<T>) JAXBUtil.getAllMarkables(system);

		}else{
			goldAnnotations = JAXBUtil.getAnnotations(gold, labels);
			systemAnnotations = JAXBUtil.getAnnotations(system, labels);
		}
		if(systemAnnotations == null)
			systemAnnotations = new ArrayList<T>();
		if(goldAnnotations == null)
			goldAnnotations = new ArrayList<T>();
		
		validate(systemAnnotations, system.getFilename());

		Collections.sort(goldAnnotations);
		Collections.sort(systemAnnotations);

		String text = gold.getTEXT().getContent();
		int length = text.length();
		for (int i = 0; i < length; i++) {
			String[] goldLabels = getLabels(i, i, goldAnnotations);
			String[] systemLabels = getLabels(i, i, systemAnnotations);
			evaluate(goldLabels, systemLabels, outcome);
		}
		//}
	}
	
	/**
	 * Updates the outcome with respect to the labels found.
	 * @param goldLabels - gold labels
	 * @param systemLabels - system labels
	 * @param outcome -outcome
	 */
	private static void evaluate(String[] goldLabels, String[] systemLabels,
			Outcome outcome) {
		List<String> goldList = new ArrayList<String>();
		if(goldLabels != null)
			goldList = new ArrayList<String>(Arrays.asList(goldLabels));
		
		List<String> systemList = new ArrayList<String>();
		if(systemLabels != null)
			systemList = new ArrayList<String>(Arrays.asList(systemLabels));
		
		JAXBUtil.normalizeLists(goldList, systemList, NULL);//make lists of the same size by adding additional nulls
		
		//
		for (Iterator<String> iterator = systemList.iterator(); iterator.hasNext();) {
			String systemLabel = iterator.next();
			int goldLabelIndex = goldList.indexOf(systemLabel); // look for the same label in the gold list
			
			// if not found, try to find the Null
			if(goldLabelIndex == -1)
				goldLabelIndex = goldList.indexOf(NULL);
			
			// if NULL label is not a gold label either, pick the first one
			if(goldLabelIndex == -1 && goldList.size() > 0)
				goldLabelIndex = 0;
			/*
			else{
				System.err.println(new IllegalArgumentException(String.format("Ups! No label pair is found. System label: %s Gold labels: %s", 
						systemLabel, goldList)));
			}
			*/
			
			String goldLabel = goldList.get(goldLabelIndex);
			// store the results
			outcome.evaluate(goldLabel, systemLabel);
			// remove labels
			iterator.remove();
			goldList.remove(goldLabelIndex);
		}	
	}

	/**
	 * Retrieves all label of the textual span provided by the start and end offsets. The labels are derived from JAXB-class names.
	 * @param startOffset - start offset of the span.
	 * @param endOffset - end offset of the span.
	 * @param annotations - list of annotations.
	 * @return label 
	 */
	private static <T extends Comparable<? super T>> String[] getLabels(int startOffset, int endOffset, List<T> annotations){
		List<String> labels = null;
		
		if(annotations != null && annotations.size() != 0){
			Markable annotation = null;
			annotation = (Markable) annotations.get(annotations.size()-1);// Optimisation. Very last annotation in the text
			if(startOffset > annotation.getStart().intValue() 
					&& startOffset >= annotation.getEnd().intValue()){
				/*
				System.out.println(String.format("Offset: %s-%s is farther than the very last annotation offset: %s-%s", 
						startOffset, endOffset, annotation.getStart().intValue(), annotation.getEnd().intValue()));
				*/
				;
			}
			else{
				for (T t : annotations) {
					annotation = (Markable) t;
					if(annotation.getStart().intValue() == -1 
							&& annotation.getEnd().intValue() == -1)
						continue;
					else if(annotation.getStart().intValue() <= startOffset
							&& annotation.getEnd().intValue() > endOffset){
						if(labels == null)
							labels = new ArrayList<String>();
						
						String label = annotation.getClass().getSimpleName();
						label = truncateTo(label, MAX_LEN);
						labels.add(label); // save all labels available for this span
					}
				}
			}
		}
		String[] result = null;
		if(labels != null){
			result = new String[labels.size()];
			labels.toArray(result);
		}
		return result;
	}

	/**
	 * @param string
	 * @param maxLen TODO
	 * @return
	 */
	private static String truncateTo(String string, int maxLen) {
		string = (string.length()>=maxLen)?string.substring(0, maxLen-2):string;
		return string;
	}
	
	
	/**
	 * Validates markables:
	 * 	if the span start < end 
	 * 
	 * @param annotations
	 * @param filename
	 */
	private static <T extends Comparable<? super T>> void validate(List<T> annotations, String filename) {
		for (T obj : annotations) {
			Markable m = (Markable) obj;
			if(m.getEnd().intValue() > -1
					&& m.getStart().intValue() > -1 
					&& m.getStart().intValue() >= m.getEnd().intValue())
				System.err.println(new IllegalArgumentException(String.format("Wrong annotation span. File: %s Markable: %s Start:%s End:%s", 
						filename, m.getClass().getSimpleName(), m.getStart().intValue(), m.getEnd().intValue())));
		}
	}
	
	private static boolean validParams(String[] args) {
		boolean result = false;
		if(args.length >= 2){
			if(sameDataType(args) && exists(args))
				result = true;
		}
		return result;
	}

	private static boolean exists(String[] args) {
		boolean result = false;
		File goldData = new File(args[0]);
		File systemData = new File(args[1]);
		if(goldData.exists() && systemData.exists()){
			result = true;
		}
		return result;
	}

	private static boolean sameDataType(String[] args) {
		boolean result = false;
		File goldData = new File(args[0]);
		File systemData = new File(args[1]);
		if(goldData.isDirectory() == systemData.isDirectory()){
			result = true;
		}else if(goldData.isFile() == systemData.isFile())
			result = true;
		return result;
	}
}
