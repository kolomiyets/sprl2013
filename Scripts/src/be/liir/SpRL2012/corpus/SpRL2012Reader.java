package be.liir.SpRL2012.corpus;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import be.liir.SpRL.corpus.SpRLReader;
import be.liir.SpRL.jaxb.SpRL;
import be.liir.SpRL.jaxb.TAGS;
import be.liir.SpRL.jaxb.TEXT;
import be.liir.SpRL.jaxb.util.FileUtils;
import be.liir.SpRL.jaxb.util.JAXBUtil;
import be.liir.SpRL.jaxb.util.StringUtils;
import be.liir.SpRL.model.Containable;
import be.liir.SpRL.model.Markable;
import be.liir.SpRL.model.WithContent;
import be.liir.SpRL.model.WithIdentifier;
import be.liir.SpRL2012.jaxb.DOC;
import be.liir.SpRL2012.jaxb.LANDMARK;
import be.liir.SpRL2012.jaxb.RELATION;
import be.liir.SpRL2012.jaxb.SENTENCE;
import be.liir.SpRL2012.jaxb.SPATIALINDICATOR;
import be.liir.SpRL2012.jaxb.TRAJECTOR;

public class SpRL2012Reader {
	
	public static final String SPRL_JAXB_PACKAGE = be.liir.SpRL2012.jaxb.DOC.class.getPackage().getName();
	private static final String NEW_LINE = "\n\n";
	private static final int TOKEN_DELTA = 2;
	private static final int SENTENCE_NUM = Integer.MAX_VALUE;//599;//Integer.MAX_VALUE; // number of sentences to process. Integer.MAX_VALUE means to process all sentences 
	private static final int SENTENCE_START_NUM = 600;//0;//600;
	
	private static enum SPRL_2012_ID_PREFIX{s, tw, lw, sw, r};
	private static Map<String, String> OLD_NEWID_MAP = new HashMap<String, String>();
	private static Collection<be.liir.SpRL.jaxb.RELATION> relationList = new ArrayList<be.liir.SpRL.jaxb.RELATION>();
	
	public static void main(String[] args) {
		String corpusPath = "data/SpRL2012/Phrases";
		String outputPath = "data/SpRL2012/Phrases/mod";
		SpRLReader.setConst();
		File[] files = SpRLReader.readCorpus(corpusPath, "xml");
		modifyFiles(files, outputPath);
	}

	private static Object[] modifyFiles(File[] files, String outputpath) {
		List<Object> resultList = new ArrayList<Object>();
		for (File file : files) {
			try {
				modifyFile(file, outputpath);
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
		Object[] result = new Object[resultList.size()];
		return resultList.toArray(result);
	}

	private static void modifyFile(File file, String outputpath) throws SAXException, IOException, ParserConfigurationException, JAXBException {
		DOC document = getDocument(file);
		SpRL newDoc = new SpRL();
		createOffsets(document, newDoc, SENTENCE_START_NUM, SENTENCE_NUM);
		if(!(outputpath.charAt(outputpath.length()-1)==(File.separatorChar)))
			outputpath+=File.separatorChar;
		FileUtils.checkOutputPath(outputpath);
		SpRLReader.marshal(newDoc, SpRLReader.SPRL_JAXB_PACKAGE, outputpath+file.getName());
		//System.out.println(SpRLReader.marshalToString(newDoc, SpRLReader.SPRL_JAXB_PACKAGE));
	}

	/**
	 * Translates an SpRL2012 file into a SpRL2013 format file
	 * @param document
	 * @param sprl
	 * @param sentMaxNum TODO
	 * @param startSentNum TODO
	 */
	private static void createOffsets(DOC document, SpRL sprl, int startSentNum, int sentMaxNum) {
		List<SENTENCE> sentences = document.getSENTENCE();
		int sentTotal = 0;
		
		int lastIndex = 0;//last char index in the final text content output
		for (int i = startSentNum; i < sentences.size() && sentTotal<=sentMaxNum; i++) {
			SENTENCE sentence = sentences.get(i);
			lastIndex = createOffsets(sentence, lastIndex, sprl);
			sentTotal++;

		}
		/*
		for (SENTENCE sentence : sentences) 
			lastIndex = createOffsets(sentence, lastIndex, sprl);
		*/
		
		
		String context = sprl.getTEXT().getContent();
		List<? extends WithIdentifier> content = JAXBUtil.getAllAnnotations(sprl);
		for (WithIdentifier instance : content) {
			if(! (instance instanceof be.liir.SpRL.jaxb.RELATION)){
				Markable m = (Markable) instance;
				if(m.getStart().intValue()>-1)
					m.setText(context.substring(m.getStart().intValue(), m.getEnd().intValue()));
			}
		}
		
		/*
		List<be.liir.SpRL.jaxb.TRAJECTOR> trs = sprl.getTAGS().getTRAJECTOR();
		
		for (be.liir.SpRL.jaxb.TRAJECTOR trajector : trs) {
			if(trajector.getStart().intValue()>-1){
				System.out.println(String.format("%s: [%s] \t[%s]", trajector.getClass(), context.substring(trajector.getStart().intValue(), trajector.getEnd().intValue()), trajector.getText()));	
			}
			
		}
		*/
		
	}

	private static int createOffsets(SENTENCE sentence, int lastIndex, SpRL sprl) {
		int result = lastIndex;
		String sentenceContext = sentence.getCONTENT().getContent();
		

		
		List<Containable> list = sentence.getTRAJECTOR();
		process(list, sentenceContext, lastIndex, sprl);
		
		list = sentence.getLANDMARK();
		process(list, sentenceContext, lastIndex, sprl);
		
		list = sentence.getSPATIALINDICATOR();
		process(list, sentenceContext, lastIndex, sprl);

		List<RELATION> relList = sentence.getRELATION();
		process(relList, sprl);
		
		OLD_NEWID_MAP = new HashMap<String, String>();// reset the map of IDs for new sentence

		
		TEXT text = sprl.getTEXT();
		int initLen = 0;
		String allContent = "";
		if(text != null){
			allContent = text.getContent();
		}else{
			text = new TEXT();
			sprl.setTEXT(text);
		}
		
		StringBuilder sb = new StringBuilder(allContent);
		initLen = sb.length();
		sb.append(sentenceContext).append(NEW_LINE);
		lastIndex+=(sb.length()-initLen);
		text.setContent(sb.toString());
		
		return lastIndex;
	}

	private static void process(List<RELATION> relList, SpRL sprl) {
		for (RELATION relation : relList) {
			try {
				process(relation, sprl);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void process(RELATION relation, SpRL sprl) throws Exception {
		List<? extends WithIdentifier> content = JAXBUtil.getAllAnnotations(sprl);
		String trId = OLD_NEWID_MAP.get(relation.getTr());
		String lmId = OLD_NEWID_MAP.get(relation.getLm());
		String siId = OLD_NEWID_MAP.get(relation.getSp());
		
		WithIdentifier trajector = JAXBUtil.findIdentifiable(trId, content);
		WithIdentifier landmark = JAXBUtil.findIdentifiable(lmId, content);
		WithIdentifier spIndicator = JAXBUtil.findIdentifiable(siId, content);
		

		if(trajector == null || landmark == null || spIndicator == null){
			throw new Exception(String.format("One of the relation arguments is not found. " +
					"Trajector:[%s] \t Landmark:[%s] \t Spatial Indicator: [%s]", 
					relation.getTr(), relation.getLm(), relation.getSp()));
		}
		
		
		be.liir.SpRL.jaxb.RELATION newRelation = new be.liir.SpRL.jaxb.RELATION();
		newRelation.setTrajectorId(trajector.getId());
		newRelation.setLandmarkId(landmark.getId());
		newRelation.setSpatialIndicatorId(spIndicator.getId());
		newRelation.setGeneralType(relation.getGeneralType());
		
		newRelation.setId(getNewId(newRelation));//set ID
		sprl.getTAGS().getRELATION().add(newRelation);
	}

	private static String getNewId(WithIdentifier identifiable) {
		String prefix = JAXBUtil.getIDPrefix(identifiable);
		int id = JAXBUtil.getNewId(prefix);
		String newId = prefix+id;
		return newId;
	}

	/**
	 * Processes a list of markables
	 * @param markables
	 * @param sentenceContext
	 * @param lastIndex
	 * @param sprl
	 * @return
	 */
	private static void process(List<Containable> markables,
			String sentenceContext, int lastIndex, SpRL sprl) {
		
		for (Containable markable : markables) {
			try {
				process(markable, sentenceContext, lastIndex, sprl);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/**
	 * Process one markable and returns the last char index
	 * @param markable
	 * @param sentenceContext
	 * @param lastIndex
	 * @param sprl
	 * @return
	 * @throws Exception 
	 */
	private static void process(Containable markable, String sentenceContext,
			int lastIndex, SpRL sprl) throws Exception {
		int result = lastIndex;
		
		String mContent = markable.getContent().replace("\n", "");
		int start = -1;
		int end = -1;
		
		if(mContent.length()>0){
			//start = sentenceContext.indexOf(mContent);
			start = sentenceContext.toLowerCase().indexOf(mContent.toLowerCase());
			if(sentenceContext.lastIndexOf(mContent) != start){
				int tokenNum = StringUtils.getIntId(markable.getId());
				String[] tokens = sentenceContext.split(" ");
				start = findStart(mContent, tokens, tokenNum, sentenceContext);
				if(start ==-1)
					throw new Exception(String.format("Multiple alignments are possible for sentence [%s] \t markable [%s]", 
						sentenceContext, mContent));
			}
		}
		
		Markable obj = null;
		if(markable instanceof TRAJECTOR){
			obj = new be.liir.SpRL.jaxb.TRAJECTOR();
		}else if(markable instanceof LANDMARK){
			obj = new be.liir.SpRL.jaxb.LANDMARK();
		}else if(markable instanceof SPATIALINDICATOR){
			obj = new be.liir.SpRL.jaxb.SPATIALINDICATOR();
		}else throw new Exception(String.format("No correcpondance found for [%s]", markable));

		if(start > -1){
			end = start+mContent.length();
			
			obj.setStart(BigInteger.valueOf(start + lastIndex));
			obj.setEnd(BigInteger.valueOf(end + lastIndex));
			
		}else{//non-consuming annotation
			obj.setStart(BigInteger.valueOf(-1));
			obj.setEnd(BigInteger.valueOf(-1));
		}
			
		// set ID with the old one, as it is referenced in the relation, later it will be replaced
		WithIdentifier idObj = (WithIdentifier) obj;
		String newId = getNewId(idObj);
		idObj.setId(newId);

		OLD_NEWID_MAP.put(markable.getId(), newId);//prepare new IDs and remember mappings, later old IDs will be replaced


		if(sprl.getTAGS() == null)
			sprl.setTAGS(new TAGS());

		if(obj instanceof be.liir.SpRL.jaxb.TRAJECTOR)
			sprl.getTAGS().getTRAJECTOR().add((be.liir.SpRL.jaxb.TRAJECTOR) obj);
		else if(obj instanceof be.liir.SpRL.jaxb.LANDMARK)
			sprl.getTAGS().getLANDMARK().add((be.liir.SpRL.jaxb.LANDMARK) obj);
		else if(obj instanceof be.liir.SpRL.jaxb.SPATIALINDICATOR)
			sprl.getTAGS().getSPATIALINDICATOR().add((be.liir.SpRL.jaxb.SPATIALINDICATOR) obj);

		
	}

	/**
	 * If multiple aligments are possible, it finds the token referenced by interger in ID 
	 * @param mContent
	 * @param tokens
	 * @param tokenNum
	 * @param sentenceContext
	 * @return
	 */
	private static int findStart(String mContent, String[] tokens, int tokenNum, String sentenceContext) {
		int result = -1;
		String[] searchStrs = mContent.trim().split(" ");
		String searchStr = mContent.trim();
		if(searchStrs.length > 1){
			searchStr = searchStrs[0];
		}
		 
		if(!(tokenNum >=tokens.length)){
			int found = -1;
			for (int i = (tokenNum-TOKEN_DELTA)>-1?tokenNum-TOKEN_DELTA:0; (i < tokens.length) && (i < tokenNum+TOKEN_DELTA) && found == -1; i++) {
				if(tokens[i].equalsIgnoreCase(searchStr))
					found = i;
			}
			
			if(found!=-1){
				if(found == tokens.length-1){
					result = sentenceContext.lastIndexOf(tokens[found]);
				}else{
					StringBuilder sb = new StringBuilder();
					for (int i = found; i < tokens.length && i<found+TOKEN_DELTA+2; i++) {
						sb.append(tokens[i]).append(" ");	
					}
					
					result = sentenceContext.indexOf(sb.toString().trim());
				}
			}
		}
		return result;
	}

	private static DOC getDocument(File file) throws SAXException, IOException, ParserConfigurationException, JAXBException {
		Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		Node root = (Node)xmlDocument.getDocumentElement();
		JAXBContext jc = JAXBContext.newInstance(SPRL_JAXB_PACKAGE);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (DOC)unmarshaller.unmarshal(root);
	}
	
}
