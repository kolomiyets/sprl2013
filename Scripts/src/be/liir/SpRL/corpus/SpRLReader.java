package be.liir.SpRL.corpus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import be.liir.SpRL.jaxb.CP;
import be.liir.SpRL.jaxb.DIRECTION;
import be.liir.SpRL.jaxb.DISTANCE;
import be.liir.SpRL.jaxb.MOTIONINDICATOR;
import be.liir.SpRL.jaxb.ObjectFactory;
import be.liir.SpRL.jaxb.PATH;
import be.liir.SpRL.jaxb.RELATION;
import be.liir.SpRL.jaxb.SPATIALINDICATOR;
import be.liir.SpRL.jaxb.SPEX;
import be.liir.SpRL.jaxb.SpRL;
import be.liir.SpRL.jaxb.TAGS;
import be.liir.SpRL.jaxb.TRAJECTOR;
import be.liir.SpRL.jaxb.util.JAXBUtil;
import be.liir.SpRL.jaxb.util.StringUtils;
import be.liir.SpRL.model.Markable;
import be.liir.SpRL.model.WithIdentifier;
import be.lindo.api.corpus.CorpusReader;
import be.lindo.api.util.WebConst;

public class SpRLReader {

	//private static final String WHITESPACE_CHAR_PATTERN = "\\s";
	private static final String WHITESPACE_CHAR_PATTERN = "[ \\t\\n\\x0B\\f\\r,.:;]";
	//private static final String ANY_CHAR_PATTERN = "\\S";
	private static final String ANY_CHAR_PATTERN = "[^ \\t\\n\\x0B\\f\\r,.:;()]";
	public static String SPRL_JAXB_PACKAGE = be.liir.SpRL.jaxb.SpRL.class.getPackage().getName();
	public static String URL_REGEX = ".*http://.*";
	
	public static String CP_REGEX = "[0-9]{1,3}°?+[N,S][ ][0-9]{1,3}°?+[W,E]";

	private static Map<String, Class> idPrefixClassMap = new HashMap<String, Class>();
	private static Map<Class, String> classIdPrefixMap = new HashMap<Class, String>();
	
	
	
	public static SpRL getDocument(String filename) throws SAXException, IOException, ParserConfigurationException, JAXBException{
		Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
		Node root = (Node)xmlDocument.getDocumentElement();
		JAXBContext jc = JAXBContext.newInstance(SPRL_JAXB_PACKAGE);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		SpRL document = (SpRL)unmarshaller.unmarshal(root);
		document.setFilename(filename);
		
		afterInit(document);

		return document;
	}

	/**
	 * @param document
	 */
	private static void afterInit(SpRL document) {
		// initialize all other xml-transient fields for annotations
		JAXBUtil.afterInit(document.getTAGS().getDIRECTION());
		updateMap(document.getTAGS().getDIRECTION());
		
		JAXBUtil.afterInit(document.getTAGS().getDISTANCE());
		updateMap(document.getTAGS().getDISTANCE());

		JAXBUtil.afterInit(document.getTAGS().getLANDMARK());
		updateMap(document.getTAGS().getLANDMARK());

		JAXBUtil.afterInit(document.getTAGS().getMOTIONINDICATOR());
		updateMap(document.getTAGS().getMOTIONINDICATOR());

		JAXBUtil.afterInit(document.getTAGS().getPATH());
		updateMap(document.getTAGS().getPATH());

		JAXBUtil.afterInit(document.getTAGS().getRELATION());
		updateMap(document.getTAGS().getRELATION());

		JAXBUtil.afterInit(document.getTAGS().getSPATIALINDICATOR());
		updateMap(document.getTAGS().getSPATIALINDICATOR());

		JAXBUtil.afterInit(document.getTAGS().getSPEX());
		updateMap(document.getTAGS().getSPEX());

		JAXBUtil.afterInit(document.getTAGS().getTRAJECTOR());
		updateMap(document.getTAGS().getTRAJECTOR());
	}
	
	
	private static void updateMap(List<? extends WithIdentifier> list) {
		WithIdentifier obj = JAXBUtil.getIdentifyable(list);
		if(obj != null){
			String prefix = StringUtils.getCharPrefix(obj.getId());
			if(!idPrefixClassMap.containsKey(prefix))
				idPrefixClassMap.put(prefix, obj.getClass());
			
			if(!classIdPrefixMap.containsKey(obj.getClass()))
				classIdPrefixMap.put(obj.getClass(), prefix);
		}
	}

	
	public static File[] readCorpus(String path, String extension){
		CorpusReader reader = new CorpusReader(path);
		File[] files = reader.getFiles();
		List<File> outFiles = null;
		for (File file : files) {
			int extInd = file.getName().lastIndexOf(".");
			if(extInd>0 && extInd < file.getName().length()){
				String ext = file.getName().substring(extInd+1);
				if(ext.equalsIgnoreCase(extension)){
					if(outFiles == null)
						outFiles = new ArrayList<File>();
					outFiles.add(file);
				}
			}
		}
		
		File[] arrFiles = null;
		if(outFiles != null){
			arrFiles = new File[outFiles.size()];
			arrFiles = outFiles.toArray(arrFiles);
		}
		
		return arrFiles;
	}
	
	
	public static void setConst(){
		ClassLoader loader = CorpusAnalysis.class.getClassLoader();
		if(loader==null)
	           loader = ClassLoader.getSystemClassLoader();
		String file = "lindo.properties";// add LINDO dir into the classpath
		
		URL url = loader.getResource(file);
		System.out.println(url.getFile());
		try {
			System.out.println((new File(url.toURI())).getAbsolutePath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		WebConst.setConst(url);
		
	}
	

	private static boolean checkOutputPath(String outputPath) {
		boolean result = false;
		
		File path = new File(outputPath);
		if(!path.exists()){
			path.mkdirs();
			result = true;
		}else if(path.isDirectory())
			result = true;
		
		return result;
	}

	private static String generateFilePath(String outputPath, String fileName){
		if(outputPath.charAt(outputPath.length()-1) != File.separatorChar)
			outputPath += File.separatorChar;
		String filePath = outputPath+fileName;
		return filePath;
	}
	
	private static void modifyCorpus(File[] files, String outputPath, boolean reset) {
		checkOutputPath(outputPath);
		
		for (File file : files) {
			modifyFile(file, outputPath, reset);
		}
	}

	/**
	 * Marshals the JAXB object into an XML as a String
	 * @param object - JAXB-object
	 * @param jaxbPackage TODO
	 * @return - xml-document as string
	 */
	public static String marshalToString(Object object, String jaxbPackage){
		StringWriter writer = new StringWriter();
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(jaxbPackage);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					   new Boolean(true));
			marshaller.marshal(object, writer);

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writer.toString();
	}
	
	/**
	 * Marshals the JAXB-object into an XML-file with filepath (path+filename) provided.
	 * @param object - JAXB-object
	 * @param jaxbPackage TODO
	 * @param filepath - (path+filename) for the XML document to output
	 */
	public static void marshal(Object object, String jaxbPackage, String filepath){
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(jaxbPackage);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					   new Boolean(true));
			marshaller.marshal(object, new FileOutputStream(filepath));

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static int getContentStartIndex(String text, int contentStartsLine) {
		int result = -1;
		int numNonEmptLines = 0;
		Scanner scanner = new Scanner(text);
		String line = null;
		while(scanner.hasNextLine() && numNonEmptLines < contentStartsLine){ //iterating to the first text line which is the 3rd non-empty line
			line = scanner.nextLine();
			if(line.trim().length() > 0)
				numNonEmptLines++;
			if(numNonEmptLines < contentStartsLine)
				result+=line.length()+1; //adding 1 for "/n"

		}
		return result;
	}

	/**
	 * Extracts header lines from the content, which are separated by blank lines. The number of headers to extract is specified by 
	 * <code>headersNum</code>.  
	 * @param text - text content
	 * @param headersNum - number of header lines to extract
	 * @return an array of string lines separated by blank lines at the beginning of the document. 
	 */
	private static String[] getHeaders(String text, int headersNum) {
		int numNonEmptLines = 0;
		List<String> headers = null;

		Scanner scanner = new Scanner(text);
		String line = null;
		while((line = scanner.nextLine()) != null && numNonEmptLines < headersNum){ //iterating to the first text line which is the 3rd non-empty line
			if(line.trim().length() > 0){
				numNonEmptLines++;
				if(headers == null)
					headers = new ArrayList<String>();
				headers.add(line.trim());
			}

		}
		String[] sHeaders = new String[headers.size()];
		sHeaders = headers.toArray(sHeaders);
		return sHeaders;
	}
	
	/**
	 * Finds a header by the specified regex.
	 * @param regex - regex
	 * @param headers - list of strings (all headers)
	 * @return - string from the list of strings that matches the specified header.
	 */
	private static String getHeader(String regex, String ... headers) {
		String result = null;
		for (int i = 0; (i < headers.length) && result == null; i++) {
			if(headers[i].matches(regex))
				result = headers[i];
		}
		return result;
	}

	private static void modifyFile(File file, String outputPath, boolean reset) {
		try {
			SpRL sprlDocument = getDocument(file.getAbsolutePath());
			String fileName = generateFilePath(outputPath, file.getName());
			int headerEndsAt = modifyText(sprlDocument);
			
			String content = sprlDocument.getTEXT().getContent();
			
			List<WithIdentifier> allTags = new ArrayList<WithIdentifier>();
			
			List<? extends Markable> markables = sprlDocument.getTAGS().getTRAJECTOR();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getLANDMARK();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getSPATIALINDICATOR();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getMOTIONINDICATOR();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getPATH();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getDISTANCE();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getDIRECTION();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			
			updateOffset(markables, headerEndsAt, content);
			markables = sprlDocument.getTAGS().getSPEX();
			allTags.addAll((Collection<? extends WithIdentifier>) markables);
			updateOffset(markables, headerEndsAt, content);

			modifyRelations(sprlDocument.getTAGS().getRELATION(), allTags);
			
			if(!(outputPath.charAt(outputPath.length()-1)==(File.separatorChar)))
				outputPath+=File.separatorChar;
			
			if(reset)
				reset(sprlDocument);
			
			SpRLReader.marshal(sprlDocument, SPRL_JAXB_PACKAGE, outputPath+file.getName());
			
			//System.out.println(marshalToString(sprlDocument, SPRL_JAXB_PACKAGE));
			//System.out.println();

		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void reset(SpRL sprlDocument) {
		TAGS dummy = new TAGS();
		dummy.getTRAJECTOR().add(new TRAJECTOR());
		//dummy.getTRAJECTOR().clear();
		sprlDocument.setTAGS(dummy);
	}

	private static void modifyRelations(List<RELATION> relations, List<WithIdentifier> allTags) {
		for (RELATION relation : relations) {
			try {
				modifyRelation(relation, allTags);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void modifyRelation(RELATION relation, List<WithIdentifier> allTags) throws Exception {
		
		/*
		 * spatial indicator field may contain:
		 * 		- spatial indicator id
		 * 		- motion indicator id
		 * 		- (rarely) direction id
		 * 		- (almost impossible) distance id
		 * 		- (almost inpossible) spex id (error)
		 * 		- something but not one of the above - error
		 * 
		 * so if spatial indicator field contains something which is not a spatial indicator then move it 
		 * to the corresponding attribute field 
		 */
		
		String spatialIndicator = relation.getSpatialIndicatorId();
		if(spatialIndicator != null && spatialIndicator.trim().length() > 0){
			String prefix = StringUtils.getCharPrefix(spatialIndicator);// finds the prefix of the id in the spatial indicator position
			Class obj = idPrefixClassMap.get(prefix);
			if(obj == null)
				throw new Exception(String.format("No object for the specified id prefix [%s] found.", prefix));
			
			if(obj.equals(MOTIONINDICATOR.class)){
				System.err.println(String.format("Swapping attribute values MotionId [%s] with SpatialID [%s]", relation.getSpatialIndicatorId(), spatialIndicator));
				relation.setMotionId(spatialIndicator);
				relation.setSpatialIndicatorId("");
			}else if(obj.equals(DIRECTION.class)){
				System.err.println(String.format("Swapping attribute values DirectionID [%s] with SpatialID [%s]", relation.getDirectionId(), spatialIndicator));
				relation.setDirectionId(spatialIndicator);
				relation.setSpatialIndicatorId("");
			}else if(obj.equals(DISTANCE.class)){
				System.err.println(String.format("Swapping attribute values DistanceID [%s] with SpatialID [%s]", relation.getDistanceId(), spatialIndicator));
				relation.setDistanceId(spatialIndicator);
				relation.setSpatialIndicatorId("");
			}else if(obj.equals(SPEX.class)){
				throw new Exception("SPEX Identifier is found in MotionId ");
			}else if(obj.equals(SPATIALINDICATOR.class)){
				;// good
			}else {
				throw new Exception(String.format("Unknown type id is found in SpatialID [%s]", spatialIndicator));
			}
		}

		
		String landmarkIndicator = relation.getLandmarkId();
		if(landmarkIndicator != null && landmarkIndicator.trim().length() > 0){
			String prefix = StringUtils.getCharPrefix(landmarkIndicator);// finds the prefix of the id in the landmark indicator position
			Class obj = idPrefixClassMap.get(prefix);
			if(obj == null)
				throw new Exception(String.format("No object for the specified id prefix [%s] found.", prefix));
			
			if(obj.equals(PATH.class)){
				System.err.println(String.format("Swapping attribute values LandmarkId [%s] with PathId [%s]", relation.getPathId(), landmarkIndicator));
				if(relation.getPathId() != null && relation.getPathId().trim().length() > 0)
					relation.setPathId(relation.getPathId().concat(", ").concat(landmarkIndicator));
				else
					relation.setPathId(landmarkIndicator);
				relation.setLandmarkId("");
			}else if(obj.equals(DIRECTION.class)){
				System.err.println(String.format("Swapping attribute values MotionId [%s] with DirectionID [%s]", relation.getDirectionId(), landmarkIndicator));
				relation.setDirectionId(landmarkIndicator);
				relation.setLandmarkId("");
			}else if(obj.equals(DISTANCE.class)){
				System.err.println(String.format("Swapping attribute values MotionId [%s] with DistanceID [%s]", relation.getDistanceId(), landmarkIndicator));
				relation.setDistanceId(landmarkIndicator);
				relation.setLandmarkId("");
			}else if(obj.equals(SPEX.class)){
				throw new Exception("SPEX Identifier is found in MotionId ");
			}
		}
		
		String rcc8 = relation.getRCC8Value();
		if(rcc8 != null && rcc8.trim().equalsIgnoreCase("TPP"))
			relation.setRCC8Value("NTPP");
		if(rcc8 != null && rcc8.trim().equalsIgnoreCase("TPP-1"))
			relation.setRCC8Value("NTPP-1");
	}

	/**
	 * Update markable offsets of by the specified offset as <code>headerEndsAt</code>. 
	 * 
	 * @param markables - list of markables objects
	 * @param headerEndsAt - offset shift
	 * @param context - text string.
	 */
	private static void updateOffset(List<? extends Markable> markables, int headerEndsAt, String context) {
		for (Markable markable : markables) {
			if(markable.getStart().intValue() > 0){
				if(markable.getStart().intValue()-headerEndsAt > 0 && markable.getEnd().intValue()-headerEndsAt < context.length()){
					markable.setStart(BigInteger.valueOf(markable.getStart().intValue()-headerEndsAt));
					markable.setEnd(BigInteger.valueOf(markable.getEnd().intValue()-headerEndsAt));
					checkOffset(markable, context, WHITESPACE_CHAR_PATTERN, ANY_CHAR_PATTERN);
				}
				else throw new ArrayIndexOutOfBoundsException();
			}
			
		}
	}

	/**
	 * Checks if markable can be trimmed or expanded so that the markable does not start or end with a whitespace, 
	 * neither starts or ends at the middle of a token.
	 *  
	 * @param markable - JAXB-representation of markable
	 * @param context - text string
	 * @param trimCharPattern - char pattern for trimable characters
	 * @param expandCharPattern - char pattern for eligible token-begin and -end characters
	 */
	private static void checkOffset(Markable markable, String context,
			String trimCharPattern, String expandCharPattern) {
		
		String firstChar = context.substring(markable.getStart().intValue(), markable.getStart().intValue()+1);
		String lastChar = context.substring(markable.getEnd().intValue()-1, markable.getEnd().intValue());
		if(markable.getEnd().intValue()- markable.getStart().intValue() > 1 &&
				firstChar.matches(trimCharPattern)){ // if the first markable char matches the trim pattern, then trim it from the beginning
				System.err.println(String.format("Offset problem: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
				trimOffset(markable, context, 0, trimCharPattern);
				System.err.println(String.format("New offset: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
				System.err.println();
		}
		if (markable.getEnd().intValue()- markable.getStart().intValue() > 1 &&
				lastChar.matches(trimCharPattern)){ //if the last markable char matches the trim pattern, then trim it from the end
			System.err.println(String.format("Offset problem: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
			trimOffset(markable, context, (markable.getEnd().intValue() - markable.getStart().intValue()), trimCharPattern);
			System.err.println(String.format("New offset: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
			System.err.println();
		}
		
		//fake wrong offset
		/*
		if(markable.getEnd().intValue() - markable.getStart().intValue()>2){
			markable.setStart(BigInteger.valueOf(markable.getStart().intValue()+1));
			markable.setEnd(BigInteger.valueOf(markable.getEnd().intValue()-1));
		}
		*/
		firstChar = context.substring(markable.getStart().intValue()-1, markable.getStart().intValue());//-1 first 
		lastChar = context.substring(markable.getEnd().intValue(), markable.getEnd().intValue()+1);
		if(markable.getEnd().intValue()- markable.getStart().intValue() > 1 &&
				firstChar.matches(expandCharPattern)){ // if the first markable char matches the trim pattern, then expand it from the beginning
			System.err.println(String.format("Offset problem: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
			expandOffset(markable, context, expandCharPattern);
			// trim needed
			trimOffset(markable, context, 0, trimCharPattern);
			trimOffset(markable, context, (markable.getEnd().intValue() - markable.getStart().intValue()), trimCharPattern);
			System.err.println(String.format("New offset: %s:\t[%s]",markable.getClass().getName(), context.substring(markable.getStart().intValue(), markable.getEnd().intValue())));
			System.err.println();
		}
		
		// update markable text 
		if(markable.getStart().intValue()>-1)
			markable.setText(context.substring(markable.getStart().intValue(), markable.getEnd().intValue()));
		
	}

	/**
	 * Checks and expands the markable offset if the annotation is misaligned with respect to eligible token-begin and -end chars.  
	 * @param markable - JAXB-reprepresentation of markable
	 * @param context - text string
	 * @param expandPattern - char pattern for eligible token-begin and -end chars.
	 */
	private static void expandOffset(Markable markable, String context, String expandPattern) {
		int startInd = markable.getStart().intValue();
		int maxInd = markable.getEnd().intValue();
		
		int altStartIndex = getAlternativeIndex(context, expandPattern, startInd, maxInd);
		
		startInd = markable.getEnd().intValue();
		int altEndIndex = getAlternativeIndex(context, expandPattern, startInd, maxInd);
		
		markable.setStart(BigInteger.valueOf(altStartIndex));
		markable.setEnd(BigInteger.valueOf(altEndIndex));
		
	}

	/**
	 * Searches in the string for the closest char index to <code>startInd</code> 
	 * which matches the specified pattern (<code>expandPattern</code>). It usually corresponds to 
	 * either the first character of the token (left index) or the first character that follows the token.
	 * @param context - string
	 * @param expandPattern - string pattern for alpha-numerical chars that has to be matched
	 * @param startInd - original char index
	 * @param maxInd - maximum char index to search
	 * @return - new char index
	 */
	private static int getAlternativeIndex(String context,
			String expandPattern, int startInd, int maxInd) {
		int i = startInd;
		while (context.substring(i-1, i).matches(expandPattern) && i > 0) {
			i--;
		}
		int altLeftStart = i; // alternative start when propagated to the left
		i = startInd;
		while (context.substring(i, i+1).matches(expandPattern) && i < maxInd) {
			i++;
		}
		int altRightStart = i; // alternative start when propagated to the Right
		
		int diffLeft = Math.abs(startInd - altLeftStart);
		int diffRight = Math.abs(startInd - altRightStart);
		
		boolean takeRight = Math.min(diffRight, diffLeft) == diffRight;
		
		if(takeRight)
			startInd = altRightStart+1;
		else 
			startInd = altLeftStart;
		
		return startInd;
	}

	/**
	 * Trims the markable offset from <code>i</code>-th position to the left or to the right 
	 * as long as the current char matches the specified <code>trimCharPattern</code> pattern.  
	 * @param markable - Markable to trim (<code>? extends Markable</code>).
	 * @param context - textual context (all text).
	 * @param i - position of trimable character in the markable offset 
	 * <br/> (e.g. if i == 0 it corresponds to the begin markable position and matches <code>trimCharPattern</code>, 
	 * <br/> if i == (Markable.size -1) it corresponds to the end markable position and matches <code>trimCharPattern</code>)
	 * @param trimCharPattern - string pattern of trimmable characters.
	 */
	private static void trimOffset(Markable markable, String context, int i,
			String trimCharPattern) {
		String content = context.substring(markable.getStart().intValue(), markable.getEnd().intValue());
		int diffOffset = getTrimNum(content, i, trimCharPattern); // offset difference = number of chars to be trimmed
		if(i == 0) // if i == 0 then trimming from the beginning
			markable.setStart(BigInteger.valueOf(markable.getStart().intValue()+diffOffset));
		else // 
			markable.setEnd(BigInteger.valueOf(markable.getEnd().intValue()-diffOffset));
	}

	/**
	 * Calculates the number of characters to trim either from the beginning (<code>i==0</code>) or the end (<code>i==(Markable.size -1)</code>).
	 * @param content - text string
	 * @param i - position to start trimming from
	 * @param trimCharPattern - string pattern of trimmable characters
	 * @return  number of characters to be trimed
	 */
	private static int getTrimNum(String content, int i, String trimCharPattern) {
		int trimNum=0;
		if(i == 0){//trim from the beginning
			for (int j = 0; j < content.length() && content.substring(j, j+1).matches(trimCharPattern); j++) {
				trimNum++;
			}
			
		}else{//trim from the end
			for (int j = i; j > 0 && content.substring(j-1, j).matches(trimCharPattern); j--) {
				trimNum++;
			}
		}
		return trimNum;
	}

	/**
	 * Modifies the textual content (marked by <code>TEXT</code>-XML tag), extracting the source URL and CP coordinates 
	 * as separate XML tags. The method returns a new text start position as integer.   
	 * 
	 * @param sprlDocument - JAXB-representation of an SPRL-document.
	 * @return new text begin index.
	 */
	private static int modifyText(SpRL sprlDocument) {
		int headerEndsAt = 0;
		if(sprlDocument.getTEXT() != null){
			String content = sprlDocument.getTEXT().getContent();
			String[] headers = getHeaders(content, 2);
			String url = getHeader(URL_REGEX, headers);
			String cp = getHeader(CP_REGEX, headers);

			if(url == null && cp == null)
				headerEndsAt = 0;
			else if(url == null || cp == null)
				headerEndsAt = getContentStartIndex(content, 2); //if one of the headers is missing, the content starts at non-empty line 2 
			else
				headerEndsAt = getContentStartIndex(content, 3); //the content starts at non-empty line 3

			if(url != null){
				be.liir.SpRL.jaxb.URL jaxbUrl =  new be.liir.SpRL.jaxb.URL();
				jaxbUrl.setContent(url);
				sprlDocument.setURL(jaxbUrl);
			}
			if(cp != null){
				CP jaxbCP = new CP();
				jaxbCP.setContent(cp);
				sprlDocument.setCP(jaxbCP);
			}
				
			if(headerEndsAt > 0){
				String mContent = content.substring(headerEndsAt);
				sprlDocument.getTEXT().setContent(mContent);
			}
		}
		return headerEndsAt;
	}

	
	public static void main(String[] args) {
		//String corpusPath = "data/Confluence.Checked/annotations.charis/autoMod";
		String corpusPath = "data/SpRL2013Gold/autoMod";
		String outputPath = "data/SpRL2013Gold/final";
		boolean reset = false;
		
		setConst();
		
		
		File[] files = readCorpus(corpusPath, "xml");
		modifyCorpus(files, outputPath, reset);
		
		//modifyFile(new File("E:/Data/Oleksandr/workspace/SpatialML/data/Confluence.Checked/annotations.charis/autoMod/test.xml"), outputPath);
	}

}
