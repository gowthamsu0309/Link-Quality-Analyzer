package dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


/**
 * CsvDashboard — A JavaFX desktop application for visualizing
 * link quality analysis CSV output files.
 *
 * Supports:
 *   - Cluster CSV files    → Pie Chart + Bar Chart
 *   - Change Point CSV     → Bar Chart
 *   - AR Model CSV         → Residual Variance Histogram + Coefficient Heatmap
 *
 * @author  Your Name
 * @version 1.0
 */
public class CsvDashboard extends Application {

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Central tabbed panel where charts are displayed. */
    private TabPane tabPane;

    /** Reference to the main window (needed by FileChooser). */
    private Stage primaryStage;


    // ── JavaFX Entry Point ────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Link Quality CSV Dashboard");

        // 1. Create the top Menu Bar
        MenuBar menuBar = createMenuBar();

        // 2. Create the TabPane for displaying charts
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS); // Fill vertical space

        // 3. Assemble the root layout
        VBox root = new VBox(menuBar, tabPane);
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        // 4. Show the application window
        Scene scene = new Scene(root, 800, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    // ── Menu Bar ──────────────────────────────────────────────────────────────

    /**
     * Builds the application menu bar with three file-open options.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        // Open Cluster CSV
        MenuItem openCluster = new MenuItem("Open Cluster File...");
        openCluster.setOnAction(e -> {
            File f = loadFile("Open Cluster CSV",
                    new FileChooser.ExtensionFilter("Cluster CSVs", "clusters_*.csv"));
            if (f != null) processClusterFile(f);
        });

        // Open Change Point CSV
        MenuItem openCp = new MenuItem("Open Change Point (CP) File...");
        openCp.setOnAction(e -> {
            File f = loadFile("Open Change Point CSV",
                    new FileChooser.ExtensionFilter("CP CSVs", "cp_*.csv"));
            if (f != null) processChangePointFile(f);
        });

        // Open AR Model CSV
        MenuItem openAr = new MenuItem("Open AR Model File...");
        openAr.setOnAction(e -> {
            File f = loadFile("Open AR Model CSV",
                    new FileChooser.ExtensionFilter("AR CSVs", "ar_*.csv"));
            if (f != null) processArFile(f);
        });

        fileMenu.getItems().addAll(openCluster, openCp, openAr);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    /**
     * Opens the native OS file chooser dialog.
     *
     * @param title  Dialog window title
     * @param filter Extension filter for the file picker
     * @return Selected File, or null if user cancelled
     */
    private File loadFile(String title, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setInitialDirectory(new File("../LinkAnalyzerProject/output"));
        return fileChooser.showOpenDialog(primaryStage);
    }


    // ── Cluster File Processing ───────────────────────────────────────────────

    /**
     * Loads and visualizes a cluster CSV file as a Pie Chart and Bar Chart.
     */
    private void processClusterFile(File file) {
        try {
            Map<String, Integer> counts = parseClusterCsv(file);
            PieChart pie = createPieChart(counts);
            pie.setTitle("Cluster Distribution: " + file.getName());
            BarChart<String, Number> bar = createBarChart(counts);
            bar.setTitle("Cluster Counts: " + file.getName());

            tabPane.getTabs().clear();
            tabPane.getTabs().add(new Tab("Pie Chart", pie));
            tabPane.getTabs().add(new Tab("Bar Chart", bar));
        } catch (Exception ex) {
            handleError(ex);
        }
    }

    /**
     * Parses a cluster CSV file.
     * Expected format: series_index, cluster_label
     *
     * @return Map of cluster label → count
     */
    private Map<String, Integer> parseClusterCsv(File file) throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String label = parts[1].trim();
                    counts.put(label, counts.getOrDefault(label, 0) + 1);
                }
            }
        }
        return counts;
    }

    /**
     * Creates a PieChart from cluster count data.
     */
    private PieChart createPieChart(Map<String, Integer> data) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        data.forEach((key, value) -> pieData.add(new PieChart.Data("Cluster " + key, value)));
        return new PieChart(pieData);
    }

    /**
     * Creates a BarChart from cluster count data.
     */
    private BarChart<String, Number> createBarChart(Map<String, Integer> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Cluster");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Series");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((key, value) ->
                series.getData().add(new XYChart.Data<>("Cluster " + key, value)));
        chart.getData().add(series);
        return chart;
    }


    // ── Change Point File Processing ──────────────────────────────────────────

    /**
     * Loads and visualizes a change point CSV file as a Bar Chart.
     */
    private void processChangePointFile(File file) {
        try {
            Map<Integer, Integer> cpCounts = parseChangePointCsv(file);
            BarChart<String, Number> bar = createChangePointBarChart(cpCounts);
            bar.setTitle("Change Points per Series: " + file.getName());

            tabPane.getTabs().clear();
            tabPane.getTabs().add(new Tab("Change Point Counts", bar));
        } catch (Exception ex) {
            handleError(ex);
        }
    }

    /**
     * Parses a change point CSV file.
     * Expected format: series_index, change_points (semicolon-separated)
     *
     * @return TreeMap of series index → number of change points (sorted)
     */
    private Map<Integer, Integer> parseChangePointCsv(File file) throws Exception {
        Map<Integer, Integer> counts = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int seriesIndex = Integer.parseInt(parts[0].trim());
                int count = 0;
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    count = parts[1].split(";").length;
                }
                counts.put(seriesIndex, count);
            }
        }
        return counts;
    }

    /**
     * Creates a BarChart from change point count data.
     */
    private BarChart<String, Number> createChangePointBarChart(Map<Integer, Integer> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Series Index");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Change Points");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((key, value) ->
                series.getData().add(new XYChart.Data<>("Series " + key, value)));
        chart.getData().add(series);
        return chart;
    }


    // ── AR Model File Processing ──────────────────────────────────────────────

    /**
     * Internal data class to hold one AR model result row.
     */
    private static class ARResult {
        int seriesIndex;
        double residVar;
        double[] coeffs;
    }

    /**
     * Loads and visualizes an AR model CSV file as a histogram and heatmap.
     */
    private void processArFile(File file) {
        try {
            List<ARResult> arData = parseArCsv(file);

            BarChart<String, Number> histogram = createResidVarHistogram(arData);
            histogram.setTitle("Residual Variance Distribution: " + file.getName());

            Node heatmap = createCoefficientHeatMap(arData);

            tabPane.getTabs().clear();
            tabPane.getTabs().add(new Tab("Residual Variance", histogram));
            tabPane.getTabs().add(new Tab("Coefficient Heatmap", heatmap));
        } catch (Exception ex) {
            handleError(ex);
        }
    }

    /**
     * Parses an AR model CSV file.
     * Expected format: series_index, residual_variance, coefficients (semicolon-separated)
     *
     * @return List of ARResult objects
     */
    private List<ARResult> parseArCsv(File file) throws Exception {
        List<ARResult> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                ARResult res = new ARResult();
                res.seriesIndex = Integer.parseInt(parts[0].trim());
                res.residVar    = Double.parseDouble(parts[1].trim());

                String[] coeffStrings = parts[2].split(";");
                res.coeffs = new double[coeffStrings.length];
                for (int i = 0; i < coeffStrings.length; i++) {
                    res.coeffs[i] = Double.parseDouble(coeffStrings[i].trim());
                }
                results.add(res);
            }
        }
        return results;
    }

    /**
     * Creates a 10-bin residual variance histogram.
     */
    private BarChart<String, Number> createResidVarHistogram(List<ARResult> data) {
        double maxVar  = data.stream().mapToDouble(r -> r.residVar).max().orElse(1.0);
        int    numBins = 10;
        int[]  bins    = new int[numBins];

        for (ARResult res : data) {
            int bin = (int) Math.floor((res.residVar / maxVar) * (numBins - 1));
            bins[bin]++;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Residual Variance Bins");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < numBins; i++) {
            String label = String.format("%.2f-%.2f",
                    (i * maxVar / numBins), ((i + 1) * maxVar / numBins));
            series.getData().add(new XYChart.Data<>(label, bins[i]));
        }
        chart.getData().add(series);
        return chart;
    }

    /**
     * Creates a color-coded heatmap of AR model coefficients.
     * Color scale: Blue (negative) → White (zero) → Red (positive)
     */
    private Node createCoefficientHeatMap(List<ARResult> data) {
        if (data.isEmpty()) return new Label("No data to display.");

        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(10));

        int numCoeffs = data.get(0).coeffs.length;

        // Find max absolute value for symmetric color scaling
        double maxAbs = 0;
        for (ARResult res : data) {
            for (double c : res.coeffs) {
                maxAbs = Math.max(maxAbs, Math.abs(c));
            }
        }

        // Column headers
        for (int j = 0; j < numCoeffs; j++) {
            grid.add(new Label("Coeff " + (j + 1)), j + 1, 0);
        }

        // Data rows
        for (int i = 0; i < data.size(); i++) {
            ARResult res = data.get(i);
            grid.add(new Label("Series " + res.seriesIndex), 0, i + 1);

            for (int j = 0; j < res.coeffs.length; j++) {
                double    val   = res.coeffs[j];
                Color     color = getColorForValue(val, -maxAbs, maxAbs);
                Rectangle rect  = new Rectangle(50, 20, color);
                rect.setStroke(Color.BLACK);
                Tooltip.install(rect, new Tooltip(String.format("%.4f", val)));
                grid.add(rect, j + 1, i + 1);
            }
        }

        return new ScrollPane(grid);
    }


    // ── Color Utility ─────────────────────────────────────────────────────────

    /**
     * Maps a numeric value to a color on a Blue–White–Red scale.
     *
     * @param value The data value to colorize
     * @param min   Minimum bound (should be negative for symmetric scaling)
     * @param max   Maximum bound
     * @return Interpolated JavaFX Color
     */
    private Color getColorForValue(double value, double min, double max) {
        if (value > max) value = max;
        if (value < min) value = min;

        if (value < 0) {
            double perc = value / min; // 0.0 (near zero) → 1.0 (most negative)
            return Color.WHITE.interpolate(Color.BLUE, perc);
        } else {
            double perc = value / max; // 0.0 (near zero) → 1.0 (most positive)
            return Color.WHITE.interpolate(Color.RED, perc);
        }
    }


    // ── Error Handling ────────────────────────────────────────────────────────

    /**
     * Handles exceptions from file parsing.
     * TODO: Replace with a JavaFX Alert dialog for user-facing errors.
     */
    private void handleError(Exception ex) {
        System.err.println("Failed to process file: " + ex.getMessage());
        ex.printStackTrace();
    }


    // ── Application Entry Point ───────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
