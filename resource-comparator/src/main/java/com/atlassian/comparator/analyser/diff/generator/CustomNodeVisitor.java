package com.atlassian.comparator.analyser.diff.generator;

import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import de.danielbechler.diff.path.NodePath;
import de.danielbechler.util.Strings;

public class CustomNodeVisitor implements DiffNode.Visitor {
    private final Object working;
    private final Object base;
    private final StringBuilder stringBuilder;

    public CustomNodeVisitor(Object working, Object base, StringBuilder stringBuilder) {
        this.working = working;
        this.base = base;
        this.stringBuilder = stringBuilder;
    }

    public void node(final DiffNode node, final Visit visit) {
        if (filter(node)) {
            final String text = differenceToString(node, base, working);
            stringBuilder.append("\n").append(text);
        }
    }

    protected boolean filter(final DiffNode node) {
        return (node.isRootNode() && !node.hasChanges())
                || (node.hasChanges() && !node.hasChildren());
    }

    protected String differenceToString(final DiffNode node, final Object base, final Object modified) {
        final NodePath nodePath = node.getPath();
        final String stateMessage = translateState(node.getState(), node.canonicalGet(base), node.canonicalGet(modified));
        final String propertyMessage = String.format("'%s': %s", node.getElementSelector().toHumanReadableString(), stateMessage);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(propertyMessage);
        if (node.isCircular()) {
            stringBuilder.append(" (Circular reference detected: The property has already been processed at another position.)");
        }
        return stringBuilder.toString();
    }

    protected void print(final String text) {
        System.out.println(text);
    }

    private static String translateState(final DiffNode.State state, final Object base, final Object modified) {
        if (state == DiffNode.State.IGNORED) {
            return "has been ignored";
        } else if (state == DiffNode.State.CHANGED) {
            return String.format("[ %s ] ==> [ %s ]",
                    Strings.toSingleLineString(base),
                    Strings.toSingleLineString(modified));
        } else if (state == DiffNode.State.ADDED) {
            return String.format("[ ] => [ %s ]", Strings.toSingleLineString(modified));
        } else if (state == DiffNode.State.REMOVED) {
            return String.format("[ %s ] => [ ]", Strings.toSingleLineString(base));
        } else if (state == DiffNode.State.UNTOUCHED) {
            return "";
        } else if (state == DiffNode.State.CIRCULAR) {
            return "has already been processed at another position. (Circular reference!)";
        }
        return '(' + state.name() + ')';
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }
}
