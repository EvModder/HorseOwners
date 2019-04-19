package net.evmodder.EvLib;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.ChatColor;

public class Text{
	public static final char colorSymbol = ChatColor.WHITE.toString().charAt(0);
	static Character[] SET_VALUES = new Character[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
										  'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'};
	public static final Set<Character> colorChars = new HashSet<Character>(Arrays.asList(SET_VALUES));
	public static String translateAlternateColorCodes(char altColorChar, String textToTranslate){
		char[] msg = textToTranslate.toCharArray();
		for(int i=1; i<msg.length; ++i){
			if(msg[i-1] == altColorChar && colorChars.contains(msg[i]) && !isEscaped(msg, i-1)){
				msg[i-1] = colorSymbol;
			}
		}
		return new String(msg);
	}

	public static boolean isEscaped(char[] str, int x){
		boolean escaped = false;
		while(x != 0 && str[--x] == '\\') escaped = !escaped;
		return escaped;
	}

/*	public static String stripColor(String str){
		char[] chars = str.toCharArray();
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < chars.length; ++i){
			if(chars[i] == colorSymbol && i+1 != chars.length && colorChars.contains(chars[i+1])) ++i;
			else builder.append(chars[i]);
		}
		return builder.toString();
	}*/
}