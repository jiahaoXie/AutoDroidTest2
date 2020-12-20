package edu.nju.autodroid.strategy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import edu.nju.autodroid.androidagent.IAndroidAgent;
import edu.nju.autodroid.hierarchyHelper.Action;
import edu.nju.autodroid.hierarchyHelper.AndroidWindow;
import edu.nju.autodroid.hierarchyHelper.LayoutNode;
import edu.nju.autodroid.hierarchyHelper.LayoutTree;
import edu.nju.autodroid.hierarchyHelper.TreeHelper;
import edu.nju.autodroid.hierarchyHelper.TreeSearchOrder;
import edu.nju.autodroid.utils.AdbTool;
import edu.nju.autodroid.utils.Configuration;
import edu.nju.autodroid.utils.Logger;
import edu.nju.autodroid.windowtransaction.Group;
import edu.nju.autodroid.windowtransaction.GroupTransaction;

/**
 *由GroupWeightedSelectionStrategy.java所改
 *合并布局树 生成应用胎记
 *@author xjh
 */
public class birthMark implements IStrategy{
    private GroupTransaction groupTransaction = new GroupTransaction();
    private List<Group> groupList = new ArrayList<Group>();
    //private LayoutTree birth;	//保存每次本应用中布局树的归并结果

    protected String runtimePackage;
    protected IAndroidAgent androidAgent;
    private Action lastAction = null;
    protected int maxSteps;
    protected String startActivity;
    private int currentSteps;
    private Logger actionLogger = null;
    private  int lastGraphVetexCount = 0;
    private  int lastGraphEdgeCount = 0;
    private BufferedWriter bw;
    protected int MaxNoChangCount = 80;		//将生成策略的循环终止条件 由原来的200改为150
    protected double similarityThreshold = 0.8;

    public birthMark(IAndroidAgent androidAgent, int maxSteps, String startActivity, Logger actionLogger){
        this.MaxNoChangCount = Configuration.getDeltaC();
        this.similarityThreshold = Configuration.getDeltaL();
        this.androidAgent = androidAgent;
        this.maxSteps = maxSteps;
        this.startActivity = startActivity;
        currentSteps = 0;
        this.actionLogger = actionLogger;
        groupTransaction.addWindow(Group.OutWindow);
        File dir = new File("NoChangeCountLogger");
        if(!dir.exists())
            dir.mkdirs();
        try {
            bw = new BufferedWriter(new FileWriter(dir.getAbsolutePath() + "/" + new File(actionLogger.getLoggerFilePath()).getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getStrategyName() {
//        return "Layout group strategy";
    	return "Layout File Birthmark Generation strategy";
    }

    @Override
    public String getStrategyDescription() {
//        return "Layout group strategy";
    	return "Layout File Birthmark Generation strategy";
    }

    @Override
    public String getRuntimePackageName() {
        return runtimePackage;
    }

    @Override
    public boolean run() {
        try {
        	/** 5
        	 * 获取正在运行的文件包getRuntimePackage()；开启活动startActivity（）
        	 */
            String packageName = androidAgent.getRuntimePackage();
            int packageCount = 10;
            while((packageName == null || !startActivity.contains(packageName)) && packageCount-->0){
                androidAgent.startActivity(startActivity);
                Thread.sleep(1000);
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Wait for activity start");
                packageName = androidAgent.getRuntimePackage();
            }
            if(packageName == null)
                return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (NullPointerException e){
            e.printStackTrace();
            return false;
        }

        //获取当前运行package
        runtimePackage = androidAgent.getRuntimePackage();
        /** 6
         * 获取当前布局树 getCurrentLayout（）
         * 将多个布局树合并生成一个布局树成应用胎记
         */
        try{        	
            int scrollCount = 5;
            while(scrollCount-->0)//尝试右滑scrollCount次
            {             	
            	int[] windowSize = getCurrentLayout(androidAgent).getScreenSize();	
                		//getCurrentLayout()返回值为布局树it  这里是数组的方式去存贮布局树
                AdbTool.doSwipe(androidAgent.getDevice(), windowSize[0], windowSize[1]/2, 0, windowSize[1]/2);
                Thread.sleep(1000);
            }
            
            GL<Group> gl = getCurrentGL();	
            	
            int unChangedCount = 0;
            while(currentSteps <= maxSteps){
                try {
                    bw.write(unChangedCount+"");
                    bw.newLine();
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Action action = new Action();	//在strategy中使用的动作
                action.actionType = Action.ActionType.NoMoreAction;
                if(unChangedCount>=MaxNoChangCount)
                   break;
                if(gl.L == null){
                    action.actionType = Action.ActionType.NoAction;
                }
                else{
                    List<LayoutNode> nodeList = gl.L.findAll(new Predicate<LayoutNode>() {
                        @Override
                        public boolean test(LayoutNode node) {
                            return node.clickable||node.longClickable||node.focusable||node.scrollable||node.checkable;
                        }
                    }, TreeSearchOrder.DepthFirst);	//nodeList是一个数组列表 存储的是布局树的叶子结点
                    if(nodeList.size()>0) {
                    	//这里对叶子结点以及叶子结点代表的动作类型加权 是为了避免在遍历app过程中重复遍历一个叶子结点
                        LayoutNode nodeSelected = weightedRandSelected(nodeList);
                        int actioType = weightedActionType(nodeSelected.weight_actionType);
                        nodeSelected.weight -= 1;
                        if (nodeSelected.weight < 0)
                            nodeSelected.weight = 0;
                        nodeSelected.weight_actionType[actioType] -= 1;
                        if (nodeSelected.weight_actionType[actioType] < 0)
                            nodeSelected.weight_actionType[actioType] = 0;
                        action.actionNode = nodeSelected;
                        action.actionType = Action.ActionType.values()[actioType];
                    }else{
                        action.actionType = Action.ActionType.NoAction;
                    }
                }
                if(action.actionType == Action.ActionType.NoMoreAction)
                    break;
                else if(action.actionType == Action.ActionType.NoAction){
                    unChangedCount++;
                }
                else{
                    doAction(action);
                    GL<Group> gl_p = gl;
                    gl = getCurrentGL();
                    //birth=TreeHelper.merge(gl_p.L,gl.L);	//归并生成最后的胎记 
                    
                    /**
                     *后面 应该接着写 将胎记输出到制定的文件夹目录 
                     */
                    //这里后面的内容是与图有关的
                    //groupTransaction.addTransaction(gl_p.G.getId(), gl.G.getId(), action);
                    if(action.actionNode != null)
                        actionLogger.info(currentSteps + "\t" + gl_p.G.getId()+"\t" + gl.G.getId()+"\t"+ action.actionType.name()+"\t" + action.actionNode.className + "\t" + action.actionNode.indexXpath.replace(' ',';') + "\t" + getDumpedWindowString()+"\t" + androidAgent.getLayout());
                    else
                        actionLogger.info(currentSteps + "\t" + gl_p.G.getId()+"\t" + gl_p.G.getId()+"\t"+ action.actionType.name()+ "\tnull\tnull\t" + getDumpedWindowString()+"\t" + androidAgent.getLayout());

                    if(isChanged()){
                        unChangedCount = 0;
                        action.actionNode.weight += 2;
                        action.actionNode.weight_actionType[action.actionType.ordinal()] += 2;
                    }else{
                        unChangedCount++;
                    }
                    System.out.println("unChangedCount " + unChangedCount);
                }
                
                if(tryStayingCurrentApplication()){
                    gl = getCurrentGL();
                }else{
                    Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " 已经跳出当前package");
                    return false;
                }
                currentSteps++;
            }
        }
        catch (IOException e){
            Logger.logException(androidAgent.getDevice().getSerialNumber() + e.getMessage());
            return false;
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ShellCommandUnresponsiveException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * (自我小结) 这里应该是判断是否遍历完图的所有顶点和所有边 遍历完为false,没遍历完为true
     * @return true/false
     */
    private boolean isChanged(){
        int vertexCount = groupTransaction.getWindows().length;		//windows数组的大小
        int edgeCount = groupTransaction.getTransactions().size();	//所有的边的总数
        boolean changed;
        if(vertexCount == lastGraphVetexCount && edgeCount == lastGraphEdgeCount){
        	//lastGraphVetexCount lastGraphEdgeCount初始值为0
            changed =  false;
        }
        else{
            changed = true;
        }
        lastGraphEdgeCount = edgeCount;
        lastGraphVetexCount = vertexCount;
        return changed;
    }
    /**
     * (自我小结) 对节点列表中的节点进行随机加权
     * @return 节点选择
     */
    protected LayoutNode weightedRandSelected(List<LayoutNode> nodeList){
        int weightSum = 0;
        for(LayoutNode n : nodeList){
            weightSum += n.weight;
        }
        LayoutNode nodeToChoose = null;
        if(weightSum == 0){
            nodeToChoose = nodeList.get(new Random().nextInt(nodeList.size()));
        }
        else{
            int weightStep = 0;
            for(int i=0; i<nodeList.size(); i++){
                weightStep += nodeList.get(i).weight;
                if(new Random(new Date().getTime()).nextDouble() <= weightStep*1.0/weightSum){
                    nodeToChoose = nodeList.get(i);
                    break;
                }
            }
        }
        return nodeToChoose;
    }
    /**
     * (自我小结) 加权动作类型
     * @param weight_actiontype
     * @return
     */
    protected int weightedActionType(double[] weight_actiontype){
        int weightSum = 0;
        for(double w : weight_actiontype){
            weightSum += w;
        }
        int actioTypeIndex = 0;
        if(weightSum == 0){
            actioTypeIndex = new Random().nextInt(weight_actiontype.length);
        }else{
            int weightStep = 0;
            for(int i=0; i<weight_actiontype.length; i++){
                weightStep += weight_actiontype[i];
                if(new Random(new Date().getTime()).nextDouble() <= weightStep*1.0/weightSum){
                    actioTypeIndex = i;
                    break;
                }
            }
        }
        return actioTypeIndex;
    }

	/**
	  * @param androidAgent
	 * @return	布局树it
	 */
    protected LayoutTree getCurrentLayout(IAndroidAgent androidAgent) {
        String layoutXML = androidAgent.getLayout();
        while(layoutXML == null)
        {
            layoutXML = androidAgent.getLayout();
            Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Waiting for layout。。。");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LayoutTree lt = new LayoutTree(layoutXML);
        return lt;
    }
    
    /**
     * 用于布局相似度计算，两个布局之间相似度高 则合并到同一组里(具体过程在论文算法一中有详述)
     * 这里 做创新，在归并生成应用胎记时 首先将同一分组的布局树 首先进行归并
     * @return gl
     */
    protected GL<Group> getCurrentGL(){
        GL<Group> gl;
        if(androidAgent.getRuntimePackage().equals(runtimePackage)) {
            Group newGroup = new Group(new Date().getTime() + "");
            LayoutTree lc = getCurrentLayout(androidAgent);
            gl = getSimilaryLayout(lc);
            if (gl.G == null) {
                gl.G = newGroup;
                groupList.add(gl.G);
                gl.L = lc;
                newGroup.addLayout(lc);
            } else {
                double sim = lc.similarityWith(gl.L);
                if (sim >= 0.99) {
                    return gl;
                } else if (sim >= similarityThreshold) {                 
                    //方案1：在遍历阶段不做任何归并操作，在遍历后归并list中所有树
                    MergeWeight(gl.L, lc);
                    gl.G.addLayout(lc);
                    System.out.println("当前布局树信息："+lc.toString());
                    actionLogger.info(lc.toString());
                    
//                    //方案2：边遍历边合并，就无需使用G属性，L属性足够
//                    MergeWeight(gl.L, lc);
//                	  lc=TreeHelper.merge(gl.L, lc);
//                    gl.L = lc;
                } else {
                    gl.G = newGroup;
                    groupList.add(gl.G);
                    gl.G.addLayout(lc);
                }
            }
        }else{
            gl = new GL<Group>();
            gl.G = Group.OutWindow;
        }
        if(groupTransaction.getWindow(gl.G.getId()) == null){
            groupTransaction.addWindow(gl.G);
        }
        return gl;
    }
    
    /**
     * 将同一个分组group中的最大相似度 作为group的相似度值
     * @param lc
     * @return
     */
    private GL<Group> getSimilaryLayout(LayoutTree lc){
        double max=-1;
        GL gl = new GL();
        for(Group win : groupTransaction.getWindows()){
            for(LayoutTree l :win.getLayouts()){
                double sim = l.similarityWith(lc);
                if(sim>max){
                    gl.L = l;
                    gl.G = win;
                    max = sim;
                }
            }
        }
        return gl;
    }

    /**
     * lm的内容传给lc()
     * @param lm
     * @param lc
     */
    private void MergeWeight(LayoutTree lm, LayoutTree lc){
        for(LayoutNode n : lm.findAll(new Predicate<LayoutNode>() {
            @Override
            public boolean test(LayoutNode node) {
                return true;
            }
        }, TreeSearchOrder.DepthFirst))
        {
            LayoutNode nc = lc.getNodeByXPath(n.indexXpath);
            if(nc!= null)
            {
                nc.weight =n.weight;
                for(int i=0; i<nc.weight_actionType.length; i++)
                    nc.weight_actionType[i] = n.weight_actionType[i];
            }
        }
    }

    @Override
    public void writeToFile(String fileName) {
        //groupTransaction.writeToFile(fileName);
    	writeMergeTreeToFile(fileName);
    }
    private void writeMergeTreeToFile(String filename) {
    	try {
	    	File directory = new File(filename);
	    	if(directory.exists()) {
	    		directory.delete();
	    	}
	    	directory.mkdirs();
	    	
	    	File birthMarkFile = new File(directory.getAbsolutePath());
	    	if(birthMarkFile.exists()) {
	    		birthMarkFile.delete();
	    	}
    	
			birthMarkFile.createNewFile();
			FileWriter birthBw = new FileWriter(birthMarkFile);
			BufferedWriter birthFw = new BufferedWriter(birthBw);
			
			birthBw.write(mergeAllGroupTrees().toString());	//
			
			birthBw.close();
			birthFw.close();
			
//    		File treeInfo = new File(filename);
//    		if(treeInfo.exists()){
//    			System.out.println("文件已存在");
//    		}else{
//    			// 创建文件，根据给定的路径创建
//    			treeInfo.createNewFile(); // 创建文件，根据给定的路径创建
//    			System.out.println("创建文件成功~");
//    		}
    		
			Logger.logInfo("程序胎记保存完成！");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }
    
    @Override
	public  LayoutTree mergeAllGroupTrees() {
    	int grouplen = groupList.size();
    	
		File treeInfo = new File("TreeInfo.txt");
		if(treeInfo.exists()){
			System.out.println("文件已存在");
		}else{
			// 创建文件，根据给定的路径创建
			try {
				treeInfo.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // 创建文件，根据给定的路径创建
			System.out.println("创建文件成功~");
		}
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(treeInfo, true);

			OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
			// 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
			writer.write("[bMark416]一共有多少组树："+grouplen);
			writer.append("\r\n");
			// 换行
			writer.close();
			// 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉
			fos.close();
			// 关闭输出流,释放系统资源
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	//将下面所有输出到控制台的布局树相关信息 保存到中间文本中 方便统计
//    	System.out.println("[bMark416]一共有多少组树："+grouplen);
    	LayoutTree result = (grouplen>0)? mergeSingleGroupTrees(groupList.get(0)):null;
    	for(int i=1; i<grouplen; i++) {
    		result = TreeHelper.merge(mergeSingleGroupTrees(groupList.get(i)), result);
    	}
    	
    	try {
			fos = new FileOutputStream(treeInfo, true);
			OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
			// 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
			writer.write("***********************************");
			writer.append("\r\n");
			// 换行
			writer.close();
			// 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉
			fos.close();
			// 关闭输出流,释放系统资源
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return result;
    }
    
    private LayoutTree mergeSingleGroupTrees(Group group) {
    	//先做循环归并，跑通了做两两归并
    	List<LayoutTree> ltree = group.getLayouts();
    	int ltreelen = ltree.size();    	
//    	System.out.println("bMark[428] 该组有多少颗树："+ltreelen );
    	
    	//写入文件
    	File treeInfo = new File("TreeInfo.txt");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(treeInfo, true);
			OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
			// 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
			writer.write("bMark[428] 该组有多少颗树："+ltreelen);
			writer.append("\r\n");
			// 换行
			writer.close();
			// 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉
			fos.close();
			// 关闭输出流,释放系统资源
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	LayoutTree result = (ltreelen>0)? ltree.get(0):null;
    	for(int i=1; i<ltreelen; i++) {
    		result = TreeHelper.merge(ltree.get(i), result);
    	}
    	
//    	System.out.println("[bMark 433]合并后的树："+result.toString());
    	try {
			fos = new FileOutputStream(treeInfo, true);
			OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
			// 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
			writer.write("[bMark 433]合并后的树："+result.toString());
			writer.append("\r\n");
			// 换行
			writer.close();
			// 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉
			fos.close();
			// 关闭输出流,释放系统资源
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return result;
    }       

    protected boolean tryStayingCurrentApplication(){
        if(androidAgent.getRuntimePackage().equals(runtimePackage)){
            return true;
        }
        else{
            androidAgent.pressBack();
            if(androidAgent.getRuntimePackage().equals(runtimePackage)){
                return true;
            }
            else{
                int tryCount = 3;
                while (tryCount-- >= 0) {
                    androidAgent.startActivity(startActivity);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (androidAgent.getRuntimePackage().equals(runtimePackage)) {
                        return true;
                    }

                    androidAgent.pressBack();
                    androidAgent.pressHome();
                    if(tryCount <= 0)
                    {
                        try {
                            AdbTool.killTask(androidAgent.getDevice(), AdbTool.getTaskId(androidAgent.getDevice(), androidAgent.getRuntimePackage()));
                        } catch (TimeoutException | AdbCommandRejectedException | IOException | ShellCommandUnresponsiveException e) {
                            Logger.logException(androidAgent.getDevice().getSerialNumber() + e.getMessage());
                        }
                    }
                }

                Logger.logInfo(androidAgent.getDevice().getSerialNumber() +" Try restart but failed");
                return false;

            }
        }
    }
    private String getDumpedWindowString(){
        String str = "";
        List<AndroidWindow> androidWindowList = androidAgent.getAndroidWindows();
        while(androidWindowList == null){
            try {
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Wait for androidWindowList!");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            androidWindowList = androidAgent.getAndroidWindows();
        }
        for(AndroidWindow aw : androidWindowList){
            str += aw.id+":"+aw.activityName+":"+aw.session+";";
        }
        return str.replace(' ','-');
    }
    /**
     * 对每种行为 进行日志输出
     * @param action
     */
    protected void doAction(Action action){
        switch (action.actionType){
            case Click:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tClick");
                androidAgent.doClick(action.actionNode);
                break;
            case LongClick:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tLongClick" );
                androidAgent.doLongClick(action.actionNode);
                break;
            /*case SetText:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + windowTransaction.getWindowSize() + "	SetText");
                androidAgent.doSetText(action.actionNode, "123");
                break;*/
            case ScrollBackward:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "	ScrollBackward");
                androidAgent.doScrollBackward(action.actionNode, 55);
                break;
            case ScrollForward:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "	ScrollForward");
               androidAgent.doScrollForward(action.actionNode, 55);
                break;
            case Back:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tBack" );
                androidAgent.pressBack();
                break;
            case Home:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tHome" );
                androidAgent.pressHome();
                break;
            case SwipeToLeft:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tSwipeToLeft" );
                androidAgent.doSwipeToLeft(action.actionNode);
                break;
            case SwipeToRight:
                Logger.logInfo(androidAgent.getDevice().getSerialNumber() + " Step " + currentSteps + "\tWindowCount " + groupTransaction.getWindowSize() + "\tSwipeToRight" );
                androidAgent.doSwipeToRight(action.actionNode);
                break;
        }
    }
}
