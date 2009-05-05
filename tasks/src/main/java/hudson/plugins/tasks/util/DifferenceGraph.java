package hudson.plugins.tasks.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Builds a graph with the difference between new and fixed warnings for a
 * specified result action.
 *
 * @author Ulli Hafner
 */
public class DifferenceGraph extends BuildResultGraph {
    /**
     * Creates a PNG image trend graph.
     *
     * @param configuration
     *            the configuration parameters
     * @param resultAction
     *            the result action to start the graph computation from
     * @param pluginName
     *            the name of the plug-in
     * @return the graph
     */
    @Override
    public JFreeChart create(final GraphConfiguration configuration,
            final ResultAction<? extends BuildResult> resultAction, final String pluginName) {
        ArrayList<Pair<Integer, Integer>> fixedWarnings = new ArrayList<Pair<Integer, Integer>>();
        ArrayList<Pair<Integer, Integer>> newWarnings = new ArrayList<Pair<Integer, Integer>>();

        extractPoints(configuration, resultAction, fixedWarnings, newWarnings);
        XYSeriesCollection xySeriesCollection = computeDifferenceSeries(fixedWarnings, newWarnings);

        return createXYChart(xySeriesCollection);
    }

    /**
     * Computes the difference series from the counted warnings.
     *
     * @param fixedWarnings
     *            the fixed warnings
     * @param newWarnings
     *            the new warnings
     * @return the series to plot
     */
    private XYSeriesCollection computeDifferenceSeries(
            final ArrayList<Pair<Integer, Integer>> fixedWarnings,
            final ArrayList<Pair<Integer, Integer>> newWarnings) {
        XYSeries fixedSeries = new XYSeries("fixed");
        XYSeries newSeries = new XYSeries("new");

        int fixedCount = 0;
        int newCount = 0;
        for (int i = 0; i < fixedWarnings.size(); i++) {
            Pair<Integer, Integer> point = fixedWarnings.get(i);
            int build = point.getHead();
            fixedCount += point.getTail();
            point = newWarnings.get(i);
            newCount += point.getTail();

            fixedSeries.add(build, fixedCount);
            newSeries.add(build, newCount);
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries(fixedSeries);
        xySeriesCollection.addSeries(newSeries);
        return xySeriesCollection;
    }

    /**
     * Extracts the points to draw. Iterates through all builds and stores the
     * number of warnings in the corresponding lists.
     *
     * @param configuration
     *            the configuration parameters
     * @param resultAction
     *            the result action to start the graph computation from
     * @param fixedWarnings
     *            list of pairs with the points for the fixed warnings
     * @param newWarnings
     *            list of pairs with the points for the new warnings
     */
    private void extractPoints(final GraphConfiguration configuration, final ResultAction<? extends BuildResult> resultAction,
            final ArrayList<Pair<Integer, Integer>> fixedWarnings, final ArrayList<Pair<Integer, Integer>> newWarnings) {
        ResultAction<? extends BuildResult> action = resultAction;
        int buildCount = 0;
        Calendar buildTime = action.getBuild().getTimestamp();
        while (true) {
            BuildResult current = action.getResult();

            int build = action.getBuild().getNumber();
            fixedWarnings.add(new Pair<Integer, Integer>(build, current.getNumberOfFixedWarnings()));
            newWarnings.add(new Pair<Integer, Integer>(build, current.getNumberOfNewWarnings()));

            if (action.hasPreviousResultAction()) {
                action = action.getPreviousResultAction();
            }
            else {
                break;
            }

            if (configuration.isBuildCountDefined()) {
                buildCount++;
                if (buildCount >= configuration.getBuildCount()) {
                    break;
                }
            }

            if (configuration.isDayCountDefined()) {
                Calendar oldBuildTime = action.getBuild().getTimestamp();
                if (computeDayDelta(buildTime, oldBuildTime) >= configuration.getDayCount()) {
                    break;
                }
            }
        }

        Collections.reverse(fixedWarnings);
        Collections.reverse(newWarnings);
    }
}

