import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将log转成时序图脚本
 */
public class SequenceDiagramsProcessor {

    public static final String TRACE_INDICATOR = "<--->";
    public static final String ENTER_INDICATOR = "enter";
    public static final String EXIT_INDICATOR = "exit";
    private Map<String, List<TraceNode>> threadTraceMap = new HashMap<>();

    public String process(String src, String threadName) {
        try {
            if (src != null) {
                // 清空上一次的数据
                threadTraceMap.clear();

                String[] inputLines = src.split("\n");
                for (String inputLine : inputLines) {
                    TraceNode newNode = lineToTraceNode(inputLine);
                    if (newNode == null) {
                        continue;
                    }
                    List<TraceNode> traceNodes = threadTraceMap.get(newNode.threadName);
                    if (traceNodes == null) {
                        traceNodes = new ArrayList<>();
                        threadTraceMap.put(newNode.threadName, traceNodes);

                        // 添加根节点in
                        traceNodes.add(createRootTraceNode(newNode, DIRECTION.IN));
                    }

                    traceNodes.add(newNode);
                }
                // 添加根节点的out
                Set<String> threadNames = threadTraceMap.keySet();
                for (String name : threadNames) {
                    List<TraceNode> traceNodes = threadTraceMap.get(name);
                    if (traceNodes.size() > 0) {
                        traceNodes.add(createRootTraceNode(traceNodes.get(0), DIRECTION.OUT));
                    }
                }

                return transformToDiagramsScript(threadName);
            }
            return null;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private TraceNode createRootTraceNode(TraceNode newNode, DIRECTION direction) {
        TraceNode rootNode = new TraceNode();
        rootNode.dir = direction;
        rootNode.methodName = "root_method";
        rootNode.className = "ROOT";
        rootNode.threadName = newNode.threadName;
        rootNode.hash = 0;
        rootNode.ts = 0;
        return rootNode;
    }

    private String transformToDiagramsScript(String threadName) {
        /*
        Title: Here is a title
        A->B: Normal line
        B-->C: Dashed line
        C->D: Open arrow
        D-->>C: Dashed open arrow
        Note right of C: China thinks\nabout it
         */
        StringBuffer resultSB = new StringBuffer();
        // 标题
        resultSB.append("Title: thread[").append(threadName).append("]\n");
        // 节点
        LinkedList<TraceNode> stack = new LinkedList<>();
        for (TraceNode curNode : threadTraceMap.get(threadName)) {
            if (stack.size() == 0) {
                if (curNode.dir == DIRECTION.IN) {
                    stack.addLast(curNode);
                }
            } else {
                TraceNode prevNode = stack.getLast();
                if (isSameClassAndMethod(curNode, prevNode)
                        && prevNode.dir == DIRECTION.IN
                        && curNode.dir == DIRECTION.OUT) {
                    // 把本层的IN节点remove掉
                    stack.removeLast();
                    if (stack.size() > 0) {
                        // 获取所在层的节点
                        TraceNode layerNote = stack.getLast();
                        resultSB.append(curNode.className)
                                .append("-->")
                                .append(layerNote.className) // 当前层的名字
                                .append(": ")
                                .append(curNode.methodName).append("[").append(curNode.ts - prevNode.ts).append("ms]") // IN节点的时间戳
                                .append("\n");
                    }
                } else {
                    if (!(prevNode.hash == curNode.hash && prevNode.className.equals(curNode.className))
                            && curNode.dir == DIRECTION.IN) {
                        resultSB.append(prevNode.className)
                                .append("->")
                                .append(curNode.className)
                                .append(": ")
                                .append(curNode.methodName)
                                .append("\n");
                    }
                    stack.addLast(curNode);
                }
            }
        }
        return resultSB.toString();
    }

    private boolean isSameClassAndMethod(TraceNode curNode, TraceNode prevNode) {
        return prevNode.hash == curNode.hash
                && prevNode.className.equals(curNode.className)
                && prevNode.methodName.equals(curNode.methodName);
    }

    private TraceNode lineToTraceNode(String inputLine) {
        int i = inputLine.indexOf(TRACE_INDICATOR);
        if (i == -1) {
            return null;
        }
        String log = inputLine.substring(i + TRACE_INDICATOR.length() + 1);
        TraceNode node = new TraceNode();
        if (log.startsWith(ENTER_INDICATOR)) {
            node.dir = DIRECTION.IN;
            log = log.substring(ENTER_INDICATOR.length());
        } else if (log.startsWith(EXIT_INDICATOR)) {
            node.dir = DIRECTION.OUT;
            log = log.substring(EXIT_INDICATOR.length());
        }
        String[] splitResult = log.split("\\|");
        node.threadName = splitResult[0].trim();
        node.className = splitResult[1].trim();
        node.methodName = splitResult[2].trim();
        node.ts = Long.valueOf(splitResult[3].trim());
        node.hash = Long.valueOf(splitResult[4].trim());
        return node;
    }

    private static class TraceNode {
        // format: <---> exit main|com.legendmohe.methoddiff.MainActivity|onCreate()|4126|188072276
        DIRECTION dir;
        String threadName;
        String className;
        String methodName;
        long ts;
        long hash;
    }

    private enum DIRECTION {
        IN,
        OUT
    }
}
