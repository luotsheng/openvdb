import StartNode from "./StartNode";
import EndNode from "./EndNode";
import ActionNode from "./ActionNode.tsx";
import BranchNode from "./BranchNode.tsx";

export const nodetypes = {
    start: StartNode,
    end: EndNode,
    action: ActionNode,
    branch: BranchNode
};