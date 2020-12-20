package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
/**
 * 导入文件
 * @author Administrator
 *
 */
public class fileRead {	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String encoding = "UTF-8";  
        File file = new File("E:\\test.txt");  
        Long filelength = file.length();  
        byte[] filecontent = new byte[filelength.intValue()];  
        try {  
            FileInputStream in = new FileInputStream(file);  
            in.read(filecontent);  
            in.close();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        try {  
        	System.out.println(new String(filecontent, encoding));   
        } catch (UnsupportedEncodingException e) {  
            System.err.println("The OS does not support " + encoding);  
            e.printStackTrace();  
             
        } 
	}
	

}
