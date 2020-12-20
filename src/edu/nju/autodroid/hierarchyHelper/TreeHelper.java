 package edu.nju.autodroid.hierarchyHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 树合并
 */
public final class TreeHelper {

    private TreeHelper() {
    }

    /**
     * 树的合并
     * @param treeOne 树一
     * @param treeTwo 树二
     * @return treeMerged 合并树
     */
    public static LayoutTree merge(LayoutTree treeOne, LayoutTree treeTwo) {
        checkRooter(treeOne, treeTwo);
        LayoutTree tree = new LayoutTree();	//Tree类中没有显示的声明构造方法 这里是调用类默认的构造方法
        return merge(tree, treeOne, treeTwo);
    }
    /**
     * TreeOne和TreeTwo 两棵树合并生成新的树
     * @param tree
     * @param treeOne
     * @param treeTwo
     * @return
     */
    private static LayoutTree merge(LayoutTree tree, LayoutTree treeOne, LayoutTree treeTwo) {
        tree.setRoot(treeOne.getRoot());
        
        recursive(tree.getRoot().getChildrenNodes(), treeOne.getRoot().getChildrenNodes(), treeTwo.getRoot().getChildrenNodes());
        return tree;
    }
    /**
     * 孩子节点 归并
     * @param newer
     * @param original
     * @param target
     */
    private static void recursive(List<LayoutNode> newer, List<LayoutNode> original, List<LayoutNode> target) {
        //遍历到根节点，直接返回（这里应该是备注写错了，，应该是遍历到叶子结点）
        if ((original == null || original.size() == 0) && (target == null || target.size() == 0)) {
            return;
        }
        //单边为空的情形
        if ((original != null && original.size() > 0) && (target == null || target.size() == 0)) {
            addAllCleanNode(newer, original);
            List<LayoutNode> nullNode=Collections.emptyList();
            for (int i = original.size() - 1; i >= 0; i--) {
                doRecursive(newer.get(i).getChildrenNodes(), original.get(i).getChildrenNodes(), nullNode);
                return;
            }
        }
        //单边为空的情形
        if ((target != null && target.size() > 0) && (original == null || original.size() == 0)) {
            addAllCleanNode(newer, target);
            List<LayoutNode> nullNode=Collections.emptyList();
            for (int i = target.size() - 1; i >= 0; i--) {
                doRecursive(newer.get(i).getChildrenNodes(), nullNode, target.get(i).getChildrenNodes());
                return;
            }
        }
        doRecursive(newer, original, target);
    }
    
   //有两种实现思路：非递归方式、递归方式 非递归方式会存在树中重复节点计数问题(譬如同为node E,但挂载在不同的node)
//    public static void compareAndPrint(LayoutTree treeOne, LayoutTree treeTwo) {
//        checkRooter(treeOne, treeTwo);
////        Set<String> setOne = getSetFromNodes(treeOne.getTotalNodes());
////        Set<String> setTwo = getSetFromNodes(treeTwo.getTotalNodes());
////        Set<String> added = new HashSet<>();
//        
//        Set<LayoutNode> setOne = (Set<LayoutNode>) treeOne.getTotalNodes();
//        Set<LayoutNode> setTwo = (Set<LayoutNode>) treeTwo.getTotalNodes();
//        Set<LayoutNode> added = new HashSet<>();
//        for (LayoutNode node : setOne) {
//            if (!setTwo.contains(node)) {
//                System.out.println(CompareResult.DELETE.name() + ":" + node);
//            }
//        }
//        for (LayoutNode node : setTwo) {
//            if (!setOne.contains(node)) {
//                added.add(node);
//            }
//        }
//        StringBuilder targetString = new StringBuilder();
//        targetString.append(CompareResult.ADD.name() + ":");
//        added.forEach(e -> {
//            targetString.append(e + "、");
//        });
//        System.out.println(targetString.deleteCharAt(targetString.length() - 1).toString());
//
//    }

//    private static Set<String> getSetFromNodes(List<LayoutNode> nodes) {
//        Set<String> set = new HashSet<>();
//        nodes.forEach(e ->
//                set.add(e.getName())
//        );
//        return set;
//    }
    
    /**
     * 树合并时 鉴别两棵树 之前是否有相同结构，相同结构合并，不同结构保留
     * @param newer
     * @param original
     * @param target
     */
    //实参1：doRecursive(newer.get(i).getChildNodes(), original.get(i).getChildNodes(), Collections.emptyList());
    //实参2： doRecursive(newer.get(i).getChildNodes(), Collections.emptyList(), target.get(i).getChildNodes());
    //实参3：doRecursive(newer, original, target);
    private static void doRecursive(List<LayoutNode> newer, List<LayoutNode> original, List<LayoutNode> target){
    	//鉴别两个节点是否同一应用节点(可以是string,亦可以是其他，依据业务设定)
        Set<LayoutNode> addedNode = new HashSet<>();	//这里集合set里的类型改为节点 LayoutNode
        //沿targetNodes为主进行扫描
        int tarlen = target.size();
        for (int i = 0; i < tarlen; i++) {
        	LayoutNode tempNode = target.get(i);
            boolean exist = false;
            for (int j = 0; j < original.size(); j++) {
                if (tempNode.equals(original.get(j))) {	//两棵树存在相同结构 则合并
                    exist = true;
                    newer.add(new LayoutNode());
                    addedNode.add(tempNode);
                    recursive(newer.get(newer.size() - 1).getChildrenNodes(), original.get(j).getChildrenNodes(), target.get(i).getChildrenNodes());
                }
            }
            if (!exist) {	//不同结构 则保留
                newer.add(tempNode);
                addedNode.add(tempNode);
            }

        }
        //沿original进行扫描
        int orilen = original.size();
        for (int i = 0; i < orilen; i++) {
            //判断是否已添加至新的merge list
        	LayoutNode tempNode = original.get(i);
            if (addedNode.contains(tempNode)) {
                continue;
            } else {
                newer.add(tempNode);
            }
        }
    }
    
    private static void addAllCleanNode(List<LayoutNode> newer, List<LayoutNode> target) {
       for (LayoutNode treeNode:target) {
//           newer.add(new LayoutNode());
    	   newer.add(treeNode);
       }
    }
    /**
     * 检查两棵树的根节点 （树的根节点不能为空） 
     * 疑惑处在 两棵树的根节点一定是相同的才对，不然怎么进行树合并？？
     * @param treeOne
     * @param treeTwo
     */
    private static void checkRooter(LayoutTree treeOne, LayoutTree treeTwo) {
        if (treeOne == null || treeTwo == null) {
            throw new IllegalArgumentException("input tree can't be null！");
        }
        if (treeOne.getRoot() == null || treeOne.getRoot() == null) {
            throw new IllegalArgumentException("input legal tree please！");
        }        
//        //根节点名称相同（或者以其他规则表明两节点相同）
//        if (!treeOne.getRoot().equals(treeTwo.getRoot())) {
//            throw new IllegalArgumentException("both tree should have the same root Node! ");
//        }
    }

    /**
     * 输出树的结构和统计信息
     *
     * @param tree
     */
    public static void printResult(String prefix, LayoutTree tree) {
        System.out.println(prefix + tree);
    }

    private enum CompareResult {
        ADD, DELETE;
    }


}
