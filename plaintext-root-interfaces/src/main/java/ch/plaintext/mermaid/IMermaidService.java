/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
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
