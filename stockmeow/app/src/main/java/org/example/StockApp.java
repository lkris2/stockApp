package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class StockApp extends JFrame {
    private static final String API_KEY = "YN9TWC4F4461M20D"; // Replace with your Alpha Vantage API key
    private static final String SYMBOL = "DIA"; // Use a valid symbol for the Dow Jones Industrial Average ETF
    private static final long INTERVAL = 5000; // 5 seconds
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=";
    private static final String API_KEY_PARAM = "&apikey=";
    private Queue<StockData> stockQueue = new LinkedList<>();
    private JLabel stockPriceLabel;
    private JButton sellButton;
    private TimeSeries series;
    private OkHttpClient client = new OkHttpClient();

    public StockApp(String title) {
        super(title);
        setLayout(new BorderLayout());

        // Create chart
        series = new TimeSeries("Stock Price");
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Stock Price Over Time",
                "Time",
                "Price",
                dataset,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        add(chartPanel, BorderLayout.CENTER);

        // Create label and button
        JPanel controlPanel = new JPanel();
        stockPriceLabel = new JLabel("Current Stock Price: $0.00");
        sellButton = new JButton("Sell Stock");
        sellButton.setEnabled(false); // Initially disabled

        controlPanel.add(stockPriceLabel);
        controlPanel.add(sellButton);
        add(controlPanel, BorderLayout.SOUTH);

        sellButton.addActionListener(e -> JOptionPane.showMessageDialog(null, "Stock Sold!"));

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchAndPredictStockData();
            }
        }, 0, INTERVAL);
    }

    private void fetchAndPredictStockData() {
        try {
            String url = BASE_URL + SYMBOL + API_KEY_PARAM + API_KEY;
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    System.out.println("Response: " + responseBody); // Debug output

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("Time Series (Daily)")) {
                        JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (Daily)");
                        String latestTime = timeSeries.keys().next();
                        JSONObject latestData = timeSeries.getJSONObject(latestTime);
                        BigDecimal price = latestData.getBigDecimal("1. open");
                        LocalDateTime timestamp = LocalDateTime.now(); // Use the current time

                        StockData stockData = new StockData(price, timestamp);
                        stockQueue.add(stockData);
                        updateChart(price, timestamp);
                        SwingUtilities.invokeLater(() -> {
                            stockPriceLabel.setText("Current Stock Price: $" + price);
                            sellButton.setEnabled(shouldSellStock(price));
                        });
                        System.out.println("Stored data: " + price + " at " + timestamp);
                    } else if (jsonResponse.has("Note")) {
                        System.out.println("Note: " + jsonResponse.getString("Note"));
                    } else if (jsonResponse.has("Error Message")) {
                        System.out.println("Error: " + jsonResponse.getString("Error Message"));
                    } else {
                        System.out.println("Unexpected response format.");
                    }
                } else {
                    System.out.println("HTTP GET Request failed with error code: " + response.code());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChart(BigDecimal price, LocalDateTime timestamp) {
        series.addOrUpdate(new Second(timestamp.getSecond(), timestamp.getMinute(), timestamp.getHour(), timestamp.getDayOfMonth(), timestamp.getMonthValue(), timestamp.getYear()), price.doubleValue());
    }

    private boolean shouldSellStock(BigDecimal currentPrice) {
        // Simple dummy logic to sell stock if price is above 50
        return currentPrice.compareTo(new BigDecimal(50)) > 0;
    }

    private static class StockData {
        private BigDecimal price;
        private LocalDateTime timestamp;

        public StockData(BigDecimal price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "StockData{" +
                    "price=" + price +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StockApp app = new StockApp("Stock Market Predictor");
            app.setSize(800, 600);
            app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}
