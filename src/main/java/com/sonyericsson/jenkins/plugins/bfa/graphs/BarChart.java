/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications AB. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.List;

import hudson.model.Job;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.db.KnowledgeBase;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;

/**
 * Bar chart displaying the number of different failure causes for a project.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class BarChart extends BFAGraph {
    private boolean byCategories;
    /**
     * Maximum bar size (quota of available space in graph).
     */
    private static final double MAX_BAR_WIDTH = 0.15;

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph
     * @param filter the filter used when fetching data for this graph
     * @param graphTitle The title of the graph
     * @param byCategories True to display categories, or false to display failure causes
     */
    public BarChart(long timestamp, int defaultW, int defaultH,
            Job project, GraphFilterBuilder filter,
            String graphTitle, boolean byCategories) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
        this.byCategories = byCategories;
    }

    @Override
    protected JFreeChart createGraph() {
        CategoryDataset dataset = createDataset();
        JFreeChart chart = ChartFactory.createBarChart(graphTitle, "", "Number of failures", dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        NumberAxis domainAxis = new NumberAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setRangeAxis(domainAxis);

        BarRenderer renderer = (BarRenderer)plot.getRenderer();
        renderer.setMaximumBarWidth(MAX_BAR_WIDTH);

        return chart;
    }

    /**
     * Creates the dataset needed for this graph.
     * @return dataset
     */
    private CategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        KnowledgeBase knowledgeBase = PluginImpl.getInstance().getKnowledgeBase();
        List<ObjectCountPair<String>> failureItems;
        long nullFailureItems = 0;
        String nullFailuteItemsName;
        if (byCategories) {
            failureItems = knowledgeBase.getNbrOfFailureCategoriesPerName(filter, -1);
            nullFailuteItemsName = GRAPH_UNCATEGORIZED;
        } else {
            failureItems = knowledgeBase.getFailureCauseNames(filter);
            nullFailureItems = knowledgeBase.getNbrOfNullFailureCauses(filter);
            nullFailuteItemsName = GRAPH_UNKNOWN;
        }
        if (failureItems != null) {
            int othersCount = 0;
            for (int i = 0; i < failureItems.size(); i++) {
                ObjectCountPair<String> countPair = failureItems.get(i);
                if (countPair.getObject() == null) {
                    nullFailureItems += countPair.getCount();
                } else if (i < MAX_GRAPH_ELEMENTS) {
                    dataset.setValue(countPair.getCount(), "", countPair.getObject());
                } else {
                    othersCount += countPair.getCount();
                }
            }
            if (othersCount > 0) {
                dataset.setValue(othersCount, "", GRAPH_OTHERS);
            }

            if (nullFailureItems > 0) {
                dataset.addValue(nullFailureItems, "", nullFailuteItemsName);
            }
        }
        return dataset;
    }

}
