package com.atlassian.comparator.analyser.diff.generator;

import com.amazonaws.services.dynamodbv2.xspec.S;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;

public class BaseDiffGenerator {


    StringBuilder builderFirst = new StringBuilder();
    StringBuilder builderLast = new StringBuilder();
    StringBuilder builder = new StringBuilder();
    final Object builderLock = new Object();
    private static BaseDiffGenerator baseDiffGenerator;

    public static BaseDiffGenerator get() {
        if (baseDiffGenerator == null) {
            baseDiffGenerator = new BaseDiffGenerator();
        }
        return baseDiffGenerator;
    }

    private BaseDiffGenerator() {
    }

    public void process(Object sourceResource, Object targetResource, StringBuilder builder) {
        DiffNode root = ObjectDifferBuilder.buildDefault().compare(sourceResource, targetResource);
        this.builder.append(builder.toString());
        CustomNodeVisitor visitor = new CustomNodeVisitor(sourceResource, targetResource, this.builder);
        synchronized (this.builderLock) {
            root.visitChildren(visitor);
        }
    }

    public String generate() {
        StringBuilder builderTemp = new StringBuilder();
        synchronized (this.builderLock) {
            builderTemp.append(builderFirst);
            builderTemp.append(this.builder);
            builderTemp.append(builderLast);
            builderFirst = new StringBuilder();
            builderLast = new StringBuilder();
            builder = new StringBuilder();
        }
        return builderTemp.toString();
    }

    public StringBuilder getBuilderFirst() {
        return builderFirst;
    }

    public void setBuilderFirst(StringBuilder builderFirst) {
        this.builderFirst = builderFirst;
    }

    public StringBuilder getBuilderLast() {
        return builderLast;
    }

    public void setBuilderLast(StringBuilder builderLast) {
        this.builderLast = builderLast;
    }
}
