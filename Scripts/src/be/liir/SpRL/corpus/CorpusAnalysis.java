package be.liir.SpRL.corpus;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.lindo.api.corpus.CorpusReader;
import be.lindo.api.nlp.adapters.OpenNLPFactory;

import be.lindo.api.util.WebConst;
import be.lindo.messages.Statement;

public class CorpusAnalysis {

	static Map<String, Object> analysis = new HashMap<String, Object>();
	
	static String sentences = "Sentences";
	static String tokenNums = "Tokens";
	static String landmarks = "Landmarks";
	static String trajectos = "Trajectors";
	static String sp_indicators = "Spatial Indicators";
	static String motion_indicators = "Motion Indicators";
	static String paths = "Paths";
	static String distances = "Dictances";
	static String directions = "Directions";
	static String sr = "SR";
	
	
	private static void setConst(){
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

	public static void main(String[] args) {
		setConst();
		//String inputPath = "data/Confluence.Checked/annotations.botond/Evaluation/ready";
		String inputPath = "data/SpRL2013Gold";
		String extension = "xml";
		test(inputPath, extension);
		
	}

	private static void test(String inputPath, String extension) {
		CorpusReader reader = new CorpusReader(inputPath);
		File[] files = reader.getFiles();
		
		for (File file : files) {
			if(!file.getName().endsWith(extension) || file.isDirectory())
				continue;
			
			try {
				analyseFile(file);
				updateAnalysis("Files", 1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		printAnalysis();
	}

	private static void printAnalysis() {
		Set<String> keys = analysis.keySet();
		StringBuilder sb = new StringBuilder();
		
		for (String string : keys) {
			sb.append(string).append(":\t").append(analysis.get(string)).append("\n");
		}
		System.out.println(sb);
	}

	private static void analyseFile(File file) throws Exception {
		org.w3c.dom.Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		NodeList text = xmlDocument.getElementsByTagName("TEXT");
		processText(text);
		NodeList trajectors = xmlDocument.getElementsByTagName("TRAJECTOR");
		updateAnalysis(trajectos, trajectors.getLength());
		
		NodeList landmarks = xmlDocument.getElementsByTagName("LANDMARK");
		updateAnalysis(CorpusAnalysis.landmarks, landmarks.getLength());
		
		NodeList sp_indicators = xmlDocument.getElementsByTagName("SPATIAL_INDICATOR");
		updateAnalysis(CorpusAnalysis.sp_indicators, sp_indicators.getLength());
		
		NodeList motion_indicators  = xmlDocument.getElementsByTagName("MOTION_INDICATOR");
		updateAnalysis(CorpusAnalysis.motion_indicators, motion_indicators.getLength());
		
		NodeList sr  = xmlDocument.getElementsByTagName("SR");
		updateAnalysis(CorpusAnalysis.sr, sr.getLength());

		NodeList paths  = xmlDocument.getElementsByTagName("PATH");
		updateAnalysis(CorpusAnalysis.paths, paths.getLength());
		
		NodeList directions  = xmlDocument.getElementsByTagName("DIRECTION");
		updateAnalysis(CorpusAnalysis.directions, directions.getLength());

		NodeList distances  = xmlDocument.getElementsByTagName("DISTANCE");
		updateAnalysis(CorpusAnalysis.distances, distances.getLength());

	}

	private static void processText(NodeList text) throws Exception {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < text.getLength(); i++) {
			Node aText = text.item(i);
			sb.append(aText.getTextContent());
			//logger.info("Child : "+ sentence.getTextContent());
		}
		
		List<String> sentences = OpenNLPFactory.getInstance().getSentences(sb.toString());
		updateAnalysis(CorpusAnalysis.sentences, sentences.size());
		int tokenNum = 0;
		for (String string : sentences) {
			Statement s = new Statement(string);
			s.processIt(true, false, false, false, false);
			tokenNum+=s.getTokens().size();
		}
		updateAnalysis(CorpusAnalysis.tokenNums, tokenNum);
	}

	private static void updateAnalysis(String key, Object updateValue) {
		if(analysis.containsKey(key)){
			analysis.put(key, ((Integer)analysis.get(key) +(Integer)updateValue));
		}else
			analysis.put(key, updateValue);
	}
	
}
