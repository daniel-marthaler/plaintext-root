package ch.plaintext.mermaid;

import java.util.List;

public interface IMermaidService {

    String generateFlowchart(String direction, List<FlowNode> nodes, List<FlowEdge> edges);

    record FlowNode(
            String id,
            String label,
            NodeStyle style
    ) {}

    record FlowEdge(
            String fromId,
            String toId,
            String label
    ) {}

    enum NodeStyle {
        INCOMING,
        OUTGOING,
        NEUTRAL,
        DEFAULT
    }
}
