package be.liir.SpRL.jaxb.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import be.liir.SpRL.jaxb.DIRECTION;
import be.liir.SpRL.jaxb.DISTANCE;
import be.liir.SpRL.jaxb.LANDMARK;
import be.liir.SpRL.jaxb.MOTIONINDICATOR;
import be.liir.SpRL.jaxb.PATH;
import be.liir.SpRL.jaxb.RELATION;
import be.liir.SpRL.jaxb.SPATIALINDICATOR;
import be.liir.SpRL.jaxb.SpRL;
import be.liir.SpRL.jaxb.TRAJECTOR;
import be.liir.SpRL.model.Markable;
import be.liir.SpRL.model.WithIdentifier;
import be.lindo.utils.LINDOConsts.TLink;

public class JAXBUtil {
	private static final String COMMA = ",";
	static enum SPRL_2013_ID_PREFIX{T, L, S, SR};

	
	private static Map<String, Integer> LAST_ID_MAP = new HashMap<String, Integer>();
	public static WithIdentifier getIdentifyable(List<? extends WithIdentifier> list){
		WithIdentifier result = null;
		if(list != null & list.size() != 0){
			result = list.get(0);
		}
		return result;
	}

	/**
	 * 
	 * @param list
	 */
	public static void afterInit(List<? extends WithIdentifier> list){
		if(list != null)
			for (WithIdentifier obj : list){ 
				// this is a hack for SpRL 2013 if an annotation has no id 
				if(obj.getId() == null){
					String prefix = JAXBUtil.getIDPrefix(obj);
					int id = JAXBUtil.getNewId(prefix);
					String newId = prefix+id;
					obj.setId(newId);
				}
				obj.afterInit();
			}
	}
	
	public static int getNewId(String prefix){
		int result = 0;
		Integer inMap = LAST_ID_MAP.get(prefix);
		if(inMap != null){
			result = inMap.intValue()+1;
		}else 
			result++;
			
		LAST_ID_MAP.put(prefix, result);

		return result;
	}
	
	public static List<? extends WithIdentifier> getAllAnnotations(SpRL document){
		List<WithIdentifier> content = new ArrayList<WithIdentifier>();
		content.addAll(document.getTAGS().getTRAJECTOR());
		content.addAll(document.getTAGS().getLANDMARK());
		content.addAll(document.getTAGS().getSPATIALINDICATOR());
		content.addAll(document.getTAGS().getMOTIONINDICATOR());
		content.addAll(document.getTAGS().getDIRECTION());
		content.addAll(document.getTAGS().getDISTANCE());
		content.addAll(document.getTAGS().getSPEX());
		content.addAll(document.getTAGS().getPATH());
		content.addAll(document.getTAGS().getRELATION());
		return content;
	}

	public static List<? extends WithIdentifier> getAllMarkables(SpRL document){
		List<WithIdentifier> content = new ArrayList<WithIdentifier>();
		content.addAll(document.getTAGS().getTRAJECTOR());
		content.addAll(document.getTAGS().getLANDMARK());
		content.addAll(document.getTAGS().getSPATIALINDICATOR());
		content.addAll(document.getTAGS().getMOTIONINDICATOR());
		content.addAll(document.getTAGS().getDIRECTION());
		content.addAll(document.getTAGS().getDISTANCE());
		content.addAll(document.getTAGS().getSPEX());
		content.addAll(document.getTAGS().getPATH());
		return content;
	}

	public static <T> List<T> getAnnotations(SpRL document, Class<T> labelClass){
		List<T> result = null;
		List<? extends WithIdentifier> content = getAllAnnotations(document);
		for (WithIdentifier withIdentifier : content) {
			if(labelClass.isInstance(withIdentifier) ){
				if(result == null)
					result = new ArrayList<T>();
				result.add((T) withIdentifier);
			}
		}
		return result;
	}

	public static <T> List<T> getAnnotations(SpRL document, List<Class<T>> labelClasses){
		List<T> result = new ArrayList<T>();
		for (Class<T> clazz : labelClasses) {
			List<T> annotations = getAnnotations(document, clazz);
			if(annotations != null)
				result.addAll(annotations);
		}
		if(result.size() == 0)
			result = null;
		return result;
	}

	public static WithIdentifier findIdentifiable(String id, List<? extends WithIdentifier> list){
		WithIdentifier result = null;
		for (WithIdentifier instance : list) {
			if(instance.getId().equals(id)){
				result = instance;
				break;
			}
		}
		return result;
	}

	/**
	 * Finds matches (textual, or both are nulls) between two lists of markable arguments. 
	 * The label of the markables is taken into account, i.e. the label and the span have to be the same.
	 *   
	 * @param goldArgs - arguments from a gold relation.
	 * @param systemArgs - arguments from a system relation
	 * @param strict TODO
	 * @return a map of arguments and the value if they have a match.
	 */
	public static Map<Class<Markable>, Boolean> getMarkableMatches(
			Map<Class<? extends Markable>, List<? extends Markable>> goldArgs,
			Map<Class<? extends Markable>, List<? extends Markable>> systemArgs, boolean strict) {
		
		Map<Class<Markable>, Boolean> result = new HashMap<Class<Markable>, Boolean>();
		//normalizeLists(goldArgs, systemArgs, null);
		
		Set<Class<? extends Markable>> systemLabels = systemArgs.keySet(); 
		Set<Class<? extends Markable>> goldLabels = goldArgs.keySet();
		
		for (Class<? extends Markable> label : systemLabels) {
			List<Markable> sysMarkable = (List<Markable>) systemArgs.get(label);
			List<Markable> goldMarkable = (List<Markable>) goldArgs.get(label);
			
			boolean match = hasExactMatchWithNulls(goldMarkable, sysMarkable, strict);
			result.put((Class<Markable>) label, match);
		}

		/*
		for (Markable arg : systemArgs) {
			if(arg != null){
				boolean match = findExactMatch(arg, goldArgs);
				result.put(arg, match);
			}
		}
		*/
		return result;
	}
	
	/**
	 * Finds span matches between two lists of markable arguments. 
	 * The label of the markables is NOT taken into account.
	 * 
 	 * @param goldArgs
	 * @param systemArgs
	 * @return
	 */
	public static Map<Class<Markable>, Boolean> getSpanMatches(
			Map<Class<? extends Markable>, List<? extends Markable>> goldArgs,
			Map<Class<? extends Markable>, List<? extends Markable>> systemArgs) {
		
		Map<Class<Markable>, Boolean> result = new HashMap<Class<Markable>, Boolean>();
		
		// get all markables spans
		Collection<List<? extends Markable>> systemSpans = systemArgs.values(); 
		Collection<List<? extends Markable>> goldSpans = goldArgs.values();
		
		// for every system span
		for (List<? extends Markable> spans : systemSpans) {
			// skip annotations which are nulls (empty)
			if(spans.size() == 1 && spans.get(0) == null)
				continue;
			
			boolean match = false; 
			Iterator goldIterator = goldSpans.iterator();
			
			//check if there is any span in gold standard which is the same as the system one
			while(!match && goldIterator.hasNext()){
				List gold = (List) goldIterator.next();
				match = hasExactMatch(gold, (List<Markable>) spans);
			}
			
			result.put((Class<Markable>) spans.get(0).getClass(), match);
		}

		return result;

	}

	/**
	 * Checks if there is a match (at least one) between the specified system and gold spans.
	 * If the specified lists are of the same size (1) and this element is null in both - it acounts for a match too. 
	 * E.g. if there is no SpIndicator (sp_id=null) when evaluating tripples.
	 * 
	 * @param goldMarkable
	 * @param sysMarkable
	 * @param strict TODO
	 * @return
	 */
	private static boolean hasExactMatchWithNulls(List<Markable> goldMarkable,
			List<Markable> sysMarkable, boolean strict) {
		
		boolean result = false;
		boolean checked = false;
		normalizeLists(sysMarkable, goldMarkable, null);
		
		for (Markable sysM : sysMarkable) {
			for (Markable goldM : goldMarkable) {
				if(sysM != null && goldM != null){
					if(strict){
						if(sysM.getStart().intValue() == goldM.getStart().intValue() &&
								sysM.getEnd().intValue() == goldM.getEnd().intValue()){
							result = true;
						}
					}else{
						long x1 = sysM.getStart().intValue(), x2 = sysM.getEnd().intValue();
						long y1 = goldM.getStart().intValue(), y2 = goldM.getEnd().intValue();
						boolean overlap = true;
						
						if(x1<y1 && x2<y2 && x1<y2 && x2<y1)
							overlap = false;//1st before the second
						else if(y1<x1 && y2<x2 && y1<x2 && y2<x1)
							overlap = false; // 1st after the 2nd
						if(overlap)
							result = true;
					}
				}else
					checked = true;
			}
		}
		
		//if the lists of markables of system and gold spans are of the same size (1 element) and it is a null - then it is also a match 
		if(checked & ! result)
			for (Markable sysM : sysMarkable) {
				for (Markable goldM : goldMarkable) {
					if(sysMarkable.size() == 1 && goldMarkable.size() == 1 &&
							sysM == null && goldM == null){
						result = true;
					}
				}
			}
		return result;
	}

	/**
	 * Checks if there is an exact match (at least one) between the specified system and gold spans.

	 * @param goldMarkable
	 * @param sysMarkable
	 * @return
	 */
	private static boolean hasExactMatch(List<Markable> goldMarkable,
			List<Markable> sysMarkable) {
		
		boolean result = false;
		normalizeLists(sysMarkable, goldMarkable, null);
		
		for (Markable sysM : sysMarkable) {
			for (Markable goldM : goldMarkable) {
				if(sysM != null && goldM != null){
					if(sysM.getStart().intValue() == goldM.getStart().intValue() &&
							sysM.getEnd().intValue() == goldM.getEnd().intValue()){
						result = true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Find the exact match between markables. The specified markable is checked against all gold markables
	 * and if the specified markable has the same span and the same annotation type as one of the gold markables,
	 * the method returns true, otherwise false 
	 * 
	 * @param markable - markable to check.
	 * @param markableList - list of markables to check against.
	 * @return true if the specified markable has the same span and annotation type with one of the markables in the specified list.
	 */
	private static <T extends Markable> boolean findExactMatch(T markable, List<T> markableList){
		boolean result = false;
		for (T obj : markableList) {
			if(obj != null && obj.getClass().equals(markable.getClass())){
				if(obj.getStart().intValue() == markable.getStart().intValue() &&
						obj.getEnd().intValue() == markable.getEnd().intValue()){
					result = true;
					break;
				}
			}
		}
		return result;
	}
	/**
	 * Normalizes two lists: finds the smaller and appends additional NULL elements making them equal in size. 
	 * @param list1
	 * @param list2
	 * @param dummy - an object which will be used to add to a smaller list.
	 */
	public static <T> void normalizeLists(List<T> list1,
			List<T> list2, Object dummy) {
		
		if(list1.size() == 0){
			list1.add((T) dummy);
		}
		if(list2.size() == 0){
			list2.add((T) dummy);
		}
		if(list1.size() != list2.size()){
			List<T> smallerList = (list1.size()<list2.size())?list1:list2;
			int size = (list1.size()<list2.size())?list2.size():list1.size();
			int smallSize = smallerList.size();
			for (int i = 0; i < size - smallSize; i++) {
				smallerList.add((T) dummy);
			}
		}
		
		if(list1.size() != list2.size()){
			System.err.println(new IllegalArgumentException("Ups! The arrays were not normlized."));
		}
	}

	/**
	 * Retrieves relation arguments (JAXB-objects) by the ID references in the specified relation.
	 * If no reference is found a null is added to the list anyway.
	 * 
	 * @param r - spatial relation
	 * @param sprl - JAXB-representation of a document in which the relation is annotated.
	 * @return a map with labels (Class) and Markable objects that participate in the specified relation.
	 */
	public static Map<Class<? extends Markable>, List<? extends Markable>> getArgs(RELATION r, SpRL sprl) {
		//List<Markable> result = new ArrayList<Markable>();
		Map<Class<? extends Markable>, List<? extends Markable>> result = new HashMap<Class<? extends Markable>, List<? extends Markable>>();
		
		List<? extends WithIdentifier> markables = getAllMarkables(sprl);
		
		TRAJECTOR tr = (TRAJECTOR) findIdentifiable(r.getTrajectorId(), markables);
		LANDMARK lm = (LANDMARK) findIdentifiable(r.getLandmarkId(), markables);
		SPATIALINDICATOR sp = (SPATIALINDICATOR) findIdentifiable(r.getSpatialIndicatorId(), markables);
		
		MOTIONINDICATOR motion = (MOTIONINDICATOR)findIdentifiable(r.getMotionId(), markables);
		
		String[] _paths = null;
		String pathId = r.getPathId();
		if(pathId != null && pathId.indexOf(COMMA) > -1)
			_paths = pathId.split(COMMA);
		
		List<PATH> paths = null;
		if(pathId != null && pathId.trim().length()>0){
			if(_paths != null){
				for (String p : _paths) {
					PATH path = (PATH)findIdentifiable(p.trim(), markables);
					if(paths == null)
						paths = new ArrayList<PATH>();
					paths.add(path);
				}
			}else{
				PATH path = (PATH)findIdentifiable(pathId, markables);
				if(paths == null)
					paths = new ArrayList<PATH>();
	
				paths.add(path);
			}
		}
			
		DIRECTION dir = (DIRECTION)findIdentifiable(r.getDirectionId(), markables);
		DISTANCE dis = (DISTANCE)findIdentifiable(r.getDistanceId(), markables);
		if(dis == null){
			dis = (DISTANCE)findIdentifiable(r.getQualitativeValue(), markables);
		}
		if(dis == null){
			dis = (DISTANCE)findIdentifiable(r.getQuantitativeValue(), markables);
		}
		
		result.put(TRAJECTOR.class, wrapToList(tr));
		result.put(LANDMARK.class, wrapToList(lm));
		result.put(SPATIALINDICATOR.class, wrapToList(sp));
		result.put(MOTIONINDICATOR.class, wrapToList(motion));
		result.put(DIRECTION.class, wrapToList(dir));
		result.put(DISTANCE.class, wrapToList(dis));
		result.put(PATH.class, (List<? extends Markable>) wrapToList(paths));
		
		
		/*result.add(tr);
		result.add(lm);
		result.add(sp);
		result.add(motion);
		result.add(dir);
		result.add(dis);
		if(paths != null)
			result.addAll(paths);
		else result.add(null);
		*/
		return result ;
	}

	/**
	 * Wraps the specified object into a list of object of its type.
	 * If the specified object is a list the same list will be returned.
	 * @param obj - object to wrap.
	 * @return a list that contains the specified object.
	 */
	public static <T> List<T> wrapToList(T obj) {
		List list = null;
		if(obj == null){
			list = new ArrayList<T>();
			list.add(null);
		}else{
			if(obj instanceof List){
				list = (List) obj;
			}else{
				list = new ArrayList<T>();
				list.add(obj);
			}
		}
		return list;
	}

	public static <T> T unwrapFromList(List<T> list) throws IllegalArgumentException{
		T obj = null;
		if(list != null && list.size() == 1){
			obj = list.get(0);
		}else if(list != null && list.size() > 1)
			throw new IllegalArgumentException();
		return obj;
	}
	
	/**
	 * Counts the number of non-null markables needed for evaluation.  
	 * 
	 * @param args - markables found as participants in a relation
	 * @param classes - array of classes the markables should be instances of. 
	 * @return number of non-null arguments which are instances of the specified classes.
	 */
	
	public static int getCntNonEmptyArgs(List<Markable> args, Class<Markable> [] classes) {
		int result = 0;
		for (Markable markable : args) {
			if(markable != null){
				for (Class<Markable> clazz : classes) {
					if(markable.getClass().equals(clazz))
						result++;
				}
			}
		}
		return result;
	}
	

	/**
	 * Counts the number of matches found with respect to the evaluation  labels.
	 * 
	 * @param matches - map of matches
	 * @param classes - array of classes (labels) used for the evaluation.
	 * @return
	 */
	public static int getCntNonEmptyArgs(Map<Class<Markable>, Boolean> matches,
			Class<Markable>[] classes) {
		int result = 0;
		Set<Entry<Class<Markable>, Boolean>> set = matches.entrySet();
		for (Entry<Class<Markable>, Boolean> entry : set) {
			Class<Markable> markable = entry.getKey();
			if(markable != null){
				for (Class<Markable> clazz : classes) {
					if(markable.equals(clazz) && entry.getValue())
						result++;
				}
			}

		}
		return result;
	}
	
	/**
	 * Retrieves all Markables from the specified list found by the class label name (extends Markabkle).
	 * @param markableList - list of markables
	 * @param clazz - class label name
	 * @return a list of markable from the list that are instances of the specified class name. 
	 */
	public static List<Markable> getMarkables(List<Markable> markableList, Class<? extends Markable> clazz){
		List<Markable> result = new ArrayList<Markable>();
		for (Markable t : markableList) {
			if(clazz.isInstance(t)){
				result.add(t);
			}
		}
		if(result.size() == 0)
			result = null;
		return result;
	}
	
	public static String printSpan(List<? extends Markable> markableList) {
		String result = null;
		StringBuilder sb = new StringBuilder();
		if(markableList != null){
			for (Markable markable : markableList) {
				if(markable != null){
					if(sb.length() > 0)
						sb.append(", ");
					sb.append(markable.getStart().intValue()).append("-").append(markable.getEnd().intValue());
				}
			}
		}
		if(sb.length() == 0)
			result = null;
		else result = sb.toString();
		
		return result;
	}


	public static <T extends Markable> T copyObj(T objToCopy) {
		T obj = null;
		try {
			obj = (T) objToCopy.getClass().newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(objToCopy.getStart() != null){
			obj.setStart(new BigInteger(objToCopy.getStart().toString()));
			obj.setEnd(new BigInteger(objToCopy.getEnd().toString()));
			String text = objToCopy.getText();
			if(text != null)
				text = new String (text);
			//obj.setText(new String(objToCopy.getText()));
			obj.setText(text);
		}
		return obj;
	}

	/**
	 * @param identifiable
	 */
	public static String getIDPrefix(WithIdentifier identifiable) {
		String prefix = "";
		if(identifiable instanceof be.liir.SpRL.jaxb.TRAJECTOR)
			prefix = SPRL_2013_ID_PREFIX.T.toString();
		else if(identifiable instanceof be.liir.SpRL.jaxb.LANDMARK)
			prefix = SPRL_2013_ID_PREFIX.L.toString();
		else if(identifiable instanceof be.liir.SpRL.jaxb.SPATIALINDICATOR)
			prefix = SPRL_2013_ID_PREFIX.S.toString();
		else if(identifiable instanceof be.liir.SpRL.jaxb.RELATION)
			prefix = SPRL_2013_ID_PREFIX.SR.toString();
		return prefix;
	}

}
