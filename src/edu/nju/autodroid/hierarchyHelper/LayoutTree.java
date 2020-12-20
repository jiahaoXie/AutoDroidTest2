package edu.nju.autodroid.hierarchyHelper;
import static java.lang.Boolean.parseBoolean;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.davidmoten.rtree.Entries;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;

import edu.nju.autodroid.utils.Logger;
import edu.nju.autodroid.utils.Utils;
import rx.functions.Action1;

/**
 * Created by ysht on 2016/3/7 0007. 用于保存Layout的数据结构（以树的形式）
 */
public class LayoutTree {
	private LayoutNode root;// LayoutTree是一个根节点为空的树，根节点不包含数据

	public LayoutNode getRoot() {
		return root;
	}

	public void setRoot(LayoutNode root) {
		this.root = root;
	}
		
	private String layoutXML;
	// findAll函数的变量，用于保存findAll中的中间结果
	private List<LayoutNode> findList = new ArrayList<LayoutNode>();

	private int totalChildrenCountBeforeCompress = 0;

	// 用于rect area相似度判断的R树。会在第一次使用的时候创建。
	private RTree<LayoutNode, Rectangle> layoutRTree = null;

	/**
	 * 增加函数 用来计算布局树的高度
	 */
	private List<LayoutNode> totalNodes = new ArrayList<LayoutNode>();
	public List<LayoutNode> getTotalNodes() {
        return totalNodes;
    }
	
	private int getTreeDepth(LayoutNode root) {
		totalNodes.add(root);
		List<Integer> depth = new ArrayList<Integer>();
		if (root.getChildrenNodes() == null || root.getChildrenNodes().size() == 0) {
			return 1;
		}
		for (int i = 0; i < root.getChildrenNodes().size(); i++) {
			int temp;
			temp = getTreeDepth(root.getChildrenNodes().get(i)) + 1;
			depth.add(temp);
		}
		Collections.sort(depth, Collections.reverseOrder());
		return depth.get(0);

	}
	/**
	 * 由String类型的layoutXML 创建布局树并进行有效压缩（删除只包含一个孩子的树节点）
	 * @param layoutXML
	 */
	public LayoutTree(String layoutXML) {
		root = new LayoutNode();
		try {
			this.layoutXML = layoutXML;
			createTree(layoutXML); // 建树 (测试方法中建树之后 root的totalChildrenCount由0变为10)
			totalChildrenCountBeforeCompress = root.getTotalChildrenCount();
//			this.toString();
			compressTree();	//有效压缩之后 root的totalChildrenCOunt变为3
		} catch (Exception e) {
			Logger.logException(e);
		}
	}

	public LayoutTree() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 当一个viewgroup（包含子节点）的child个数只有1个的话，那么将child替代该节点（压缩布局树）
	 */
	private void compressTree() {
		compressTree(root);
	}

	private void compressTree(LayoutNode node) {
		for (LayoutNode n : node.getChildren()) {
			LayoutNode temp = n;
			while (temp.getChildrenCount() == 1) {
				node.replaceChild(temp.getChildren().get(0), temp);
				temp = temp.getChildren().get(0);
			}
		}
		for (LayoutNode n : node.getChildren()) {
			compressTree(n);
		}
	}

	public String getLayoutXML() {
		return layoutXML;
	}

	public int getTreeSize() {
		return root.getTotalChildrenCount();
	}

	public int getTreeSizeBeforeCompress() {
		return totalChildrenCountBeforeCompress;
	}

	public int[] getScreenSize() {
		for (LayoutNode node : root.getChildren()) {
			return new int[] { node.bound[2], node.bound[3] };
		}
		return new int[] { 480, 800 };
	}

	/**
	 * 计算当前layoutTree和另一个layoutTree的相似度,采用树的BFS序列的编辑距离最为参照
	 * 
	 * @param layoutTree
	 *            待比较的另一个layoutTree
	 * @return 0.0-1.0之间的相似度值
	 */
	public double similarityWith(LayoutTree layoutTree) {
		/*
		 * if(layoutTree.layoutXML.equals(this.layoutXML)) return 1.0;
		 */
		// 事实上这个更浪费时间

		int editDis = Utils.EditDistance(getTreeBFSHashes(), layoutTree.getTreeBFSHashes());
		return 1.0 - editDis * 1.0 / Math.max(root.getTotalChildrenCount(), layoutTree.getTreeSize());
	}

	public double similarityWith(LayoutTree layoutTree, LayoutSimilarityAlgorithm algorithm) {
		switch (algorithm) {
		case BFSThenEditdistane:
			return similarityWith(layoutTree);
		case RectArea:
			return similarityWithByRectArea(layoutTree);
		case RegionRatio:
			return similarityWithByRegionRatio(layoutTree);
		default:
			throw new UnsupportedOperationException("不支持的相似度计算算法");
		}
	}

	protected double similarityWithByRectArea(LayoutTree layoutTree) {
		if (layoutRTree == null) {
			createRTree();
		}

		if (layoutTree.layoutRTree == null) {
			layoutTree.createRTree();
		}
		final double[] tsim = new double[1];
		final int[] count = new int[1];
		layoutTree.layoutRTree.entries().subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
			@Override
			public void call(final Entry<LayoutNode, Rectangle> otherEntry) {
				// 对每个，查找覆盖的元素
				final double[] maxSim = new double[] { 0 };
				layoutRTree.search(otherEntry.geometry()).subscribe(new Action1<Entry<LayoutNode, Rectangle>>() {
					@Override
					public void call(Entry<LayoutNode, Rectangle> myEntry) {
						// 找到有重叠的区域，那么则计算覆盖率
						double sim = Utils.getNormalizedOverlapArea(myEntry.value().bound, otherEntry.value().bound);
						if (sim > maxSim[0])
							maxSim[0] = sim;
					}
				});
				tsim[0] += maxSim[0];
				count[0] += 1;
			}
		});

		if (count[0] == 0)
			return 0;

		return tsim[0] / count[0];
	}

	// 不对layoutRTree是否为空进行判断。直接生成新的RTree赋给layoutRTree
	protected RTree createRTree() {
		layoutRTree = RTree.create();
		List<LayoutNode> leafLayoutNode = findAll(new Predicate<LayoutNode>() {
			@Override
			public boolean test(LayoutNode node) {
				return node.getChildrenCount() == 0;
			}
		}, TreeSearchOrder.DepthFirst);

		List<Entry<LayoutNode, Rectangle>> entryList = new ArrayList<Entry<LayoutNode, Rectangle>>();
		for (LayoutNode n : leafLayoutNode) {
			int x1, x2, y1, y2;
			x1 = Math.min(n.bound[0], n.bound[2]);
			x2 = Math.max(n.bound[0], n.bound[2]);
			y1 = Math.min(n.bound[1], n.bound[3]);
			y2 = Math.max(n.bound[1], n.bound[3]);
			entryList.add(Entries.entry(n, Geometries.rectangle(x1, y1, x2, y2)));
		}

		layoutRTree = layoutRTree.add(entryList);
		return layoutRTree;
	}

	// 通过二分图匹配算的图的相似度。速度太慢了。。。
	protected double similarityWithByRectArea2(LayoutTree layoutTree) {
		List<LayoutNode> nodes1 = findAll(new Predicate<LayoutNode>() {
			@Override
			public boolean test(LayoutNode node) {
				return true;
			}
		}, TreeSearchOrder.BoardFirst);
		List<LayoutNode> nodes2 = layoutTree.findAll(new Predicate<LayoutNode>() {
			@Override
			public boolean test(LayoutNode node) {
				return true;
			}
		}, TreeSearchOrder.BoardFirst);
		int maxSize = Math.max(nodes1.size(), nodes2.size());
		double[][] weight = new double[maxSize][maxSize];
		int[] match = new int[maxSize];
		for (int i = 0; i < nodes1.size(); i++) {
			for (int j = 0; j < nodes2.size(); j++) {
				weight[i][j] = Utils.getNormalizedOverlapArea(nodes1.get(i).bound, nodes2.get(j).bound);
			}
		}
		double sim = Utils.biGraph(true, weight, match) * 1.0 / Math.max(nodes1.size(), nodes2.size());
		return sim;
	}

	// 先将每个控件按照面积排序，然后用二分图匹配，权值就是面积比（小/大）
	protected double similarityWithByRegionRatio(LayoutTree layoutTree) {
		final SimpleWeightedGraph<LayoutNode, DefaultWeightedEdge> biPartitieGraph = new SimpleWeightedGraph<LayoutNode, DefaultWeightedEdge>(
				new ClassBasedEdgeFactory<LayoutNode, DefaultWeightedEdge>(DefaultWeightedEdge.class));
		List<LayoutNode> nodes1 = findAll(new Predicate<LayoutNode>() {
			@Override
			public boolean test(LayoutNode node) {
				if (node.getChildrenCount() == 0) {
					biPartitieGraph.addVertex(node);
					return true;
				}
				return false;
			}
		}, TreeSearchOrder.BoardFirst);
		List<LayoutNode> nodes2 = layoutTree.findAll(new Predicate<LayoutNode>() {
			@Override
			public boolean test(LayoutNode node) {
				if (node.getChildrenCount() == 0) {
					biPartitieGraph.addVertex(node);
					return true;
				}
				return false;
			}
		}, TreeSearchOrder.BoardFirst);
		for (LayoutNode n1 : nodes1) {
			for (LayoutNode n2 : nodes2) {
				double ratio = regionRatio(n1, n2);
				if (ratio >= 0.7)
					biPartitieGraph.setEdgeWeight(biPartitieGraph.addEdge(n1, n2), regionRatio(n1, n2));
			}
		}
		MaximumWeightBipartiteMatching<LayoutNode, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
				biPartitieGraph, new HashSet<>(nodes1), new HashSet<>(nodes2));
		MatchingAlgorithm.Matching<LayoutNode, DefaultWeightedEdge> matchResult = matching.getMatching();
		return matchResult.getWeight() / Math.min(nodes1.size(), nodes2.size());
	}

	private double regionRatio(LayoutNode n1, LayoutNode n2) {
		int r1 = Math.abs(n1.bound[0] - n1.bound[2]) * Math.abs(n1.bound[1] - n1.bound[3]);
		int r2 = Math.abs(n2.bound[0] - n2.bound[2]) * Math.abs(n2.bound[1] - n2.bound[3]);
		if (r1 > r2)
			return r2 * 1.0 / r1;
		return r1 * 1.0 / r2;
	}

	/**
	 * 得到树BFS遍历的哈希散列值
	 * 
	 * @return
	 */
	protected Integer[] getTreeBFSHashes() {
		Integer[] hashes = new Integer[root.getTotalChildrenCount()];
		int i = 0;
		Queue<LayoutNode> nodeQueue = new LinkedList<LayoutNode>();
		for (LayoutNode n : root.getChildren()) {
			nodeQueue.offer(n);
		}

		while (!nodeQueue.isEmpty()) {
			LayoutNode cn = nodeQueue.poll();
			// 当出现TreeView时，我们不管内部的结构
			if (!cn.className.contains("TreeView")) 
			{
				for (LayoutNode n : cn.getChildren()) {
					nodeQueue.offer(n);
				}
			}
			hashes[i++] = cn.className.hashCode();
			// hashCode()这个方法返回对象的散列码，返回值是int类型的散列码。
			// 对象的散列码是为了更好的支持基于哈希机制的Java集合类，例如 Hashtable, HashMap, HashSet等
		}

		return hashes;
	}

	/**
	 * 查找所有满足条件的节点
	 * 
	 * @param predicate
	 *            条件
	 * @param searchOrder
	 *            遍历顺序
	 * @return 满足条件的以searhOrder为顺序的节点列表，若未查找到，返回一个空列表
	 */
	public List<LayoutNode> findAll(Predicate<LayoutNode> predicate, TreeSearchOrder searchOrder) {
		findList.clear();
		if (searchOrder == TreeSearchOrder.DepthFirst) {
			for (LayoutNode n : root.getChildren()) {
				findAll(n, predicate);
			}
		} else if (searchOrder == TreeSearchOrder.BoardFirst) {
			Queue<LayoutNode> q = new LinkedList<LayoutNode>();
			for (LayoutNode n : root.getChildren()) {
				q.offer(n);
			}
			while (!q.isEmpty()) {
				LayoutNode ln = q.poll();
				if (predicate.test(ln)) {
					findList.add(ln);
				}
				for (LayoutNode n : ln.getChildren()) {
					q.offer(n);
				}
			}
		}

		List<LayoutNode> result = new ArrayList<LayoutNode>();
		result.addAll(findList);
		return result;
	}

	// 用于DFS模式的findAll
	private void findAll(LayoutNode node, Predicate<LayoutNode> predicate) {
		if (node == null)
			return;
		if (predicate.test(node))
			findList.add(node);
		for (LayoutNode n : node.getChildren()) {
			findAll(n, predicate);
		}
	}

	/**
	 * 由layoutXML（String类型）建树（由布局文件xml提取布局树）
	 * 
	 * @param layoutXML
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	private void createTree(String layoutXML) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.parse(new ByteArrayInputStream(layoutXML.getBytes("utf-8")));
		Element rootEle = doc.getDocumentElement();
		if (rootEle == null)
			return;
		NodeList nodes = rootEle.getChildNodes();
		if (nodes == null)
			return;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
				LayoutNode ln = parseActivityNode(node);
				ln.indexXpath = ln.index + "";
				root.addChild(ln);
				recursionCreateTree(node, ln); // 递归创建树
			}
		}
	}

	/**
	 * 递归创建树 
	 * @param curNode
	 * @param parent
	 */
	private void recursionCreateTree(Node curNode, LayoutNode parent) {
		if (curNode == null)
			return;
		NodeList nodes = curNode.getChildNodes();
		if (nodes == null)
			return;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
				LayoutNode ln = parseActivityNode(node);
				ln.indexXpath = parent.indexXpath + " " + ln.index;
				parent.addChild(ln);
				recursionCreateTree(node, ln);
			}
		}
	}

	/**
	 * 通过XML中的Node节点创建LayoutNode
	 * 
	 * @param node
	 *            用于转换的节点
	 * @return 根据node创建的LayoutNode
	 */
	private LayoutNode parseActivityNode(Node node) {
		LayoutNode layoutNode = new LayoutNode();
		NamedNodeMap nnm = node.getAttributes();
		layoutNode.index = Integer.parseInt(nnm.getNamedItem("index").getNodeValue());
		layoutNode.text = nnm.getNamedItem("text").getNodeValue();
		layoutNode.className = nnm.getNamedItem("class").getNodeValue();
		layoutNode.packageName = nnm.getNamedItem("package").getNodeValue();
		layoutNode.contentDesc = nnm.getNamedItem("content-desc").getNodeValue();
		layoutNode.checkable = parseBoolean(nnm.getNamedItem("checkable").getNodeValue());
		layoutNode.checked = parseBoolean(nnm.getNamedItem("checked").getNodeValue());
		layoutNode.clickable = parseBoolean(nnm.getNamedItem("clickable").getNodeValue());
		layoutNode.enabled = parseBoolean(nnm.getNamedItem("enabled").getNodeValue());
		layoutNode.focusable = parseBoolean(nnm.getNamedItem("focusable").getNodeValue());
		layoutNode.focuesd = parseBoolean(nnm.getNamedItem("focused").getNodeValue());
		layoutNode.scrollable = parseBoolean(nnm.getNamedItem("scrollable").getNodeValue());
		layoutNode.longClickable = parseBoolean(nnm.getNamedItem("long-clickable").getNodeValue());
		layoutNode.password = parseBoolean(nnm.getNamedItem("password").getNodeValue());
		layoutNode.selected = parseBoolean(nnm.getNamedItem("selected").getNodeValue());
		String boundStr = nnm.getNamedItem("bounds").getNodeValue();
		Matcher matcher = Pattern.compile("[0-9]+").matcher(boundStr);
		if (matcher.find())
			layoutNode.bound[0] = Integer.parseInt(matcher.group());
		if (matcher.find())
			layoutNode.bound[1] = Integer.parseInt(matcher.group());
		if (matcher.find())
			layoutNode.bound[2] = Integer.parseInt(matcher.group());
		if (matcher.find())
			layoutNode.bound[3] = Integer.parseInt(matcher.group());
		return layoutNode;
	}

	/**
	 * (自我小结) 通过路径获取节点
	 * 
	 * @param indexXPath
	 * @return node
	 */
	public LayoutNode getNodeByXPath(String indexXPath) {
		String[] indexStrs = indexXPath.split(" ");
		int[] indexes = new int[indexStrs.length];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = Integer.parseInt(indexStrs[i]);
		}
		LayoutNode node = root;
		for (int i = 0; i < indexes.length; i++) {
			if (node.getChildrenCount() <= indexes[i])
				return null;
			node = node.getChildren().get(indexes[i]);
		}
		return node;
	}
	
	@Override
	/**
	 * tiString()方法 没有输出
	 */
	public String toString() {
		String result="";

		System.out.println("根节点的子节点个数："+this.root.getTotalChildrenCount());
		for(LayoutNode t:this.root.getChildrenNodes()){
//			System.out.println("[Ltree]layer1:" + this.root.getChildrenNodes().size());
				//注释掉中间输出结果
			//System.out.println(t.toString()); 
			result+=toStringHelper(t,1);
		}
		return result;
	}
	
	public String toStringHelper(LayoutNode lnode, int layer){
		String result = "";
//		System.out.println("[Ltree]layer" + (layer) + ":" + lnode.getChildrenNodes().size());
//		System.out.println(lnode.toString());	//注释掉中间结果(测试的时候可以运行看一下结果)
		result += lnode.toString();
		for(LayoutNode t:lnode.getChildrenNodes()){
//			System.out.println("[Ltree]layer" + (layer+1) + ":" + lnode.getChildrenNodes().size());	
					//注释掉中间结果
			result+=toStringHelper(t, layer+1);
		}
		return result;
	}
	
	//测试方法 先测试一下 toString()方法能否输出树结构
	public static void main(String[] args) throws UnsupportedEncodingException {		 
		String encoding = "UTF-8";  
        File file = new File("E:\\test.xml");  
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
        
        String path=new String(filecontent,encoding);		
        System.out.println(path);			
		LayoutTree tree=new LayoutTree(path);		
		System.out.println("最后的布局树为："+tree.toString());        
   } 
	
}

