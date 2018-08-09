package com.higgsup.kpi.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilsValidate {

	public static Boolean containRegex(String str) {
		Pattern pattern = Pattern.compile("^((?!.*\\..*\\.)(?!.*_.*_)[A-Za-z0-9_.])*$");
		Matcher matcher = pattern.matcher(str);
		if (matcher.matches()) {
			return false;
		}		
		return true;
	}
	
	public static Boolean isValidPhoneNumber(String str) {
		Pattern pattern = Pattern.compile("^[0-9]{0, 11}$");
		Matcher matcher = pattern.matcher(str);
		if (!matcher.matches()) {
			return false;
		}		
		return true;
	}
	
	public static Boolean isValidEmail(String str) {
		Pattern pattern = Pattern.compile("^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$");
		Matcher matcher = pattern.matcher(str);
		if (!matcher.matches()) {
			return false;
		}		
		return true;
	}
}
