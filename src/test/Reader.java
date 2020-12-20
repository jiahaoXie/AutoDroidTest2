package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
/**
 * 逐行读入文本中的数据 存放到map中
 * @author xjh 2018.06.13
 */
public class Reader {
	private static final Integer ONE = 1;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Map<String, Integer> map = new HashMap<String, Integer>();

        /* 读取数据 */
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("G:\\apk\\anzhi\\apkFirstStageRusult\\browserResult0604.txt")),"UTF-8"));
            String lineTxt = null;
            while ((lineTxt = br.readLine()) != null) {
            	System.out.println(lineTxt);
                String[] names = lineTxt.split(",  ");
                System.out.println("total sum:"+names.length);
                for (String name : names) {
                    if (map.keySet().contains(name)) {
                        map.put(name, (map.get(name) + ONE));
                    } else {
                        map.put(name, ONE);
                    }
                }
                System.out.println("names length:"+names.length);
                
            }
            br.close();
        } catch (Exception e) {
            System.err.println("read errors :" + e);
        }
        
        //输出
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
           }

	}

}
