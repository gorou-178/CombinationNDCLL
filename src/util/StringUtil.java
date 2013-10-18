package util;

public class StringUtil {
	public final static String SEP = System.getProperty("line.separator");
	
	public static boolean isEmpty(String value) {
		return (value == null ? true : value.length() == 0 ? true : false);
	}
	
	public static boolean isNotEmpty(String value) {
		return !StringUtil.isEmpty(value);
	}
	
	public static boolean isBlank(String value) {
		value = StringUtil.trim(value);
		return StringUtil.isEmpty(value);
	}
	
	public static boolean isNotBlank(String value) {
		return !StringUtil.isBlank(value);
	}
	
	public static String trim(String value) {
		if (StringUtil.isEmpty(value)) {
			return value;
		}
		value = value.trim();
		return value.replaceAll("Å@", "");
	}
	
}
