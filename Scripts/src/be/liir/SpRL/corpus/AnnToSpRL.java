package be.liir.SpRL.corpus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.lindo.api.corpus.CorpusReader;
import be.lindo.api.util.WebConst;

public class AnnToSpRL {
	//public static String URL_REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	public static String URL_REGEX = ".*http://.*";
		
	public static String CP_REGEX = "[0-9]{1,3}°?+[N,S][ ][0-9]{1,3}°?+[W,E]";
	
	private static Map<String, String> MAE_DTD_MAPPING;

	private static void setMapping(){
			MAE_DTD_MAPPING = new HashMap<String, String>();
			MAE_DTD_MAPPING.put("<ISO-Space>", "<SpRL>");
			MAE_DTD_MAPPING.put("</ISO-Space>", "</SpRL>");
			
			MAE_DTD_MAPPING.put("<SR ", "<RELATION ");
			
			MAE_DTD_MAPPING.put(" fromID=", " trajector_id=");
			MAE_DTD_MAPPING.put(" fromText=", " trajector_text=");
			
			MAE_DTD_MAPPING.put(" toID=", " landmark_id=");
			MAE_DTD_MAPPING.put(" toText=", " landmark_text=");

			MAE_DTD_MAPPING.put(" scopeID=", " spatial_indicator_id=");
			MAE_DTD_MAPPING.put(" scopeText=", " spatial_indicator_text=");
	}
	
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
		setMapping();
		String inputPath = "data/SpRL2013Gold";
		String extension = "xml";
		String outputPath = "data/SpRL2013Gold/autoMod";
		if(checkOutput(outputPath))
			transformCorpus(inputPath, extension, outputPath);
		
	}

	private static boolean checkOutput(String outputPath) {
		boolean result = false;
		
		File path = new File(outputPath);
		if(!path.exists()){
			path.mkdirs();
			result = true;
		}else if(path.isDirectory())
			result = true;
		
		return result;
	}

	private static void transformCorpus(String inputPath, String extension, String outputPath) {
		CorpusReader reader = new CorpusReader(inputPath);
		File[] files = reader.getFiles();
		
		for (File file : files) {
			if(!file.getName().endsWith(extension))
				continue;
			
			try {
				//analyseFile(file);
				String content = modifyStringFile(file);
				outputInFile(content, file.getName(), outputPath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Outputs the string into a file with the specified file name into an output path. 
	 * @param content - content as a string.
	 * @param fileName - file name for the file to store.
	 * @param outputPath - output path for files.
	 * @return a file.
	 */
	private static File outputInFile(String content, String fileName, String outputPath) {
		File result = null;
		if(outputPath.charAt(outputPath.length()-1) != File.separatorChar)
			outputPath += File.separatorChar;
		
		String filePath = outputPath+fileName;
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8");
			out.write(content);
			out.flush();
			out.close();
			result = new File(filePath);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	private static void analyseFile(File file) throws Exception {
		org.w3c.dom.Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		NodeList text = xmlDocument.getElementsByTagName("TEXT");
		processText(text);
		
		/*
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
		 */
	}

	/**
	 * Reads the original file specified by <code>file</code>, replaces xml tags and attributes and outputs the content as a string.
	 * 
	 * @param file - input file
	 * @return content as a string
	 * 
	 * @throws FileNotFoundException
	 */
	private static String modifyStringFile(File file) throws FileNotFoundException{
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(file, "UTF-8");
		while (scanner.hasNextLine()) {
			//String string = (String) scanner.nextLine();
			String string = scanner.nextLine();
				//string = new String(line.getBytes(), "UTF-8");
			sb.append(modify(string)).append("\n");
			
		}
		return sb.toString();
	}
	
	/**
	 * Replaces default xml tags and xml attributes with the ones designed for SpRL.
	 * 
	 * @param string - input string
	 * @return modified string.
	 */
	private static String modify(String string) {
		String nString = new String(string);
		Set<String> sourceTexts = MAE_DTD_MAPPING.keySet();
		boolean anyChange = false;
		for (String sourceSting : sourceTexts) {
			if(nString.contains(sourceSting)){
				String replacement = MAE_DTD_MAPPING.get(sourceSting);
				System.out.println(String.format("Pattern found: %s . Replaced by %s. ", sourceSting, replacement));
				//nString = nString.replaceAll(sourceSting, replacement);
				System.out.println(String.format("Old line: \t%s\nNew line:\t%s", nString, nString = nString.replaceAll(sourceSting, replacement)));
				anyChange = true;
			}
		}
		return nString;
	}


	private static void processText(NodeList text) throws Exception {
		String t = getText(text);
		int headerEndsAt = getContentStartIndex(t);
		String[] headers = getHeaders(t, 2);
		String url = getHeader(URL_REGEX, headers);
		String cp = getHeader(CP_REGEX, headers);
		if(url == null && cp == null)
			headerEndsAt = 0;
		
	}

	private static String getText(NodeList text) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < text.getLength(); i++) {
			Node aText = text.item(i);
			sb.append(aText.getTextContent());
		}
		return sb.toString();
	}

	
	
	private static int getContentStartIndex(String text) {
		int result = -1;
		int numNonEmptLines = 0;
		Scanner scanner = new Scanner(text);
		String line = null;
		while(scanner.hasNextLine() && numNonEmptLines < 3){ //iterating to the first text line which is the 3rd non-empty line
			line = scanner.nextLine();
			if(line.trim().length() > 0)
				numNonEmptLines++;
			if(numNonEmptLines < 3)
				result+=line.length()+1; //adding 1 for "/n"

		}
		return result;
	}

	/**
	 * Extracts header lines from the content, which are separated by blank lines. The number of headers to extract is specified by 
	 * <code>headersNum</code>.  
	 * @param text - text content
	 * @param headersNum - number of header lines to extract
	 * @return an array of string lines separated by blank lines at the begining fo the document. 
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
	
	private static String getHeader(String regex, String ... headers) {
		String result = null;
		for (int i = 0; (i < headers.length) && result == null; i++) {
			if(headers[i].matches(regex))
				result = headers[i];
		}
		return result;
	}
}
