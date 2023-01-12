package util;

public class GrammarStringUtils {
	/*
	 * 返回去除参数列表的函数名或泛型列表的类名.
	 * 例: speak<parameter_list>()</parameter_list>
	 * 对于此例, 返回"speak".
	*/
	public static String removeArgs(String str) {
                if (str == null) return "";
		String[] splited = str.split("<");
		return splited[0].trim();
	}
	// 从抽象语法树中的文件名提取包名.
	// 例: default/package1/Animal.java -> default.package1
	public static String getPackageName(String str) {
		String[] splited = str.split("/");
		if (splited.length <= 1) return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < splited.length - 2; i++) sb.append(splited[i]).append('.');
		sb.append(splited[splited.length - 2]);
		return sb.toString();
	}
	// 从Java语法的文件名转为AST中的文件名.
	// 例: default.package1.Animal.java -> default/package1/Animal.java
	public static String toASTFileName(String fileName) {
		StringBuilder sb = new StringBuilder();
		String[] splited = fileName.split("\\.");
		if (splited.length <= 2) return fileName;
		for (int i = 0; i < splited.length - 2; i++)
			sb.append(splited[i]).append('/');
		sb.append(splited[splited.length - 2]).append('.').append(splited[splited.length - 1]);
		return sb.toString();
	}
	// 上述过程的逆过程.
	public static String toJavaFileName(String fileName) {
		StringBuilder sb = new StringBuilder();
		String[] splited = fileName.split("/");
		if (splited.length <= 2) return fileName;
		for (int i = 0; i < splited.length - 1; i++)
			sb.append(splited[i]).append('.');
		sb.append(splited[splited.length - 1]);
		return sb.toString();
	}
	// 返回(Java包)名的第一部分.
	public static String getFirstPart(String str) {
		String[] splited = str.split("\\.");
		return splited[0];
	}
	// 返回(Java包)名的最后一部分.
	public static String getLastPart(String str) {
		String[] splited = str.split("\\.");
		return splited[splited.length - 1];
	}
	public static String removeSpaceLineBreaks(String str) {
		return str.replaceAll("\\r\\n|\\r|\\n|\\s", "");
	}
}
