package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 读取文本中的内容 并对特定字符进行替换 最后返回文件
 * 
 * @author Administrator
 *
 */
public class fileRead02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length == 0) {
			System.out.println("Please input inputFile path~");
			return;
		}
		try {
			BufferedReader bufReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(args[0]))));// 数据流读取文件

			StringBuffer strBuffer = new StringBuffer();
			String empty = "G:";
			String tihuan = "";
			for (String temp = null; (temp = bufReader.readLine()) != null; temp = null) {
				if (temp.indexOf("H:") != -1) { // 判断当前行是否存在想要替换掉的字符 -1表示存在
					tihuan = temp.substring(0, 2);
					temp = temp.replace(tihuan, empty);// 替换为你想要的东东
				}
				strBuffer.append(temp);
				strBuffer.append(System.getProperty("line.separator"));// 行与行之间的分割
			}
			bufReader.close();
			PrintWriter printWriter = new PrintWriter(args[0]);// 替换后输出的文件位置
			printWriter.write(strBuffer.toString().toCharArray());
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
