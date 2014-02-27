package be.liir.SpRL.jaxb.util;

public class StringUtils {

	public static String ALPHA_CHARS_REGEX = "[a-zA-Z]";
	
	/**
	 * @param string
	 * @return
	 */
	public static String getCharPrefix(String string) {
		String result = null;
		int i = 0;
		while ((i< string.length()-1) && 
				(string.substring(i, i+1).matches(StringUtils.ALPHA_CHARS_REGEX))){
			i++;
		}
		if(i != 0 && i < string.length()){
			result = string.substring(0, i);
		}
		return result;
	}

	public static int getIntId(String strId){
		int result = -1;
		String prefix = getCharPrefix(strId);
		if(prefix != null){
			if(prefix.length() != strId.length()){
				String sInt = strId.substring(prefix.length());
				result = Integer.parseInt(sInt);
			}else{
				
			}
		}
		return result;
	}

}
